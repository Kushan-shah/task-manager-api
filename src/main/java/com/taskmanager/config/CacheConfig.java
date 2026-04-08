package com.taskmanager.config;

import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import java.time.Duration;

@Configuration
public class CacheConfig {

    /**
     * Redis-backed distributed cache for production.
     * Uses JSON serialization for human-readable cache entries.
     * TTL: 10 minutes — auto-evicts stale dashboard data.
     *
     * Interview tip: "I used Spring Cache abstraction backed by Redis.
     * The distributed cache ensures consistency across horizontally-scaled instances."
     */
    @Bean
    @Profile("!dev")
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new GenericJackson2JsonRedisSerializer())
                )
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }

    /**
     * In-memory cache fallback for dev profile (no Redis dependency needed).
     */
    @Bean
    @Profile("dev")
    public CacheManager devCacheManager() {
        return new ConcurrentMapCacheManager("dashboard");
    }
}
