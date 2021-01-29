import Vue from 'vue'
import Vuex from 'vuex'
import App from './App.vue'
import BootstrapVue from "bootstrap-vue"
import AudioRecorder from 'vue-audio-recorder'
// import socketio from 'socket.io-client';
// import VueSocketIO from 'vue-socket.io';
import "bootstrap/dist/css/bootstrap.min.css"
import "bootstrap-vue/dist/bootstrap-vue.css"
import '@fortawesome/fontawesome-free/css/all.css'
import '@fortawesome/fontawesome-free/js/all.js'


Vue.use(Vuex)
Vue.use(BootstrapVue)
Vue.use(AudioRecorder)
Vue.config.productionTip = false

const store = new Vuex.Store({
  state: {
    user: {
      uid: null,
      displayName: null,
      photoURL: null,
      email: null,
      accessToken: null,
    }
  },
  mutations: {
    setUser(state, user) {
      state.user = { ...user };
    },
    clearUser(state) {
      state.user = {
        uid: null,
        displayName: null,
        photoURL: null,
        email: null,
        accessToken: null,
      };
    }
  },
  getters: {
    accessToken: (state) => state.user.accessToken,
    user: (state) => state.user,
  }
});

new Vue({
  render: h => h(App),
  store,
}).$mount('#app')

export default store
