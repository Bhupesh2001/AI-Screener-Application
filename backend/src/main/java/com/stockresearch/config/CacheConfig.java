// backend/src/main/java/com/stockresearch/config/CacheConfig.java
package com.stockresearch.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Cache TTLs are sized per data-volatility, not a single blanket value -
 * see the real-data integration plan, section 4.2 ("Caching layer, keyed
 * by data volatility"). RefreshScheduler runs hourly, so anything meant to
 * stay in step with it (price, announcements) is capped below that; only
 * fundamentals - which genuinely only change quarterly at the source - get
 * the long TTL.
 *
 * A plain CaffeineCacheManager can't give different caches different TTLs
 * (one shared Caffeine spec applies to every cache name it manages), so
 * this builds each cache individually and wires them into a
 * SimpleCacheManager instead. Still just Caffeine underneath - no new
 * caching framework, per the project's "avoid unnecessary complexity" spec.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(
                buildCache("fundamentals", 24, TimeUnit.HOURS), // Screener.in data changes quarterly
                buildCache("price", 1, TimeUnit.HOURS),          // matches RefreshScheduler's hourly cadence
                buildCache("announcements", 1, TimeUnit.HOURS),  // event-driven; polled more eagerly than fundamentals
                buildCache("news", 1, TimeUnit.HOURS)            // same reasoning as announcements - stay in step with the scheduler
        ));
        return manager;
    }

    private Cache buildCache(String name, long duration, TimeUnit unit) {
        return new CaffeineCache(name, Caffeine.newBuilder()
                .expireAfterWrite(duration, unit)
                .maximumSize(100)
                .build());
    }
}