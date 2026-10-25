package com.example.usermanagement.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheWriter;

import java.time.Duration;
import java.util.concurrent.Callable;

public class LeaseAwareRedisCache extends RedisCache {

    private static final Logger log =
            LoggerFactory.getLogger(LeaseAwareRedisCache.class);

    private static final Duration WAIT_INTERVAL =
            Duration.ofMillis(100);

    private static final Duration MAX_WAIT_TIME =
            Duration.ofSeconds(6);

    private final CacheLeaseService cacheLeaseService;

    protected LeaseAwareRedisCache(
            String name,
            RedisCacheWriter cacheWriter,
            RedisCacheConfiguration cacheConfiguration,
            CacheLeaseService cacheLeaseService) {

        super(name, cacheWriter, cacheConfiguration);
        this.cacheLeaseService = cacheLeaseService;
    }

    @Override
    public <T> T get(Object key, Callable<T> valueLoader) {
        log.info(
            "LeaseAwareRedisCache.get() CALLED | cache={} | key={} | thread={}",
            getName(),
            key,
            Thread.currentThread().getName()
    );

        String cacheKey = getName() + "::" + key;

        String request =
                Thread.currentThread().getName();

        /*
         * STEP 1
         * Normal cache lookup.
         */
        Cache.ValueWrapper cachedValue = get(key);

        if (cachedValue != null) {

            log.info(
                    "[{}] CACHE HIT | key={}",
                    request,
                    cacheKey
            );

            return (T) cachedValue.get();
        }

        log.info(
                "[{}] CACHE MISS | key={}",
                request,
                cacheKey
        );

        /*
         * STEP 2
         * Try to acquire the lease.
         */
        log.info(
                "[{}] TRYING TO ACQUIRE LEASE | key={}",
                request,
                cacheKey
        );

        String leaseToken =
                cacheLeaseService.tryAcquire(cacheKey);

        /*
         * STEP 3
         * Lease acquired.
         */
        if (leaseToken != null) {

            log.info(
                    "[{}] LEASE ACQUIRED | key={} | This request will load the data",
                    request,
                    cacheKey
            );

            try {

                log.info(
                        "[{}] LOADING DATA FROM DATABASE | key={}",
                        request,
                        cacheKey
                );

                return super.get(key, valueLoader);

            } finally {

                cacheLeaseService.release(
                        cacheKey,
                        leaseToken
                );

                log.info(
                        "[{}] LEASE RELEASED | key={}",
                        request,
                        cacheKey
                );
            }
        }

        /*
         * STEP 4
         * Lease denied.
         *
         * Another request is already loading the data.
         */
        log.info(
                "[{}] LEASE DENIED | key={} | Another request already owns the lease",
                request,
                cacheKey
        );

        log.info(
                "[{}] WAITING FOR CACHE TO BE POPULATED | key={}",
                request,
                cacheKey
        );

        /*
         * STEP 5
         * Wait for the cache owner to populate Redis.
         */
        long deadline =
                System.nanoTime()
                        + MAX_WAIT_TIME.toNanos();

        while (System.nanoTime() < deadline) {

            Cache.ValueWrapper value = get(key);

            if (value != null) {

                log.info(
                        "[{}] CACHE POPULATED BY ANOTHER REQUEST | key={}",
                        request,
                        cacheKey
                );

                return (T) value.get();
            }

            try {

                Thread.sleep(
                        WAIT_INTERVAL.toMillis()
                );

            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();

                log.warn(
                        "[{}] THREAD INTERRUPTED WHILE WAITING | key={}",
                        request,
                        cacheKey
                );

                throw new Cache.ValueRetrievalException(
                        key,
                        valueLoader,
                        e
                );
            }
        }

        /*
         * STEP 6
         * Wait timeout reached.
         *
         * Try to acquire the lease again.
         */
        log.info(
                "[{}] WAIT TIMEOUT | key={} | Trying to acquire lease again",
                request,
                cacheKey
        );

        leaseToken =
                cacheLeaseService.tryAcquire(cacheKey);

        if (leaseToken != null) {

            log.info(
                    "[{}] LEASE RE-ACQUIRED | key={} | Previous owner may have failed",
                    request,
                    cacheKey
            );

            try {

                log.info(
                        "[{}] LOADING DATA FROM DATABASE AFTER LEASE RE-ACQUISITION | key={}",
                        request,
                        cacheKey
                );

                return super.get(key, valueLoader);

            } finally {

                cacheLeaseService.release(
                        cacheKey,
                        leaseToken
                );

                log.info(
                        "[{}] LEASE RELEASED AFTER RE-ACQUISITION | key={}",
                        request,
                        cacheKey
                );
            }
        }

        /*
         * STEP 7
         * Another request still owns the lease.
         *
         * Continue waiting.
         */
        log.info(
                "[{}] LEASE STILL OWNED BY ANOTHER REQUEST | key={} | Continuing to wait",
                request,
                cacheKey
        );

        while (true) {

            Cache.ValueWrapper value = get(key);

            if (value != null) {

                log.info(
                        "[{}] CACHE POPULATED WHILE WAITING | key={}",
                        request,
                        cacheKey
                );

                return (T) value.get();
            }

            try {

                Thread.sleep(
                        WAIT_INTERVAL.toMillis()
                );

            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();

                log.warn(
                        "[{}] THREAD INTERRUPTED WHILE WAITING | key={}",
                        request,
                        cacheKey
                );

                throw new Cache.ValueRetrievalException(
                        key,
                        valueLoader,
                        e
                );
            }
        }
    }
}
