package com.fluxusbackend.shrinkageservice.infrastructure.bootstrap;

import com.fluxusbackend.shrinkage.domain.model.aggregates.Category;
import com.fluxusbackend.shrinkage.domain.model.aggregates.ShrinkageReason;
import com.fluxusbackend.shrinkage.infrastructure.persistence.jpa.repositories.CategoryRepository;
import com.fluxusbackend.shrinkage.infrastructure.persistence.jpa.repositories.ShrinkageReasonRepository;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class ShrinkageCatalogSeeder implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final ShrinkageReasonRepository shrinkageReasonRepository;

    public ShrinkageCatalogSeeder(
            CategoryRepository categoryRepository,
            ShrinkageReasonRepository shrinkageReasonRepository
    ) {
        this.categoryRepository = categoryRepository;
        this.shrinkageReasonRepository = shrinkageReasonRepository;
    }

    @Override
    public void run(String... args) {
        seedCategories();
        seedShrinkageReasons();
    }

    private void seedCategories() {
        List<String> categories = List.of(
                "Frutas y Verduras",
                "Lácteos",
                "Carnes y Pescados",
                "Panadería",
                "Abarrotes"
        );

        categories.stream()
                .filter(name -> !categoryRepository.existsByNameIgnoreCase(name))
                .map(Category::new)
                .forEach(categoryRepository::save);
    }

    private void seedShrinkageReasons() {
        List<String> reasons = List.of(
                "Fecha de vencimiento próxima",
                "Empaque dañado",
                "Sobre stock/Excedente de demanda",
                "Otro"
        );

        reasons.stream()
                .filter(name -> !shrinkageReasonRepository.existsByNameIgnoreCase(name))
                .map(ShrinkageReason::new)
                .forEach(shrinkageReasonRepository::save);
    }
}
