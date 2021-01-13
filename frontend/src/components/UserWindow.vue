<template>
  <div :class="windowTheme">
    <ChatWindow :chatMessages="chatMessages" :owner="owner"></ChatWindow>
    <InputArea @addMessage="addMessage"></InputArea>
  </div>
</template>

<script>
import ChatWindow from "./ChatWindow.vue";
import InputArea from "./InputArea.vue";

export default {
  name: "UserWindow",
  components: {
    ChatWindow,
    InputArea,
  },
  props: {
    windowTheme: {
      type: String,
      required: true,
    },
    chatMessages: {
      type: Array,
      required: true,
    },
    owner: {
      type: String,
      required: true,
    },
    avatar: {
      type: String,
      required: true,
    },
  },
  data: () => {
    return {};
  },
  computed: {},
  methods: {
    addMessage(newMessage) {
      this.$emit("addMessage", {
        body: newMessage,
        author: this.owner,
        avatar: this.avatar,
      });
    },
  },
};
</script>

<style scoped>
.left-container {
  flex: 50%;
  background: #8db78e66;
}

.right-container {
  flex: 50%;
  background: #b78d9c66;
}
</style>
