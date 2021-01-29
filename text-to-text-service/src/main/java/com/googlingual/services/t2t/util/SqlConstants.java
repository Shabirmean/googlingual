package com.googlingual.services.t2t.util;

public class SqlConstants {
  public static final String INSERT_MESSAGE_QUERY = "INSERT INTO messages_v2 ("
      + "id, "
      + "is_audio, "
      + "message_locale, "
      + "message, "
      + "audio_locale, "
      + "audio_message, "
      + "msg_index, "
      + "chatroom_id, "
      + "sender "
      + ") VALUES ("
      + "   UUID_TO_BIN(?),"      // id
      + "   ?,"                   // is_audio
      + "   ?,"                   // message_locale
      + "   ?,"                   // message
      + "   ?,"                   // audio_locale
      + "   ?,"                   // audio_message
      + "   ?,"                   // msg_index
      + "   UUID_TO_BIN(?),"      // chatroom_id
      + "   ?"                    // sender
      + ");";
}
