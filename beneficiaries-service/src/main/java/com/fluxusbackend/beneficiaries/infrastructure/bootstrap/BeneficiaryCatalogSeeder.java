package com.fluxusbackend.beneficiaries.infrastructure.bootstrap;

import com.fluxusbackend.beneficiary.domain.model.aggregates.InstitutionType;
import com.fluxusbackend.beneficiary.infrastructure.persistence.jpa.repositories.InstitutionTypeRepository;
import com.fluxusbackend.location.domain.model.aggregates.Country;
import com.fluxusbackend.location.infrastructure.persistence.jpa.repositories.CountryRepository;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class BeneficiaryCatalogSeeder implements CommandLineRunner {

    private final InstitutionTypeRepository institutionTypeRepository;
    private final CountryRepository countryRepository;

    public BeneficiaryCatalogSeeder(
            InstitutionTypeRepository institutionTypeRepository,
            CountryRepository countryRepository
    ) {
        this.institutionTypeRepository = institutionTypeRepository;
        this.countryRepository = countryRepository;
    }

    @Override
    public void run(String... args) {
        seedCountries();
        seedInstitutionTypes();
    }

    private void seedCountries() {
        List<String> countries = List.of("Perú");
        var existingNames = countryRepository.findAll().stream()
                .map(Country::getName)
                .map(String::toLowerCase)
                .toList();

        countries.stream()
                .filter(name -> !existingNames.contains(name.toLowerCase()))
                .map(Country::new)
                .forEach(countryRepository::save);
    }

    private void seedInstitutionTypes() {
        List<String> types = List.of(
                "Banco de Alimentos",
                "Comedor Popular",
                "Albergue de Menores",
                "Asociación Vecinal",
                "Colegio Público"
        );
        var existingNames = institutionTypeRepository.findAll().stream()
                .map(InstitutionType::getName)
                .map(String::toLowerCase)
                .toList();

        types.stream()
                .filter(name -> !existingNames.contains(name.toLowerCase()))
                .map(InstitutionType::new)
                .forEach(institutionTypeRepository::save);
    }
}
