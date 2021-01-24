package com.googlingual.services.storage;

import static com.googlingual.services.storage.util.SqlConstants.INSERT_MESSAGE_QUERY;
import static com.googlingual.services.storage.util.SqlConstants.LAST_INDEX_QUERY;

import com.google.cloud.functions.BackgroundFunction;
import com.google.cloud.functions.Context;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.ProjectTopicName;
import com.google.pubsub.v1.PubsubMessage;
import com.googlingual.services.storage.InitMessage.PubSubMessage;
import com.googlingual.services.storage.sdk.dao.MessageDao;
import com.googlingual.services.storage.sdk.message.ChatMessage;
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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;
import javax.sql.DataSource;

public class InitMessage implements BackgroundFunction<PubSubMessage> {

  private static final Logger logger = Logger.getLogger(InitMessage.class.getName());
  private static final String DB_NAME = "googlingual";
  private static final String CLOUD_SQL_CONNECTION_NAME = "gcloud-dpe:us-central1:gsp-shabirmean";
  private static final String DB_USER = "root";
  private static final String DB_PASS = "7o0fafvczzmFl8Lg";
  private static final String LOCALE_EN = "en";
  private static final String AUDIO_EN_US = "en-US";
  private static final String MSG_INDEX_CLOUMN = "msg_index";
  private static final String NEW_STORED_MESSAGE_TOPIC = "new-stored-message";
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

  private static Connection getConnection() throws SQLException {
    if (pool == null) {
      HikariConfig config = new HikariConfig();
      config.setJdbcUrl(String.format("jdbc:mysql:///%s", DB_NAME));
      config.setUsername(DB_USER);
      config.setPassword(DB_PASS);
      config.addDataSourceProperty("socketFactory", "com.google.cloud.sql.mysql.SocketFactory");
      config.addDataSourceProperty("cloudSqlInstance", CLOUD_SQL_CONNECTION_NAME);
      config.addDataSourceProperty("ipTypes", "PRIVATE");
      config.setMaximumPoolSize(DEFAULT_SQL_POOL_SIZE);
      pool = new HikariDataSource(config);
    }
    return pool.getConnection();
  }

  @Override
  public void accept(PubSubMessage pubSubMessage, Context context) {
    String receivedData = pubSubMessage.data != null ? pubSubMessage.data : "Received no data";
    logger.info("New message: " + receivedData);
    receivedData = new String(Base64.getDecoder().decode(pubSubMessage.data));
    ChatMessage chatMessage = ChatMessage.fromJsonString(receivedData);
    logger.info(chatMessage.toString());
    Connection connection = null;

    try {
      connection = getConnection();
      connection.setAutoCommit(false);
      PreparedStatement getLastMsgIndexStmt = connection.prepareStatement(LAST_INDEX_QUERY);
      PreparedStatement insertMessageStmt = connection.prepareStatement(INSERT_MESSAGE_QUERY);

      String newMessageId = UUID.randomUUID().toString();
      String chatRoomId = chatMessage.getRoomId().toString();
      String message = chatMessage.getText() != null ? chatMessage.getText().getMessage() : null;
      String audioMessage = chatMessage.isAudio() ? chatMessage.getAudio().getMessage() : null;
      String messageLocale =
          chatMessage.getText() != null ? chatMessage.getText().getLocale() : LOCALE_EN;
      String audioLocale = chatMessage.isAudio() ? chatMessage.getAudio().getLocale() : AUDIO_EN_US;

      getLastMsgIndexStmt.setString(1, chatRoomId);
      getLastMsgIndexStmt.setBoolean(2, true);
      ResultSet rs = getLastMsgIndexStmt.executeQuery();
      int nextIndex = rs.next() ? rs.getInt(MSG_INDEX_CLOUMN) + 1 : 0;
      rs.close();

      insertMessageStmt.setString(1, newMessageId);
      insertMessageStmt.setBoolean(2, chatMessage.isAudio());
      insertMessageStmt.setString(3, messageLocale);
      insertMessageStmt.setString(4, message);
      insertMessageStmt.setString(5, audioLocale);
      insertMessageStmt.setString(6, audioMessage);
      insertMessageStmt.setInt(7, nextIndex);
      insertMessageStmt.setBoolean(8, true);
      insertMessageStmt.setString(9, chatRoomId);
      insertMessageStmt.setString(10, chatMessage.getAuthor().getId().toString());
      insertMessageStmt.execute();
      connection.commit();
      logger.info(
          String.format("Inserted new message [id: %s], [message: %s]", newMessageId, message));
      forwardToTranslationService(MessageDao.fromChat(chatMessage, nextIndex));

      getLastMsgIndexStmt.close();
      insertMessageStmt.close();
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

  private void forwardToTranslationService(MessageDao messageDao) {
    String publishMessage = messageDao.getJsonString();
    ByteString byteStr = ByteString.copyFrom(publishMessage, StandardCharsets.UTF_8);
    PubsubMessage pubsubApiMessage = PubsubMessage.newBuilder().setData(byteStr).build();
    try {
      Publisher publisher = getPublisher(NEW_STORED_MESSAGE_TOPIC);
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
