package com.googlingual.services.storage.sdk.message;

public class TextMessage {
  private static final String DEFAULT_LOCALE = "en";
  private String message;
  private String locale;

  public TextMessage() {
  }

  public TextMessage(String message) {
    this(message, DEFAULT_LOCALE);
  }

  public TextMessage(String message, String locale) {
    this.message = message;
    this.locale = locale;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getLocale() {
    return locale;
  }

  public void setLocale(String locale) {
    this.locale = locale;
  }
}
