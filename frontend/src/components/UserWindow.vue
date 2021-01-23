<template>
  <div :class="windowTheme">
    <ChatWindow :chatMessages="chatMessages" :userId="user.id"></ChatWindow>
    <InputArea @addMessage="addMessage" @addAudioMessage="addAudioMessage"></InputArea>
  </div>
</template>

<script>
import socketio from 'socket.io-client';
import ChatWindow from "./ChatWindow.vue";
import InputArea from "./InputArea.vue";

// const SOCKETS_API = (process.env.SOCKET_SERVER_URL) ?
//   process.env.SOCKET_SERVER_URL : 'http://localhost:8081';
const SOCKETS_API = 'https://googlingual-delivery-dot-gcloud-dpe.ue.r.appspot.com';

export default {
  name: "UserWindow",
  components: {
    ChatWindow,
    InputArea,
  },
  props: {
    windowTheme: {
      type: String,
      required: true,
    },
    roomId: {
      type: String,
      required: true,
    },
    chatMessages: {
      type: Array,
      required: true,
    },
    user: {
      type: Object,
      required: true,
    },
    avatar: {
      type: String,
      required: true,
    },
  },
  data: () => {
    return {
      socket: socketio(SOCKETS_API),
    };
  },
  created() {
    this.socket.on('connect', this.connect);
    this.socket.on('disconnect', this.disconnect);
    this.socket.on('userRegistered', this.userRegistered);
    this.socket.on('chatRoomMessage', this.chatRoomMessage);
  },
  methods: {
    connect() {
      console.log(`Connected to sockets server with id: ${this.socket.id}`);
      this.socket.emit('newSocketConnection', {
        sId: this.socket.id,
        uId: this.user.id,
        textLocale: this.user.textLocale,
        audioLocale:this.user.audioLocale,
      });
    },
    disconnect() {
      console.log(`Client ${this.socket.id} disconnected from sockets server`);
    },
    userRegistered(data) {
      console.log(`User registration message:`, data);
    },
    chatRoomMessage(payload) {
      console.log(payload);
      if (payload.sender === this.user.id) {
        return;
      }
      this.$emit('appendMessage', {
        recipient: this.user,
        ...payload,
      });
    },
    addMessage(newMessage) {
      this.$emit("sendMessage", {
        textMessage: newMessage,
        author: this.user,
        roomId: this.roomId,
      });
    },
    addAudioMessage(audioMessage) {
      this.$emit("sendAudioMessage", {
        audioMessage: 'Audio recording...',
        author: this.user,
        roomId: this.roomId,
      }, audioMessage);
    },
  },
};
</script>

<style scoped>
.left-container {
  flex: 50%;
  background: #8db78e66;
}

.right-container {
  flex: 50%;
  background: #b78d9c66;
}
</style>
