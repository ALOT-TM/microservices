package com.fluxusbackend.donationlogistics.domain.model.commands;

import com.fluxusbackend.donationlogistics.domain.model.valueobjects.PickupDate;
import com.fluxusbackend.donationlogistics.domain.model.valueobjects.DonationId;
import java.util.Objects;

public record MarkDonationPendingPickupCommand(DonationId donationId, PickupDate pickupDate) {
    public MarkDonationPendingPickupCommand {
        Objects.requireNonNull(donationId, "Donation id is required");
        Objects.requireNonNull(pickupDate, "Pickup date is required");
    }
}


