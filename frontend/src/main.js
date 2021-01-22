import Vue from 'vue'
import App from './App.vue'
import BootstrapVue from "bootstrap-vue"
import AudioRecorder from 'vue-audio-recorder'
import socketio from 'socket.io-client';
import VueSocketIO from 'vue-socket.io';
import "bootstrap/dist/css/bootstrap.min.css"
import "bootstrap-vue/dist/bootstrap-vue.css"
import '@fortawesome/fontawesome-free/css/all.css'
import '@fortawesome/fontawesome-free/js/all.js'


// const SOCKETS_API = (process.env.SOCKET_SERVER_URL) ? process.env.SOCKET_SERVER_URL : 'http://localhost:8081';
const SOCKETS_API = 'https://googlingual-delivery-dot-gcloud-dpe.ue.r.appspot.com';
export const SocketInstance = socketio(SOCKETS_API);
Vue.use(new VueSocketIO({
  debug: true,
  connection: SocketInstance,
}))

Vue.use(BootstrapVue)
Vue.use(AudioRecorder)
Vue.config.productionTip = false

new Vue({
  render: h => h(App),
}).$mount('#app')


