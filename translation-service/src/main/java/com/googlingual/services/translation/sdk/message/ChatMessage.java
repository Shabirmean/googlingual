/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.googlingual.services.translation.sdk.message;

import com.google.gson.Gson;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;

public class ChatMessage {

  private UUID roomId;
  private Author author;
  private TextMessage text;
  private AudioMessage audio;
  private static final Gson GSON_PARSER = new Gson();

  public ChatMessage() {
  }

  public ChatMessage(UUID roomId, Author author, TextMessage text, AudioMessage audio) {
    this.roomId = roomId;
    this.author = author;
    this.text = text;
    this.audio = audio;
  }

  public UUID getRoomId() {
    return roomId;
  }

  public void setRoomId(UUID roomId) {
    this.roomId = roomId;
  }

  public void setAudio(AudioMessage audio) {
    this.audio = audio;
  }

  public AudioMessage getAudio() {
    return audio;
  }

  public void setAuthor(Author author) {
    this.author = author;
  }

  public Author getAuthor() {
    return author;
  }

  public TextMessage getText() {
    return text;
  }

  public void setText(TextMessage text) {
    this.text = text;
  }

  public boolean isAudio() {
    return audio != null && StringUtils.isNotBlank(audio.getMessage());
  }

  public String getJsonString() {
    return GSON_PARSER.toJson(this);
  }

  public String toString() {
    return new StringBuilder().append("(\n").append("roomId").append(": ").append(roomId)
        .append("author id")
        .append(": ").append(author.getId()).append("author name").append(": ")
        .append(author.getUsername())
        .append("text").append(": ").append(text.getMessage()).append("text locale").append(": ")
        .append(text.getLocale()).append("audio locale").append(": ").append(isAudio() ? audio.getLocale() : "")
        .toString();
  }

  public static ChatMessage fromJsonString(String jsonMessage) {
    return GSON_PARSER.fromJson(jsonMessage, ChatMessage.class);
  }
}
