package com.googlingual.services.s2t.util;

public class SqlConstants {
  public static final String UPDATE_MESSAGE_QUERY =
      "UPDATE messages_v2 SET message = ?, message_locale = ? WHERE id = UUID_TO_BIN(?)";
}
