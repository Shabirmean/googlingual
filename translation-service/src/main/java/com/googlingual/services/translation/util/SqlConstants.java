package com.googlingual.services.translation.util;

public class SqlConstants {
  public static final String SELECT_MESSAGE_QUERY = ""
      + "SELECT BIN_TO_UUID(chatroom_id) chatroom_id, is_audio "
      + "FROM messages_v2 "
      + "WHERE id = UUID_TO_BIN('%s');";

  public static final String GET_LOCALES_QUERY = ""
      + "SELECT message_locale "
      + "FROM roomusers_v2 "
      + "WHERE chatroom_id = UUID_TO_BIN('%s') "
      + "AND disconnected = 0 "
      + "GROUP BY message_locale;";
}
