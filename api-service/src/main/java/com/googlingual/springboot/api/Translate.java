package com.googlingual.springboot.api;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;
import java.util.logging.Level;

import com.google.cloud.pubsub.v1.Publisher;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.ProjectTopicName;
import com.google.pubsub.v1.PubsubMessage;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("v1")
@CrossOrigin(origins = "*")
public class Translate {
  private static final Logger logger = Logger.getLogger(Translate.class.getName());
  private static final String CF_PUBLISH_TOPIC = "hello-world-sub";
  private static final String PROJECT_ID = "gcloud-dpe";

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
