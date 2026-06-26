package com.fluxusbackend.authaccess.domain.model.dto;

import java.util.Map;

public record NotificationEvent(
    String recipient,
    String notificationType,
    String batchId,
    String productName,
    Map<String, String> details
) {}
