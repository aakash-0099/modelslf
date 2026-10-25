package com.example.usermanagement.cache;

import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import com.example.usermanagement.cache.CacheLeaseService;
import com.example.usermanagement.cache.LeaseAwareRedisCache;

public class LeaseAwareRedisCacheManager extends RedisCacheManager {

    private final CacheLeaseService cacheLeaseService;

    public LeaseAwareRedisCacheManager(
            RedisCacheWriter cacheWriter,
            RedisCacheConfiguration defaultCacheConfiguration,
            CacheLeaseService cacheLeaseService) {

        super(cacheWriter, defaultCacheConfiguration);
        this.cacheLeaseService = cacheLeaseService;
    }

    @Override
    protected RedisCache createRedisCache(
            String name,
            RedisCacheConfiguration cacheConfiguration) {

        return new LeaseAwareRedisCache(
                name,
                getCacheWriter(),
                cacheConfiguration,
                cacheLeaseService
        );
    }
}