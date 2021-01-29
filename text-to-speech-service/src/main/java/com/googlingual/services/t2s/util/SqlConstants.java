package com.googlingual.services.t2s.util;

public class SqlConstants {
  public static final String UPDATE_MESSAGE_QUERY =
      "UPDATE messages_v2 SET audio_message = ?, audio_locale = ? WHERE id = UUID_TO_BIN(?)";
}
