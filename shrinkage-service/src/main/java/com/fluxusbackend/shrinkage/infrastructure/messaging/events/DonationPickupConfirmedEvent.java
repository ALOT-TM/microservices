package com.fluxusbackend.shrinkage.infrastructure.messaging.events;

import java.time.LocalDate;

public record DonationPickupConfirmedEvent(
        Long shrinkageId,
        LocalDate pickupConfirmationDate
) {}
