package com.googlingual.services.t2s.util;

public class SqlConstants {
  public static final String UPDATE_MESSAGE_QUERY =
      "UPDATE messages SET message = ?, message_locale = ? WHERE id = UUID_TO_BIN(?)";
}
