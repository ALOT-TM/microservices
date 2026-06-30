package com.fluxusbackend.donationlogistics.application.internal.commandservices;

import com.fluxusbackend.donationlogistics.application.internal.outboundservices.acl.ExternalBeneficiaryService;
import com.fluxusbackend.donationlogistics.application.internal.outboundservices.acl.ExternalShrinkageService;
import com.fluxusbackend.donationlogistics.domain.model.aggregates.DonationRequest;
import com.fluxusbackend.donationlogistics.domain.model.commands.AcceptDonationRequestCommand;
import com.fluxusbackend.donationlogistics.domain.model.commands.CancelDonationRequestCommand;
import com.fluxusbackend.donationlogistics.domain.model.commands.CreateDonationRequestCommand;
import com.fluxusbackend.donationlogistics.domain.model.commands.RejectDonationRequestCommand;
import com.fluxusbackend.donationlogistics.domain.model.commands.ConfirmDonationRequestPickupCommand;
import com.fluxusbackend.donationlogistics.domain.model.enums.DonationRequestStatus;
import com.fluxusbackend.donationlogistics.domain.model.valueobjects.BeneficiaryReferenceId;
import com.fluxusbackend.donationlogistics.domain.model.valueobjects.ShrinkageReferenceId;
import com.fluxusbackend.donationlogistics.domain.services.DonationRequestCommandService;
import com.fluxusbackend.donationlogistics.infrastructure.messaging.RabbitMQConfig;
import com.fluxusbackend.donationlogistics.infrastructure.messaging.events.DonationRequestAcceptedEvent;
import com.fluxusbackend.donationlogistics.infrastructure.persistence.jpa.repositories.DonationRequestRepository;
import com.fluxusbackend.shared.application.audit.StatusChangeLogService;
import com.fluxusbackend.shared.domain.model.valueobjects.CompanyId;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fluxusbackend.donationlogistics.infrastructure.clients.AuthAccessClient;
import com.fluxusbackend.donationlogistics.infrastructure.clients.ShrinkageClient;
import com.fluxusbackend.donationlogistics.infrastructure.clients.BeneficiaryInstitutionClient;
import com.fluxusbackend.donationlogistics.infrastructure.messaging.events.NotificationEvent;
import java.util.Map;
import java.util.Objects;

@Service
public class DonationRequestCommandServiceImpl implements DonationRequestCommandService {

    private final DonationRequestRepository repository;
    private final ExternalShrinkageService externalShrinkageService;
    private final ExternalBeneficiaryService externalBeneficiaryService;
    private final StatusChangeLogService statusChangeLogService;
    private final RabbitTemplate rabbitTemplate;
    private final AuthAccessClient authAccessClient;
    private final ShrinkageClient shrinkageClient;
    private final BeneficiaryInstitutionClient beneficiaryInstitutionClient;

    public DonationRequestCommandServiceImpl(
            DonationRequestRepository repository,
            ExternalShrinkageService externalShrinkageService,
            ExternalBeneficiaryService externalBeneficiaryService,
            StatusChangeLogService statusChangeLogService,
            RabbitTemplate rabbitTemplate,
            AuthAccessClient authAccessClient,
            ShrinkageClient shrinkageClient,
            BeneficiaryInstitutionClient beneficiaryInstitutionClient
    ) {
        this.repository = repository;
        this.externalShrinkageService = externalShrinkageService;
        this.externalBeneficiaryService = externalBeneficiaryService;
        this.statusChangeLogService = statusChangeLogService;
        this.rabbitTemplate = rabbitTemplate;
        this.authAccessClient = authAccessClient;
        this.shrinkageClient = shrinkageClient;
        this.beneficiaryInstitutionClient = beneficiaryInstitutionClient;
    }

