package com.fluxusbackend.companyretail.infrastructure.messaging.events;

public record HeadquarterRegisteredEvent(
        Long headquarterId,
        Long companyId
) {}
