
package com.example.usermanagement.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "cache_invalidation_events")
public class CacheInvalidationEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String cacheName;

    @Column(nullable = false, length = 255)
    private String cacheKey;

    @Column(nullable = false)
    private Instant createdAt;

    protected CacheInvalidationEvent() {
    }

    public CacheInvalidationEvent(
            String cacheName,
            String cacheKey
    ) {
        this.cacheName = cacheName;
        this.cacheKey = cacheKey;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getCacheName() {
        return cacheName;
    }

    public String getCacheKey() {
        return cacheKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