    @Override
    @Transactional
    public DonationRequest handle(CreateDonationRequestCommand command) {
        var shrinkageRef = new ShrinkageReferenceId(command.mermaId());
        var beneficiaryRef = new BeneficiaryReferenceId(command.beneficiaryId());

        externalShrinkageService.fetchShrinkageById(shrinkageRef.value())
                .orElseThrow(() -> new IllegalArgumentException("Shrinkage not found"));
        externalBeneficiaryService.fetchBeneficiaryById(beneficiaryRef.value())
                .orElseThrow(() -> new IllegalArgumentException("Beneficiary not found"));

        if (repository.existsByBeneficiaryIdAndShrinkageId(beneficiaryRef.value(), shrinkageRef.value())) {
            throw new IllegalStateException("Ya existe una solicitud para esta merma");
        }

        if (externalShrinkageService.isShrinkageExpired(shrinkageRef.value())) {
            throw new IllegalStateException("No se puede donar merma vencida (la fecha de vencimiento no debe ser anterior o igual al día de hoy)");
        }

        var shrinkageStatus = externalShrinkageService.fetchShrinkageStatus(shrinkageRef.value());
        if (!"DONABLE".equals(shrinkageStatus)) {
            throw new IllegalStateException("Only donable shrinkages can receive requests");
        }

        var companyId = externalShrinkageService.fetchShrinkageCompanyId(shrinkageRef.value())
                .orElseThrow(() -> new IllegalArgumentException("Shrinkage does not have an associated company"));

        var request = new DonationRequest(shrinkageRef, beneficiaryRef, new CompanyId(companyId), command.notes());
        var saved = repository.save(request);

        // Publish email notification (SOLICITADO) to Retail users
        try {
            var shrinkage = shrinkageClient.getShrinkage(saved.getShrinkageReferenceId().value());
            var beneficiary = beneficiaryInstitutionClient.getBeneficiaryInstitution(saved.getBeneficiaryReferenceId().value());
            String beneficiaryName = beneficiary != null ? beneficiary.name() : "ONG Asociada";
            
            var users = authAccessClient.listUsers();
            for (var user : users) {
                if ("RETAIL".equals(user.actor()) && Objects.equals(user.retailCompanyId(), saved.getCompanyId().value())) {
                    rabbitTemplate.convertAndSend(
                        "notification.events.exchange",
                        "notification.email.solicitado",
                        new NotificationEvent(
                            user.email(),
                            "SOLICITADO",
                            saved.getId().toString(),
                            shrinkage != null ? shrinkage.name() : "Lote de Merma",
                            Map.of(
                                "cantidad", shrinkage != null && shrinkage.quantity() != null ? shrinkage.quantity() + " unidades" : "N/A",
                                "ong", beneficiaryName,
                                "tienda", shrinkage != null && shrinkage.retailCompanyHeadquarterId() != null ? "Local #" + shrinkage.retailCompanyHeadquarterId() : "Local Principal"
                            )
                        )
                    );
                }
            }
        } catch (Exception e) {
            System.err.println("Error publishing SOLICITADO notification: " + e.getMessage());
        }

        return saved;
    }

    @Override
    @Transactional
    public DonationRequest handle(AcceptDonationRequestCommand command) {
        var request = repository.findById(command.requestId().value())
                .orElseThrow(() -> new IllegalArgumentException("Donation request not found"));

        var shrinkageStatus = externalShrinkageService.fetchShrinkageStatus(request.getShrinkageReferenceId().value());
        if (!"DONABLE".equals(shrinkageStatus) && !"REQUESTED".equals(shrinkageStatus) && !"IN_PROCESS".equals(shrinkageStatus)) {
            throw new IllegalStateException("La merma ya no esta disponible para donacion (estado actual: " + shrinkageStatus + ")");
        }

        var fromStatus = request.getStatus();
        request.accept();
        var saved = repository.save(request);
        statusChangeLogService.recordChange(
                "DONATION_REQUEST",
                saved.getDonationRequestId().value(),
                fromStatus.name(),
                saved.getStatus().name()
        );
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.REQUEST_ROUTING_KEY,
                new DonationRequestAcceptedEvent(request.getShrinkageReferenceId().value())
        );

