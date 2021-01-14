<template>
  <div class="chat-box">
    <form>
      <div class="form-group">
        <label for="chatMessage">Your message</label>
        <textarea
          class="form-control rounded-0"
          id="chatMessage"
          rows="2"
          cols="100"
          v-model="message"
        ></textarea>
        <small id="chatMessageInfo" class="form-text text-muted"
          >We'll translate it for you</small
        >
        <div>
          <button
            type="button"
            v-on:click="addMessage"
            class="btn btn-primary"
            style="margin:5px"
          >
            <i class="fa fa-paper-plane" aria-hidden="true"></i>
          </button>

          <button
            type="button"
            v-on:click="recordAudio"
            class="btn btn-primary"
            style="margin:5px"
          >
            <i class="fa fa-microphone" style="font-size:18px"></i>
          </button>
        </div>
      </div>
    </form>
  </div>
</template>

<script>
export default {
  name: "InputArea",
  props: {},
  data: () => {
    return {
      message: "",
    };
  },
  computed: {},
  methods: {
    addMessage() {
      this.$emit("addMessage", this.message);
      this.message = "";
    },
    recordAudio() {
      // if (window.hasOwnProperty("webkitSpeechRecognition")) {
      //   var recognition = new webkitSpeechRecognition();

      //   recognition.continuous = false;
      //   recognition.interimResults = false;
      //   recognition.lang = "en-US";
      //   recognition.start();

      //   recognition.onresult = function(e) {
      //     alert(e.results[0][0].transcript);
      //     recognition.stop();
      //   };
      //   recognition.onerror = function(e) {
      //     recognition.stop();
      //   };
      // }
    },
  },
};
</script>

<style scoped>
.chat-box {
  justify-content: center;
  display: flex;
}
</style>
