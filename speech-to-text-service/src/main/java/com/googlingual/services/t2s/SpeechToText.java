package com.googlingual.services.t2s;

import static com.googlingual.services.t2s.util.SqlConstants.UPDATE_MESSAGE_QUERY;

import com.google.cloud.functions.BackgroundFunction;
import com.google.cloud.functions.Context;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.cloud.speech.v1.RecognitionAudio;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.RecognitionConfig.AudioEncoding;
import com.google.cloud.speech.v1.RecognizeResponse;
import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.speech.v1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1.SpeechRecognitionResult;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.ProjectTopicName;
import com.google.pubsub.v1.PubsubMessage;
import com.googlingual.services.t2s.SpeechToText.PubSubMessage;
import com.googlingual.services.t2s.sdk.dao.MessageDao;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;

public class SpeechToText implements BackgroundFunction<PubSubMessage> {

  private static final Logger logger = Logger.getLogger(SpeechToText.class.getName());
  private static final String DB_NAME = "googlingual";
  private static final String CLOUD_SQL_CONNECTION_NAME = "gcloud-dpe:us-central1:gsp-shabirmean";
  private static final String DB_USER = "root";
  private static final String DB_PASS = "7o0fafvczzmFl8Lg";
  private static final String PROJECT_GCLOUD_DPE = "gcloud-dpe";
  private static final int DEFAULT_SQL_POOL_SIZE = 10;
  private static DataSource pool = null;
  private static final Map<String, Publisher> publisherMap = new HashMap<>();

  private static Publisher getPublisher(String topic) throws IOException {
    Publisher publisher = publisherMap.get(topic);
    if (publisher == null) {
      publisher = Publisher.newBuilder(ProjectTopicName.of(PROJECT_GCLOUD_DPE, topic)).build();
      publisherMap.put(topic, publisher);
    }
    return publisher;
  }

  private static Connection getConnection(int poolSize) throws SQLException {
    if (pool == null) {
      HikariConfig config = new HikariConfig();
      config.setJdbcUrl(String.format("jdbc:mysql:///%s", DB_NAME));
      config.setUsername(DB_USER);
      config.setPassword(DB_PASS);
      config.addDataSourceProperty("socketFactory", "com.google.cloud.sql.mysql.SocketFactory");
      config.addDataSourceProperty("cloudSqlInstance", CLOUD_SQL_CONNECTION_NAME);
      config.addDataSourceProperty("ipTypes", "PRIVATE");
      config.setMaximumPoolSize(poolSize);
      pool = new HikariDataSource(config);
    }
    return pool.getConnection();
  }

  @Override
  public void accept(PubSubMessage pubSubMessage, Context context) {
    String receivedData = pubSubMessage.data != null ? pubSubMessage.data : "Received no data";
    logger.info("Received encoded audio message: " + receivedData);
    receivedData = new String(Base64.getDecoder().decode(pubSubMessage.data));
    MessageDao exchangeMessage = MessageDao.fromJsonString(receivedData);
    logger.info("Decoded Audio message: " + exchangeMessage.toString());

    Connection connection = null;
    String encodedAudio = exchangeMessage.getAudioMessage();
    String audioLocale = exchangeMessage.getAudioLocale();
    String textLocale = audioLocale.split("-")[0];
    UUID messageId = exchangeMessage.getId();
    // Instantiates a client
    try (SpeechClient speechClient = SpeechClient.create()) {
      byte[] audioBytes = Base64.getDecoder().decode(encodedAudio);
      ByteString byteContent = ByteString.copyFrom(audioBytes);
      RecognitionConfig config = RecognitionConfig.newBuilder()
          .setEncoding(AudioEncoding.LINEAR16)
          .setLanguageCode(audioLocale)
          .build();
      RecognitionAudio audio = RecognitionAudio.newBuilder().setContent(byteContent).build();
      RecognizeResponse response = speechClient.recognize(config, audio);
      List<SpeechRecognitionResult> results = response.getResultsList();
      for (SpeechRecognitionResult result : results) {
        SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
        logger.info(String.format("Transcription: %s%n", alternative.getTranscript()));
      }
      String transcribedText = results.get(0).getAlternativesList().get(0).getTranscript();

      connection = getConnection(DEFAULT_SQL_POOL_SIZE);
      connection.setAutoCommit(false);
      PreparedStatement updateMessageQuery = connection.prepareStatement(UPDATE_MESSAGE_QUERY);
      updateMessageQuery.setString(1, transcribedText);
      updateMessageQuery.setString(2, textLocale);
      updateMessageQuery.setString(3, messageId.toString());
      updateMessageQuery.executeUpdate();
      connection.commit();
      updateMessageQuery.close();
      logger.info(
          String.format("Inserted transcribed text of audio message [id: %s]\n[lang: %s]\n[%s]",
              messageId, textLocale, transcribedText));
      logger.info(String.format(
          "Setting audio message [false] for transcribed message [%s] and re-publishing", messageId));
      exchangeMessage.setAudio(false);
      publishTranscribedMessage(exchangeMessage);
    } catch (IOException | SQLException ex) {
      logger.log(Level.SEVERE, "Failed to convert audio to text: [" + messageId + "]", ex);
    } finally {
      try {
        if (connection != null) {
          connection.rollback();
          connection.close();
        }
      } catch (SQLException se) {
        se.printStackTrace();
      }
    }
  }

  private void publishTranscribedMessage(MessageDao messageDao) {
    String publishMessage = messageDao.getJsonString();
    ByteString byteStr = ByteString.copyFrom(publishMessage, StandardCharsets.UTF_8);
    PubsubMessage pubsubApiMessage = PubsubMessage.newBuilder().setData(byteStr).build();
    try {
      Publisher publisher = getPublisher("new-stored-message");
      publisher.publish(pubsubApiMessage).get();
    } catch (IOException | InterruptedException | ExecutionException e) {
      e.printStackTrace();
    }
  }

  public static class PubSubMessage {
    String data;
    Map<String, String> attributes;
    String messageId;
    String publishTime;
  }
}
