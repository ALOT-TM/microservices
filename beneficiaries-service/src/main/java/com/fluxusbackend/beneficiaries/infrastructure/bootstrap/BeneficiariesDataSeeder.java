package com.fluxusbackend.beneficiaries.infrastructure.bootstrap;

import com.fluxusbackend.beneficiary.domain.model.aggregates.InstitutionType;
import com.fluxusbackend.beneficiary.infrastructure.persistence.jpa.repositories.InstitutionTypeRepository;
import com.fluxusbackend.location.domain.model.aggregates.Country;
import com.fluxusbackend.location.infrastructure.persistence.jpa.repositories.CountryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class BeneficiariesDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(BeneficiariesDataSeeder.class);

    private final CountryRepository countryRepository;
    private final InstitutionTypeRepository institutionTypeRepository;

    public BeneficiariesDataSeeder(CountryRepository countryRepository, InstitutionTypeRepository institutionTypeRepository) {
        this.countryRepository = countryRepository;
        this.institutionTypeRepository = institutionTypeRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Iniciando la carga de datos inicial (Data Seeding) en beneficiaries-service...");
        seedCountry();
        seedInstitutionType();
        log.info("Proceso de Data Seeding en beneficiaries-service completado.");
    }

    private void seedCountry() {
        try {
            if (countryRepository.count() == 0) {
                log.info("La tabla country está vacía. Insertando registro inicial...");
                countryRepository.save(new Country("Peru"));
                log.info("Registro 'Peru' insertado con éxito en country.");
            } else {
                log.info("La tabla country ya contiene datos. Se omite la inserción.");
            }
        } catch (Exception e) {
            log.error("Error al inicializar la tabla country: {}", e.getMessage(), e);
        }
    }

    private void seedInstitutionType() {
        try {
            if (institutionTypeRepository.count() == 0) {
                log.info("La tabla institution_type está vacía. Insertando registro inicial...");
                institutionTypeRepository.save(new InstitutionType("Supermercado"));
                log.info("Registro 'Supermercado' insertado con éxito en institution_type.");
            } else {
                log.info("La tabla institution_type ya contiene datos. Se omite la inserción.");
            }
        } catch (Exception e) {
            log.error("Error al inicializar la tabla institution_type: {}", e.getMessage(), e);
        }
    }
}
