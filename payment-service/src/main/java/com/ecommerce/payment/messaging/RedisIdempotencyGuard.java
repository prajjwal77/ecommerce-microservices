
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
            Duration.ofHours(1);

    private static final Duration COMPLETED_TTL =
            Duration.ofDays(7);

    private static final DefaultRedisScript<Long> COMPLETE_SCRIPT =
            new DefaultRedisScript<>(
                    "if redis.call('GET', KEYS[1]) == ARGV[1] then " +
                    "return redis.call('SET', KEYS[1], 'COMPLETED', " +
                    "'EX', ARGV[2]) and 1 else return 0 end",
                    Long.class
            );

    private static final DefaultRedisScript<Long> RELEASE_SCRIPT =
            new DefaultRedisScript<>(
                    "if redis.call('GET', KEYS[1]) == ARGV[1] then " +
                    "return redis.call('DEL', KEYS[1]) else return 0 end",
                    Long.class
            );

    private final StringRedisTemplate redisTemplate;

    public RedisIdempotencyGuard(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String claim(String eventId) {
        String key = key(eventId);
        String token = UUID.randomUUID().toString();

        Boolean claimed = redisTemplate.opsForValue().setIfAbsent(
                key, token, PROCESSING_TTL
        );

        return Boolean.TRUE.equals(claimed) ? token : null;
    }

    public void markCompleted(String eventId, String token) {
        Long result = redisTemplate.execute(
                COMPLETE_SCRIPT,
                List.of(key(eventId)),
                token,
                String.valueOf(COMPLETED_TTL.toSeconds())
        );

        if (!Long.valueOf(1).equals(result)) {
            throw new IllegalStateException(
                    "Lost Redis processing claim for event " + eventId
            );
        }
    }

    public void release(String eventId, String token) {
        redisTemplate.execute(
                RELEASE_SCRIPT,
                List.of(key(eventId)),
                token
        );
    }

    private String key(String eventId) {
        return "payment:event:" + eventId;
    }
}
