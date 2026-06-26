package com.fluxusbackend.notificationservice.infrastructure.messaging;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String NOTIFICATION_EXCHANGE = "notification.events.exchange";
    public static final String EMAIL_QUEUE = "notification.email.queue";
    public static final String EMAIL_ROUTING_KEY = "notification.email.#";

    public static final String EMAIL_DLX = "notification.email.dlx";
    public static final String EMAIL_DLQ = "notification.email.dlq";
    public static final String EMAIL_DLQ_ROUTING_KEY = "notification.email.dead";

    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(NOTIFICATION_EXCHANGE);
    }

    @Bean
    public Queue emailQueue() {
        return QueueBuilder.durable(EMAIL_QUEUE)
                .withArgument("x-dead-letter-exchange", EMAIL_DLX)
                .withArgument("x-dead-letter-routing-key", EMAIL_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding emailBinding(Queue emailQueue, TopicExchange notificationExchange) {
        return BindingBuilder.bind(emailQueue).to(notificationExchange).with(EMAIL_ROUTING_KEY);
    }

    // Dead Letter Exchange and Queue Configuration
    @Bean
    public TopicExchange emailDeadLetterExchange() {
        return new TopicExchange(EMAIL_DLX);
    }

    @Bean
    public Queue emailDeadLetterQueue() {
        return QueueBuilder.durable(EMAIL_DLQ).build();
    }

    @Bean
    public Binding emailDeadLetterBinding(Queue emailDeadLetterQueue, TopicExchange emailDeadLetterExchange) {
        return BindingBuilder.bind(emailDeadLetterQueue).to(emailDeadLetterExchange).with(EMAIL_DLQ_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
