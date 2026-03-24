package com.taskmanager.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfig {

    // In-memory cache — works out of the box, no Redis needed.
    // Interview tip: "I used Spring Cache abstraction. Swapping to Redis is just
    // changing this bean to RedisCacheManager — zero code changes in services."
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("dashboard");
    }
}
