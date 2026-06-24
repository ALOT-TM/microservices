package com.fluxusbackend.shrinkage.infrastructure.messaging.listeners;

import com.fluxusbackend.shrinkage.domain.model.commands.MarkShrinkageInProcessCommand;
import com.fluxusbackend.shrinkage.domain.model.valueobjects.ShrinkageId;
import com.fluxusbackend.shrinkage.domain.services.ShrinkageCommandService;
import com.fluxusbackend.shrinkage.infrastructure.messaging.RabbitMQConfig;
import com.fluxusbackend.shrinkage.infrastructure.messaging.events.DonationRequestAcceptedEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class DonationRequestListener {

    private final ShrinkageCommandService commandService;

    public DonationRequestListener(ShrinkageCommandService commandService) {
        this.commandService = commandService;
    }

    @RabbitListener(queues = RabbitMQConfig.REQUEST_QUEUE_NAME)
    public void handleDonationRequestAccepted(DonationRequestAcceptedEvent event) {
        System.out.println("Received DonationRequestAcceptedEvent for shrinkage ID: " + event.shrinkageId());
        try {
            var command = new MarkShrinkageInProcessCommand(new ShrinkageId(event.shrinkageId()));
            commandService.handle(command);
            System.out.println("Successfully marked shrinkage ID " + event.shrinkageId() + " as IN_PROCESS");
        } catch (Exception e) {
            System.err.println("Error processing DonationRequestAcceptedEvent for shrinkage ID " + event.shrinkageId());
            e.printStackTrace();
            throw e;
        }
    }
}
