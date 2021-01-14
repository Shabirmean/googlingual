package com.googlingual.springboot;

public class AudioMessage {
  private String message;
  private String locale;

  public AudioMessage() {
  }

  public AudioMessage(String message) {
    this(message, "en-US");
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
