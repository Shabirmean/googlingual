<template>
  <div :class="windowTheme" class="user-container">
     <div v-if="loading" class="icon">
        <div class="bar" style="background-color: #3498db; margin-left: -60px;"></div>
        <div class="bar" style="background-color: #e74c3c; margin-left: -20px;"></div>
        <div class="bar" style="background-color: #f1c40f; margin-left: 20px;"></div>
        <div class="bar" style="background-color: #27ae60; margin-left: 60px;"></div>
    </div>
    <ChatWindow v-if="!loading"
      ref="chatWindow"
      @loadAudioLocales="loadAudioLocales"
      @updateUserPref="updateUserPref"
      :loadingAudio="loadingAudio"
      :chatMessages="chatMessages"
      :userId="user.id"
      :textLocaleOptions="textLocaleOptions"
      :audioLocaleOptions="audioLocaleOptions"
      :defaultLocale="defaultLocale"></ChatWindow>
    <InputArea v-if="!loading" @addMessage="addMessage" @addAudioMessage="addAudioMessage"></InputArea>
  </div>
</template>

<script>
import GooglingualApi from "@/services/api/Googlingual";
import socketio from 'socket.io-client';
import ChatWindow from "./ChatWindow.vue";
import InputArea from "./InputArea.vue";

const SOCKETS_API = (process.env.VUE_APP_SOCKET_SERVER_URL) ?
  process.env.VUE_APP_SOCKET_SERVER_URL : 'http://localhost:8081';

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
    defaultLocale: {
      type: String,
      default: 'en',
    },
  },
  data: () => {
    return {
      socket: socketio(SOCKETS_API),
      loading: true,
      locales: [{ code: 'en', name: 'English' }],
      voices: ['None'],
      localesGiveupCount: 0,
      voicesGiveupCount: 0,
      fetchingVoices: false,
    };
  },
  async created() {
    this.socket.on('connect', this.connect);
    this.socket.on('disconnect', this.disconnect);
    this.socket.on('userRegistered', this.userRegistered);
    this.socket.on('chatRoomMessage', this.chatRoomMessage);
    await this.loadLocales();
  },
  computed: {
    textLocaleOptions() {
      const localesArr = this.locales.reduce((acc, curr) => {
        acc = [ ...acc, ({ value: curr.code, text: curr.name }) ];
        return acc;
      }, []);
      return localesArr;
    },
    audioLocaleOptions() {
      const localesArr = this.voices.reduce((acc, curr) => {
        acc = [ ...acc, ({ value: curr, text: curr }) ];
        return acc;
      }, []);
      return localesArr;
    },
    loadingAudio() {
      return this.fetchingVoices;
    },
  },
  methods: {
    hasElements(arr) {
      return arr && arr.length > 0;
    },
    async loadLocales() {
      const localesResp = await GooglingualApi.locales();
      console.log(localesResp);
      this.localesGiveupCount += 1;
      if (localesResp && localesResp.status === 200) {
        this.locales = this.hasElements(localesResp.data.results) ? localesResp.data.results : [ { code: 'en', name: 'English' } ];
        this.loading = false;
        return;
      }
      if (this.localesGiveupCount >= 10) {
        this.loading = false;
        return;
      }
      setTimeout(this.loadLocales, 7500);
    },
    async loadAudioLocales(lang) {
      this.fetchingVoices = true;
      const localesResp = await GooglingualApi.audioLocales(lang);
      this.voicesGiveupCount += 1;
      if (localesResp && localesResp.status === 200) {
        this.voices = this.hasElements(localesResp.data.results) ? localesResp.data.results : [ 'None' ];
      } else if (this.voicesGiveupCount < 10) {
        setTimeout(this.loadAudioLocales.bind(null, lang), 7500);
      }
      this.fetchingVoices = false;
    },
    connect() {
      console.log(`Connected to sockets server with id: ${this.socket.id}`);
      this.socket.emit('newSocketConnection', {
        sId: this.socket.id,
        uId: this.user.id,
        textLocale: this.user.textLocale,
        audioLocale:this.user.audioLocale,
        audioEnabled: true,
      });
    },
    disconnect() {
      console.log(`Client ${this.socket.id} disconnected from sockets server`);
    },
    userRegistered(data) {
      console.log(`User registration message:`, data);
    },
    updateUserPref(pref) {
      this.socket.emit('updateUserPref', {
        sId: this.socket.id,
        uId: this.user.id,
        ...pref,
      });
    },
    chatRoomMessage(payload) {
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
        textLocale: this.$refs.chatWindow.selectedTextLocale,
        audioLocale: this.$refs.chatWindow.selectedAudioLocale,
        author: this.user,
        roomId: this.roomId,
      });
    },
    addAudioMessage(audioMessage) {
      this.$emit("sendAudioMessage", {
        textMessage: 'Audio recording...',
        textLocale: this.$refs.chatWindow.selectedTextLocale,
        audioLocale: this.$refs.chatWindow.selectedAudioLocale,
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
.user-container {
  padding-left: 24px;
  padding-right: 24px;
}

/**
body,div{
    padding: 0;
    box-sizing: border-box;
}
body{
    /*light ocean */
    /**
    background-color: #eee;
    font-family: 'Nunito', sans-serif;
    text-align: center;
}
**/

.icon{
    position: relative;
    /*left: 50vw;*/
    margin: auto;
    width: 0;
    top: 25vh;
    text-align: center;
    cursor: pointer;
}

.bar{
    position: absolute;
    transform: translate(-50%,-50%);
    /*align-items: center;*/
    top : -10px;

    background-color: #f98866;
    width: 20px;
    height: 20px;
    margin: 5px 0;
    border-radius: 10px;
    transition: 0.3s;
    animation: dope 1.5s ease-in-out 0s infinite;
}

.bar:nth-child(1){
    animation-delay: 0s;
}
.bar:nth-child(2){
    animation-delay: 0.15s;
}
.bar:nth-child(3){
    animation-delay: 0.3s;
}
.bar:nth-child(4){
    animation-delay: 0.45s;
}


@keyframes dope {
    0%   {
        height: 20px;
    }
    50%  {
        height:60px;
    }
    100% {
        height: 20px;
    }
}
</style>
