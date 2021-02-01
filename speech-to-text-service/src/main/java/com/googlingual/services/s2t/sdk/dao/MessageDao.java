package com.googlingual.services.s2t.sdk.dao;

import com.google.gson.Gson;
import com.googlingual.services.s2t.sdk.message.ChatMessage;
import com.googlingual.services.s2t.sdk.pubsub.PubSubExchangeMessage;
import java.util.UUID;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class MessageDao {
  private static final Gson GSON = new Gson();
  private UUID id;
  private UUID chatRoomId;
  private String sender;
  private int messageIndex;
  private String message;
  private String audioMessage;
  private String messageLocale;
  private String audioLocale;
  private boolean isAudio;

  public MessageDao() {}

  public static MessageDao fromChat(ChatMessage chatMessage, int index) {
    MessageDao newMessage = new MessageDao();
    newMessage.id = UUID.randomUUID();
    newMessage.chatRoomId = chatMessage.getRoomId();
    newMessage.sender = chatMessage.getAuthor().getId();
    newMessage.messageIndex = index;
    newMessage.message = chatMessage.getText() != null ? chatMessage.getText().getMessage() : null;
    newMessage.audioMessage = chatMessage.isAudio() ? chatMessage.getAudio().getMessage() : null;
    newMessage.messageLocale = chatMessage.getText() != null ? chatMessage.getText().getLocale() : "en";
    newMessage.audioLocale = chatMessage.isAudio() ? chatMessage.getAudio().getLocale() : "en-US";
    newMessage.isAudio = chatMessage.isAudio();
    return newMessage;
  }

  public static MessageDao fromPubSubExchange(PubSubExchangeMessage pubSubExchangeMessage, String textLocale, String translatedText) {
    MessageDao newMessage = new MessageDao();
    MessageDao oldMessage = pubSubExchangeMessage.getMessage();
    newMessage.id = UUID.randomUUID();
    newMessage.chatRoomId = oldMessage.chatRoomId;
    newMessage.sender = oldMessage.sender;
    newMessage.messageIndex = oldMessage.messageIndex;
    newMessage.message = translatedText;
    newMessage.audioMessage = null;
    newMessage.messageLocale = textLocale;
    newMessage.audioLocale = null;
    newMessage.isAudio = false;
    return newMessage;
  }

  public String getJsonString() {
    return GSON.toJson(this);
  }

  public static MessageDao fromJsonString(String jsonMessage) {
    return GSON.fromJson(jsonMessage, MessageDao.class);
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getChatRoomId() {
    return chatRoomId;
  }

  public void setChatRoomId(UUID chatRoomId) {
    this.chatRoomId = chatRoomId;
  }

  public String getSender() {
    return sender;
  }

  public void setSender(String sender) {
    this.sender = sender;
  }

  public int getMessageIndex() {
    return messageIndex;
  }

  public void setMessageIndex(int messageIndex) {
    this.messageIndex = messageIndex;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getAudioMessage() {
    return audioMessage;
  }

  public void setAudioMessage(String audioMessage) {
    this.audioMessage = audioMessage;
  }

  public String getMessageLocale() {
    return messageLocale;
  }

  public void setMessageLocale(String messageLocale) {
    this.messageLocale = messageLocale;
  }

  public String getAudioLocale() {
    return audioLocale;
  }

  public void setAudioLocale(String audioLocale) {
    this.audioLocale = audioLocale;
  }

  public boolean isAudio() {
    return isAudio;
  }

  public void setAudio(boolean audio) {
    isAudio = audio;
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
        .append("id", id)
        .append("chatRoomId", chatRoomId)
        .append("sender", sender)
        .append("messageIndex", messageIndex)
        .append("message", message)
        .append("audioMessage", audioMessage)
        .append("messageLocale", messageLocale)
        .append("audioLocale", audioLocale)
        .append("isAudio", isAudio)
        .toString();
  }
}
