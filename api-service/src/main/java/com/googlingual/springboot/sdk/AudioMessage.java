package com.googlingual.springboot.sdk;

public class AudioMessage {
  private static final String DEFAULT_LOCALE = "en-US";
  private String message;
  private String locale;

  public AudioMessage() {
  }

  public AudioMessage(String message) {
    this(message, DEFAULT_LOCALE);
  }

  public AudioMessage(String message, String locale) {
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
