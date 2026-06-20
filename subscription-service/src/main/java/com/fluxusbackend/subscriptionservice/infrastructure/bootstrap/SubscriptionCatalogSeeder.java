package com.fluxusbackend.subscriptionservice.infrastructure.bootstrap;

import com.fluxusbackend.subscription.domain.model.aggregates.Plan;
import com.fluxusbackend.subscription.infrastructure.persistence.jpa.repositories.PlanRepository;
import java.math.BigDecimal;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionCatalogSeeder implements CommandLineRunner {

    private final PlanRepository planRepository;

    public SubscriptionCatalogSeeder(PlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    @Override
    public void run(String... args) {
        seedPlans();
    }

    private void seedPlans() {
        seedPlan((short) 1, "Básico", new BigDecimal("49.00"), 3, 1000);
        seedPlan((short) 2, "Profesional", new BigDecimal("149.00"), 15, 10000);
        seedPlan((short) 3, "Enterprise", new BigDecimal("999.00"), 999, 99999);
    }

    private void seedPlan(Short id, String name, BigDecimal price, Integer maxUsers, Integer maxStorage) {
        if (!planRepository.existsById(id)) {
            planRepository.save(new Plan(id, name, price, true, maxUsers, maxStorage));
        }
    }
}
