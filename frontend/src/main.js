import Vue from 'vue'
import App from './App.vue'
import BootstrapVue from "bootstrap-vue"
import AudioRecorder from 'vue-audio-recorder'
import "bootstrap/dist/css/bootstrap.min.css"
import "bootstrap-vue/dist/bootstrap-vue.css"
import '@fortawesome/fontawesome-free/css/all.css'
import '@fortawesome/fontawesome-free/js/all.js'

Vue.use(BootstrapVue)
Vue.use(AudioRecorder)
Vue.config.productionTip = false

new Vue({
  render: h => h(App),
}).$mount('#app')
