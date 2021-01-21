package com.googlingual.services.t2s;

import static com.googlingual.services.t2s.util.SqlConstants.UPDATE_MESSAGE_QUERY;

import com.google.cloud.functions.BackgroundFunction;
import com.google.cloud.functions.Context;
import com.google.cloud.texttospeech.v1.AudioConfig;
import com.google.cloud.texttospeech.v1.AudioEncoding;
import com.google.cloud.texttospeech.v1.SsmlVoiceGender;
import com.google.cloud.texttospeech.v1.SynthesisInput;
import com.google.cloud.texttospeech.v1.SynthesizeSpeechResponse;
import com.google.cloud.texttospeech.v1.TextToSpeechClient;
import com.google.cloud.texttospeech.v1.VoiceSelectionParams;
import com.google.protobuf.ByteString;
import com.googlingual.services.t2s.TextToSpeech.PubSubMessage;
import com.googlingual.services.t2s.sdk.dao.MessageDao;
import com.googlingual.services.t2s.sdk.pubsub.PubSubExchangeMessage;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;

public class TextToSpeech implements BackgroundFunction<PubSubMessage> {

  private static final Logger logger = Logger.getLogger(TextToSpeech.class.getName());
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

    Connection connection = null;
    MessageDao messageDao = exchangeMessage.getMessage();
    String textToBeMadeAudio = messageDao.getMessage();
    String destinationLocale = exchangeMessage.getDestinationLocale();
    // Instantiates a client
    try (TextToSpeechClient textToSpeechClient = TextToSpeechClient.create()) {
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

      connection = getConnection(DEFAULT_SQL_POOL_SIZE);
      PreparedStatement updateMessageQuery = connection.prepareStatement(UPDATE_MESSAGE_QUERY);
      updateMessageQuery.setString(1, encodedMessage);
      updateMessageQuery.setString(2, destinationLocale);
      updateMessageQuery.setString(3, messageDao.getId().toString());
      updateMessageQuery.executeUpdate();
      connection.commit();
      updateMessageQuery.close();
      logger.info(String.format("Inserted translated audio for message [id: %s]\n[lang: %s]\n[%s]",
          messageDao.getId(), destinationLocale, encodedMessage));

    } catch (IOException | SQLException ex) {
      logger.log(Level.SEVERE, "Failed to convert text to audio: [" + textToBeMadeAudio + "]", ex);
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

  public static class PubSubMessage {

    String data;
    Map<String, String> attributes;
    String messageId;
    String publishTime;
  }
}
