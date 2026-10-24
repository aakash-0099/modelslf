package com.example.usermanagement.cache;

import java.time.Duration;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    private final RedisCacheErrorHandler redisCacheErrorHandler;

    public CacheConfig(
            RedisCacheErrorHandler redisCacheErrorHandler) {

        this.redisCacheErrorHandler = redisCacheErrorHandler;
    }

    @Bean
    public CacheManager cacheManager(
            RedisConnectionFactory redisConnectionFactory,
            CacheLeaseService cacheLeaseService) {

        RedisCacheConfiguration cacheConfiguration =
                RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofMinutes(10))
                        .serializeKeysWith(
                                RedisSerializationContext.SerializationPair
                                        .fromSerializer(
                                                new StringRedisSerializer()))
                        .serializeValuesWith(
                                RedisSerializationContext.SerializationPair
                                        .fromSerializer(
                                                new GenericJackson2JsonRedisSerializer()));

        RedisCacheWriter cacheWriter =
                RedisCacheWriter.nonLockingRedisCacheWriter(
                        redisConnectionFactory);

        return new LeaseAwareRedisCacheManager(
                cacheWriter,
                cacheConfiguration,
                cacheLeaseService
        );
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return redisCacheErrorHandler;
    }
}