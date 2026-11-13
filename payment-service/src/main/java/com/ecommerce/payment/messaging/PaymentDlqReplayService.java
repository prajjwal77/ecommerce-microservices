
package com.ecommerce.payment.messaging;

import com.rabbitmq.client.GetResponse;
import com.rabbitmq.client.Channel;

import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Service
public class PaymentDlqReplayService {

    private static final String DLQ = "payment.dlq";
    private static final String PAYMENT_QUEUE = "payment.queue";
    private static final long CONFIRM_TIMEOUT_SECONDS = 5;

    private final ConnectionFactory connectionFactory;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public PaymentDlqReplayService(
            ConnectionFactory connectionFactory,
            RabbitTemplate rabbitTemplate,
            ObjectMapper objectMapper) {
        this.connectionFactory = connectionFactory;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    public boolean replayNextFromDlq() {
        Connection connection = null;
        Channel channel = null;

        try {
            connection = connectionFactory.createConnection();
            channel = connection.createChannel(false);

            // Fetch one message without acknowledging it.
            GetResponse original = channel.basicGet(DLQ, false);

            if (original == null) {
                System.out.println("No messages available in " + DLQ);
                return false;
            }

            long deliveryTag =
                    original.getEnvelope().getDeliveryTag();

            String json = new String(
                    original.getBody(),
                    StandardCharsets.UTF_8);

            OrderCreatedEvent event =
                    objectMapper.readValue(json, OrderCreatedEvent.class);

            if (event.eventId() == null
                    || event.eventId().isBlank()) {
                throw new IllegalArgumentException(
                        "DLQ message has no eventId");
            }

            CorrelationData correlationData =
                    new CorrelationData(event.eventId());

            // Publish directly to payment.queue, not order.events.
            rabbitTemplate.convertAndSend(
                    "",
                    PAYMENT_QUEUE,
                    json,
                    correlationData);

            CorrelationData.Confirm confirm =
                    correlationData.getFuture().get(
                            CONFIRM_TIMEOUT_SECONDS,
                            TimeUnit.SECONDS);

            if (!confirm.isAck()) {
                throw new IllegalStateException(
                        "RabbitMQ negatively acknowledged replay publication");
            }

            if (correlationData.getReturned() != null) {
                throw new IllegalStateException(
                        "Replay message was returned as unroutable");
            }

            // Only acknowledge the original after confirmed publication.
            channel.basicAck(deliveryTag, false);

            System.out.println(
                    "DLQ replay publication confirmed for eventId: "
                            + event.eventId());

            return true;

        } catch (Exception ex) {
            // Original remains unacknowledged. Closing its channel
            // requeues it, although publication may be uncertain on timeout.
            throw new IllegalStateException(
                    "DLQ replay did not complete safely; "
                            + "the original message was not acknowledged",
                    ex);

        } finally {
            if (channel != null) {
                try {
                    channel.close();
                } catch (Exception ignored) {
                    // Closing the channel allows unacknowledged
                    // deliveries to be requeued by RabbitMQ.
                }
            }

            if (connection != null) {
                try {
                    connection.close();
                } catch (Exception ignored) {
                    // Connection cleanup.
                }
            }
        }
    }
}
