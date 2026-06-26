package com.fluxusbackend.notificationservice.infrastructure.messaging;

import com.fluxusbackend.notificationservice.application.service.EmailService;
import com.fluxusbackend.notificationservice.domain.model.NotificationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Component;

@Component
public class NotificationRabbitListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationRabbitListener.class);
    private final EmailService emailService;

    public NotificationRabbitListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @RabbitListener(queues = RabbitMQConfig.EMAIL_QUEUE)
    public void processNotification(NotificationEvent event) {
        log.info("Received notification event for recipient: {} of type: {}", event.recipient(), event.notificationType());
        try {
            emailService.sendEmailNotification(event);
            log.info("Successfully processed and sent email to {}", event.recipient());
        } catch (MailException e) {
            log.error("SMTP Mail delivery failed for recipient {}. Routing message to DLQ.", event.recipient(), e);
            throw new AmqpRejectAndDontRequeueException("SMTP Mail delivery failed, routing to DLQ", e);
        } catch (Exception e) {
            log.error("Error processing notification event for recipient {}. Routing message to DLQ.", event.recipient(), e);
            throw new AmqpRejectAndDontRequeueException("Unexpected error during processing, routing to DLQ", e);
        }
    }
}
