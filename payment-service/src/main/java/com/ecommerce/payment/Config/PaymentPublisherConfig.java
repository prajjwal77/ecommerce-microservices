
package com.ecommerce.payment.Config;

import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory.ConfirmType;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
public class PaymentPublisherConfig {

    private final CachingConnectionFactory connectionFactory;

    public PaymentPublisherConfig(
            CachingConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @PostConstruct
    public void configurePublisherConfirms() {
        connectionFactory.setPublisherConfirmType(
                ConfirmType.CORRELATED);
        connectionFactory.setPublisherReturns(true);
    }
}
