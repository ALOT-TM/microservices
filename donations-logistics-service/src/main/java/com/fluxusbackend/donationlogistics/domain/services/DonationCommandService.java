package com.fluxusbackend.donationlogistics.domain.services;

import com.fluxusbackend.donationlogistics.domain.model.aggregates.Donation;
import com.fluxusbackend.donationlogistics.domain.model.commands.ConfirmDonationPickupCommand;
import com.fluxusbackend.donationlogistics.domain.model.commands.CreateDonationCommand;
import com.fluxusbackend.donationlogistics.domain.model.commands.MarkDonationPendingPickupCommand;

public interface DonationCommandService {
    Donation handle(CreateDonationCommand command);

    Donation handle(MarkDonationPendingPickupCommand command);

    Donation handle(ConfirmDonationPickupCommand command);
}


