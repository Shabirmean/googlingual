package com.googlingual.services.storage.util;

public class SqlConstants {
  public static final String LAST_INDEX_QUERY = "SELECT "
      + "BIN_TO_UUID(m.id) id, "
      + "m.msg_index,"
      + "BIN_TO_UUID(m.chatroom_id) chatroom_id "
      + "FROM messages m "
      + "INNER JOIN ("
      + "   SELECT MAX(msg_index) msg_index, chatroom_id "
      + "   FROM messages "
      + "   WHERE chatroom_id = UUID_TO_BIN(?) "
      + "   AND is_original = ? "
      + "   GROUP BY chatroom_id"
      + ") b "
      + "ON m.msg_index = b.msg_index "
      + "AND m.chatroom_id = b.chatroom_id;";

  public static final String INSERT_MESSAGE_QUERY = "INSERT INTO messages ("
      + "id, "
      + "is_audio, "
      + "message_locale, "
      + "message, "
      + "audio_locale, "
      + "audio_message, "
      + "msg_index, "
      + "is_original, "
      + "chatroom_id, "
      + "sender "
      + ") VALUES ("
      + "   UUID_TO_BIN(?),"      // id
      + "   ?,"                   // is_audio
      + "   ?,"                   // message_locale
      + "   ?,"                   // message
      + "   ?,"                   // message_locale
      + "   ?,"                   // audio_message
      + "   ?,"                   // msg_index
      + "   ?,"                   // is_original
      + "   UUID_TO_BIN(?),"      // chatroom_id
      + "   UUID_TO_BIN(?)"       // sender
      + ");";
}
