package com.fluxusbackend.donationlogistics.application.internal.commandservices;

import com.fluxusbackend.donationlogistics.application.internal.outboundservices.acl.ExternalBeneficiaryService;
import com.fluxusbackend.donationlogistics.application.internal.outboundservices.acl.ExternalShrinkageService;
import com.fluxusbackend.donationlogistics.domain.model.aggregates.Donation;
import com.fluxusbackend.donationlogistics.domain.model.commands.ConfirmDonationPickupCommand;
import com.fluxusbackend.donationlogistics.domain.model.commands.CreateDonationCommand;
import com.fluxusbackend.donationlogistics.domain.model.commands.MarkDonationPendingPickupCommand;
import com.fluxusbackend.donationlogistics.domain.services.DonationCommandService;
import com.fluxusbackend.donationlogistics.infrastructure.messaging.RabbitMQConfig;
import com.fluxusbackend.donationlogistics.infrastructure.messaging.events.DonationPickupConfirmedEvent;
import com.fluxusbackend.donationlogistics.infrastructure.persistence.jpa.repositories.DonationRepository;
import com.fluxusbackend.shared.application.audit.StatusChangeLogService;
import jakarta.transaction.Transactional;
import java.util.NoSuchElementException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import com.fluxusbackend.donationlogistics.infrastructure.clients.AuthAccessClient;
import com.fluxusbackend.donationlogistics.infrastructure.clients.ShrinkageClient;
import com.fluxusbackend.donationlogistics.infrastructure.messaging.events.NotificationEvent;
import java.util.Map;
import java.util.Objects;

@Service
public class DonationCommandServiceImpl implements DonationCommandService {

    private final DonationRepository repository;
    private final ExternalShrinkageService externalShrinkageService;
    private final ExternalBeneficiaryService externalBeneficiaryService;
    private final com.fluxusbackend.shared.application.security.AclService aclService;
    private final StatusChangeLogService statusChangeLogService;
    private final RabbitTemplate rabbitTemplate;
    private final AuthAccessClient authAccessClient;
    private final ShrinkageClient shrinkageClient;

    public DonationCommandServiceImpl(
            DonationRepository repository,
            ExternalShrinkageService externalShrinkageService,
            ExternalBeneficiaryService externalBeneficiaryService,
            com.fluxusbackend.shared.application.security.AclService aclService,
            StatusChangeLogService statusChangeLogService,
            RabbitTemplate rabbitTemplate,
            AuthAccessClient authAccessClient,
            ShrinkageClient shrinkageClient
    ) {
        this.repository = repository;
        this.externalShrinkageService = externalShrinkageService;
        this.externalBeneficiaryService = externalBeneficiaryService;
        this.aclService = aclService;
        this.statusChangeLogService = statusChangeLogService;
        this.rabbitTemplate = rabbitTemplate;
        this.authAccessClient = authAccessClient;
        this.shrinkageClient = shrinkageClient;
    }

    @Override
    @Transactional
    public Donation handle(CreateDonationCommand command) {
        var shrinkage = externalShrinkageService.fetchShrinkageById(command.shrinkageReferenceId().value())
                .orElseThrow(() -> new NoSuchElementException("Shrinkage not found"));
        var beneficiary = externalBeneficiaryService.fetchBeneficiaryById(command.beneficiaryReferenceId().value())
                .orElseThrow(() -> new NoSuchElementException("Beneficiary not found"));

        var status = externalShrinkageService.fetchShrinkageStatus(command.shrinkageReferenceId().value());
        if (!"DONABLE".equals(status) && !"IN_PROCESS".equals(status)) {
            throw new IllegalStateException("Shrinkage must be DONABLE or IN_PROCESS before registering donation");
        }

        var donation = new Donation(
                shrinkage,
                beneficiary,
                command.quantity(),
                command.scheduledPickupDate()
        );
        // require retail user and set company id on donation to match shrinkage
        var companyId = aclService.requireRetailCompanyForCreate();
        // verify shrinkage belongs to same company
        var shrinkageCompany = externalShrinkageService.fetchShrinkageCompanyId(shrinkage.value());
        if (shrinkageCompany.isEmpty() || !shrinkageCompany.get().equals(companyId.value())) {
            throw new SecurityException("Shrinkage does not belong to the current user's company");
        }
        donation.setCompanyId(companyId);
        var saved = repository.save(donation);
        statusChangeLogService.recordChange(
                "DONATION",
                saved.getDonationId().value(),
                null,
                saved.getStatus().name()
        );
        return saved;
    }

    @Override
    @Transactional
    public Donation handle(MarkDonationPendingPickupCommand command) {
        var donation = repository.findById(command.donationId().value())
                .orElseThrow(() -> new NoSuchElementException("Donation not found"));
        aclService.ensureSameCompanyForRetail(donation);
        var fromStatus = donation.getStatus();
        donation.markPendingPickup(command.pickupDate());
        var saved = repository.save(donation);
        statusChangeLogService.recordChange(
            "DONATION",
            saved.getDonationId().value(),
            fromStatus.name(),
            saved.getStatus().name()
        );
        return saved;
    }

    @Override
    @Transactional
    public Donation handle(ConfirmDonationPickupCommand command) {
        var donation = repository.findById(command.donationId().value())
                .orElseThrow(() -> new NoSuchElementException("Donation not found"));
        aclService.ensureSameCompanyForRetail(donation);
        var fromStatus = donation.getStatus();
        donation.confirmPickup(command.pickupConfirmationDate(), command.comment());
        repository.save(donation);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.PICKUP_ROUTING_KEY,
                new DonationPickupConfirmedEvent(
                        donation.getShrinkageReferenceId().value(),
                        command.pickupConfirmationDate().value()
                )
        );
        statusChangeLogService.recordChange(
                "DONATION",
                donation.getDonationId().value(),
                fromStatus.name(),
                donation.getStatus().name()
        );

        // Publish email notification (RECOGIDO) to Retail users
        try {
            var shrinkage = shrinkageClient.getShrinkage(donation.getShrinkageReferenceId().value());
            
            var users = authAccessClient.listUsers();
            for (var user : users) {
                if ("RETAIL".equals(user.actor()) && Objects.equals(user.retailCompanyId(), donation.getCompanyId().map(com.fluxusbackend.shared.domain.model.valueobjects.CompanyId::value).orElse(null))) {
                    rabbitTemplate.convertAndSend(
                        "notification.events.exchange",
                        "notification.email.recogido",
                        new NotificationEvent(
                            user.email(),
                            "RECOGIDO",
                            donation.getDonationId().value().toString(),
                            shrinkage != null ? shrinkage.name() : "Lote de Merma",
                            Map.of(
                                "cantidad", donation.getQuantity() != null ? donation.getQuantity().amount() + " unidades" : "N/A",
                                "tienda", shrinkage != null && shrinkage.retailCompanyHeadquarterId() != null ? "Local #" + shrinkage.retailCompanyHeadquarterId() : "Local Principal",
                                "fecha", command.pickupConfirmationDate().value().toString()
                            )
                        )
                    );
                }
            }
        } catch (Exception e) {
            System.err.println("Error publishing RECOGIDO notification: " + e.getMessage());
        }

        return donation;
    }
}


