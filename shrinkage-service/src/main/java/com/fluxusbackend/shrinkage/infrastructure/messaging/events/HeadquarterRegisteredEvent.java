package com.fluxusbackend.shrinkage.infrastructure.messaging.events;

public record HeadquarterRegisteredEvent(
        Long headquarterId,
        Long companyId
) {}
