package com.ecommerce.payment.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class PaymentConsumer {

    private static final Logger log =
            LoggerFactory.getLogger(PaymentConsumer.class);

    private final ObjectMapper objectMapper;
    private final RedisIdempotencyGuard idempotencyGuard;
    private final PaymentRetryPublisher retryPublisher;

    @Value("${payment.retry-test.enabled:false}")
    private boolean retryTestEnabled;

    @Value("${payment.processing-test.enabled:false}")
    private boolean processingTestEnabled;

    @Value("${payment.processing-test.delay-ms:10000}")
    private long processingTestDelayMs;

    private final ConcurrentHashMap<String, AtomicInteger> retryAttempts =
            new ConcurrentHashMap<>();

    public PaymentConsumer(
            ObjectMapper objectMapper,
            RedisIdempotencyGuard idempotencyGuard,
            PaymentRetryPublisher retryPublisher) {

        this.objectMapper = objectMapper;
        this.idempotencyGuard = idempotencyGuard;
        this.retryPublisher = retryPublisher;
    }

    @RabbitListener(queues = "payment.queue")
    public void consume(Message message) {

        try {
            JsonNode event = objectMapper.readTree(message.getBody());

            JsonNode eventIdNode = event.get("eventId");

            if (eventIdNode == null || eventIdNode.isNull()) {
                throw new IllegalArgumentException(
                        "Payment event does not contain eventId"
                );
            }

            String eventId = eventIdNode.asText();

            RedisIdempotencyGuard.ClaimResult claim =
                    idempotencyGuard.claim(eventId);

            // Case 1: Event already completed
            if (claim.status()
                    == RedisIdempotencyGuard.ClaimStatus.COMPLETED) {

                log.info(
                        "Duplicate payment event skipped: {}",
                        eventId
                );
                return;
            }

            // Case 2: Another consumer currently owns this event
            if (claim.status()
                    == RedisIdempotencyGuard.ClaimStatus.IN_PROGRESS) {

                log.info(
                        "Event currently processing. Deferring: {}",
                        eventId
                );

                retryPublisher.publish(message);

                log.info(
                        "Event safely published to delayed retry queue: {}",
                        eventId
                );

                return;
            }

            // Case 3: This consumer acquired the lock
            String token = claim.token();

            try {

                log.info(
                        "Payment Service processing event: {}",
                        eventId
                );

                // Test scenario 1: Simulate temporary retry failures
                if (retryTestEnabled
                        && eventId.startsWith("retry-test-")) {

                    int attempt = retryAttempts
                            .computeIfAbsent(
                                    eventId,
                                    id -> new AtomicInteger(0)
                            )
                            .incrementAndGet();

                    log.info(
                            "Retry test attempt {} for event {}",
                            attempt,
                            eventId
                    );

                    if (attempt <= 2) {
                        throw new RuntimeException(
                                "Simulated payment failure: attempt " + attempt
                        );
                    }
                }

                // Test scenario 2: Simulate slow processing
                if (processingTestEnabled
                        && eventId.startsWith("processing-test-")) {

                    log.info(
                            "Simulating slow payment processing: {} ms",
                            processingTestDelayMs
                    );

                    try {
                        Thread.sleep(processingTestDelayMs);
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();

                        throw new RuntimeException(
                                "Payment processing interrupted",
                                ex
                        );
                    }
                }

                /*
                 * IMPORTANT:
                 * Call your existing payment business logic here.
                 *
                 * For example:
                 * paymentService.processPayment(event);
                 *
                 * Do not remove your existing payment processing logic.
                 */

                processPaymentEvent(event);

                // Mark event completed only after successful processing
                idempotencyGuard.markCompleted(eventId, token);

                log.info(
                        "Payment event completed successfully: {}",
                        eventId
                );

            } catch (RuntimeException ex) {

                // Release only if this consumer still owns the lock
                idempotencyGuard.release(eventId, token);

                log.error(
                        "Payment processing failed for event: {}",
                        eventId,
                        ex
                );

                throw ex;
            }

        } catch (RuntimeException ex) {
            throw ex;

        } catch (Exception ex) {
            throw new RuntimeException(
                    "Unable to deserialize payment event",
                    ex
            );
        }
    }

    private void processPaymentEvent(JsonNode event) {

        /*
         * Replace this method body with your existing payment business logic.
         * This placeholder does not execute a real payment.
         */

        log.info("Payment event received: {}", event);
    }
}