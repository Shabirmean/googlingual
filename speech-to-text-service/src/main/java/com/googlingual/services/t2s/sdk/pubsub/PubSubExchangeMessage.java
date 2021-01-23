package com.googlingual.services.t2s.sdk.pubsub;

import com.google.gson.Gson;
import com.googlingual.services.t2s.sdk.dao.MessageDao;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class PubSubExchangeMessage {
  private static final Gson GSON = new Gson();
  private MessageDao message;
  private String destinationLocale;
  private Set<String> audioDestinationLocales;

  public PubSubExchangeMessage(MessageDao message,
      String destinationLocale,
      Set<String> audioDestinationLocales) {
    this.message = message;
    this.destinationLocale = destinationLocale;
    this.audioDestinationLocales = audioDestinationLocales;
  }

  public PubSubExchangeMessage(MessageDao message, String destinationLocale) {
    this(message, destinationLocale, new HashSet<>());
  }

  public String getJsonString() {
    return GSON.toJson(this);
  }

  public static PubSubExchangeMessage fromJsonString(String jsonMessage) {
    return GSON.fromJson(jsonMessage, PubSubExchangeMessage.class);
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
        .append("message", message)
        .append("destinationLocale", destinationLocale)
        .append("audioDestinationLocale", audioDestinationLocales)
        .toString();
  }

  public MessageDao getMessage() {
    return message;
  }

  public void setMessage(MessageDao message) {
    this.message = message;
  }

  public String getDestinationLocale() {
    return destinationLocale;
  }

  public void setDestinationLocale(String destinationLocale) {
    this.destinationLocale = destinationLocale;
  }

  public Set<String> getAudioDestinationLocales() {
    return audioDestinationLocales;
  }

  public void setAudioDestinationLocales(Set<String> audioDestinationLocales) {
    this.audioDestinationLocales = audioDestinationLocales;
  }
}
