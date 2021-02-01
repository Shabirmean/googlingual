package com.googlingual.services.s2t;

import static com.googlingual.services.s2t.util.SqlConstants.SELECT_MESSAGE_QUERY;
import static com.googlingual.services.s2t.util.SqlConstants.UPDATE_MESSAGE_QUERY;

import com.google.cloud.functions.BackgroundFunction;
import com.google.cloud.functions.Context;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import com.google.cloud.secretmanager.v1.SecretVersionName;
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
import com.googlingual.services.s2t.SpeechToText.PubSubMessage;
import com.googlingual.services.s2t.sdk.dao.MessageDao;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.apache.commons.lang3.StringUtils;

@SuppressWarnings("unused")
public class SpeechToText implements BackgroundFunction<PubSubMessage> {

  private static final Logger logger = Logger.getLogger(SpeechToText.class.getName());
  private static final String SECRET_VERSION = "latest";
  private static final String PROJECT_GCLOUD_DPE = System.getenv("GCP_PROJECT");
  private static final String PUBLISH_STORED_MSG_TOPIC = System.getenv("PUBLISH_STORED_MSG_TOPIC");
  private static final String DB_USER_SECRET_KEY = System.getenv("DB_USER_SECRET_KEY");
  private static final String DB_PASS_SECRET_KEY = System.getenv("DB_PASS_SECRET_KEY");
  private static final String DB_NAME_SECRET_KEY = System.getenv("DB_NAME_SECRET_KEY");
  private static final String DB_CONNECTION_SECRET_KEY = System.getenv("DB_CONNECTION_SECRET_KEY");
  private static final String GOOGLE_MYSQL_CONNECTOR_FACTORY = "com.google.cloud.sql.mysql.SocketFactory";
  private static final String PRIVATE_NET = "PRIVATE";
  private static final int DEFAULT_SQL_POOL_SIZE = 10;
  private static final Map<String, Publisher> publisherMap = new HashMap<>();

  private static String DB_USER;
  private static String DB_PASS;
  private static String DB_NAME;
  private static String DB_CONNECTION_NAME;
  private static DataSource pool = null;

  private static Publisher getPublisher() throws IOException {
    Publisher publisher = publisherMap.get(PUBLISH_STORED_MSG_TOPIC);
    if (publisher == null) {
      publisher = Publisher.newBuilder(ProjectTopicName.of(PROJECT_GCLOUD_DPE, PUBLISH_STORED_MSG_TOPIC)).build();
      publisherMap.put(PUBLISH_STORED_MSG_TOPIC, publisher);
    }
    return publisher;
  }

