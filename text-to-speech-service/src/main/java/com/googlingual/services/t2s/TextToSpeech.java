package com.googlingual.services.t2s;

import static com.googlingual.services.t2s.util.SqlConstants.UPDATE_MESSAGE_QUERY;

import com.google.cloud.functions.BackgroundFunction;
import com.google.cloud.functions.Context;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient;
import com.google.cloud.secretmanager.v1.SecretVersionName;
import com.google.cloud.texttospeech.v1.AudioConfig;
import com.google.cloud.texttospeech.v1.AudioEncoding;
import com.google.cloud.texttospeech.v1.SsmlVoiceGender;
import com.google.cloud.texttospeech.v1.SynthesisInput;
import com.google.cloud.texttospeech.v1.SynthesizeSpeechResponse;
import com.google.cloud.texttospeech.v1.TextToSpeechClient;
import com.google.cloud.texttospeech.v1.VoiceSelectionParams;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.ProjectTopicName;
import com.google.pubsub.v1.PubsubMessage;
import com.googlingual.services.t2s.TextToSpeech.PubSubMessage;
import com.googlingual.services.t2s.sdk.dao.MessageDao;
import com.googlingual.services.t2s.sdk.pubsub.PubSubExchangeMessage;
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
public class TextToSpeech implements BackgroundFunction<PubSubMessage> {

  private static final Logger logger = Logger.getLogger(TextToSpeech.class.getName());
  private static final String SECRET_VERSION = "latest";
  private static final String PROJECT_GCLOUD_DPE = System.getenv("GCP_PROJECT");
  private static final String PUBLISH_TRANSLATED_MSG_TOPIC = System.getenv("PUBLISH_TRANSLATED_MSG_TOPIC");
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
  private static TextToSpeechClient textToSpeechClient;

  private Publisher getPublisher() throws IOException {
    Publisher publisher = publisherMap.get(PUBLISH_TRANSLATED_MSG_TOPIC);
    if (publisher == null) {
      publisher = Publisher.newBuilder(ProjectTopicName.of(PROJECT_GCLOUD_DPE, PUBLISH_TRANSLATED_MSG_TOPIC)).build();
      publisherMap.put(PUBLISH_TRANSLATED_MSG_TOPIC, publisher);
    }
    return publisher;
  }

  private TextToSpeechClient getTextToSpeechClient() throws IOException {
    if (textToSpeechClient == null) {
      textToSpeechClient = TextToSpeechClient.create();
    }
    return textToSpeechClient;
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

    Connection connection = null;
    MessageDao messageDao = exchangeMessage.getMessage();
    String textToBeMadeAudio = messageDao.getMessage();
    String destinationLocale = exchangeMessage.getDestinationLocale();
    // Instantiates a client
    try {
      TextToSpeechClient textToSpeechClient = getTextToSpeechClient();
      // Set the text input to be synthesized
      SynthesisInput input = SynthesisInput.newBuilder().setText(textToBeMadeAudio).build();
      // Build the voice request, select the language code ("en-US") and the ssml voice gender ("neutral")
      VoiceSelectionParams voice = VoiceSelectionParams.newBuilder()
          .setLanguageCode(destinationLocale)
          .setSsmlGender(SsmlVoiceGender.NEUTRAL).build();
      // Select the type of audio file you want returned
      AudioConfig audioConfig = AudioConfig.newBuilder().setAudioEncoding(AudioEncoding.MP3).build();
      // Perform the text-to-speech request on the text input with the selected voice parameters and audio file type
      SynthesizeSpeechResponse response = textToSpeechClient.synthesizeSpeech(input, voice, audioConfig);
      // Get the audio contents from the response
      ByteString audioContents = response.getAudioContent();
      String encodedMessage = com.google.api.client.util.Base64.encodeBase64String(audioContents.toByteArray());

      connection = getConnection();
      connection.setAutoCommit(false);
      PreparedStatement updateMessageQuery = connection.prepareStatement(UPDATE_MESSAGE_QUERY);
      updateMessageQuery.setString(1, encodedMessage);
      updateMessageQuery.setString(2, destinationLocale);
      updateMessageQuery.setString(3, messageDao.getId().toString());
      updateMessageQuery.executeUpdate();
      connection.commit();
      updateMessageQuery.close();
      logger.info(String.format("Inserted translated audio for message [id: %s]\n[index: %s]\n[lang: %s]\n[%s]",
          messageDao.getId(), messageDao.getMessageIndex(), destinationLocale, encodedMessage));
      exchangeMessage.getMessage().setAudioMessage(encodedMessage);
      exchangeMessage.getMessage().setAudioLocale(destinationLocale);
      publishTranslatedMessage(exchangeMessage);
    } catch (IOException | SQLException ex) {
      logger.log(Level.SEVERE, "Failed to convert text to audio: [" + textToBeMadeAudio + "]", ex);
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

  private void publishTranslatedMessage(PubSubExchangeMessage exchangeMessage) {
    String publishMessage = exchangeMessage.getJsonString();
    ByteString byteStr = ByteString.copyFrom(publishMessage, StandardCharsets.UTF_8);
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
