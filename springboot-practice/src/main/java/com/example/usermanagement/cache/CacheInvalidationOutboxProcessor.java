package com.example.usermanagement.cache;

import com.example.usermanagement.entity.CacheInvalidationEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnection;
import java.nio.charset.StandardCharsets;

import java.util.List;

@Component
public class CacheInvalidationOutboxProcessor {

    private final RedisConnectionFactory redisConnectionFactory;
    private static final Logger log =
            LoggerFactory.getLogger(
                    CacheInvalidationOutboxProcessor.class
            );

    private final CacheInvalidationEventRepository eventRepository;

    public CacheInvalidationOutboxProcessor(
            CacheInvalidationEventRepository eventRepository,
            RedisConnectionFactory redisConnectionFactory
    ) {
        this.eventRepository = eventRepository;
        this.redisConnectionFactory = redisConnectionFactory;
    }

    @Scheduled(fixedDelay = 100000)
    public void processEvents() {

        List<CacheInvalidationEvent> events =
                eventRepository.findAllByOrderByIdAsc();

        for (CacheInvalidationEvent event : events) {

            try {

                String redisKey =
                        event.getCacheName()
                                + "::"
                                + event.getCacheKey();

                // Talking  directly to redis using RedisConnectionFactory.
                try (RedisConnection connection =
                             redisConnectionFactory.getConnection()) {

                    connection.keyCommands().del(
                            redisKey.getBytes(StandardCharsets.UTF_8)               // Delete the cache entry from Redis by constructing the Redis key 
                                                                                    // using the cache name and cache key from the event and converting it to bytes.
                    );
                }

                eventRepository.delete(event);

                log.info(
                        "Processed cache invalidation event. " +
                        "id={}, redisKey={}",
                        event.getId(),
                        redisKey
                );

            } catch (RuntimeException exception) {

                log.warn(
                        "Failed to process cache invalidation event. " +
                        "id={}, cache={}, key={}. Will retry.",
                        event.getId(),
                        event.getCacheName(),
                        event.getCacheKey(),
                        exception
                );
            }
        }
    }
}