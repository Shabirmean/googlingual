<template>
  <div class="chat-container">
    <UserWindow
      :windowTheme="'left-container'"
      :chatMessages="chatMessagesForShabirmean"
      :user="getShabirmean"
      roomId="cb3bf2c8-56dd-11eb-8833-42010a723002"
      @sendMessage="sendMessage"
      @sendAudioMessage="sendAudioMessage"
      @appendMessage="appendMessage"
      @signOut="signOut"
    ></UserWindow>
    <UserWindow
      :windowTheme="'right-container'"
      :chatMessages="chatMessagesForMom"
      :user="getKairun"
      roomId="cb3bf2c8-56dd-11eb-8833-42010a723002"
      defaultLocale="ta"
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

const WANAKKAM =
  "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";
const DUMMY_MSGS = {
  "1": {
    id: 1,
    author: {
      id: 'ApBnjofClqZXMWg55YgRqTtsIWi2',
      username: 'shabirmean',
      avatar: "avatar-male.png",
    },
    textMessage: "Welcome to the chat, I'm Bob!",
    textLocale: 'en',
    audioLocale: 'ta-IN',
    textOriginal: "Welcome to the chat, I'm Bob!",
    audioMessage: `data:audio/ogg;base64,${WANAKKAM}`,
  },
  "2": {
    id: 2,
    author: {
      id: '55YgRqTtsIWi2ApBnjofClqZXMWg',
      username: 'kairunnisa',
      avatar: "avatar-female.png",
    },
    textMessage: "Thank you Bob",
    textLocale: 'en',
    audioLocale: 'ta-IN',
    textOriginal: "Thank you Bob",
    audioMessage: `data:audio/ogg;base64,${WANAKKAM}`,
  },
};

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
      shabirmean: {
        id: 'ApBnjofClqZXMWg55YgRqTtsIWi2',
        username: 'shabirmean',
        textLocale: 'en',
        audioLocale: 'en-US',
        avatar: 'avatar-male.png',
        isAudioEnabled: true,
      },
      kairunnisa: {
        id: '55YgRqTtsIWi2ApBnjofClqZXMWg',
        username: 'kairunnisa',
        textLocale: 'ta',
        audioLocale: 'ta-IN',
        avatar: 'avatar-female.png',
        isAudioEnabled: true,
      },
      perUserMessages: {
        shabirmean: DUMMY_MSGS,
        kairunnisa: DUMMY_MSGS
      },
      pingTimer: 4,
      pingChron: null,
    };
  },
  computed: {
    chatMessagesForShabirmean() {
      return Object.values(this.perUserMessages.shabirmean)
        .map((m) => {
          m.author.avatar = this.user.photoURL;
          return m;
        })
        .sort((a, b) => a.id < b.id);
    },
    chatMessagesForMom() {
      return Object.values(this.perUserMessages.kairunnisa).sort((a, b) => a.id < b.id);
    },
    getShabirmean() {
      return {
        ...this.user,
        username: 'shabirmean',
        textLocale: 'en',
        audioLocale: 'en-US',
        avatar: 'avatar-male.png',
        isAudioEnabled: true,
        id: this.$store.getters.user.uid,
      };
    },
    getKairun() {
      return {
        username: 'kairunnisa',
        textLocale: 'ta',
        audioLocale: 'ta-IN',
        avatar: 'avatar-female.png',
        isAudioEnabled: true,
        id: '55YgRqTtsIWi2ApBnjofClqZXMWg',
      };
    },
  },
  created() {
    console.log(this.user);
    this.sendPing();
    this.checkAndPing();
    this.pingChron = setInterval(() => { this.pingTimer -= 1; }, 1000);
  },
  methods: {
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
    getUser(uId) {
      const users = [this.shabirmean, this.kairunnisa];
      return users.find((a) => a.id == uId);
    },
    async sendMessage(msg) {
      if (!msg.textMessage || msg.textMessage === '\n') {
        return;
      }
      const messages = this.perUserMessages[msg.author.username];
      let maxIndex = Object.keys(messages).sort().reverse()[0];
      maxIndex = maxIndex ? Number(maxIndex) + 1 : 0;
      this.perUserMessages[msg.author.username] = {
        ...messages,
        [maxIndex.toString()]: {
          id: maxIndex,
          ...msg,
          textLocale: msg.textLocale,
          textOriginal: msg.textMessage,
        },
      };

      GooglingualApi.send({
        roomId: msg.roomId,
        author: {
          id: msg.author.id,
          username: msg.author.username,
        },
        text: {
          message: msg.textMessage,
          locale: msg.textLocale,
        },
      });
    },
    async appendMessage(msg) {
      console.log(msg);
      const messages = this.perUserMessages[msg.recipient.username];
      console.log(messages);
      const msgAuthor = this.getUser(msg.sender);
      if (!msgAuthor) {
        return;
      }
      let audio = null;
      if (msg.audioMessage) {
        audio = new Audio(`data:audio/wav;base64,${msg.audioMessage}`);
      }
      messages[msg.messageIndex.toString()] = {
        author: msgAuthor,
        id: msg.messageIndex,
        textMessage: msg.message,
        textLocale: msg.messageLocale,
        audioMessage: audio ? audio.src : null,
        audioLocale: msg.audioLocale,
      };
      this.perUserMessages[msg.recipient.username] = { ...messages };
    },
    async sendAudioMessage(msg, audioMessage) {
      if (!audioMessage || !audioMessage.url) {
        return;
      }
      const messages = this.perUserMessages[msg.author.username];
      let maxIndex = Object.keys(messages).sort().reverse()[0];
      maxIndex = maxIndex ? Number(maxIndex) + 1 : 0;
      this.perUserMessages[msg.author.username] = {
        ...messages,
        [maxIndex.toString()]: {
          id: maxIndex,
          ...msg,
          textLocale: msg.author.textLocale,
          textOriginal: msg.textMessage,
          audioMessage: audioMessage.url,
        },
      };
      const apiMessage = {
        roomId: msg.roomId,
        author: {
          id: msg.author.id,
          username: msg.author.username,
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
