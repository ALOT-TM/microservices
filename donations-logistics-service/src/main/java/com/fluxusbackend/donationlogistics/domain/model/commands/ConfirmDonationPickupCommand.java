package com.fluxusbackend.donationlogistics.domain.model.commands;

import com.fluxusbackend.donationlogistics.domain.model.valueobjects.DonationId;
import com.fluxusbackend.donationlogistics.domain.model.valueobjects.PickupConfirmationDate;
import java.util.Optional;

public record ConfirmDonationPickupCommand(DonationId donationId, PickupConfirmationDate pickupConfirmationDate, Optional<String> comment) {
    public ConfirmDonationPickupCommand {
        if (donationId == null) {
            throw new IllegalArgumentException("Donation id is required");
        }
        if (pickupConfirmationDate == null) {
            throw new IllegalArgumentException("Pickup confirmation date is required");
        }
        if (comment == null) {
            comment = Optional.empty();
        }
    }
}


