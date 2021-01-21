package com.googlingual.services.t2t;

import static com.googlingual.services.t2t.util.SqlConstants.INSERT_MESSAGE_QUERY;

import com.google.cloud.pubsub.v1.Publisher;
import com.google.cloud.translate.Translate;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.Translation;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.ProjectTopicName;
import com.google.pubsub.v1.PubsubMessage;
import com.googlingual.services.t2t.TextToText.PubSubMessage;
import com.google.cloud.functions.BackgroundFunction;
import com.google.cloud.functions.Context;
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
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;

public class TextToText implements BackgroundFunction<PubSubMessage> {
  private static final Logger logger = Logger.getLogger(TextToText.class.getName());
  private static final String DB_NAME = "googlingual";
  private static final String CLOUD_SQL_CONNECTION_NAME = "gcloud-dpe:us-central1:gsp-shabirmean";
  private static final String DB_USER = "root";
  private static final String DB_PASS = "7o0fafvczzmFl8Lg";
  private static final int DEFAULT_SQL_POOL_SIZE = 10;
  private static DataSource pool = null;

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
    PubSubExchangeMessage exchangeMessage = PubSubExchangeMessage.fromJsonString(receivedData);
    logger.info(exchangeMessage.toString());

    MessageDao messageDao = exchangeMessage.getMessage();
    String messageToTranslate = messageDao.getMessage();
    String sourceLocale = messageDao.getMessageLocale();
    String destinationLocale = exchangeMessage.getDestinationLocale();
    if (sourceLocale.equals(destinationLocale)) {
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
      connection = getConnection(DEFAULT_SQL_POOL_SIZE);
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

      for (String audioLocale: exchangeMessage.getAudioDestinationLocales()) {
        PubSubExchangeMessage newExchangeMessage = new PubSubExchangeMessage(updatedDao, audioLocale);
        forwardToTextToSpeechService(newExchangeMessage);
        logger.info(String.format("Published T2S translation request for audio lang [%s]", audioLocale));
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

  private void forwardToTextToSpeechService(PubSubExchangeMessage exchangeMessage) {
    String publishMessage = exchangeMessage.getJsonString();
    ByteString byteStr = ByteString.copyFrom(publishMessage, StandardCharsets.UTF_8);
    PubsubMessage pubsubApiMessage = PubsubMessage.newBuilder().setData(byteStr).build();
    try {
      Publisher publisher = Publisher.newBuilder(
          ProjectTopicName.of("gcloud-dpe", "new-text-to-speech-message")).build();
      publisher.publish(pubsubApiMessage).get();
      publisher.shutdown();
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
