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

const socketsMap = {};
const socketIdToUserMap = {};
const userToSocketIdMap = {};

app.set('view engine', 'pug');
app.use("/assets", express.static(path.join(__dirname, 'assets')));

app.get('/', (req, res) => {
  res.render('index.pug');
});

io.on('connection', socket => {
  socketsMap[socket.id] = socket;
  socket.on('newSocketConnection', data => {
    console.log(data);
    const connectionId = data.sId;
    const userId = data.uId;
    if (!socketsMap[connectionId]) {
      return;
    }
    const existingConnections = userToSocketIdMap[userId] || [];
    userToSocketIdMap[userId] = [ ...existingConnections, connectionId];
    socketIdToUserMap[connectionId] = userId;
    io.emit('userRegistered', data);
  });
});

io.on('disconnect', socket => {
  delete socketsMap[socket.id];
  const socketUser = socketIdToUserMap[socket.id];
  const userSockets = socketIdToUserMap[socketUser];
  const socketIndex = userSockets.indexOf(socket.id);
  if (socketIndex > -1) {
    userSockets.splice(socketIndex, 1);
  }
});

const subscriptionName = 'projects/gcloud-dpe/subscriptions/delivery-service-subscription';
const timeout = 60;
const pubSubClient = new PubSub();

function listenForMessages() {
  const subscription = pubSubClient.subscription(subscriptionName);

  let messageCount = 0;
  const messageHandler = message => {
    console.log(`Received message ${message.id}:`);
    console.log(`\tData: ${message.data}`);
    console.log(`\tAttributes: ${message.attributes}`);
    messageCount += 1;
    message.ack();
  };

  subscription.on('message', messageHandler);
  setTimeout(() => {
    subscription.removeListener('message', messageHandler);
    console.log(`${messageCount} message(s) received.`);
  }, timeout * 1000);
}

if (module === require.main) {
  const PORT = process.env.PORT || 8081;
  server.listen(PORT, () => {
    console.log(`App listening on port ${PORT}`);
    console.log('Press Ctrl+C to quit.');
  });
  listenForMessages();
}

module.exports = server;
