package com.fluxusbackend.donationlogistics.domain.model.commands;

import com.fluxusbackend.donationlogistics.domain.model.valueobjects.DonationRequestId;
import com.fluxusbackend.donationlogistics.domain.model.valueobjects.PickupConfirmationDate;
import java.util.Optional;

public record ConfirmDonationRequestPickupCommand(
        DonationRequestId requestId,
        PickupConfirmationDate pickupConfirmationDate,
        Optional<String> comment
) {
    public ConfirmDonationRequestPickupCommand {
        if (requestId == null) {
            throw new IllegalArgumentException("Donation request id is required");
        }
        if (pickupConfirmationDate == null) {
            throw new IllegalArgumentException("Pickup confirmation date is required");
        }
        if (comment == null) {
            comment = Optional.empty();
        }
    }
}
