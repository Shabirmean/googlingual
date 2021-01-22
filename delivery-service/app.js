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

const socketsMap = {};          // socketId to sockets
const socketIdToUserMap = {};   // sockerId to userId
const userToSocketIdsMap = {};  // userId socketIdList
const subscriptionName = 'projects/gcloud-dpe/subscriptions/socket-service-subscription';
const pubSubClient = new PubSub();

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
    io.emit('userRegistered', data);
  });
});

io.on('disconnect', socket => {
  cleanUpSocketReferences(socket);
});

function cleanUpSocketReferences(socket) {
  delete socketsMap[socket.id];         // delete reference to socket info
  delete socketIdToUserMap[socket.id];  // delete socket to use binding
  const userId = socketIdToUserMap[socket.id];
  const socketList = userToSocketIdsMap[userId];
  const socketIndex = socketList.indexOf(socket.id);
  if (socketIndex > -1) {
    socketList.splice(socketIndex, 1);  // remove socket for user's list
  }
  if (userToSocketIdsMap[userId].length == 0) {
    delete userToSocketIdsMap[userId];   // remove user since has 0 connections
  }
}

const createPool = async () => {
  const config = {
    connectionLimit: 5,
    connectTimeout: 10000, // 10 seconds
    acquireTimeout: 10000, // 10 seconds
    waitForConnections: true, // Default: true
    queueLimit: 0, // Default: 0
  };
  return await createTcpPool(config);
};

const createPoolAndEnsureSchema = async () => await createPool();
let pool;

function listenForMessages(socketsMap) {
  console.log("Registering subscriber....");
  const subscription = pubSubClient.subscription(subscriptionName);
  subscription.on('message', handlePubSubMessage);
}

async function handlePubSubMessage(message) {
  const payload = JSON.parse(message.data);
  console.log(`Received message ${message.id}:\n
    \tData: ${payload}\n
    \tAttributes: ${message.attributes}`
  );
  if (socketsMap[message.data]) {
    console.log(`Socket found for sId: ${payload.message.id}`);
    socketsMap[message.data].emit('chatRoomMessage', payload);
  } else {
    console.log(`No socket found for message ${payload.message.id}`);
  }
  message.ack();



  const chatRoom = payload.message.chatRoomId;
  pool = pool || (await createPoolAndEnsureSchema());
  try {
    const stmt = `SELECT
       BIN_TO_UUID(user_id) user_id,
       message_locale,
       audio_locale
      FROM roomusers
      WHERE chatroom_id = UUID_TO_BIN(?);`;
    const roomUsersQuery = pool.query(stmt, [chatRoom]);
    const roomUsers = await roomUsersQuery;
    console.log('Response from mysql');
    roomUsers.array.forEach(res => {
      console.log(`User: ${res.user_id}`);
    });
  } catch (err) {
    logger.error(err);
  }
}

const createTcpPool = async config => {
  return await mysql.createPool({
    user: 'root',                 // process.env.DB_USER, // e.g. 'my-db-user'
    password: '7o0fafvczzmFl8Lg', //process.env.DB_PASS, // e.g. 'my-db-password'
    database: 'googlingual',      // process.env.DB_NAME, // e.g. 'my-database'
    host: '10.114.49.3',          // dbSocketAddr[0], // e.g. '127.0.0.1'
    port: '3306',                 // e.g. '3306'
    ...config,
  });
};


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
