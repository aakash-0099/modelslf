package com.example.usermanagement.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class RateLimitService {

    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    private final long capacity;
    private final long refillTokens;
    private final long refillDurationSeconds;

    public RateLimitService(
            @Value("${rate-limit.capacity}") long capacity,
            @Value("${rate-limit.refill-tokens}") long refillTokens,
            @Value("${rate-limit.refill-duration-seconds}") long refillDurationSeconds
    ) {
        this.capacity = capacity;
        this.refillTokens = refillTokens;
        this.refillDurationSeconds = refillDurationSeconds;
    }

    public boolean isAllowed(String key) {
        Bucket bucket = buckets.computeIfAbsent(key, this::createBucket);

        return bucket.tryConsume(1);
    }

    private Bucket createBucket(String key) {
        Refill refill = Refill.greedy(
                refillTokens,
                Duration.ofSeconds(refillDurationSeconds)
        );

        Bandwidth limit = Bandwidth.classic(
                capacity,
                refill
        );

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}