package com.fluxusbackend.shrinkage.application.internal.commandservices;

import com.fluxusbackend.shrinkage.domain.model.aggregates.Shrinkage;
import com.fluxusbackend.shrinkage.domain.model.commands.MarkShrinkageDonableCommand;
import com.fluxusbackend.shrinkage.domain.model.commands.MarkShrinkageDonatedCommand;
import com.fluxusbackend.shrinkage.domain.model.commands.MarkShrinkageInProcessCommand;
import com.fluxusbackend.shrinkage.domain.model.commands.MarkShrinkageNotDonableCommand;
import com.fluxusbackend.shrinkage.domain.model.commands.MarkShrinkageRequestedCommand;
import com.fluxusbackend.shrinkage.domain.model.commands.RegisterShrinkageCommand;
import com.fluxusbackend.shrinkage.domain.services.ShrinkageCommandService;
import com.fluxusbackend.shrinkage.infrastructure.persistence.jpa.repositories.CategoryRepository;
import com.fluxusbackend.shrinkage.infrastructure.persistence.jpa.repositories.ShrinkageReasonRepository;
import com.fluxusbackend.shrinkage.infrastructure.persistence.jpa.repositories.ShrinkageRepository;
import com.fluxusbackend.shrinkage.domain.model.aggregates.HeadquarterCache;
import com.fluxusbackend.shrinkage.infrastructure.persistence.jpa.repositories.HeadquarterCacheRepository;
import com.fluxusbackend.shared.application.audit.StatusChangeLogService;
import jakarta.transaction.Transactional;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;

import com.fluxusbackend.shrinkage.infrastructure.clients.AuthAccessClient;
import com.fluxusbackend.shrinkage.infrastructure.messaging.events.NotificationEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import java.util.Map;

@Service
public class ShrinkageCommandServiceImpl implements ShrinkageCommandService {

    private final ShrinkageRepository repository;
    private final HeadquarterCacheRepository headquarterCacheRepository;
    private final CategoryRepository categoryRepository;
    private final ShrinkageReasonRepository shrinkageReasonRepository;
    private final com.fluxusbackend.shared.application.security.AclService aclService;
    private final StatusChangeLogService statusChangeLogService;
    private final RabbitTemplate rabbitTemplate;
    private final AuthAccessClient authAccessClient;

    public ShrinkageCommandServiceImpl(
            ShrinkageRepository repository,
            HeadquarterCacheRepository headquarterCacheRepository,
            CategoryRepository categoryRepository,
            ShrinkageReasonRepository shrinkageReasonRepository,
            com.fluxusbackend.shared.application.security.AclService aclService,
            StatusChangeLogService statusChangeLogService,
            RabbitTemplate rabbitTemplate,
            AuthAccessClient authAccessClient
    ) {
        this.repository = repository;
        this.headquarterCacheRepository = headquarterCacheRepository;
        this.categoryRepository = categoryRepository;
        this.shrinkageReasonRepository = shrinkageReasonRepository;
        this.aclService = aclService;
        this.statusChangeLogService = statusChangeLogService;
        this.rabbitTemplate = rabbitTemplate;
        this.authAccessClient = authAccessClient;
    }

    @Override
    @Transactional
    public Shrinkage handle(RegisterShrinkageCommand command) {
        var category = categoryRepository.findById(command.categoryId())
            .orElseThrow(() -> new NoSuchElementException("Category not found"));
        var reason = shrinkageReasonRepository.findById(command.shrinkageReasonId())
            .orElseThrow(() -> new NoSuchElementException("Shrinkage reason not found"));

        var companyId = aclService.requireRetailCompanyForCreate();
        var headquarterCompanyId = getHeadquarterCompanyId(command.retailCompanyHeadquarterId());
        if (!headquarterCompanyId.equals(companyId.value())) {
            throw new SecurityException("Headquarter does not belong to the current company");
        }

        var shrinkage = new Shrinkage(
            command.retailCompanyHeadquarterId(),
            category,
            reason,
            command.name(),
            command.quantity(),
            command.expirationDate(),
            command.specificReason(),
            command.pickupDate(),
            command.shrinkageValue()
        );
        shrinkage.setCompanyId(companyId);
        var saved = repository.save(shrinkage);
        return saved;
    }