  private void loadDbCredentials() {
    try (SecretManagerServiceClient client = SecretManagerServiceClient.create()) {
      SecretVersionName dbUserSecret = SecretVersionName.of(PROJECT_GCLOUD_DPE, DB_USER_SECRET_KEY, SECRET_VERSION);
      SecretVersionName dbPasswordSecret = SecretVersionName.of(PROJECT_GCLOUD_DPE, DB_PASS_SECRET_KEY, SECRET_VERSION);
      SecretVersionName dbNameSecret = SecretVersionName.of(PROJECT_GCLOUD_DPE, DB_NAME_SECRET_KEY, SECRET_VERSION);
      SecretVersionName dbConnectionSecret = SecretVersionName.of(PROJECT_GCLOUD_DPE, DB_CONNECTION_SECRET_KEY, SECRET_VERSION);
      DB_USER = client.accessSecretVersion(dbUserSecret).getPayload().getData().toStringUtf8();
      DB_PASS = client.accessSecretVersion(dbPasswordSecret).getPayload().getData().toStringUtf8();
      DB_NAME = client.accessSecretVersion(dbNameSecret).getPayload().getData().toStringUtf8();
      DB_CONNECTION_NAME = client.accessSecretVersion(dbConnectionSecret).getPayload().getData().toStringUtf8();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private Connection getConnection() throws SQLException {
    if (pool == null) {
      loadDbCredentials();
      HikariConfig config = new HikariConfig();
      config.setJdbcUrl(String.format("jdbc:mysql:///%s", DB_NAME));
      config.setUsername(DB_USER);
      config.setPassword(DB_PASS);
      config.addDataSourceProperty("socketFactory", GOOGLE_MYSQL_CONNECTOR_FACTORY);
      config.addDataSourceProperty("cloudSqlInstance", DB_CONNECTION_NAME);
      config.addDataSourceProperty("ipTypes", PRIVATE_NET);
      config.setMaximumPoolSize(DEFAULT_SQL_POOL_SIZE);
      pool = new HikariDataSource(config);
    }
    return pool.getConnection();
  }

  private MessageDao loadMessage(String messageId) {
    Connection connection = null;
    MessageDao messageDao = null;
    try {
      String query = String.format(SELECT_MESSAGE_QUERY, messageId);
      connection = getConnection();
      Statement sqlStatement = connection.createStatement();
      ResultSet resultSet = sqlStatement.executeQuery(query);
      while (resultSet.next()) {
        String audioMessage = resultSet.getString("audio_message");
        String audioLocale = resultSet.getString("audio_locale");
        messageDao = new MessageDao();
        messageDao.setId(UUID.fromString(messageId));
        messageDao.setAudioMessage(audioMessage);
        messageDao.setAudioLocale(audioLocale);
      }
      resultSet.close();
      sqlStatement.close();
    } catch (SQLException throwables) {
      throwables.printStackTrace();
    } finally {
      try {
        if (connection != null) {
          connection.close();
        }
      } catch (SQLException se) {
        se.printStackTrace();
      }
    }
    return messageDao;
  }

  @Override
  public void accept(PubSubMessage pubSubMessage, Context context) {
    if (pubSubMessage.data == null || StringUtils.isBlank(pubSubMessage.data)) {
      logger.warning("Received pubsub message with null/blank message id.");
      return;
    }
    String messageId = new String(Base64.getDecoder().decode(pubSubMessage.data));
    logger.info("Processing message id: " + messageId);

    Connection connection = null;
    MessageDao messageDao = loadMessage(messageId);
    String encodedAudio = messageDao.getAudioMessage();
    String audioLocale = messageDao.getAudioLocale();
    String textLocale = audioLocale.split("-")[0];
    // Instantiates a client
    try {
      String transcribedText = transcribeMessage(encodedAudio, audioLocale);
      connection = getConnection();
      String query = String.format(UPDATE_MESSAGE_QUERY, transcribedText, textLocale, false, messageId);
      Statement updateMessageQuery = connection.createStatement();
      updateMessageQuery.executeUpdate(query);
      updateMessageQuery.close();
      logger.info(
          String.format("Inserted transcribed text of audio message [id: %s]\n[lang: %s]\n[%s]",
              messageId, textLocale, transcribedText));
      logger.info(String.format(
          "Setting audio message [false] for transcribed message [%s] and re-publishing", messageId));
      publishTranscribedMessage(messageId);
    } catch (IOException | SQLException ex) {
      logger.log(Level.SEVERE, "Failed to convert audio to text: [" + messageId + "]", ex);
      try {
        if (connection != null) {
          connection.rollback();
        }
      } catch (SQLException se) {
        se.printStackTrace();
      }
    } finally {
      try {
        if (connection != null) {
          connection.close();
        }
      } catch (SQLException se) {
        se.printStackTrace();
      }
    }
  }

  private String transcribeMessage(String encodedAudio, String audioLocale) throws IOException {
    SpeechClient speechClient = SpeechClient.create();
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
    speechClient.close();
    return results.get(0).getAlternativesList().get(0).getTranscript();
  }

  private void publishTranscribedMessage(String messageId) {
    ByteString byteStr = ByteString.copyFrom(messageId, StandardCharsets.UTF_8);
    PubsubMessage pubsubApiMessage = PubsubMessage.newBuilder().setData(byteStr).build();
    try {
      Publisher publisher = getPublisher();
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
