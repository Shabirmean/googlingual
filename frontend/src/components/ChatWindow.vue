<template>
  <div>
    <div class="locale-select locale-area">
      <div class="text-locale">
        <b-form-select v-model="selectedTextLocale" :options="textLocaleOptions" class="select-audio bg-dark text-white"></b-form-select>
      </div>
      <div class="audio-locale">
        <b-form-select v-model="selectedAudioLocale" :options="audioLocaleOptions" class="select-audio bg-dark text-white"></b-form-select>
        <b-form-checkbox v-model="audioEnabled" class="check-box">
          {{ isAudioEnabled }}
        </b-form-checkbox>
      </div>
    </div>
    <section ref="chatArea" class="chat-area">
        <div
          v-for="message in chatMessages"
          v-bind:key="message.id"
        >
          <MessageBlock
            :message="message"
            :sent="message.author.id === userId"
            :avatar="message.author.avatar"
            :author="message.author.username">
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
    userId: {
      type: String,
      required: true,
    },
    textLocaleOptions: {
      type: Array,
      required: true,
    },
    audioLocaleOptions: {
      type: Array,
      required: true,
    },
  },
  data: () => {
    return {
      selectedTextLocale: { value: 'en', text: 'English' },
      selectedAudioLocale: { value: 'en-US', text: 'en-US' },
      audioEnabled: false,
    };
  },
  computed: {
    isAudioEnabled() {
      return this.audioEnabled ? 'Voice message enabled' : 'You will not receive voice messages';
    },
  },
  created() {
    this.selectedTextLocale = this.textLocaleOptions[0].value;
    this.selectedAudioLocale = this.audioLocaleOptions[0].value;
  },
  watch: {
    textLocaleOptions(newV, oldV) {
      if (!oldV) {
        this.selectedTextLocale = newV[0].value;
      }
    },
    audioLocaleOptions(newV, oldV) {
      if (!oldV) {
        this.selectedAudioLocale = newV[0].value;
      }
    }
  }
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
.locale-area {
  /*   border: 1px solid #39403d; */
  background: #5f6b6b24;
  overflow: auto;
  max-width: 900px;
  margin: 2em auto 2em auto;
  box-shadow: 2px 2px 5px 2px rgba(0, 0, 0, 0.3);
}
.locale-select {
  display: flex;
}
.audio-locale {
  flex: 1;
  margin: 15px;
}
.text-locale {
  flex: 1;
  margin: 15px;
}
.check-box {
  display: flex;
  margin-top: 10px;
  font-weight: 900;
}
</style>
