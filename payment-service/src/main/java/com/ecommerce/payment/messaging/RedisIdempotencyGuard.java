package com.ecommerce.payment.messaging;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Component
public class RedisIdempotencyGuard {

    private static final Duration PROCESSING_TTL =
            Duration.ofMinutes(2);

    private static final Duration COMPLETED_TTL =
            Duration.ofDays(7);

    private static final String COMPLETED = "COMPLETED";

    private final StringRedisTemplate redisTemplate;

    public RedisIdempotencyGuard(
            StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public enum ClaimStatus {
        ACQUIRED,
        IN_PROGRESS,
        COMPLETED
    }

    public record ClaimResult(
            ClaimStatus status,
            String token
    ) {}

    private static final DefaultRedisScript<Long> CLAIM_SCRIPT =
            new DefaultRedisScript<>(
                    """
                    local current = redis.call('GET', KEYS[1])

                    if not current then
                        redis.call(
                            'SET',
                            KEYS[1],
                            ARGV[1],
                            'EX',
                            ARGV[2]
                        )
                        return 1
                    end

                    if current == 'COMPLETED' then
                        return 3
                    end

                    return 2
                    """,
                    Long.class
            );

    private static final DefaultRedisScript<Long> COMPLETE_SCRIPT =
            new DefaultRedisScript<>(
                    """
                    local current = redis.call('GET', KEYS[1])

                    if current == ARGV[1] then
                        redis.call(
                            'SET',
                            KEYS[1],
                            'COMPLETED',
                            'EX',
                            ARGV[2]
                        )
                        return 1
                    end

                    return 0
                    """,
                    Long.class
            );

    private static final DefaultRedisScript<Long> RELEASE_SCRIPT =
            new DefaultRedisScript<>(
                    """
                    local current = redis.call('GET', KEYS[1])

                    if current == ARGV[1] then
                        return redis.call('DEL', KEYS[1])
                    end

                    return 0
                    """,
                    Long.class
            );

    public ClaimResult claim(String eventId) {

        String key = "payment:idempotency:" + eventId;

        String token = "PROCESSING:" + UUID.randomUUID();

        Long result = redisTemplate.execute(
                CLAIM_SCRIPT,
                List.of(key),
                token,
                String.valueOf(PROCESSING_TTL.toSeconds())
        );

        if (result == null) {
            throw new IllegalStateException(
                    "Redis returned null while claiming event: " + eventId
            );
        }

        if (result == 1L) {
            return new ClaimResult(ClaimStatus.ACQUIRED, token);
        }

        if (result == 3L) {
            return new ClaimResult(ClaimStatus.COMPLETED, null);
        }

        return new ClaimResult(ClaimStatus.IN_PROGRESS, null);
    }

    public void markCompleted(String eventId, String token) {

        String key = "payment:idempotency:" + eventId;

        Long result = redisTemplate.execute(
                COMPLETE_SCRIPT,
                List.of(key),
                token,
                String.valueOf(COMPLETED_TTL.toSeconds())
        );

        if (result == null || result != 1L) {
            throw new IllegalStateException(
                    "Lost Redis processing ownership for event: " + eventId
            );
        }
    }

    public void release(String eventId, String token) {

        if (token == null) {
            return;
        }

        String key = "payment:idempotency:" + eventId;

        redisTemplate.execute(
                RELEASE_SCRIPT,
                List.of(key),
                token
        );
    }
}