        // Publish email notification (ASIGNADO) to Beneficiary users
        try {
            var shrinkage = shrinkageClient.getShrinkage(saved.getShrinkageReferenceId().value());
            
            var users = authAccessClient.listUsers();
            for (var user : users) {
                if ("BENEFICIARY".equals(user.actor()) && Objects.equals(user.beneficiaryInstitutionId(), saved.getBeneficiaryReferenceId().value())) {
                    rabbitTemplate.convertAndSend(
                        "notification.events.exchange",
                        "notification.email.asignado",
                        new NotificationEvent(
                            user.email(),
                            "ASIGNADO",
                            saved.getId().toString(),
                            shrinkage != null ? shrinkage.name() : "Lote de Merma",
                            Map.of(
                                "cantidad", shrinkage != null && shrinkage.quantity() != null ? shrinkage.quantity() + " unidades" : "N/A",
                                "tienda", shrinkage != null && shrinkage.retailCompanyHeadquarterId() != null ? "Local #" + shrinkage.retailCompanyHeadquarterId() : "Local Principal",
                                "horario", "Lunes a Viernes de 09:00 a 12:00"
                            )
                        )
                    );
                }
            }
        } catch (Exception e) {
            System.err.println("Error publishing ASIGNADO notification: " + e.getMessage());
        }

        rejectOtherPendingRequests(request);
        return saved;
    }

    @Override
    @Transactional
    public DonationRequest handle(RejectDonationRequestCommand command) {
        var request = repository.findById(command.requestId().value())
                .orElseThrow(() -> new IllegalArgumentException("Donation request not found"));
        var fromStatus = request.getStatus();
        request.reject();
        var saved = repository.save(request);
        statusChangeLogService.recordChange(
                "DONATION_REQUEST",
                saved.getDonationRequestId().value(),
                fromStatus.name(),
                saved.getStatus().name()
        );
        return saved;
    }

    @Override
    @Transactional
    public DonationRequest handle(CancelDonationRequestCommand command) {
        var request = repository.findById(command.requestId().value())
                .orElseThrow(() -> new IllegalArgumentException("Donation request not found"));
        var fromStatus = request.getStatus();
        request.cancel();
        var saved = repository.save(request);
        statusChangeLogService.recordChange(
                "DONATION_REQUEST",
                saved.getDonationRequestId().value(),
                fromStatus.name(),
                saved.getStatus().name()
        );
        return saved;
    }

    @Override
    @Transactional
    public DonationRequest handle(ConfirmDonationRequestPickupCommand command) {
        var request = repository.findById(command.requestId().value())
                .orElseThrow(() -> new IllegalArgumentException("Donation request not found"));
        var fromStatus = request.getStatus();
        request.confirmPickup(command.pickupConfirmationDate(), command.comment().orElse(null));
        var saved = repository.save(request);
        var updated = externalShrinkageService.markShrinkageDonated(request.getShrinkageReferenceId().value());
        if (!updated) {
            throw new IllegalStateException("Unable to mark shrinkage as donated");
        }
        statusChangeLogService.recordChange(
                "DONATION_REQUEST",
                saved.getDonationRequestId().value(),
                fromStatus.name(),
                saved.getStatus().name()
        );
        return saved;
    }


    private void rejectOtherPendingRequests(DonationRequest acceptedRequest) {
        var otherRequests = repository.findByShrinkageId(acceptedRequest.getShrinkageReferenceId().value());
        for (var other : otherRequests) {
            if (!other.getDonationRequestId().value().equals(acceptedRequest.getDonationRequestId().value())
                    && other.getStatus() == DonationRequestStatus.PENDING) {
                var oldOtherStatus = other.getStatus();
                other.reject();
                repository.save(other);
                statusChangeLogService.recordChange(
                        "DONATION_REQUEST",
                        other.getDonationRequestId().value(),
                        oldOtherStatus.name(),
                        other.getStatus().name()
                );
            }
        }
    }
}
