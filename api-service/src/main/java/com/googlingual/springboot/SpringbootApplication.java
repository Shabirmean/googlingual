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
    public ChatMessage translate(@RequestBody ChatMessage message) {
        logger.log(Level.INFO, "Received message: " + message.toString());
        return message;
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