    private Long getHeadquarterCompanyId(Long headquarterId) {
        return headquarterCacheRepository.findById(headquarterId)
            .map(HeadquarterCache::getCompanyId)
            .orElseThrow(() -> new NoSuchElementException("Retail company headquarter not found in local cache"));
    }

    @Override
    @Transactional
    public Shrinkage handle(MarkShrinkageDonableCommand command) {
        var shrinkage = repository.findById(command.shrinkageId().value())
                .orElseThrow(() -> new NoSuchElementException("Shrinkage not found"));
        aclService.ensureSameCompanyForRetail(shrinkage);
        var fromStatus = shrinkage.getStatus();
        shrinkage.markDonable();
        var saved = repository.save(shrinkage);
        statusChangeLogService.recordChange(
            "SHRINKAGE",
            saved.getShrinkageId(),
            fromStatus.name(),
            saved.getStatus().name()
        );

        // Publish email notification for all subscribed ONGs (Beneficiary Users)
        try {
            var users = authAccessClient.listUsers();
            for (var user : users) {
                if ("BENEFICIARY".equals(user.actor())) {
                    rabbitTemplate.convertAndSend(
                        "notification.events.exchange",
                        "notification.email.donable",
                        new NotificationEvent(
                            user.email(),
                            "DONABLE",
                            saved.getShrinkageId().toString(),
                            saved.getName(),
                            Map.of(
                                "cantidad", saved.getQuantity() + " unidades",
                                "vencimiento", saved.getExpirationDate() != null ? saved.getExpirationDate().toString() : "N/A",
                                "tienda", saved.getRetailCompanyHeadquarterId() != null ? "Local #" + saved.getRetailCompanyHeadquarterId() : "Local Principal"
                            )
                        )
                    );
                }
            }
        } catch (Exception e) {
            System.err.println("Error publishing DONABLE notification: " + e.getMessage());
        }

        return saved;
    }

    @Override
    @Transactional
    public Shrinkage handle(MarkShrinkageNotDonableCommand command) {
        var shrinkage = repository.findById(command.shrinkageId().value())
                .orElseThrow(() -> new NoSuchElementException("Shrinkage not found"));
        aclService.ensureSameCompanyForRetail(shrinkage);
        var fromStatus = shrinkage.getStatus();
        shrinkage.markNotDonable();
        var saved = repository.save(shrinkage);
        statusChangeLogService.recordChange(
            "SHRINKAGE",
            saved.getShrinkageId(),
            fromStatus.name(),
            saved.getStatus().name()
        );
        return saved;
    }

    @Override
    @Transactional
    public Shrinkage handle(MarkShrinkageDonatedCommand command) {
        var shrinkage = repository.findById(command.shrinkageId().value())
                .orElseThrow(() -> new NoSuchElementException("Shrinkage not found"));
        aclService.ensureSameCompanyForRetail(shrinkage);
        var fromStatus = shrinkage.getStatus();
        shrinkage.markDonated();
        var saved = repository.save(shrinkage);
        statusChangeLogService.recordChange(
            "SHRINKAGE",
            saved.getShrinkageId(),
            fromStatus.name(),
            saved.getStatus().name()
        );
        return saved;
    }

    @Override
    @Transactional
    public Shrinkage handle(MarkShrinkageInProcessCommand command) {
        var shrinkage = repository.findById(command.shrinkageId().value())
                .orElseThrow(() -> new NoSuchElementException("Shrinkage not found"));
        aclService.ensureSameCompanyForRetail(shrinkage);
        var fromStatus = shrinkage.getStatus();
        shrinkage.markInProcess();
        var saved = repository.save(shrinkage);
        statusChangeLogService.recordChange(
            "SHRINKAGE",
            saved.getShrinkageId(),
            fromStatus.name(),
            saved.getStatus().name()
        );
        return saved;
    }

    @Override
    @Transactional
    public Shrinkage handle(MarkShrinkageRequestedCommand command) {
        var shrinkage = repository.findById(command.shrinkageId().value())
                .orElseThrow(() -> new NoSuchElementException("Shrinkage not found"));
        aclService.ensureSameCompanyForRetail(shrinkage);
        var fromStatus = shrinkage.getStatus();
        shrinkage.markRequested();
        var saved = repository.save(shrinkage);
        statusChangeLogService.recordChange(
            "SHRINKAGE",
            saved.getShrinkageId(),
            fromStatus.name(),
            saved.getStatus().name()
        );
        return saved;
    }
}


