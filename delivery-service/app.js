// Copyright 2018 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

'use strict';

require('@google-cloud/debug-agent').start({ serviceContext: { enableCanary: false }, allowExpressions: true });
const {PubSub} = require('@google-cloud/pubsub');
const {SecretManagerServiceClient} = require('@google-cloud/secret-manager');
const mysql = require('promise-mysql');
const express = require('express');
const cors = require('cors');
const path = require('path');
const app = express();
const server = require('http').Server(app);
const io = require('socket.io')(server, {
  cors: {
    origin: '*',
  }
});

let socketsMap = {};          // socketId to sockets
let socketIdToUserMap = {};   // sockerId to userId
let userToSocketIdsMap = {};  // userId socketIdList
let userInfoMap = {};         // holds user preferences

const GCLOUD_PROJECT = process.env.GOOGLE_CLOUD_PROJECT;
const textMessageSubscription = `projects/${GCLOUD_PROJECT}/${process.env.TEXT_MESSAGE_SUBSCRIPTION}`;
const audioMessageSubscription = `projects/${GCLOUD_PROJECT}/${process.env.AUDIO_MESSAGE_SUBSCRIPTION}`;
const pubSubClientForText = new PubSub();
const pubSubClientForAudio = new PubSub();

const dbUserSecretKey = `projects/${GCLOUD_PROJECT}/${process.env.DB_USER_SECRET}`;
const dbPasswordSecretKey = `projects/${GCLOUD_PROJECT}/${process.env.DB_PASSWORD_SECRET}`;
const dbNameSecretKey = `projects/${GCLOUD_PROJECT}/${process.env.DB_NAME_SECRET}`;
const dbHostSecretKey = `projects/${GCLOUD_PROJECT}/${process.env.DB_HOST_SECRET}`;
const client = new SecretManagerServiceClient();

let dbUser;
let dbPassword;
let dbServer;
let hostIp;
let hostPort;

async function loadDbAccessCredentials() {
  const [user] = await client.accessSecretVersion({ name: dbUserSecretKey });
  const [password] = await client.accessSecretVersion({ name: dbPasswordSecretKey });
  const [database] = await client.accessSecretVersion({ name: dbNameSecretKey });
  const [hostAndPort] = await client.accessSecretVersion({ name: dbHostSecretKey });

  dbUser = user.payload.data.toString();
  dbPassword = password.payload.data.toString();
  dbServer = database.payload.data.toString();

  const hostPortPair = hostAndPort.payload.data.toString().split(':');
  hostIp = hostPortPair[0];
  hostPort = hostPortPair[1];
}

app.use(cors({
  origin: 'https://www.googlingual.com'
}));
app.use("/assets", express.static(path.join(__dirname, 'assets')));
app.set('view engine', 'pug');
app.get('/', (req, res) => {
  res.render('index.pug');
});

io.on('connection', socket => {
  socketsMap[socket.id] = socket;
  socket.on('newSocketConnection', data => {
    registerNewUser(data);
    const socketId = data.sId;
    const userId = data.uId;
    if (!socketsMap[socketId]) {
      return;
    }
    const existingConnections = userToSocketIdsMap[userId] || [];
    userToSocketIdsMap[userId] = [ ...existingConnections, socketId];
    socketIdToUserMap[socketId] = userId;
    updateUserInfomation(data);
    io.emit('userRegistered', data);
  });

  socket.on('updateUserPref', (data) => {
    console.log(`Updating user preferences on [${socket.id}] for user ${data.uId}`);
    updateUserInfomation(data);
  });

  socket.on('disconnect', () => {
    console.log(`Socket ${socket.id} disconnected...`);
    cleanUpSocketReferences(socket);
  });
});

function updateUserInfomation(data) {
  registerNewUser(data, true);
  const userId = data.uId;
  userInfoMap[userId] = {
    textLocale: data.textLocale,
    audioLocale: data.audioLocale,
  }
}

function cleanUpSocketReferences(socket) {
  const userId = socketIdToUserMap[socket.id];
  unregisterUser(userId, socket.id);
  const socketList = userToSocketIdsMap[userId] || [];
  const socketIndex = socketList.indexOf(socket.id);
  if (socketIndex > -1) {
    socketList.splice(socketIndex, 1);                          // remove socket for user's list
  }
  if (socketList.length == 0) {
    const { [userId]: sockList, ...rest0 } = userToSocketIdsMap;
    const { [userId]: prefs, ...rest1 } = userInfoMap;
    userToSocketIdsMap = rest0;
    userInfoMap = rest1;                                 // remove user since has 0 connections
  }
  const { [socket.id]: currId1, ...rest2 } = socketsMap;        // delete reference to socket info
  const { [socket.id]: currId2, ...rest3 } = socketIdToUserMap; // delete socket to use binding
  socketsMap = rest2;
  socketIdToUserMap = rest3;
}

const createPool = async () => {
  return await mysql.createPool({
    user: dbUser,
    password: dbPassword,
    database: dbServer,
    host: hostIp,
    port: hostPort,
    connectionLimit: 5,
    connectTimeout: 10000,                          // 10 seconds
    acquireTimeout: 10000,                          // 10 seconds
    waitForConnections: true,                       // Default: true
    queueLimit: 0,                                  // Default: 0
  });
};

