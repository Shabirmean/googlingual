import Vue from 'vue'
import App from './App.vue'
import BootstrapVue from "bootstrap-vue"
import AudioRecorder from 'vue-audio-recorder'
// import socketio from 'socket.io-client';
// import VueSocketIO from 'vue-socket.io';
import "bootstrap/dist/css/bootstrap.min.css"
import "bootstrap-vue/dist/bootstrap-vue.css"
import '@fortawesome/fontawesome-free/css/all.css'
import '@fortawesome/fontawesome-free/js/all.js'


// const SOCKETS_API = (process.env.SOCKET_SERVER_URL) ? process.env.SOCKET_SERVER_URL : 'http://localhost:8081';
// const SOCKETS_API = 'https://googlingual-delivery-dot-gcloud-dpe.ue.r.appspot.com';
// export const SocketInstance = socketio(SOCKETS_API);
// Vue.use(new VueSocketIO({
//   debug: false, // true,
//   connection: SocketInstance,
// }))

Vue.use(BootstrapVue)
Vue.use(AudioRecorder)
Vue.config.productionTip = false

// <!-- The core Firebase JS SDK is always required and must be listed first -->
// <script src="https://www.gstatic.com/firebasejs/8.2.4/firebase-app.js"></script>

// <!-- TODO: Add SDKs for Firebase products that you want to use
//      https://firebase.google.com/docs/web/setup#available-libraries -->
// <script src="https://www.gstatic.com/firebasejs/8.2.4/firebase-analytics.js"></script>

// <script>
//   // Your web app's Firebase configuration
//   // For Firebase JS SDK v7.20.0 and later, measurementId is optional
//   var firebaseConfig = {
//     apiKey: "AIzaSyAJSWYHwTI9PsZ5P7esyhVkLOxdVkk4vow",
//     authDomain: "gcloud-dpe.firebaseapp.com",
//     projectId: "gcloud-dpe",
//     storageBucket: "gcloud-dpe.appspot.com",
//     messagingSenderId: "240150654928",
//     appId: "1:240150654928:web:6b3ff15f9fbaa3672113d5",
//     measurementId: "G-VZH6Z5JSMR"
//   };
//   // Initialize Firebase
//   firebase.initializeApp(firebaseConfig);
//   firebase.analytics();
// </script>

// const firebaseConfig = {
//   apiKey: "AIzaSyAJSWYHwTI9PsZ5P7esyhVkLOxdVkk4vow",
//   authDomain: "gcloud-dpe.firebaseapp.com",
//   projectId: "gcloud-dpe",
//   storageBucket: "gcloud-dpe.appspot.com",
//   messagingSenderId: "240150654928",
//   appId: "1:240150654928:web:6b3ff15f9fbaa3672113d5",
//   measurementId: "G-VZH6Z5JSMR"
// };

new Vue({
  render: h => h(App),
}).$mount('#app')


