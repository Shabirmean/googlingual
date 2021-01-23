package com.googlingual.services.translation;

import static com.googlingual.services.translation.util.SqlConstants.GET_LOCALES_QUERY;

import com.google.cloud.functions.BackgroundFunction;
import com.google.cloud.functions.Context;
import com.google.cloud.pubsub.v1.Publisher;
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

public class InitTranslation implements BackgroundFunction<PubSubMessage> {

  private static final Logger logger = Logger.getLogger(InitTranslation.class.getName());
  private static final String DB_NAME = "googlingual";
  private static final String CLOUD_SQL_CONNECTION_NAME = "gcloud-dpe:us-central1:gsp-shabirmean";
  private static final String DB_USER = "root";
  private static final String DB_PASS = "7o0fafvczzmFl8Lg";
  private static final int DEFAULT_SQL_POOL_SIZE = 10;
  private static final String PROJECT_GCLOUD_DPE = "gcloud-dpe";
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
      connection = getConnection(DEFAULT_SQL_POOL_SIZE);
      connection.setAutoCommit(false);
      PreparedStatement getMessageLocaleStmt = connection.prepareStatement(GET_LOCALES_QUERY);
      getMessageLocaleStmt.setString(1, messageDao.getChatRoomId().toString());
      ResultSet rs = getMessageLocaleStmt.executeQuery();
      Map<String, Set<String>> textToSpeechLocales = new HashMap<>();

      while (rs.next()) {
        String msgLocale = rs.getString("message_locale");
        String audLocale = rs.getString("audio_locale");
        textToSpeechLocales.computeIfAbsent(msgLocale, ml -> new HashSet<>());
        String[] audLocaleParts = audLocale.split("-");
        if (audLocaleParts.length > 0) {
          Set<String> audSet = textToSpeechLocales.computeIfAbsent(audLocaleParts[0], (k) -> new HashSet<>());
          audSet.add(audLocale);
        }
      }
      connection.commit();
      rs.close();
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

  private void forwardToSpeechToTextService(MessageDao messageDao) {
    String publishMessage = messageDao.getJsonString();
    ByteString byteStr = ByteString.copyFrom(publishMessage, StandardCharsets.UTF_8);
    PubsubMessage pubsubApiMessage = PubsubMessage.newBuilder().setData(byteStr).build();
    try {
      Publisher publisher = getPublisher("new-speech-to-text-message");
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
      Publisher publisher = getPublisher("new-text-to-text-message");
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
