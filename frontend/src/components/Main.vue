<template>
  <div class="chat-container">
    <UserWindow
      :windowTheme="'left-container'"
      :chatMessages="chatMessagesForUser"
      :user="currentUser"
      roomId="cb3bf2c8-56dd-11eb-8833-42010a723002"
      @updateUserPref="updateUserPref"
      @sendMessage="sendMessage"
      @sendAudioMessage="sendAudioMessage"
      @appendMessage="appendMessage"
      @signOut="signOut"
    ></UserWindow>
  </div>
</template>

<script>
import GooglingualApi from "@/services/api/Googlingual";
import UserWindow from "./UserWindow.vue";

export default {
  name: "Main",
  components: {
    UserWindow,
  },
  props: {
    user: {
      type: Object,
      required: true,
    }
  },
  data: () => {
    return {
      userMessages: [{
        id: 1,
        author: {
          id: 'googlingual-bot',
          displayName: 'googlingual-bot',
          avatar: "cbo_reallyhappy.png",
        },
        textMessage: "Welcome to Googlingual chat!",
        textLocale: 'en',
        audioLocale: 'en-IN',
        textOriginal: "Welcome to Googlingual chat!",
        audioMessage: null,
      }],
      pingTimer: 4,
      pingChron: null,
    };
  },
  computed: {
    chatMessagesForUser() {
      return this.userMessages;
    },
    currentUser() {
      return this.user;
    },
  },
  created() {
    // this.sendPing();
    // this.checkAndPing();
    // this.pingChron = setInterval(() => { this.pingTimer -= 1; }, 1000);
  },
  methods: {
    getLastIndex() {
      return this.userMessages[this.userMessages.length - 1].id;
    },
    checkAndPing() {
      if (this.pingTimer <= 0) {
        // this.sendPing();
        this.pingTimer = 4;
      }
      setTimeout(this.checkAndPing, 4000);
    },
    sendPing() {
      GooglingualApi.send({
        roomId: 'cb3bf539-56dd-11eb-8833-42010a723002',
        author: {
          id: 'bd63bae8-5744-11eb-8833-42010a723002',
          username: 'afifa',
        },
        text: {
          message: 'Ping',
          locale: 'en',
        },
      }, true);
    },
    updateUserPref(pref) {
      this.$store.commit('updatePref', pref);
    },
    async sendMessage(msg) {
      if (!msg.textMessage || msg.textMessage === '\n') {
        return;
      }
      const newMsg = {
        ...msg,
        id: this.getLastIndex() + 1,
        textLocale: msg.textLocale,
        textOriginal: msg.textMessage,
      };
      this.userMessages = [...this.userMessages, newMsg];
      GooglingualApi.send({
        roomId: msg.roomId,
        author: {
          id: msg.author.id,
          username: msg.author.displayName,
        },
        text: {
          message: msg.textMessage,
          locale: msg.textLocale,
        },
      });
    },
    async appendMessage(msg) {
      if (msg.sender.id === this.user.id) {
        return;
      }
      let audio = null;
      if (msg.audioMessage) {
        audio = new Audio(`data:audio/wav;base64,${msg.audioMessage}`);
      }
      const receivedMsg = {
        author: msg.sender,
        id: msg.messageIndex,
        textMessage: msg.message,
        textLocale: msg.messageLocale,
        audioMessage: audio ? audio.src : null,
        audioLocale: msg.audioLocale,
      };
      const lastIndex = this.getLastIndex();
      const lastMessage = this.userMessages[this.userMessages.length - 1];
      if (receivedMsg.id === lastIndex &&
        lastMessage.textMessage === receivedMsg.textMessage &&
        lastMessage.audioMessage === receivedMsg.audioMessage) {
        return;
      }
      console.log(`Appending received msg:
        ID: ${msg.id},
        INDEX: ${msg.messageIndex}.
        HAS AUDIO: ${!!msg.audioMessage}`);
      if (lastIndex === receivedMsg.id && lastMessage.textMessage === receivedMsg.textMessage) {
        this.userMessages[this.userMessages.length - 1].audioMessage = receivedMsg.audioMessage;
        return;
      }
      this.userMessages = [ ...this.userMessages, receivedMsg ];
      this.reorderMsgs();
    },
    async sendAudioMessage(msg, audioMessage) {
      if (!audioMessage || !audioMessage.url) {
        return;
      }
      const newMsg = {
        id: this.getLastIndex() + 1,
        ...msg,
        textLocale: msg.author.textLocale,
        textOriginal: msg.textMessage,
        audioMessage: audioMessage.url,
      };
      this.userMessages = [...this.userMessages, newMsg];
      const apiMessage = {
        roomId: msg.roomId,
        author: {
          id: msg.author.id,
          username: msg.author.displayName,
        },
        audio: {
          message: null,
          locale: msg.audioLocale,
        },
      }
      const reader = new FileReader();
      reader.onloadend = async () => {
        await this.updateTranscribedMessage(reader, apiMessage);
      };
      await reader.readAsDataURL(audioMessage.blob);
    },
    async updateTranscribedMessage(reader, apiMessage) {
      const base64data = reader.result;
      const encodedAudio = base64data.substr(base64data.indexOf(",") + 1);
      const toSendJson = {
        ...apiMessage,
        text: {
          message: null,
          locale: null,
        },
        audio: {
          ...apiMessage.audio,
          message: encodedAudio,
        },
      };
      GooglingualApi.send(toSendJson);
    },
    async reorderMsgs(msg) {
      let plusOne = false;
      const currentMsgs = this.userMessages.sort((a, b) => a.id - b.id);
      const reordered = currentMsgs.reduce((acc, curr, idx) => {
        if (idx === 0) {
          acc = [curr];
          return acc;
        }
        if (curr.id === acc[idx - 1].idx) {
          plusOne = true;
          acc = [...acc, { ...curr, id: curr.id + 1 }];
        } else {
          acc = [...acc, curr];
        }
        return acc;
      }, []);
      if (plusOne) {
        console.log('Reordered messages...');
        this.userMessages = reordered;
      }
    },
    signOut() {
      this.$emit('signOut');
    },
  },
};
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
.chat-container {
  display: flex;
  justify-content: center;
}
</style>
