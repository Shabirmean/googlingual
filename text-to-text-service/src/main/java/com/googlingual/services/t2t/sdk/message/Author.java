package com.googlingual.services.t2t.sdk.message;

import java.util.UUID;

public class Author {
  private String id;
  private String username;

  public Author() {
  }

  public Author(String id, String username) {
    this.id = id;
    this.username = username;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }
}
