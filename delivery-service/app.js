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

const {PubSub} = require('@google-cloud/pubsub');
const mysql = require('promise-mysql');
const express = require('express');
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
const textMessageSubscription = 'projects/gcloud-dpe/subscriptions/socket-service-subscription';
const audioMessageSubscription = 'projects/gcloud-dpe/subscriptions/socket-service-subscription-for-audio';
const pubSubClientForText = new PubSub();
const pubSubClientForAudio = new PubSub();

app.set('view engine', 'pug');
app.use("/assets", express.static(path.join(__dirname, 'assets')));
app.get('/', (req, res) => {
  res.render('index.pug');
});

io.on('connection', socket => {
  socketsMap[socket.id] = socket;
  socket.on('newSocketConnection', data => {
    console.log(data);
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
  const userId = data.uId;
  userInfoMap[userId] = {
    textLocale: data.textLocale,
    audioLocale: data.audioLocale,
  }
}

function cleanUpSocketReferences(socket) {
  const userId = socketIdToUserMap[socket.id];
  const socketList = userToSocketIdsMap[userId] || [];
  const socketIndex = socketList.indexOf(socket.id);
  if (socketIndex > -1) {
    socketList.splice(socketIndex, 1);                          // remove socket for user's list
  }
  if (userToSocketIdsMap[userId].length == 0) {
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
    user: 'root',                                   // process.env.DB_USER, // e.g. 'my-db-user'
    password: '7o0fafvczzmFl8Lg',                   //process.env.DB_PASS, // e.g. 'my-db-password'
    database: 'googlingual',                        // process.env.DB_NAME, // e.g. 'my-database'
    host: '10.114.49.3',                            // dbSocketAddr[0], // e.g. '34.71.243.72'
    port: '3306',                                   // e.g. '3306'
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

async function fetchConnectedUsers(chatRoom) {
  dbPool = dbPool || (await createPoolAndEnsureSchema());
  try {
    const stmt = `SELECT
       BIN_TO_UUID(user_id) user_id,
       message_locale,
       audio_locale
      FROM roomusers
      WHERE chatroom_id = UUID_TO_BIN(?);`;
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
  // console.log(payload);

  const chatMessage = payload.message;
  const chatRoom = chatMessage.chatRoomId;
  const roomUsers = await getUsers(chatRoom);
  roomUsers.forEach(user => {
    const uId = user.user_id;
    const socketIdList = userToSocketIdsMap[uId];
    if (!socketIdList || !userInfoMap[uId]) {
      return;
    }

    const isAudio = !!chatMessage.audioMessage;
    if ((isAudio && userInfoMap[uId].audioLocale === chatMessage.audioLocale) ||
        (!isAudio && userInfoMap[uId].textLocale === chatMessage.messageLocale)) {
      socketIdList.forEach(sockId => {
        if (socketsMap[sockId]) {
          // console.log(`Socket found for userId: ${uId} --> ${sockId}`);
          socketsMap[sockId].emit('chatRoomMessage', chatMessage);
        }
      });
    }
  });
  message.ack();
}

function listenForMessages(socketsMap) {
  console.log("Registering subscriber....");
  const textSubscription = pubSubClientForText.subscription(textMessageSubscription);
  const audioSubscription = pubSubClientForAudio.subscription(audioMessageSubscription);
  textSubscription.on('message', handlePubSubMessage);
  audioSubscription.on('message', handlePubSubMessage);
}

if (module === require.main) {
  console.log("Starting node app....");
  listenForMessages(socketsMap);

  const PORT = process.env.PORT || 8081;
  server.listen(PORT, () => {
    console.log(`App listening on port ${PORT}`);
    console.log('Press Ctrl+C to quit.');
  });
}

module.exports = server;
