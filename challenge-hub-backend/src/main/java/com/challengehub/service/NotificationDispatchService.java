package com.challengehub.service;

import java.util.Map;

public interface NotificationDispatchService {

    boolean createNotification(String userId, String type, String referenceId, Map<String, Object> payload);
}
