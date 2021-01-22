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
});

const subscriptionName = 'projects/gcloud-dpe/subscriptions/delivery-service-subscription';
const timeout = 60;
const pubSubClient = new PubSub();

function listenForMessages(socketsMap) {
  console.log("Registering subscriber....");
  const subscription = pubSubClient.subscription(subscriptionName);

  const messageHandler = message => {
    console.log(`Received message ${message.id}:`);
    console.log(`\tData: ${message.data}`);
    console.log(`\tAttributes: ${message.attributes}`);
    Object.keys(socketsMap).forEach(function(key) {
      console.log(`Found sockId: ${key}`);
    });
    if (socketsMap[message.data]) {
      console.log(`Socket found for sId: ${message.data}`);
      socketsMap[message.data].emit('chatRoomMessage', message.data);
    } else {
      console.log(`No socket found for message ${message.data}`);
    }
    message.ack();
  };
  subscription.on('message', messageHandler);
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
