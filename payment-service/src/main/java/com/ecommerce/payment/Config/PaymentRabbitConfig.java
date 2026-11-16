package com.ecommerce.payment.Config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class PaymentRabbitConfig {

    public static final String PAYMENT_QUEUE = "payment.queue";
    public static final String PAYMENT_DLX = "payment.dlx";
    public static final String PAYMENT_DLQ = "payment.dlq";
    public static final String PAYMENT_RETRY_QUEUE =
            "payment.processing.retry";

    public static final String PAYMENT_FAILED_ROUTING_KEY =
            "payment.failed";

    @Bean
    public TopicExchange paymentDeadLetterExchange() {
        return new TopicExchange(PAYMENT_DLX, true, false);
    }

    @Bean
    public Queue paymentDeadLetterQueue() {
        return new Queue(PAYMENT_DLQ, true);
    }

    @Bean
    public Binding paymentDeadLetterBinding() {
        return BindingBuilder
                .bind(paymentDeadLetterQueue())
                .to(paymentDeadLetterExchange())
                .with(PAYMENT_FAILED_ROUTING_KEY);
    }

    @Bean
    public Queue paymentQueue() {

        Map<String, Object> arguments = new HashMap<>();

        arguments.put("x-dead-letter-exchange", PAYMENT_DLX);
        arguments.put(
                "x-dead-letter-routing-key",
                PAYMENT_FAILED_ROUTING_KEY
        );

        return new Queue(
                PAYMENT_QUEUE,
                true,
                false,
                false,
                arguments
        );
    }

    @Bean
    public Queue paymentProcessingRetryQueue() {

        Map<String, Object> arguments = new HashMap<>();

        // Wait 5 seconds before retrying
        arguments.put("x-message-ttl", 5000);

        // Return expired messages to the original payment queue
        arguments.put("x-dead-letter-exchange", "");
        arguments.put("x-dead-letter-routing-key", PAYMENT_QUEUE);

        return new Queue(
                PAYMENT_RETRY_QUEUE,
                true,
                false,
                false,
                arguments
        );
    }
}