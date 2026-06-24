package com.fluxusbackend.shrinkage.infrastructure.messaging.listeners;

import com.fluxusbackend.shrinkage.domain.model.commands.MarkShrinkageDonatedCommand;
import com.fluxusbackend.shrinkage.domain.model.valueobjects.ShrinkageId;
import com.fluxusbackend.shrinkage.domain.services.ShrinkageCommandService;
import com.fluxusbackend.shrinkage.infrastructure.messaging.RabbitMQConfig;
import com.fluxusbackend.shrinkage.infrastructure.messaging.events.DonationPickupConfirmedEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class DonationPickupListener {

    private final ShrinkageCommandService commandService;

    public DonationPickupListener(ShrinkageCommandService commandService) {
        this.commandService = commandService;
    }

    @RabbitListener(queues = RabbitMQConfig.PICKUP_QUEUE_NAME)
    public void handleDonationPickupConfirmed(DonationPickupConfirmedEvent event) {
        System.out.println("Received DonationPickupConfirmedEvent for shrinkage ID: " + event.shrinkageId());
        try {
            var command = new MarkShrinkageDonatedCommand(new ShrinkageId(event.shrinkageId()));
            commandService.handle(command);
            System.out.println("Successfully marked shrinkage ID " + event.shrinkageId() + " as DONATED");
        } catch (Exception e) {
            System.err.println("Error processing DonationPickupConfirmedEvent for shrinkage ID " + event.shrinkageId());
            e.printStackTrace();
            throw e; // Throwing triggers RabbitMQ's retry / DLQ mechanisms
        }
    }
}
