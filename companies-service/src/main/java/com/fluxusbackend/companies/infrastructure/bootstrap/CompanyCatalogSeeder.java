package com.fluxusbackend.companies.infrastructure.bootstrap;

import com.fluxusbackend.location.domain.model.aggregates.Country;
import com.fluxusbackend.location.infrastructure.persistence.jpa.repositories.CountryRepository;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class CompanyCatalogSeeder implements CommandLineRunner {

    private final CountryRepository countryRepository;

    public CompanyCatalogSeeder(CountryRepository countryRepository) {
        this.countryRepository = countryRepository;
    }

    @Override
    public void run(String... args) {
        seedCountries();
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
}
