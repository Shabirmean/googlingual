<template>
  <div>
    <section ref="chatArea" class="chat-area">
        <div
          v-for="message in chatMessages"
          v-bind:key="message.id"
        >
          <MessageBlock
            :message="message"
            :sent="message.author === owner"
            :avatar="message.avatar"
            :name="message.author">
          </MessageBlock>
        </div>
      </section>


  </div>

</template>


<script>
import MessageBlock from './MessageBlock.vue'

export default {
  name: "ChatWindow",
  components: {
    MessageBlock
  },
  props: {
    chatMessages: {
      type: Array,
      required: true,
    },
    owner: {
      type: String,
      required: true,
    },
  },
  data: () => {
    return {};
  },
  computed: {},
  methods: {},
};
</script>


<style scoped>
.chat-area {
  /*   border: 1px solid #39403d; */
  background: #5f6b6b24;
  height: 50vh;
  padding: 1em;
  overflow: auto;
  max-width: 900px;
  margin: 2em auto 2em auto;
  box-shadow: 2px 2px 5px 2px rgba(0, 0, 0, 0.3);
}
</style>
