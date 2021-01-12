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

package com.googlingual.springboot;

public class ChatMessage {
  private String message;
  private String locale;
  private AudioMessage audioMessage;

  public ChatMessage() {
  }

  public ChatMessage(String message, AudioMessage audioMessage, String locale) {
    this.message = message;
    this.audioMessage = audioMessage;
    this.locale = locale;
  }

  public String getMessage() {
    return message;
  }

  public AudioMessage getAudioMessage() {
    return audioMessage;
  }

  public String getLocale() {
    return locale;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public void setAudioMessage(AudioMessage audioMessage) {
    this.audioMessage = audioMessage;
  }

  public void setLocale(String locale) {
    this.locale = locale;
  }

  public String toString() {
    return new StringBuilder().append(message).append(" (").append(locale).append(")").toString();
  }
}
