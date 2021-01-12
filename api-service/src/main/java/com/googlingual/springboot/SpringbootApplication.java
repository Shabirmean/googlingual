/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.googlingual.springboot;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.cloud.pubsub.v1.Publisher;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.ProjectTopicName;
import com.google.pubsub.v1.PubsubMessage;
import com.google.cloud.translate.*;
import com.google.cloud.translate.Translate.TranslateOption;

import com.google.cloud.texttospeech.v1.AudioConfig;
import com.google.cloud.texttospeech.v1.AudioEncoding;
import com.google.cloud.texttospeech.v1.SsmlVoiceGender;
import com.google.cloud.texttospeech.v1.SynthesisInput;
import com.google.cloud.texttospeech.v1.SynthesizeSpeechResponse;
import com.google.cloud.texttospeech.v1.TextToSpeechClient;
import com.google.cloud.texttospeech.v1.VoiceSelectionParams;

// [START gae_java11_helloworld]
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class SpringbootApplication {
  private static final Logger logger = Logger.getLogger(SpringbootApplication.class.getName());
  private static final String CF_PUBLISH_TOPIC = "hello-world-sub";
  private static final String PROJECT_ID = "gcloud-dpe";
  private static final String TEXT_LOCALE_TAMIL = "ta";
  private static final String SPEECH_LOCALE_TAMIL = "ta-IN";
  private static final Translate translationService = TranslateOptions.getDefaultInstance().getService();
  private static final TranslateOption NMT_MODEL = Translate.TranslateOption.model("nmt");

  public static void main(String[] args) {
    SpringApplication.run(SpringbootApplication.class, args);
  }

  @GetMapping("/")
  public String hello() {
    ByteString byteStr = ByteString.copyFrom(getSaltString(), StandardCharsets.UTF_8);
    PubsubMessage pubsubApiMessage = PubsubMessage.newBuilder().setData(byteStr).build();

    // Attempt to publish the message
    String responseMessage = "Hello Shabirmean is here!";
    try {
      Publisher publisher = Publisher.newBuilder(ProjectTopicName.of(PROJECT_ID, CF_PUBLISH_TOPIC)).build();
      publisher.publish(pubsubApiMessage).get();
    } catch (InterruptedException | ExecutionException | IOException e) {
      logger.log(Level.SEVERE, "Error publishing Pub/Sub message: " + e.getMessage(), e);
      responseMessage = "Error publishing Pub/Sub message; see logs for more info.";
    }
    return responseMessage;
  }

  @PostMapping(path = "/translate", consumes = "application/json", produces = "application/json")
  public ChatMessage translate(@RequestBody ChatMessage chatMessage) {
    Translation translation = translationService.translate(chatMessage.getMessage(),
        Translate.TranslateOption.sourceLanguage(chatMessage.getLocale()),
        Translate.TranslateOption.targetLanguage(TEXT_LOCALE_TAMIL), NMT_MODEL);
    String tranlatedMessage = translation.getTranslatedText();
    AudioMessage translatedAudioMessage = textToSpeech(tranlatedMessage, SPEECH_LOCALE_TAMIL);
    logger.log(Level.INFO, "Received message: " + chatMessage.toString());
    logger.log(Level.INFO, "Translated message with audio: " + tranlatedMessage);
    return new ChatMessage(tranlatedMessage, translatedAudioMessage, chatMessage.getLocale());
  }

  private AudioMessage textToSpeech(String textMessage, String language) {
    // Instantiates a client
    try (TextToSpeechClient textToSpeechClient = TextToSpeechClient.create()) {
      // Set the text input to be synthesized
      SynthesisInput input = SynthesisInput.newBuilder().setText(textMessage).build();

      // Build the voice request, select the language code ("en-US") and the ssml
      // voice gender
      // ("neutral")
      VoiceSelectionParams voice = VoiceSelectionParams.newBuilder().setLanguageCode(language)
          .setSsmlGender(SsmlVoiceGender.NEUTRAL).build();

      // Select the type of audio file you want returned
      AudioConfig audioConfig = AudioConfig.newBuilder().setAudioEncoding(AudioEncoding.MP3).build();

      // Perform the text-to-speech request on the text input with the selected voice
      // parameters and
      // audio file type
      SynthesizeSpeechResponse response = textToSpeechClient.synthesizeSpeech(input, voice, audioConfig);

      // Get the audio contents from the response
      ByteString audioContents = response.getAudioContent();
      return new AudioMessage(audioContents.toStringUtf8());
    } catch (IOException ex) {
      logger.log(Level.SEVERE, "Failed to convert text to audio: [" + textMessage + "]", ex);
      return new AudioMessage();
    }
  }

  private String getSaltString() {
    String SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
    StringBuilder salt = new StringBuilder();
    Random rnd = new Random();
    while (salt.length() < 18) { // length of the random string.
      int index = (int) (rnd.nextFloat() * SALTCHARS.length());
      salt.append(SALTCHARS.charAt(index));
    }
    String saltStr = salt.toString();
    return saltStr;
  }

}
// [END gae_java11_helloworld]
