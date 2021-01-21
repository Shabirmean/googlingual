package com.googlingual.services.t2t.sdk.message;

import java.util.UUID;

public class Author {
  private UUID id;
  private String username;

  public Author() {
  }

  public Author(UUID id, String username) {
    this.id = id;
    this.username = username;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }
}
