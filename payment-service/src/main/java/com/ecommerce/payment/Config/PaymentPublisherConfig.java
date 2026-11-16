package com.ecommerce.payment.Config;

import jakarta.annotation.PostConstruct;

import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory.ConfirmType;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentPublisherConfig {

    private final CachingConnectionFactory connectionFactory;
    private final RabbitTemplate rabbitTemplate;

    public PaymentPublisherConfig(
            CachingConnectionFactory connectionFactory,
            RabbitTemplate rabbitTemplate) {

        this.connectionFactory = connectionFactory;
        this.rabbitTemplate = rabbitTemplate;
    }

    @PostConstruct
    public void configurePublisherReliability() {

        // Enable correlated publisher confirms
        connectionFactory.setPublisherConfirmType(
                ConfirmType.CORRELATED
        );

        // Enable publisher returns
        connectionFactory.setPublisherReturns(true);

        // Ensure unroutable messages are returned
        rabbitTemplate.setMandatory(true);
    }
}