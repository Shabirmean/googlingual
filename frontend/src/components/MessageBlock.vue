<template>
  <div class="message-container">
    <p
      class="message"
      :class="{ 'message-out': !isAuthor, 'message-in': isAuthor }"
    >
      <img
        v-if="isAuthor"
        class="image-chart image-left"
        :src="require(`@/assets/${avatar}`)"
        :alt="name"
      />
      {{ displayMessage }}
      <img
        v-if="!isAuthor"
        class="image-chart image-right"
        :src="require(`@/assets/${avatar}`)"
        :alt="name"
      />
    </p>
    <audio v-if="!isAuthor" controls :class="{ 'audio-out': !isAuthor }">
      <source :src="message.audioMessage"/>
      Your browser does not support the audio element.
    </audio>
  </div>
</template>

<script>
export default {
  name: "MessageBlock",
  props: {
    message: {
      type: Object,
      required: true,
    },
    sent: {
      type: Boolean,
      required: true,
    },
    avatar: {
      type: String,
      default: "avataaar-default.png",
    },
    name: {
      type: String,
      default: "Person",
    },
  },
  data: () => {
    return {};
  },
  computed: {
    displayMessage() {
      return this.isAuthor ? this.message.original : this.message.body;
    },
    isAuthor() {
      return this.sent;
    },
  },
  methods: {},
};
</script>

<style scoped>
p {
  margin-top: 1em;
}

.message-container {
  display: flex;
  flex-direction: column;
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
  background: #f7386aa1;
  color: black;
  margin-left: 55%;
  text-align: right;
}
.message-in {
  background: #27922bfa;
  color: white;
}

.audio-out {
  margin-left: 55%;
  text-align: right;
}

.image-chart {
  width: 12%;
  border-radius: 50%;
}

.image-left {
  margin-right: 12px;
}
.image-right {
  margin-left: 12px;
}
</style>
