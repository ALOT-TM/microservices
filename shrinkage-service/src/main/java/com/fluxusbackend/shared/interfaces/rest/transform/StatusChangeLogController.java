package com.fluxusbackend.shared.interfaces.rest.transform;

import com.fluxusbackend.authaccess.application.internal.services.AuthorizationService;
import com.fluxusbackend.authaccess.domain.model.enums.UserActor;
import com.fluxusbackend.shared.domain.model.aggregates.StatusChangeLog;
import com.fluxusbackend.shared.infrastructure.persistence.jpa.repositories.StatusChangeLogRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audit/status-changes")
@Tag(name = "Audit", description = "Status change audit logs")
@SecurityRequirement(name = "bearer")
public class StatusChangeLogController {

    private final StatusChangeLogRepository repository;
    private final AuthorizationService authorizationService;
    private final com.fluxusbackend.shrinkage.infrastructure.persistence.jpa.repositories.ShrinkageRepository shrinkageRepository;

    public StatusChangeLogController(
            StatusChangeLogRepository repository,
            AuthorizationService authorizationService,
            com.fluxusbackend.shrinkage.infrastructure.persistence.jpa.repositories.ShrinkageRepository shrinkageRepository
    ) {
        this.repository = repository;
        this.authorizationService = authorizationService;
        this.shrinkageRepository = shrinkageRepository;
    }

    private List<StatusChangeLog> filterLogsForRetail(List<StatusChangeLog> logs) {
        if (UserActor.RETAIL == authorizationService.getCurrentUserActor()) {
            var currentCompany = authorizationService.getCurrentUserCompanyId();
            if (currentCompany != null) {
                var companyShrinkageIds = shrinkageRepository.findByCompanyIdValue(currentCompany.value()).stream()
                        .map(com.fluxusbackend.shrinkage.domain.model.aggregates.Shrinkage::getShrinkageId)
                        .toList();
                return logs.stream()
                        .filter(log -> !"SHRINKAGE".equals(log.getEntityType()) || companyShrinkageIds.contains(log.getEntityId()))
                        .toList();
            }
        }
        return logs;
    }

    @GetMapping
    @Operation(summary = "List status change logs")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Logs retrieved",
                    content = @Content(schema = @Schema(implementation = StatusChangeLog.class))),
            @ApiResponse(responseCode = "403", description = "Access denied", content = @Content)
    })
    public List<StatusChangeLog> list(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Long entityId,
            @RequestParam(required = false) Long userId
    ) {
        authorizationService.requireActor(UserActor.RETAIL, UserActor.BENEFICIARY);
        var sort = Sort.by(Sort.Direction.DESC, "changedAt");
        if (entityType != null && entityId != null) {
            return filterLogsForRetail(repository.findByEntityTypeAndEntityId(entityType, entityId, sort));
        }
        if (entityType != null && userId != null) {
            return filterLogsForRetail(repository.findByEntityTypeAndChangedByUserId(entityType, userId, sort));
        }
        if (entityType != null) {
            return filterLogsForRetail(repository.findByEntityType(entityType, sort));
        }
        if (userId != null) {
            return filterLogsForRetail(repository.findByChangedByUserId(userId, sort));
        }
        return filterLogsForRetail(repository.findAll(sort));
    }

    @GetMapping("/{entityType}/{entityId}")
    @Operation(summary = "List status change logs for an entity")
    public List<StatusChangeLog> listByEntity(
            @PathVariable String entityType,
            @PathVariable Long entityId
    ) {
        authorizationService.requireActor(UserActor.RETAIL, UserActor.BENEFICIARY);
        var sort = Sort.by(Sort.Direction.DESC, "changedAt");
        return filterLogsForRetail(repository.findByEntityTypeAndEntityId(entityType, entityId, sort));
    }
}
