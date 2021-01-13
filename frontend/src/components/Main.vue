<template>
  <div class="chat-container">
    <UserWindow
      :windowTheme="'left-container'"
      :chatMessages="chatMessages"
      :owner="'me'"
      :avatar="'avatar-male.png'"
      @addMessage="addMessage"
    ></UserWindow>
    <UserWindow
      :windowTheme="'right-container'"
      :chatMessages="chatMessages"
      :owner="'notMe'"
      :avatar="'avatar-female.png'"
      @addMessage="addMessage"
    ></UserWindow>
  </div>
</template>

<script>
import GooglingualApi from "@/services/api/Googlingual";
import UserWindow from "./UserWindow.vue";

export default {
  name: "Main",
  components: {
    UserWindow,
  },
  data: () => {
    return {
      id: 4,
      messages: [
        {
          id: 1,
          body: "Welcome to the chat, I'm Bob!",
          author: "me",
          avatar: "avatar-male.png",
        },
        {
          id: 2,
          body: "Thank you Bob",
          author: "notMe",
          avatar: "avatar-female.png",
        },
      ],
    };
  },
  computed: {
    chatMessages() {
      return this.messages;
    },
  },
  methods: {
    addMessage(newMessage) {
      const response = GooglingualApi.translate({
        message: newMessage.body,
        locale: 'en'
      })
      console.log(response);

      this.messages = [
        ...this.messages,
        {
          id: this.id,
          ...newMessage,
        },
      ];
      this.id += 1;
      this.sentMsg = "";
    },
  },
};
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style scoped>
.chat-container {
  display: flex;
  justify-content: center;
}
</style>
