package com.fluxusbackend.donationlogistics.infrastructure.messaging.events;

public record DonationRequestAcceptedEvent(
        Long shrinkageId
) {}
