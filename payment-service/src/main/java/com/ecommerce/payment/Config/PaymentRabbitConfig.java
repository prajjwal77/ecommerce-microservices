
package com.ecommerce.payment.Config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PaymentRabbitConfig {

    @Bean
    public Declarables paymentDeadLetterTopology() {

        TopicExchange deadLetterExchange =
                new TopicExchange("payment.dlx", true, false);

        Queue deadLetterQueue =
                QueueBuilder.durable("payment.dlq").build();

        Binding deadLetterBinding =
                BindingBuilder.bind(deadLetterQueue)
                        .to(deadLetterExchange)
                        .with("payment.failed");

        Queue paymentQueue =
                QueueBuilder.durable("payment.queue")
                        .deadLetterExchange("payment.dlx")
                        .deadLetterRoutingKey("payment.failed")
                        .build();

        return new Declarables(
                deadLetterExchange,
                deadLetterQueue,
                deadLetterBinding,
                paymentQueue
        );
    }
}