const createPoolAndEnsureSchema = async () => await createPool();
let allUsers;
let dbPool;

async function getUsers(chatRoom) {
  if (allUsers && allUsers.length > 0) {
    return allUsers;
  }
  try {
    allUsers = await fetchConnectedUsers(chatRoom);
  } catch (err) {
    console.log(err);
    allUsers = [];
  }
  return allUsers;
}

const INSERT_USER_SQL = `INSERT INTO roomusers_v2
   (id, chatroom_id, message_locale, audio_locale, user_id, name, socket_id)
   VALUES (UUID_TO_BIN(UUID()), UUID_TO_BIN(?), ?, ?, ?, ?, ?);`;

const UPDATE_USER_SQL = `UPDATE roomusers_v2
   SET message_locale = ?, audio_locale = ?, socket_id = ?, disconnected = 0
   WHERE chatroom_id = UUID_TO_BIN(?)
   AND user_id = ?;`;

const DISCONNECT_USER_SQL = `UPDATE roomusers_v2
   SET disconnected = 1
   WHERE socket_id = ?
   AND user_id = ?;`;

async function registerNewUser(data, updateOnly = false) {
  dbPool = dbPool || (await createPoolAndEnsureSchema());
  try {
    let userUpdateQuery = await dbPool.query(
      UPDATE_USER_SQL, [
      data.textLocale,
      data.audioLocale,
      data.sId,
      data.chatRoomId,
      data.uId
    ]);
    if (userUpdateQuery && userUpdateQuery.affectedRows === 1) {
      console.log(`Update user query: ${JSON.stringify(userUpdateQuery)}`);
      return;
    }
    if (updateOnly) {
      return;
    }
    userUpdateQuery = await dbPool.query(
      INSERT_USER_SQL, [
      data.chatRoomId,
      data.textLocale,
      data.audioLocale,
      data.uId,
      data.displayName,
      data.sId
    ]);
    console.log(`Add new user query: ${JSON.stringify(userUpdateQuery)}`);
  } catch (err) {
    console.log(`Update user query ERROR: ${JSON.stringify(err)}`);
  }
}

async function unregisterUser(userId, socketId) {
  dbPool = dbPool || (await createPoolAndEnsureSchema());
  try {
    const disconnectQuery = await dbPool.query(DISCONNECT_USER_SQL, [socketId, userId]);
    console.log(`Disconnect user query result: ${JSON.stringify(disconnectQuery)}`);
  } catch (err) {
    console.log(`Disconnect user query ERROR: ${JSON.stringify(err)}`);
  }
}

async function fetchConnectedUsers(chatRoom) {
  dbPool = dbPool || (await createPoolAndEnsureSchema());
  try {
    const stmt = `SELECT
       user_id,
       message_locale,
       audio_locale,
       socket_id,
       name
      FROM roomusers_v2
      WHERE chatroom_id = UUID_TO_BIN(?)
      AND disconnected = 0;`;
    const roomUsersQuery = dbPool.query(stmt, [chatRoom]);
    return await roomUsersQuery;
  } catch (err) {
    console.log(err);
    return [];
  }
}

async function handlePubSubMessage(message) {
  const payload = JSON.parse(message.data);
  console.log(`Received message ${message.id} with attributes: `, message.attributes);
  message.ack();
  console.log(JSON.stringify(payload));

  const chatMessage = payload.message;
  const chatRoom = chatMessage.chatRoomId;
  const roomUsers = await getUsers(chatRoom);
  console.log(JSON.stringify(roomUsers));
  roomUsers.forEach(user => {
    const uId = user.user_id;
    const socketId = user.socket_id;
    const isAudio = !!chatMessage.audioMessage;
    if ((isAudio && userInfoMap[uId].audioLocale === chatMessage.audioLocale) ||
      (!isAudio && userInfoMap[uId].textLocale === chatMessage.messageLocale)) {
      if (socketsMap[socketId]) {
        console.log(`<SUCCESS> Publishing message for user [${uId}] over socket [${socketId}]`);
        socketsMap[socketId].emit('chatRoomMessage', chatMessage);
      } else {
        console.log(`<WARN> Socket not found for user [${uId}] with socket id [${socketId}]`);
      }
    } else {
      console.log(`<WARN> Delivery criteria not met for [${uId}] with socket id [${socketId}]`);
    }
  });
}

function listenForMessages() {
  console.log("Registering subscriber....");
  const textSubscription = pubSubClientForText.subscription(textMessageSubscription);
  const audioSubscription = pubSubClientForAudio.subscription(audioMessageSubscription);
  textSubscription.on('message', handlePubSubMessage);
  audioSubscription.on('message', handlePubSubMessage);
}

if (module === require.main) {
  console.log("Starting node app....");
  loadDbAccessCredentials();
  listenForMessages();

  const PORT = process.env.PORT || 8081;
  server.listen(PORT, () => {
    console.log(`App listening on port ${PORT}`);
    console.log('Press Ctrl+C to quit.');
  });
}

module.exports = server;
