package com.fluxusbackend.subscriptionservice.infrastructure.bootstrap;

import com.fluxusbackend.subscription.domain.model.aggregates.Plan;
import com.fluxusbackend.subscription.infrastructure.persistence.jpa.repositories.PlanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class SubscriptionDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionDataSeeder.class);

    private final PlanRepository planRepository;

    public SubscriptionDataSeeder(PlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Iniciando la carga de datos inicial (Data Seeding) en subscription-service...");
        seedPlans();
        log.info("Proceso de Data Seeding en subscription-service completado.");
    }

    private void seedPlans() {
        try {
            if (planRepository.count() == 0) {
                log.info("La tabla plan está vacía. Insertando planes iniciales...");
                List<Plan> plans = List.of(
                        new Plan((short) 1, "Básico", new BigDecimal("49.00"), true, 3, 1000),
                        new Plan((short) 2, "Profesional", new BigDecimal("149.00"), true, 15, 10000),
                        new Plan((short) 3, "Enterprise", new BigDecimal("999.00"), true, 999, 99999)
                );
                planRepository.saveAll(plans);
                log.info("Planes iniciales insertados con éxito.");
            } else {
                log.info("La tabla plan ya contiene datos. Se omite la inserción.");
            }
        } catch (Exception e) {
            log.error("Error al inicializar la tabla plan: {}", e.getMessage(), e);
        }
    }
}
