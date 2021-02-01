package com.googlingual.services.t2t.util;

public class SqlConstants {
  public static final String SELECT_MESSAGE_QUERY = ""
      + "SELECT message, message_locale, BIN_TO_UUID(chatroom_id) chatroom_id, msg_index, sender "
      + "FROM messages_v2 "
      + "WHERE id = UUID_TO_BIN('%s');";

  public static final String GET_LOCALES_QUERY = ""
      + "SELECT audio_locale "
      + "FROM roomusers_v2 "
      + "WHERE chatroom_id = UUID_TO_BIN('%s') "
      + "AND audio_locale LIKE '%s%%' "
      + "AND disconnected = 0 "
      + "GROUP BY audio_locale;";

  public static final String INSERT_MESSAGE_QUERY = "INSERT INTO messages_v2 ("
      + "id, "
      + "is_audio, "
      + "message_locale, "
      + "message, "
      + "msg_index, "
      + "chatroom_id, "
      + "sender "
      + ") VALUES ("
      + "   UUID_TO_BIN('%s'),"       // id
      + "   %s,"                      // is_audio
      + "   '%s',"                    // message_locale
      + "   '%s',"                    // message
      + "   %s,"                      // msg_index
      + "   UUID_TO_BIN('%s'),"       // chatroom_id
      + "   '%s'"                     // sender
      + ");";
}
