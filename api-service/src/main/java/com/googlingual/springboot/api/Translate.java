package com.googlingual.springboot.api;

import com.google.cloud.pubsub.v1.Publisher;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.ProjectTopicName;
import com.google.pubsub.v1.PubsubMessage;
import com.googlingual.springboot.exception.GooglingualApiException;
import com.googlingual.springboot.sdk.ChatMessage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("v1")
@CrossOrigin(origins = "*")
public class Translate {
  private static final Logger logger = Logger.getLogger(Translate.class.getName());
  private static final String CF_PUBLISH_TOPIC = "hello-world-sub";
  private static final String PROJECT_ID = "gcloud-dpe";
  Publisher publisher = createPublisher();

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

  private static Publisher createPublisher() {
    try {
      return Publisher.newBuilder(ProjectTopicName.of("gcloud-dpe", "new-untranslated-message")).build();
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
