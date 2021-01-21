package com.googlingual.services.translation.util;

public class SqlConstants {
  public static final String GET_LOCALES_QUERY = ""
      + "SELECT message_locale, audio_locale "
      + "FROM roomusers "
      + "WHERE chatroom_id = UUID_TO_BIN(?) "
      + "GROUP BY message_locale, audio_locale;";
}
