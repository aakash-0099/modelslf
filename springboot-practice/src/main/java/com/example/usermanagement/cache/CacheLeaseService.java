package com.example.usermanagement.cache;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.UUID;

@Service
public class CacheLeaseService {

    private static final String LEASE_PREFIX = "cache-lease::";
    private static final Duration LEASE_TTL = Duration.ofSeconds(5);

    private static final DefaultRedisScript<Long> RELEASE_SCRIPT =
            new DefaultRedisScript<>(
                    """
                    if redis.call('get', KEYS[1]) == ARGV[1] then
                        return redis.call('del', KEYS[1])
                    else
                        return 0
                    end
                    """,
                    Long.class
            );

    private final StringRedisTemplate redisTemplate;

    public CacheLeaseService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String tryAcquire(String cacheKey) {

        String leaseKey = LEASE_PREFIX + cacheKey;
        String token = UUID.randomUUID().toString();

        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(
                        leaseKey,
                        token,
                        LEASE_TTL
                );

        return Boolean.TRUE.equals(acquired)
                ? token
                : null;
    }

    public void release(String cacheKey, String token) {

        String leaseKey = LEASE_PREFIX + cacheKey;

        redisTemplate.execute(
                RELEASE_SCRIPT,
                Collections.singletonList(leaseKey),
                token
        );
    }
}