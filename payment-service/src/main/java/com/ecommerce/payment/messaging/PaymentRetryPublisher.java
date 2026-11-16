
package com.ecommerce.payment.messaging;

import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class PaymentRetryPublisher {

    private static final String RETRY_QUEUE =
            "payment.processing.retry";

    private static final long CONFIRM_TIMEOUT_SECONDS = 5;

    private final RabbitTemplate rabbitTemplate;

    public PaymentRetryPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(Message originalMessage) {

        CorrelationData correlationData =
                new CorrelationData();

        try {
            rabbitTemplate.send(
                    "",
                    RETRY_QUEUE,
                    originalMessage,
                    correlationData
            );

            CorrelationData.Confirm confirm =
                    correlationData.getFuture().get(
                            CONFIRM_TIMEOUT_SECONDS,
                            TimeUnit.SECONDS
                    );

            if (!confirm.isAck()) {
                throw new IllegalStateException(
                        "RabbitMQ rejected retry message: "
                                + confirm.getReason()
                );
            }

            if (correlationData.getReturned() != null) {
                throw new IllegalStateException(
                        "Retry message was returned by RabbitMQ"
                );
            }

            System.out.println(
                    "Event safely published to delayed retry queue"
            );

        } catch (Exception ex) {

            throw new IllegalStateException(
                    "Failed to publish payment retry message",
                    ex
            );
        }
    }
}