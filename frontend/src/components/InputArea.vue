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
            <button
              type="button"
              v-on:click="addMessage"
              class="btn btn-primary"
            >
              <i class="fa fa-paper-plane" aria-hidden="true"></i>
            </button>
          </div>
        </form>
  </div>
</template>

<script>
export default {
  name: "InputArea",
  props: {

  },
  data: () => {
    return {
      message: "",
    };
  },
  computed: {

  },
  methods: {
    addMessage() {
      this.$emit('addMessage', this.message);
      this.message = "";
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
