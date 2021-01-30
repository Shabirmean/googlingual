<template>
  <div class="message-container">
    <div  class="message" :class="{ 'message-out': !isAuthor, 'message-in': isAuthor }">
    <img
        class="image-chart"
        :class="{ 'image-left': !isAuthor, 'image-right': isAuthor }"
        :src="displayImage"
        :alt="author"
      />
      <div>
        <div class="message-content"> {{ displayMessage }} </div>
        <div :class="{ 'author-left': isAuthor, 'author-right': !isAuthor }"> {{ author }} </div>
      </div>
    </div>
    <audio v-if="isAudio" controls :class="{ 'audio-out': !isAuthor }">
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
    author: {
      type: String,
      default: "Person",
    },
  },
  computed: {
    displayMessage() {
      return this.isAuthor ? this.message.textOriginal : this.message.textMessage;
    },
    displayImage() {
      return this.avatar.includes('http') ? this.avatar : require(`@/assets/${this.avatar}`);
    },
    isAuthor() {
      return this.sent;
    },
    isAudio() {
      return this.message.audioMessage || this.message.textOriginal === 'Audio recording...';
    },
  },
};
</script>

<style scoped>
p {
  margin-top: 1em;
}

img {
  max-width: 50px;
  max-height: 47px;
}

.message-container {
  display: flex;
  flex-direction: column;
  margin-top: 5px;
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
  display: flex;
  margin-bottom: 5px;
}
.message-out {
  background: #f7386aa1;
  color: black;
  margin-left: 55%;
  text-align: right;
  flex-flow: row-reverse;
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
  margin-right: 5px;
  margin-left: 12px;
}
.image-right {
  margin-left: 5px;
  margin-right: 12px;
}
.message-content {
  margin-top: 5px;
}
.author-left {
  margin-top: 2px;
  font-style: italic;
  color: #ffffff82;
}
.author-right {
  margin-top: 2px;
  font-style: italic;
  color: #00000082;
}
</style>
