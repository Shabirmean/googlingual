<template>
  <div class="chat-box">
    <form>
      <div class="form-group">
        <label for="chatMessage">Your message</label>
        <textarea v-on:keyup.enter="addMessage"
          class="form-control rounded-0"
          rows="2"
          cols="100"
          v-model="message"
        ></textarea>
        <small id="chatMessageInfo" class="form-text text-muted"
          >We'll translate it for you</small
        >
        <div>
          <button
            type="button"
            v-on:click="addMessage"
            class="btn btn-primary"
            style="margin:5px"
          >
            <i class="fa fa-paper-plane" aria-hidden="true"></i>
          </button>

          <button
            v-if="!isRecording"
            v-on:click="toggleRecorder"
            type="button"
            class="btn btn-primary"
            style="margin:5px"
          >
            <i class="fa fa-microphone" style="font-size:18px"></i>
          </button>
          <button
            v-else
            v-on:click="stopRecorder"
            type="button"
            class="btn btn-danger"
            style="margin:5px"
          >
            <i class="fas fa-stop" style="font-size:18px"></i>
          </button>
          <button
            v-if="hasRecording"
            v-on:click="removeRecord"
            type="button"
            class="btn btn-secondary"
            style="margin:5px"
          >
            <i class="fas fa-save" style="font-size:18px"></i>
            <i class="fas fa-times" style="font-size:10px; margin-left:5px"></i>
          </button>
          <div v-if="isRecording" class="ar-recorder__duration">
            {{ recordedTime }}
          </div>
        </div>
      </div>
    </form>
  </div>
</template>

<script>
import Recorder from "@/services/lib/recorder";
import { convertTimeMMSS } from "@/services/lib/utils";

export default {
  name: "InputArea",
  data: () => {
    return {
      message: "",
      recorder: undefined,
      micFailed: () => {},
      beforeRecording: () => {},
      pauseRecording: () => {},
      afterRecording: (msg) => {
        console.log(msg);
      },
      time: 1,
      bitRate: 128,
      sampleRate: 44100,
      recordList: [],
    };
  },
  computed: {
    isRecording() {
      return this.recorder && this.recorder.isRecording;
    },
    recordedTime() {
      if (this.time && this.recorder.duration >= this.time * 60) {
        this.stopRecorder();
      }
      return convertTimeMMSS(this.recorder.duration);
    },
    hasRecording() {
      return this.recordList && this.recordList.length > 0;
    },
    audioMessage() {
      return this.recordList && this.recordList.length > 0 && this.recordList[0];
    },
  },
  created() {
    this.recorder = this._initRecorder();
  },
  methods: {
    addMessage() {
      if (this.hasRecording) {
        this.$emit("addAudioMessage", this.audioMessage);
      } else {
        this.$emit("addMessage", this.message);
      }
      this.recordList = [];
      this.message = "";
    },
    toggleRecorder() {
      this.recordList = [];
      this.recorder.start();
    },
    stopRecorder() {
      if (!this.isRecording) {
        return;
      }
      this.recorder.stop();
      this.recordList = [this.recorder.lastRecord()];
    },
    removeRecord() {
      this.recordList = [];
    },
    _initRecorder() {
      return new Recorder({
        beforeRecording: this.beforeRecording,
        afterRecording: this.afterRecording,
        pauseRecording: this.pauseRecording,
        micFailed: this.micFailed,
        bitRate: this.bitRate,
        sampleRate: this.sampleRate,
        format: "wav",
      });
    },
  },
  beforeDestroy() {
    this.stopRecorder();
  },
};
</script>

<style scoped>
.chat-box {
  justify-content: center;
  display: flex;
}
</style>
