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

import com.googlingual.springboot.sdk.AudioMessage;
import org.apache.commons.lang3.StringUtils;

public class Message {
  private String locale;
  private String message;
  private String translated;
  private AudioMessage audioMessage;

  public Message() {
  }

  public Message(String message, String translated, AudioMessage audioMessage, String locale) {
    this.message = message;
    this.translated = translated;
    this.audioMessage = audioMessage;
    this.locale = locale;
  }

  public String getMessage() {
    return message;
  }

  public String getTranslated() {
    return translated;
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

  public void setTranslated(String translated) {
    this.translated = translated;
  }

  public void setAudioMessage(AudioMessage audioMessage) {
    this.audioMessage = audioMessage;
  }

  public void setLocale(String locale) {
    this.locale = locale;
  }

  public boolean isAudioMessage() {
    return audioMessage != null && StringUtils.isNotBlank(audioMessage.getMessage());
  }

  public String toString() {
    return new StringBuilder().append(message).append(" (").append(locale).append(")").toString();
  }
}
