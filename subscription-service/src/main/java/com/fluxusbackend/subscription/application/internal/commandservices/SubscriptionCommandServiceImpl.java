package com.fluxusbackend.subscription.application.internal.commandservices;

import com.fluxusbackend.subscription.domain.model.aggregates.Plan;
import com.fluxusbackend.subscription.domain.model.aggregates.Subscription;
import com.fluxusbackend.subscription.domain.model.commands.CreateSubscriptionCommand;
import com.fluxusbackend.subscription.domain.model.enums.SubscriptionStatus;
import com.fluxusbackend.subscription.domain.services.SubscriptionCommandService;
import com.fluxusbackend.subscription.infrastructure.clients.RetailCompanyClient;
import com.fluxusbackend.subscription.infrastructure.persistence.jpa.repositories.PlanRepository;
import com.fluxusbackend.subscription.infrastructure.persistence.jpa.repositories.SubscriptionRepository;
import feign.FeignException;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class SubscriptionCommandServiceImpl implements SubscriptionCommandService {

    private final SubscriptionRepository subscriptionRepository;
    private final RetailCompanyClient retailCompanyClient;
    private final PlanRepository planRepository;

    public SubscriptionCommandServiceImpl(
            SubscriptionRepository subscriptionRepository,
            RetailCompanyClient retailCompanyClient,
            PlanRepository planRepository
    ) {
        this.subscriptionRepository = subscriptionRepository;
        this.retailCompanyClient = retailCompanyClient;
        this.planRepository = planRepository;
    }

    @Override
    @Transactional
    public Subscription handle(CreateSubscriptionCommand command) {
        requireRetailCompany(command.retailCompanyId());
        Plan plan = planRepository.findById(command.planId())
                .orElseThrow(() -> new IllegalArgumentException("Plan not found"));

        Instant startDate = Instant.now();
        Instant endDate = startDate.plus(30, ChronoUnit.DAYS); // Standard 30 days subscription

        var subscription = new Subscription(
                command.retailCompanyId(),
                plan,
                SubscriptionStatus.ACTIVE,
                startDate,
                endDate
        );

        return subscriptionRepository.save(subscription);
    }

    private void requireRetailCompany(Long retailCompanyId) {
        try {
            retailCompanyClient.getRetailCompany(retailCompanyId);
        } catch (FeignException.NotFound ex) {
            throw new IllegalArgumentException("Retail company not found");
        } catch (FeignException ex) {
            throw new IllegalStateException("Companies service unavailable");
        }
    }
}
