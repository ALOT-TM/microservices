package com.fluxusbackend.notificationservice.domain.model;

import java.util.Map;

public record NotificationEvent(
    String recipient,
    String notificationType,
    String batchId,
    String productName,
    Map<String, String> details
) {}
