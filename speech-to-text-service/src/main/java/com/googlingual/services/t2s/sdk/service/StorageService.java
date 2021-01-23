package com.googlingual.services.t2s.sdk.service;

import com.googlingual.services.t2s.sdk.dao.MessageDao;
import java.util.UUID;

public interface StorageService {
  public MessageDao get(UUID msgId);

  public boolean insert(UUID msgId, MessageDao message);

  public boolean update(UUID msgId, MessageDao message);

  public boolean delete(UUID msgId);
}
