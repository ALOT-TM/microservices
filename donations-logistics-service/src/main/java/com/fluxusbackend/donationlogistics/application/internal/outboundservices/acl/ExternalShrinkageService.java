package com.fluxusbackend.donationlogistics.application.internal.outboundservices.acl;

import com.fluxusbackend.donationlogistics.domain.model.valueobjects.ShrinkageReferenceId;
import com.fluxusbackend.donationlogistics.infrastructure.clients.ShrinkageClient;
import feign.FeignException;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class ExternalShrinkageService {

    private final ShrinkageClient shrinkageClient;

    public ExternalShrinkageService(ShrinkageClient shrinkageClient) {
        this.shrinkageClient = shrinkageClient;
    }

    public Optional<ShrinkageReferenceId> fetchShrinkageById(Long shrinkageId) {
        try {
            var shrinkage = shrinkageClient.getShrinkage(shrinkageId);
            return shrinkage == null ? Optional.empty() : Optional.of(new ShrinkageReferenceId(shrinkage.shrinkageId()));
        } catch (FeignException.NotFound ex) {
            return Optional.empty();
        }
    }

    public Optional<Long> fetchShrinkageCompanyId(Long shrinkageId) {
        try {
            var shrinkage = shrinkageClient.getShrinkage(shrinkageId);
            return shrinkage == null || shrinkage.companyId() == null ? Optional.empty() : Optional.of(shrinkage.companyId());
        } catch (FeignException.NotFound ex) {
            return Optional.empty();
        }
    }

    public String fetchShrinkageStatus(Long shrinkageId) {
        return shrinkageClient.getShrinkage(shrinkageId).status();
    }

    public boolean markShrinkageRequested(Long shrinkageId) {
        try {
            shrinkageClient.markRequested(shrinkageId);
            return true;
        } catch (FeignException ex) {
            return false;
        }
    }

    public boolean markShrinkageDonated(Long shrinkageId) {
        try {
            shrinkageClient.markDonated(shrinkageId);
            return true;
        } catch (feign.FeignException ex) {
            String errorMessage = "Unable to mark shrinkage as donated. Feign error: " + ex.status() + " " + ex.contentUTF8();
            System.err.println(errorMessage);
            ex.printStackTrace();
            throw new IllegalStateException(errorMessage, ex);
        }
    }

    public boolean markShrinkageInProcess(Long shrinkageId) {
        try {
            shrinkageClient.markInProcess(shrinkageId);
            return true;
        } catch (feign.FeignException ex) {
            String errorMessage = "Unable to mark shrinkage as in-process. Feign error: " + ex.status() + " " + ex.contentUTF8();
            System.err.println(errorMessage);
            ex.printStackTrace();
            throw new IllegalStateException(errorMessage, ex);
        }
    }
}
