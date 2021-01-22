<template>
  <div :class="windowTheme">
    <ChatWindow :chatMessages="chatMessages" :owner="owner"></ChatWindow>
    <InputArea @addMessage="addMessage" @addAudioMessage="addAudioMessage"></InputArea>
  </div>
</template>

<script>
import ChatWindow from "./ChatWindow.vue";
import InputArea from "./InputArea.vue";

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
    chatMessages: {
      type: Array,
      required: true,
    },
    owner: {
      type: String,
      required: true,
    },
    avatar: {
      type: String,
      required: true,
    },
  },
  data: () => {
    return {};
  },
  computed: {},
  sockets: {
    connect() {
      console.log(`Connected to sockets server with id: ${this.$socket.id}`);
      this.$socket.emit('newSocketConnection', {
        sId: this.$socket.id,
        uId: this.owner
      });
    },
    disconnect() {
      console.log(`Client ${this.$socket.id} disconnected from sockets server`);
    },
    userRegistered(data) {
      console.log(`User registration message:`, data);
    },
    chatRoomMessage(message) {
      console.log(`Received socket message:`, message);
    }
  },
  methods: {
    addMessage(newMessage) {
      this.$emit("addMessage", {
        body: newMessage,
        author: this.owner,
        avatar: this.avatar,
      });
    },
    addAudioMessage(audioMessage) {
      this.$emit("addAudioMessage", {
        body: 'Audio recording...',
        author: this.owner,
        avatar: this.avatar,
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
