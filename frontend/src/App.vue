<template>
  <div id="app">
    <div>
      <img class="main-logo" alt="Vue logo" src="./assets/convo.png">
      <Main/>
    </div>
    <button
      type="button"
      class="btn btn-primary"
      style="
        width: 100%;
        font-size: 16px;
        font-style: italic;
        font-weight: bold;
        background-color: #3d6fa5;
      ">This is not an officel Google Project. This is a personal pet project</button>
  </div>
</template>

<script>
import Main from './components/Main.vue'

export default {
  name: 'App',
  components: {
    Main
  }
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
  width: 25%;
  margin-top: -50px;
  background: #f9faff;
  border-radius: 50%;
}
</style>
