package com.googlingual.springboot;

import com.google.cloud.speech.v1.RecognitionAudio;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.RecognitionConfig.AudioEncoding;
import com.google.protobuf.ByteString;
import com.google.cloud.speech.v1.RecognizeResponse;
import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.speech.v1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1.SpeechRecognitionResult;

import com.googlingual.springboot.sdk.AudioMessage;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.logging.Logger;

public class SpeechToText {
  private static final Logger logger = Logger.getLogger(SpeechToText.class.getName());

  public static String speechToText(AudioMessage audioMessage) throws IOException {
    // Instantiates a client
    try (SpeechClient speechClient = SpeechClient.create()) {

      byte[] audioBytes = Base64.getDecoder().decode(audioMessage.getMessage());
      ByteString byteContent = ByteString.copyFrom(audioBytes);

      // Builds the sync recognize request
      RecognitionConfig config = RecognitionConfig.newBuilder().setEncoding(AudioEncoding.LINEAR16)
          .setLanguageCode("en-US").build();
      RecognitionAudio audio = RecognitionAudio.newBuilder().setContent(byteContent).build();

      // Performs speech recognition on the audio file
      RecognizeResponse response = speechClient.recognize(config, audio);
      List<SpeechRecognitionResult> results = response.getResultsList();

      for (SpeechRecognitionResult result : results) {
        // There can be several alternative transcripts for a given chunk of speech.
        // Just use the
        // first (most likely) one here.
        SpeechRecognitionAlternative alternative = result.getAlternativesList().get(0);
        logger.info(String.format("Transcription: %s%n", alternative.getTranscript()));
      }
      return results.get(0).getAlternativesList().get(0).getTranscript();
    }
  }

}
