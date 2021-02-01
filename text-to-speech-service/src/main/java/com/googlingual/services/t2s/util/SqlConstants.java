package com.googlingual.services.t2s.util;

public class SqlConstants {
  public static final String SELECT_MESSAGE_QUERY = ""
      + "SELECT message "
      + "FROM messages_v2 "
      + "WHERE id = UUID_TO_BIN('%s');";

  public static final String UPDATE_MESSAGE_QUERY =
      "UPDATE messages_v2 "
          + "SET audio_message = '%s', audio_locale = '%s' "
          + "WHERE id = UUID_TO_BIN('%s')";
}
