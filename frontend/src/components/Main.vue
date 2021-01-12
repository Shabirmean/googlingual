<template>
  <div class="chat-box">
    <form>
      <div class="form-group">
        <label for="chatMessage">Your message</label>
        <textarea
          class="form-control rounded-0"
          id="chatMessage"
          rows="10"
          cols="100"
        ></textarea>
        <small id="chatMessageInfo" class="form-text text-muted"
          >We'll translate it for you</small
        >
        <button type="submit" class="btn btn-primary">
          <i class="fa fa-paper-plane" aria-hidden="true"></i>
        </button>
      </div>
    </form>
  </div>
</template>

<script>
export default {
  name: "Main",
  props: {
    msg: String,
  },
};
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
h3 {
  margin: 40px 0 0;
}

ul {
  list-style-type: none;
  padding: 0;
}

li {
  display: inline-block;
  margin: 0 10px;
}

a {
  color: #42b983;
}

.chat-box {
  justify-content: center;
  display: flex;
}
</style>
