package com.googlingual.springboot.api;

import com.google.cloud.pubsub.v1.Publisher;
import com.google.cloud.texttospeech.v1.ListVoicesResponse;
import com.google.cloud.texttospeech.v1.TextToSpeechClient;
import com.google.cloud.texttospeech.v1.Voice;
import com.google.cloud.translate.Language;
import com.google.cloud.translate.Translate;
import com.google.cloud.translate.TranslateOptions;
import com.google.gson.Gson;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.ProjectTopicName;
import com.google.pubsub.v1.PubsubMessage;
import com.googlingual.springboot.exception.GooglingualApiException;
import com.googlingual.springboot.sdk.ChatMessage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("v1")
@CrossOrigin(origins = "*")
public class TranslateApi {

  private static final Logger logger = Logger.getLogger(TranslateApi.class.getName());
  private static final String CF_PUBLISH_TOPIC = "hello-world-sub";
  private static final String PROJECT_ID = "gcloud-dpe";
  private static final String PROJECT_GCLOUD_DPE = "gcloud-dpe";
  private static final String NEW_UNTRANSLATED_MESSAGE = "new-untranslated-message";
  private static final Gson GSON = new Gson();
  private static TextToSpeechClient textToSpeechClient;
  private static final Translate translationService = TranslateOptions.getDefaultInstance().getService();
  private static final Publisher publisher = createPublisher();

  @PostMapping(path = "/send", consumes = "application/json")
  public void send(@RequestBody ChatMessage chatMessage) throws GooglingualApiException {
    String publishMessage = chatMessage.getJsonString();
    ByteString byteStr = ByteString.copyFrom(publishMessage, StandardCharsets.UTF_8);
    PubsubMessage pubsubApiMessage = PubsubMessage.newBuilder().setData(byteStr).build();
    try {
      logger.info("Publishing message: " + publishMessage);
      publisher.publish(pubsubApiMessage).get();
    } catch (InterruptedException | ExecutionException | NullPointerException e) {
      String errMsg = String.format("Failed to publish message from user [%s] to chatRoom [%s]",
          chatMessage.getAuthor().getId(), chatMessage.getRoomId());
      logger.log(Level.SEVERE, errMsg, e);
      throw new GooglingualApiException(errMsg + "\n\t" + e.getMessage());
    }
  }

  @GetMapping(path = "/locales", produces = "application/json")
  public String getAudioLocales() {
    List<Language> languages = translationService.listSupportedLanguages();
    return GSON.toJson(languages);
  }

  @GetMapping(path = "/audioLocales/{lang}", produces = "application/json")
  public String getLocales(@PathVariable String lang) throws GooglingualApiException {
    try {
      TextToSpeechClient client = getTextToSpeechClient();
      ListVoicesResponse langVoiceResponse = client.listVoices(lang);
      List<Voice> langVoices = langVoiceResponse.getVoicesList();
      Set<String> voiceCodes = new HashSet<>();
      for (Voice v: langVoices) {
        logger.info(String.format("Name [%s] and Gender [%s]", v.getName(), v.getSsmlGender().getDescriptorForType().getFullName()));
        voiceCodes.add(v.getName());
      }
      return GSON.toJson(voiceCodes);
    } catch (IOException e) {
      String errMsg = String.format("Failed to get voices for locale [%s]. The GCloud client failed", lang);
      logger.log(Level.SEVERE, errMsg, e);
      throw new GooglingualApiException(errMsg + "\n\t" + e.getMessage());
    }
  }

  private TextToSpeechClient getTextToSpeechClient() throws IOException {
    if (textToSpeechClient == null) {
      textToSpeechClient = TextToSpeechClient.create();
    }
    return textToSpeechClient;
  }

  private static Publisher createPublisher() {
    try {
      return Publisher.newBuilder(ProjectTopicName.of(PROJECT_GCLOUD_DPE, NEW_UNTRANSLATED_MESSAGE)).build();
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  @GetMapping("/hello")
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
