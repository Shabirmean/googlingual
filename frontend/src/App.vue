<template>
  <div id="app">
    <div>
      <img class="main-logo" alt="Vue logo" src="./assets/convo.png">
      <div v-if="!isSignedIn" id="firebaseui-auth-container">
        <div id="sign-in-status" class="sign-in-info-label">You've got to Sign In...</div>
      </div>
      <div v-else>
         <div id="sign-in-status" class="sign-in-info-label">
          Welcome to Googlingual <span style="font-style:italic">{{ signedInUser.displayName }}</span>
        </div>
        <button @click="signOut" type="button" class="btn btn-danger" style="margin-bottom: 10px; font-weight: bold;">
          <i class="fas fa-sign-out-alt"></i>Sign Out
        </button>
        <Main :user="signedInUser"/>
      </div>
    </div>
    <button
      class="btn btn-primary"
      style="
        width: 100%;
        font-size: 16px;
        font-style: italic;
        font-weight: bold;
        background-color: #3d6fa5;
      ">This is not an officel Google Project. This is a personal project</button>
  </div>
</template>

<script>
import Main from './components/Main.vue';
import firebase from 'firebase/app'
import * as firebaseui from 'firebaseui';
import 'firebaseui/dist/firebaseui.css';


const APP_CONFIG = {
  apiKey: "AIzaSyAJSWYHwTI9PsZ5P7esyhVkLOxdVkk4vow",
  authDomain: "gcloud-dpe.firebaseapp.com",
};

const PROVIDER_CONFIG = {
  signInSuccessUrl: '/',
  signInOptions: [
    firebase.auth.GoogleAuthProvider.PROVIDER_ID,
    firebase.auth.EmailAuthProvider.PROVIDER_ID,
  ],
  tosUrl: 'localhost:8080/privacy.html',
  privacyPolicyUrl: function() {
    window.location.assign('localhost:8080/privacy.html');
  }
};

export default {
  name: 'App',
  components: {
    Main
  },
  data: () => {
    return {
      signedIn: false,
      signInUi: null,
    };
  },
  computed: {
    isSignedIn() {
      return this.signedIn;
    },
    signedInUser() {
      return this.$store.state.user;
    }
  },
  created() {
    this.initFireBase();
  },
  methods: {
    async initFireBase() {
      firebase.initializeApp(APP_CONFIG);
      this.signInUi = new firebaseui.auth.AuthUI(firebase.auth());
      this.signInUi.start('#firebaseui-auth-container', PROVIDER_CONFIG);
      const initApp = (appContext) => this.initAuth(appContext);
      window.addEventListener('load', () => initApp(this));
    },
    async initAuth(appContext) {
      firebase.auth().onAuthStateChanged(
        async (user) => {
          if (user) {
            const token = await user.getIdToken();
            if (!token) {
              appContext.signedIn = false;
              return;
            }
            appContext.$store.commit('setUser', {
              uid: user.uid,
              displayName: user.displayName,
              email: user.email,
              photoURL: user.photoURL,
              accessToken: token,
            });
            console.log(appContext.$store.state.user);
            appContext.signedIn = true;
          } else {
            appContext.signedIn = false;
            appContext.$store.commit('clearUser');
          }
        },
        (err) => {
          appContext.signedIn = false;
          appContext.$store.commit('clearUser');
          console.log(err);
        });
    },
    async signOut(event) {
      event.preventDefault();
      await firebase.auth().signOut();
      window.location.reload();
    },
  },
}
</script>

<style>
#app {
  font-family: Avenir, Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  text-align: center;
  color: #2c3e50;
}

.main-logo {
  width: 20%;
  /* margin-top: -50px; */
  background: #f9faff;
  border-radius: 50%;
}

.sign-in-info-label {
  margin: auto;
  padding-top: 16px;
  padding-bottom: 16px;
  width: 30%;
  font-size: 22px;
  font-weight: bold;
}
</style>
