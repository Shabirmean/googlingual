<template>
  <div>
    <div class="locale-select locale-area">
      <div class="text-locale">
        <b-form-select v-model="selectedTextLocale" :options="textLocaleOptions" @change="loadAudioLocales" class="select-audio bg-dark text-white"></b-form-select>
      </div>
      <div class="audio-locale">
        <b-form-select v-model="selectedAudioLocale" :options="audioLocaleOptions" :disabled="hasNone" @change="updateUserPref" class="select-audio bg-dark text-white"></b-form-select>
        <b-form-checkbox v-model="audioEnabled" :disabled="disableAudioSelect" @change="updateUserPref" class="check-box">
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
    loadingAudio: {
      type: Boolean,
      default: true,
    },
    defaultLocale: {
      type: String,
      default: 'en',
    },
  },
  data: () => {
    return {
      selectedTextLocale: null,
      selectedAudioLocale: null,
      audioEnabled: false,
    };
  },
  computed: {
    isAudioEnabled() {
      return this.audioEnabled ? 'Voice message enabled' : 'You will not receive voice messages';
    },
    hasNone() {
      return this.audioLocaleOptions.length === 0 || this.selectedAudioLocale === 'None';
    },
    disableAudioSelect() {
      return this.loadingAudio || this.hasNone;
    },
  },
  created() {
    const hasEnglish = this.textLocaleOptions.find(l => l.value === this.defaultLocale);
    this.selectedTextLocale = hasEnglish ? this.defaultLocale : this.textLocaleOptions[0].value;
    this.selectedAudioLocale = 'None';
    this.loadAudioLocales();
  },
  methods: {
    loadAudioLocales() {
      this.updateUserPref();
      this.$emit('loadAudioLocales', this.selectedTextLocale);
    },
    updateUserPref() {
      this.$emit('updateUserPref', {
        textLocale: this.selectedTextLocale,
        audioLocale: this.selectedAudioLocale,
        audioEnabled: this.audioEnabled,
      });
    },
  },
  watch: {
    textLocaleOptions(newV, oldV) {
      const previouslySelected = this.selectedTextLocale;
      if (!oldV) {
        this.selectedTextLocale = newV[0].value;
      } else {
        const selectedOld = newV.find(v => v.value === previouslySelected);
        this.selectedTextLocale = selectedOld && selectedOld.length > 0 ?
          selectedOld.value : newV[0].value;
      }
      if (previouslySelected !== this.selectedTextLocale) {
        this.loadAudioLocales();
      }
    },
    audioLocaleOptions(newV) {
      this.selectedAudioLocale = newV[0].value;
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
