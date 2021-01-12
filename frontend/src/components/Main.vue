<template>
  <div>
    <section ref="chatArea" class="chat-area">
      <p
        v-for="message in messages"
        v-bind:key="message.id"
        class="message"
        :class="{
          'message-out': message.author === 'me',
          'message-in': message.author !== 'notMe',
        }"
      >
        {{ message.body }}
      </p>
    </section>
    <div class="chat-box">
      <form>
        <div class="form-group">
          <label for="chatMessage">Your message</label>
          <textarea
            class="form-control rounded-0"
            id="chatMessage"
            rows="2"
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
  </div>
</template>

<script>
export default {
  name: "Main",
  props: {
    msg: String,
  },
  data: () => {
    return {
      receivedMsg: "",
      sentMsg: "",
      messages: [
        {
          id: 1,
          body: "Welcome to the chat, I'm Bob!",
          author: "notMe",
        },
        {
          id: 2,
          body: "Thank you Bob",
          author: "me",
        },
        {
          id: 3,
          body: "You're most welcome",
          author: "notMe",
        },
      ],
    };
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

.chat-area {
  /*   border: 1px solid #39403d; */
  background: #5f6b6b;
  height: 50vh;
  padding: 1em;
  overflow: auto;
  max-width: 900px;
  margin: 0 auto 2em auto;
  box-shadow: 2px 2px 5px 2px rgba(0, 0, 0, 0.3);
}
.message {
  width: 45%;
  border-radius: 10px;
  padding: 0.5em;
  /*   margin-bottom: .5em; */
  font-size: 0.8em;
  text-align: left;
  background: #eaeaff;
  font-weight: bold;
}
.message-out {
  background: #407fff;
  color: white;
  margin-left: 55%;
  text-align: right;
}
.message-in {
  background: #f7e2e2;
  color: black;
}
</style>
