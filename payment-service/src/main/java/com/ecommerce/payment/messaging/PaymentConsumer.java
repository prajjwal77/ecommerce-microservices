
package com.ecommerce.payment.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class PaymentConsumer {

    private final ObjectMapper objectMapper;
    private final RedisIdempotencyGuard idempotencyGuard;

    @Value("${payment.retry-test.enabled:false}")
    private boolean retryTestEnabled;

    private final ConcurrentHashMap<String, AtomicInteger> retryAttempts =
            new ConcurrentHashMap<>();

    public PaymentConsumer(
            ObjectMapper objectMapper,
            RedisIdempotencyGuard idempotencyGuard) {
        this.objectMapper = objectMapper;
        this.idempotencyGuard = idempotencyGuard;
    }

    @RabbitListener(queues = "payment.queue")
    public void consume(String message) {
        OrderCreatedEvent event =
                objectMapper.readValue(message, OrderCreatedEvent.class);

        if (event.eventId() == null || event.eventId().isBlank()) {
            throw new IllegalArgumentException(
                    "Order event must contain an eventId"
            );
        }

        String token = idempotencyGuard.claim(event.eventId());

        if (token == null) {
            System.out.println(
                    "Duplicate event skipped: " + event.eventId()
            );
            return;
        }

        try {
            // TEST ONLY: simulate two transient failures.
            if (retryTestEnabled
                    && event.eventId().startsWith("retry-test-")) {

                int attempt = retryAttempts
                        .computeIfAbsent(
                                event.eventId(),
                                id -> new AtomicInteger()
                        )
                        .incrementAndGet();

                System.out.println(
                        "Retry test event " + event.eventId()
                                + " - attempt " + attempt
                );

                if (attempt <= 2) {
                    throw new IllegalStateException(
                            "Simulated transient payment failure"
                    );
                }
            }

            // Demo processing only: no real payment is executed here.
            System.out.println(
                    "Payment Service processing event: " + event
            );

            idempotencyGuard.markCompleted(event.eventId(), token);

            if (retryTestEnabled
                    && event.eventId().startsWith("retry-test-")) {
                retryAttempts.remove(event.eventId());
            }

        } catch (RuntimeException ex) {
            idempotencyGuard.release(event.eventId(), token);
            throw ex;
        }
    }
}
