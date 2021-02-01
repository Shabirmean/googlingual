package com.googlingual.services.s2t.util;

public class SqlConstants {

  public static final String SELECT_MESSAGE_QUERY = ""
      + "SELECT audio_message, audio_locale "
      + "FROM messages_v2 "
      + "WHERE id = UUID_TO_BIN('%s');";

  public static final String UPDATE_MESSAGE_QUERY =
      "UPDATE messages_v2 "
          + "SET message = '%s', message_locale = '%s', is_audio = %s "
          + "WHERE id = UUID_TO_BIN('%s')";
}
