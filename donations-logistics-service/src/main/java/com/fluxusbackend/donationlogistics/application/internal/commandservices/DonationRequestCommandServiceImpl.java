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
import com.fluxusbackend.donationlogistics.infrastructure.persistence.jpa.repositories.DonationRequestRepository;
import com.fluxusbackend.shared.application.audit.StatusChangeLogService;
import com.fluxusbackend.shared.domain.model.valueobjects.CompanyId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DonationRequestCommandServiceImpl implements DonationRequestCommandService {

    private final DonationRequestRepository repository;
    private final ExternalShrinkageService externalShrinkageService;
    private final ExternalBeneficiaryService externalBeneficiaryService;
    private final StatusChangeLogService statusChangeLogService;

    public DonationRequestCommandServiceImpl(
            DonationRequestRepository repository,
            ExternalShrinkageService externalShrinkageService,
            ExternalBeneficiaryService externalBeneficiaryService,
            StatusChangeLogService statusChangeLogService
    ) {
        this.repository = repository;
        this.externalShrinkageService = externalShrinkageService;
        this.externalBeneficiaryService = externalBeneficiaryService;
        this.statusChangeLogService = statusChangeLogService;
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

        var shrinkageStatus = externalShrinkageService.fetchShrinkageStatus(shrinkageRef.value());
        if (!"DONABLE".equals(shrinkageStatus)) {
            throw new IllegalStateException("Only donable shrinkages can receive requests");
        }

        var companyId = externalShrinkageService.fetchShrinkageCompanyId(shrinkageRef.value())
                .orElseThrow(() -> new IllegalArgumentException("Shrinkage does not have an associated company"));

        var request = new DonationRequest(shrinkageRef, beneficiaryRef, new CompanyId(companyId), command.notes());
        return repository.save(request);
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
        updateShrinkageToInProcess(request.getShrinkageReferenceId().value());
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

    private void updateShrinkageToInProcess(Long shrinkageId) {
        var status = externalShrinkageService.fetchShrinkageStatus(shrinkageId);
        if (("DONABLE".equals(status) || "REQUESTED".equals(status))
                && !externalShrinkageService.markShrinkageInProcess(shrinkageId)) {
            throw new IllegalStateException("Unable to mark shrinkage as in-process");
        }
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
