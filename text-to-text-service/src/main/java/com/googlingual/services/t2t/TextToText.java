package com.googlingual.services.t2t;

import static com.googlingual.services.t2t.util.SqlConstants.INSERT_MESSAGE_QUERY;

import com.google.cloud.functions.BackgroundFunction;
import com.google.cloud.functions.Context;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import com.google.cloud.secretmanager.v1.SecretVersionName;
import com.google.cloud.translate.Translate;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.Translation;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.ProjectTopicName;
import com.google.pubsub.v1.PubsubMessage;
import com.googlingual.services.t2t.TextToText.PubSubMessage;
import com.googlingual.services.t2t.sdk.dao.MessageDao;
import com.googlingual.services.t2t.sdk.pubsub.PubSubExchangeMessage;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;

@SuppressWarnings("unused")
public class TextToText implements BackgroundFunction<PubSubMessage> {

  private static final Logger logger = Logger.getLogger(TextToText.class.getName());
  private static final String SECRET_VERSION = "latest";
  private static final String PROJECT_GCLOUD_DPE = System.getenv("GCP_PROJECT");
  private static final String PUBLISH_TEXT_TO_SPEECH_MSG_TOPIC = System.getenv("PUBLISH_TEXT_TO_SPEECH_MSG_TOPIC");
  private static final String PUBLISH_TEXT_TRANSLATED_MSG_TOPIC = System.getenv("PUBLISH_TRANSLATED_MSG_TOPIC");
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

  private static Publisher getPublisher(String topic) throws IOException {
    Publisher publisher = publisherMap.get(topic);
    if (publisher == null) {
      publisher = Publisher.newBuilder(ProjectTopicName.of(PROJECT_GCLOUD_DPE, topic)).build();
      publisherMap.put(topic, publisher);
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

  @Override
  public void accept(PubSubMessage pubSubMessage, Context context) {
    String receivedData = pubSubMessage.data != null ? pubSubMessage.data : "Received no data";
    logger.info("Received: " + receivedData);
    receivedData = new String(Base64.getDecoder().decode(pubSubMessage.data));
    PubSubExchangeMessage exchangeMessage = PubSubExchangeMessage.fromJsonString(receivedData);
    logger.info(exchangeMessage.toString());

    MessageDao messageDao = exchangeMessage.getMessage();
    String messageToTranslate = messageDao.getMessage();
    String sourceLocale = messageDao.getMessageLocale();
    String destinationLocale = exchangeMessage.getDestinationLocale();
    if (sourceLocale.equals(destinationLocale)) {
      logger.info(String.format(
          "Skipping text translation for source language [%s]. Directly forwarding to delivery and audio services", sourceLocale));
      MessageDao updatedDao = MessageDao.fromPubSubExchange(exchangeMessage, destinationLocale, messageToTranslate);
      publishTranslatedMessage(new PubSubExchangeMessage(updatedDao, destinationLocale));
      exchangeMessage.getAudioDestinationLocales()
          .forEach(al -> logger.info(String.format("Invoking text-to-speech for source lang [%s] with audio lang [%s]", sourceLocale, al)));
      for (String audioLocale: exchangeMessage.getAudioDestinationLocales()) {
        forwardToTextToSpeechService(messageDao, audioLocale);
        logger.info(String.format("Published T2S translation request for audio lang [%s]", audioLocale));
      }
      return;
    }
    logger.info(String.format("Translation pair %s --> %s", sourceLocale, destinationLocale));
    Translate translationService = TranslateOptions.getDefaultInstance().getService();
    Translation translation = translationService.translate(messageToTranslate,
        Translate.TranslateOption.sourceLanguage(sourceLocale),
        Translate.TranslateOption.targetLanguage(destinationLocale),
        Translate.TranslateOption.model("nmt"));
    String translatedMessage = translation.getTranslatedText();
    logger.log(Level.INFO,
        String.format("Translated message [%s: %s] to [%s: %s]",
            sourceLocale, messageToTranslate, destinationLocale, translatedMessage));

    Connection connection = null;
    try {
      connection = getConnection();
      connection.setAutoCommit(false);

      PreparedStatement insertMessageStmt = connection.prepareStatement(INSERT_MESSAGE_QUERY);
      MessageDao updatedDao = MessageDao.fromPubSubExchange(exchangeMessage, destinationLocale, translatedMessage);
      insertMessageStmt.setString(1, updatedDao.getId().toString());
      insertMessageStmt.setBoolean(2, updatedDao.isAudio());
      insertMessageStmt.setString(3, updatedDao.getMessageLocale());
      insertMessageStmt.setString(4, updatedDao.getMessage());
      insertMessageStmt.setString(5, updatedDao.getAudioLocale());
      insertMessageStmt.setString(6, updatedDao.getAudioMessage());
      insertMessageStmt.setInt(7, updatedDao.getMessageIndex());
      insertMessageStmt.setString(8, updatedDao.getChatRoomId().toString());
      insertMessageStmt.setString(9, updatedDao.getSender().toString());
      insertMessageStmt.execute();
      connection.commit();
      insertMessageStmt.close();
      logger.info(String.format("Inserted translated text message [id: %s]", updatedDao.getId()));
      publishTranslatedMessage(new PubSubExchangeMessage(updatedDao, destinationLocale));
      for (String audioLocale: exchangeMessage.getAudioDestinationLocales()) {
        forwardToTextToSpeechService(updatedDao, audioLocale);
        logger.info(String.format("Published T2S translation request for audio lang [%s]", audioLocale));
      }
    } catch (Exception ex) {
      ex.printStackTrace();
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

  private void forwardToTextToSpeechService(MessageDao messageDao,  String audioLocale) {
    PubSubExchangeMessage exchangeMessage = new PubSubExchangeMessage(messageDao, audioLocale);
    String publishMessage = exchangeMessage.getJsonString();
    ByteString byteStr = ByteString.copyFrom(publishMessage, StandardCharsets.UTF_8);
    PubsubMessage pubsubApiMessage = PubsubMessage.newBuilder().setData(byteStr).build();
    try {
      Publisher publisher = getPublisher(PUBLISH_TEXT_TO_SPEECH_MSG_TOPIC);
      publisher.publish(pubsubApiMessage).get();
    } catch (IOException | InterruptedException | ExecutionException e) {
      e.printStackTrace();
    }
  }

  private void publishTranslatedMessage(PubSubExchangeMessage exchangeMessage) {
    String publishMessage = exchangeMessage.getJsonString();
    ByteString byteStr = ByteString.copyFrom(publishMessage, StandardCharsets.UTF_8);
    PubsubMessage pubsubApiMessage = PubsubMessage.newBuilder().setData(byteStr).build();
    try {
      Publisher publisher = getPublisher(PUBLISH_TEXT_TRANSLATED_MSG_TOPIC);
      publisher.publish(pubsubApiMessage).get();
      logger.info(String.format("Published translated TEXT message [%s]",
          exchangeMessage.getMessage().getMessage()));
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
