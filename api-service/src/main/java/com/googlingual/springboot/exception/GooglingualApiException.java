package com.googlingual.springboot.exception;

public class GooglingualApiException extends Exception {

  private static final long serialVersionUID = 1L;

  public GooglingualApiException(String message) {
    super(message);
  }

  public GooglingualApiException(String message, Throwable cause) {
    super(message, cause);
  }

}
