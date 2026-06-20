package com.fluxusbackend.donationlogistics.interfaces.rest.dto;

import java.time.LocalDate;
import java.util.Optional;

public record ConfirmDonationPickupRequest(
        LocalDate pickupConfirmationDate,
        LocalDate receptionDate,
        Optional<String> comment
) {
    public ConfirmDonationPickupRequest {
        if (comment == null) {
            comment = Optional.empty();
        }
    }

    public LocalDate resolvedPickupConfirmationDate() {
        return pickupConfirmationDate != null ? pickupConfirmationDate : receptionDate;
    }
}
