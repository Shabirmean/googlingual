package com.googlingual.springboot;

public class AudioMessage {
  private String message;

  public AudioMessage() {
  }

  public AudioMessage(String message) {
    this.message = message;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }
}
