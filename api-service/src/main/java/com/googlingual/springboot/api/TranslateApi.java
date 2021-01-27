package com.googlingual.springboot.api;

import com.google.cloud.pubsub.v1.Publisher;
import com.google.cloud.texttospeech.v1.ListVoicesResponse;
import com.google.cloud.texttospeech.v1.TextToSpeechClient;
import com.google.cloud.texttospeech.v1.Voice;
import com.google.cloud.translate.Language;
import com.google.cloud.translate.TranslateOptions;
import com.google.gson.Gson;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.ProjectTopicName;
import com.google.pubsub.v1.PubsubMessage;
import com.googlingual.springboot.exception.GooglingualApiException;
import com.googlingual.springboot.sdk.ApiResponse;
import com.googlingual.springboot.sdk.Author;
import com.googlingual.springboot.sdk.ChatMessage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
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
  private static final String PROJECT_GCLOUD_DPE = System.getenv("GOOGLE_CLOUD_PROJECT");
  private static final String PUBLISH_UNTRANSLATED_MSG_TOPIC = System.getenv("PUBLISH_UNTRANSLATED_MSG_TOPIC");
  private static final Gson GSON = new Gson();
  private static Publisher publisher;

  @GetMapping("/hello")
  public String hello() {
    return "Hello Shabirmean is here!";
  }

  @PostMapping(path = "/send", consumes = "application/json")
  public void send(@RequestBody ChatMessage chatMessage) throws GooglingualApiException {
    String publishMessage = chatMessage.getJsonString();
    ByteString byteStr = ByteString.copyFrom(publishMessage, StandardCharsets.UTF_8);
    PubsubMessage pubsubApiMessage = PubsubMessage.newBuilder().setData(byteStr).build();
    try {
      logger.info("Publishing message: " + publishMessage);
      Publisher localPublisher = createPublisher(chatMessage.getAuthor());
      localPublisher.publish(pubsubApiMessage).get();
    } catch (InterruptedException | ExecutionException | NullPointerException e) {
      String errMsg = String.format("Failed to publish message from user [%s] to chatRoom [%s]",
          chatMessage.getAuthor().getId(), chatMessage.getRoomId());
      logger.log(Level.SEVERE, errMsg, e);
      throw new GooglingualApiException(errMsg + "\n\t" + e.getMessage());
    }
  }

  @GetMapping(path = "/locales", produces = "application/json")
  public String getLocales() {
    List<Language> languages = TranslateOptions.getDefaultInstance().getService().listSupportedLanguages();
    ApiResponse apiResponse = new ApiResponse("locales", languages);
    return GSON.toJson(apiResponse);
  }

  @GetMapping(path = "/audioLocales/{lang}", produces = "application/json")
  public String getAudioLocales(@PathVariable String lang) throws GooglingualApiException {
    try (TextToSpeechClient client = TextToSpeechClient.create()) {
      ListVoicesResponse langVoiceResponse = client.listVoices(lang);
      List<Voice> langVoices = langVoiceResponse.getVoicesList();
      Set<String> voiceCodes = new HashSet<>();
      langVoices.forEach(v -> {
        int langCodesSize = v.getLanguageCodesList().size();
        for (int i = 0; i < langCodesSize; i++) {
          voiceCodes.add(v.getLanguageCodes(i));
        }
      });
      ApiResponse apiResponse = new ApiResponse("audioLocales", new ArrayList<>(voiceCodes));
      return GSON.toJson(apiResponse);
    } catch (IOException e) {
      String errMsg = String.format("Failed to get voices for locale [%s]. The GCloud client failed", lang);
      logger.log(Level.SEVERE, errMsg, e);
      throw new GooglingualApiException(errMsg + "\n\t" + e.getMessage());
    }
  }

  private static Publisher createPublisher(Author author) throws GooglingualApiException {
    try {
      if (publisher == null) {
        publisher = Publisher.newBuilder(ProjectTopicName.of(PROJECT_GCLOUD_DPE, PUBLISH_UNTRANSLATED_MSG_TOPIC)).build();
      }
    } catch (IOException e) {
      String errMsg = String.format("Failed to create publisher to broadcast new message from [%s - %s]", author.getId(), author.getUsername());
      logger.log(Level.SEVERE, errMsg, e);
      throw new GooglingualApiException(errMsg + "\n\t" + e.getMessage());
    }
    return publisher;
  }
}
