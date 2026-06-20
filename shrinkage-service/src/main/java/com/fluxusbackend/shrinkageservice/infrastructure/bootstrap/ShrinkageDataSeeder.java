package com.fluxusbackend.shrinkageservice.infrastructure.bootstrap;

import com.fluxusbackend.shrinkage.domain.model.aggregates.Category;
import com.fluxusbackend.shrinkage.domain.model.aggregates.ShrinkageReason;
import com.fluxusbackend.shrinkage.infrastructure.persistence.jpa.repositories.CategoryRepository;
import com.fluxusbackend.shrinkage.infrastructure.persistence.jpa.repositories.ShrinkageReasonRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ShrinkageDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ShrinkageDataSeeder.class);

    private final CategoryRepository categoryRepository;
    private final ShrinkageReasonRepository shrinkageReasonRepository;

    public ShrinkageDataSeeder(CategoryRepository categoryRepository, ShrinkageReasonRepository shrinkageReasonRepository) {
        this.categoryRepository = categoryRepository;
        this.shrinkageReasonRepository = shrinkageReasonRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Iniciando la carga de datos inicial (Data Seeding) en shrinkage-service...");
        seedCategories();
        seedShrinkageReasons();
        log.info("Proceso de Data Seeding en shrinkage-service completado.");
    }

    private void seedCategories() {
        try {
            if (categoryRepository.count() == 0) {
                log.info("La tabla category está vacía. Insertando categorías iniciales...");
                List<Category> categories = List.of(
                        new Category("Frutas y Verduras"),
                        new Category("Lácteos"),
                        new Category("Carnes y Pescados"),
                        new Category("Panadería"),
                        new Category("Abarrotes")
                );
                categoryRepository.saveAll(categories);
                log.info("Categorías iniciales insertadas con éxito.");
            } else {
                log.info("La tabla category ya contiene datos. Se omite la inserción.");
            }
        } catch (Exception e) {
            log.error("Error al inicializar la tabla category: {}", e.getMessage(), e);
        }
    }

    private void seedShrinkageReasons() {
        try {
            if (shrinkageReasonRepository.count() == 0) {
                log.info("La tabla shrinkage_reason está vacía. Insertando motivos iniciales...");
                List<ShrinkageReason> reasons = List.of(
                        new ShrinkageReason("Fecha de vencimiento próxima"),
                        new ShrinkageReason("Empaque dañado"),
                        new ShrinkageReason("Sobre stock/Excedente de demanda"),
                        new ShrinkageReason("Otro")
                );
                shrinkageReasonRepository.saveAll(reasons);
                log.info("Motivos de merma iniciales insertados con éxito.");
            } else {
                log.info("La tabla shrinkage_reason ya contiene datos. Se omite la inserción.");
            }
        } catch (Exception e) {
            log.error("Error al inicializar la tabla shrinkage_reason: {}", e.getMessage(), e);
        }
    }
}
