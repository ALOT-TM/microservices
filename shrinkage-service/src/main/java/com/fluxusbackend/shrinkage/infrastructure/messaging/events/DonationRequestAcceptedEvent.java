package com.fluxusbackend.shrinkage.infrastructure.messaging.events;

public record DonationRequestAcceptedEvent(
        Long shrinkageId
) {}
