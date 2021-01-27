package com.googlingual.services.translation;

import static com.googlingual.services.translation.util.SqlConstants.GET_LOCALES_QUERY;

import com.google.cloud.functions.BackgroundFunction;
import com.google.cloud.functions.Context;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import com.google.cloud.secretmanager.v1.SecretVersionName;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.ProjectTopicName;
import com.google.pubsub.v1.PubsubMessage;
import com.googlingual.services.translation.InitTranslation.PubSubMessage;
import com.googlingual.services.translation.sdk.dao.MessageDao;
import com.googlingual.services.translation.sdk.pubsub.PubSubExchangeMessage;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;
import javax.sql.DataSource;

@SuppressWarnings("unused")
public class InitTranslation implements BackgroundFunction<PubSubMessage> {

  private static final Logger logger = Logger.getLogger(InitTranslation.class.getName());
  private static final String SECRET_VERSION = "latest";
  private static final String PROJECT_GCLOUD_DPE = System.getenv("GOOGLE_CLOUD_PROJECT");
  private static final String PUBLISH_SPEECH_TO_TEXT_MSG_TOPIC = System.getenv("PUBLISH_SPEECH_TO_TEXT_MSG_TOPIC");
  private static final String PUBLISH_TEXT_TO_TEXT_MSG_TOPIC = System.getenv("PUBLISH_TEXT_TO_TEXT_MSG_TOPIC");
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
    MessageDao messageDao = MessageDao.fromJsonString(receivedData);
    logger.info(messageDao.toString());

    if (messageDao.isAudio()) {
      forwardToSpeechToTextService(messageDao);
      return;
    }

    Connection connection = null;
    try {
      connection = getConnection();
      connection.setAutoCommit(false);
      PreparedStatement getMessageLocaleStmt = connection.prepareStatement(GET_LOCALES_QUERY);
      getMessageLocaleStmt.setString(1, messageDao.getChatRoomId().toString());
      ResultSet resultsSet = getMessageLocaleStmt.executeQuery();
      Map<String, Set<String>> textToSpeechLocales = new HashMap<>();

      while (resultsSet.next()) {
        String msgLocale = resultsSet.getString("message_locale");
        String audLocale = resultsSet.getString("audio_locale");
        textToSpeechLocales.computeIfAbsent(msgLocale, ml -> new HashSet<>());
        String[] audLocaleParts = audLocale.split("-");
        if (audLocaleParts.length > 0) {
          Set<String> audSet = textToSpeechLocales.computeIfAbsent(audLocaleParts[0], (k) -> new HashSet<>());
          audSet.add(audLocale);
        }
      }
      connection.commit();
      resultsSet.close();
      getMessageLocaleStmt.close();

      for (String locale: textToSpeechLocales.keySet()) {
        Set<String> audLocals = textToSpeechLocales.get(locale);
        PubSubExchangeMessage exchangeMessage = new PubSubExchangeMessage(messageDao, locale, audLocals);
        forwardToTextToTextService(exchangeMessage);
        StringBuilder audLocaleCollector = new StringBuilder();
        audLocals.forEach(audLocaleCollector::append);
        logger.info(String.format("Published T2T translation request for lang [%s] with audio-set [%s]", locale, audLocaleCollector));
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

  private void forwardToSpeechToTextService(MessageDao messageDao) {
    String publishMessage = messageDao.getJsonString();
    ByteString byteStr = ByteString.copyFrom(publishMessage, StandardCharsets.UTF_8);
    PubsubMessage pubsubApiMessage = PubsubMessage.newBuilder().setData(byteStr).build();
    try {
      Publisher publisher = getPublisher(PUBLISH_SPEECH_TO_TEXT_MSG_TOPIC);
      publisher.publish(pubsubApiMessage).get();
    } catch (IOException | InterruptedException | ExecutionException e) {
      e.printStackTrace();
    }
  }

  private void forwardToTextToTextService(PubSubExchangeMessage pubSubExchangeMessage) {
    String publishMessage = pubSubExchangeMessage.getJsonString();
    ByteString byteStr = ByteString.copyFrom(publishMessage, StandardCharsets.UTF_8);
    PubsubMessage pubsubApiMessage = PubsubMessage.newBuilder().setData(byteStr).build();
    try {
      Publisher publisher = getPublisher(PUBLISH_TEXT_TO_TEXT_MSG_TOPIC);
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
