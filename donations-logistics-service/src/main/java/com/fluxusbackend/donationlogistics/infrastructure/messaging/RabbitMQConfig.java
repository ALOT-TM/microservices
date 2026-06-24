package com.fluxusbackend.donationlogistics.infrastructure.messaging;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "donation.events.exchange";

    public static final String PICKUP_QUEUE_NAME = "shrinkage.donation-pickup.queue";
    public static final String PICKUP_ROUTING_KEY = "donation.pickup.confirmed";

    public static final String REQUEST_QUEUE_NAME = "shrinkage.donation-request.queue";
    public static final String REQUEST_ROUTING_KEY = "donation.request.accepted";

    @Bean
    public TopicExchange donationExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue pickupQueue() {
        return QueueBuilder.durable(PICKUP_QUEUE_NAME).build();
    }

    @Bean
    public Binding pickupBinding(Queue pickupQueue, TopicExchange donationExchange) {
        return BindingBuilder.bind(pickupQueue).to(donationExchange).with(PICKUP_ROUTING_KEY);
    }

    @Bean
    public Queue requestQueue() {
        return QueueBuilder.durable(REQUEST_QUEUE_NAME).build();
    }

    @Bean
    public Binding requestBinding(Queue requestQueue, TopicExchange donationExchange) {
        return BindingBuilder.bind(requestQueue).to(donationExchange).with(REQUEST_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
