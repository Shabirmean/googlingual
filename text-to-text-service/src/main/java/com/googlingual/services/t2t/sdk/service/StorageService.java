package com.googlingual.services.t2t.sdk.service;

import com.googlingual.services.t2t.sdk.dao.MessageDao;
import java.util.UUID;

public interface StorageService {
  public MessageDao get(UUID msgId);

  public boolean insert(UUID msgId, MessageDao message);

  public boolean update(UUID msgId, MessageDao message);

  public boolean delete(UUID msgId);
}
