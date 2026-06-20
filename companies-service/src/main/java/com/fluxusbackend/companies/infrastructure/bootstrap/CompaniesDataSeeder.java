package com.fluxusbackend.companies.infrastructure.bootstrap;

import com.fluxusbackend.location.domain.model.aggregates.Country;
import com.fluxusbackend.location.infrastructure.persistence.jpa.repositories.CountryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class CompaniesDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CompaniesDataSeeder.class);

    private final CountryRepository countryRepository;

    public CompaniesDataSeeder(CountryRepository countryRepository) {
        this.countryRepository = countryRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Iniciando la carga de datos inicial (Data Seeding) en companies-service...");
        seedCountry();
        log.info("Proceso de Data Seeding en companies-service completado.");
    }

    private void seedCountry() {
        try {
            if (countryRepository.count() == 0) {
                log.info("La tabla country en companies-service está vacía. Insertando registro inicial...");
                countryRepository.save(new Country("Peru"));
                log.info("Registro 'Peru' insertado con éxito en country (companies-service).");
            } else {
                log.info("La tabla country en companies-service ya contiene datos. Se omite la inserción.");
            }
        } catch (Exception e) {
            log.error("Error al inicializar la tabla country en companies-service: {}", e.getMessage(), e);
        }
    }
}
