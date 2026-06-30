package com.fluxusbackend.shrinkage.infrastructure.messaging.events;

import java.util.Map;

public record NotificationEvent(
    String recipient,
    String notificationType,
    String batchId,
    String productName,
    Map<String, String> details
) {}
