package com.googlingual.services.t2t;

import static com.googlingual.services.t2t.util.SqlConstants.GET_LOCALES_QUERY;
import static com.googlingual.services.t2t.util.SqlConstants.INSERT_MESSAGE_QUERY;
import static com.googlingual.services.t2t.util.SqlConstants.SELECT_MESSAGE_QUERY;

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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.apache.commons.lang3.StringUtils;

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

  private MessageDao loadMessage(String messageId) {
    Connection connection = null;
    MessageDao messageDao = null;
    try {
      String query = String.format(SELECT_MESSAGE_QUERY, messageId);
      connection = getConnection();
      Statement sqlStatement = connection.createStatement();
      ResultSet resultSet = sqlStatement.executeQuery(query);
      while (resultSet.next()) {
        String message = resultSet.getString("message");
        String messageLocale = resultSet.getString("message_locale");
        String chatroomId = resultSet.getString("chatroom_id");
        String sender = resultSet.getString("sender");
        int messageIndex = resultSet.getInt("msg_index");
        messageDao = new MessageDao();
        messageDao.setMessage(message);
        messageDao.setMessageLocale(messageLocale);
        messageDao.setChatRoomId(UUID.fromString(chatroomId));
        messageDao.setSender(sender);
        messageDao.setMessageIndex(messageIndex);
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

  private Set<String> loadAudioLocales(String textLocale, String chatroomId) {
    Connection connection = null;
    Set<String> localeSet = new HashSet<>();
    try {
      String query = String.format(GET_LOCALES_QUERY, chatroomId, textLocale);
      connection = getConnection();
      Statement sqlStatement = connection.createStatement();
      ResultSet resultSet = sqlStatement.executeQuery(query);
      while (resultSet.next()) {
        String audioLocale = resultSet.getString("audio_locale");
        localeSet.add(audioLocale);
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
    return localeSet;
  }

  @Override
  public void accept(PubSubMessage pubSubMessage, Context context) {
    if (pubSubMessage.data == null || StringUtils.isBlank(pubSubMessage.data)) {
      logger.warning("Received pubsub message with null/blank message id.");
      return;
    }
    String receivedMessage = new String(Base64.getDecoder().decode(pubSubMessage.data));
    logger.info("Processing message: " + receivedMessage);

    String[] messageParts = receivedMessage.split("::");
    if (messageParts.length != 2) {
      logger.log(Level.SEVERE,
          String.format("Invalid message [%s]. Should be in format [<message>::<locale>]", receivedMessage));
      return;
    }

    String messageId = messageParts[0];
    String destinationLocale = messageParts[1];
    MessageDao loadedMessage = loadMessage(messageId);
    if (loadedMessage == null) {
      logger.log(Level.SEVERE, String.format("Failed to load message info for id [%s]", messageId));
      return;
    }

    String sourceLocale = loadedMessage.getMessageLocale();
    String messageToTranslate = loadedMessage.getMessage();
    String chatroomId = loadedMessage.getChatRoomId().toString();
    Set<String> audioLocales = loadAudioLocales(destinationLocale, chatroomId);
    if (sourceLocale.equals(destinationLocale)) {
      logger.info(String.format(
          "Skipping text translation for source language [%s]. Directly forwarding to delivery and audio services", sourceLocale));
      audioLocales.forEach(al -> logger.info(
          String.format("Invoking text-to-speech for source lang [%s] with audio lang [%s]", sourceLocale, al)));
      forwardMessage(messageId, audioLocales);
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
      String newMessageId = UUID.randomUUID().toString();
      String query = String.format(INSERT_MESSAGE_QUERY,
          newMessageId,
          0,
          destinationLocale,
          translatedMessage,
          loadedMessage.getMessageIndex(),
          chatroomId,
          loadedMessage.getSender());
      Statement insertMessageStmt = connection.createStatement();
      insertMessageStmt.execute(query);
      insertMessageStmt.close();
      logger.info(String.format("Inserted translated text message [id: %s]", newMessageId));
      forwardMessage(newMessageId, audioLocales);
    } catch (Exception ex) {
      logger.log(Level.SEVERE, "Failed to translate text to text: [" + messageId + "]", ex);
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

  private void forwardMessage(String messageId, Set<String> audioLocales) {
    for (String audioLocale: audioLocales) {
      forwardToTextToSpeechService(messageId, audioLocale);
      logger.info(String.format("Published T2S translation request for audio lang [%s]", audioLocale));
    }
  }

  private void forwardToTextToSpeechService(String messageId,  String audioLocale) {
    String publishMessage = String.format("%s::%s", messageId, audioLocale);
    ByteString byteStr = ByteString.copyFrom(publishMessage, StandardCharsets.UTF_8);
    PubsubMessage pubsubApiMessage = PubsubMessage.newBuilder().setData(byteStr).build();
    try {
      Publisher publisher = getPublisher(PUBLISH_TEXT_TO_SPEECH_MSG_TOPIC);
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
