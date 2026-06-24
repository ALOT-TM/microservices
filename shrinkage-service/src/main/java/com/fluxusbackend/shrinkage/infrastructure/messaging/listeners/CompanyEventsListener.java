package com.fluxusbackend.shrinkage.infrastructure.messaging.listeners;

import com.fluxusbackend.shrinkage.domain.model.aggregates.HeadquarterCache;
import com.fluxusbackend.shrinkage.infrastructure.persistence.jpa.repositories.HeadquarterCacheRepository;
import com.fluxusbackend.shrinkage.infrastructure.messaging.RabbitMQConfig;
import com.fluxusbackend.shrinkage.infrastructure.messaging.events.HeadquarterRegisteredEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class CompanyEventsListener {

    private final HeadquarterCacheRepository headquarterCacheRepository;

    public CompanyEventsListener(HeadquarterCacheRepository headquarterCacheRepository) {
        this.headquarterCacheRepository = headquarterCacheRepository;
    }

    @RabbitListener(queues = RabbitMQConfig.HQ_QUEUE_NAME)
    public void handleHeadquarterRegistered(HeadquarterRegisteredEvent event) {
        System.out.println("Received HeadquarterRegisteredEvent. Mapping HQ " + event.headquarterId() + " to company " + event.companyId());
        try {
            var cache = new HeadquarterCache(event.headquarterId(), event.companyId());
            headquarterCacheRepository.save(cache);
            System.out.println("Successfully cached headquarter ID " + event.headquarterId());
        } catch (Exception e) {
            System.err.println("Error caching headquarter ID " + event.headquarterId());
            e.printStackTrace();
            throw e;
        }
    }
}
