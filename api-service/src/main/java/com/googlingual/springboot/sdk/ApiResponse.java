package com.googlingual.springboot.sdk;

import java.util.List;

public class ApiResponse {
  private String type;
  private List<? extends Object> results;

  public ApiResponse(String type, List<? extends Object> results) {
    this.type = type;
    this.results = results;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public List<? extends Object> getResults() {
    return results;
  }

  public void setResults(List<? extends Object> results) {
    this.results = results;
  }
}
