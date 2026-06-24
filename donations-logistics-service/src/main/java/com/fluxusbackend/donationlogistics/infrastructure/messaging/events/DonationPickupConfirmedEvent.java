package com.fluxusbackend.donationlogistics.infrastructure.messaging.events;

import java.time.LocalDate;

public record DonationPickupConfirmedEvent(
        Long shrinkageId,
        LocalDate pickupConfirmationDate
) {}
