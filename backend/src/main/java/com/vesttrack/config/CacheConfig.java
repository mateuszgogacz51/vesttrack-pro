package com.vesttrack.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Cache dla notowan instrumentow oraz kursow walut NBP.
 * Cel: nie odpytywac zewnetrznych API czesciej niz raz na 15 minut na ten sam klucz,
 * co jest kluczowe dla optymalizacji i unikniecia rate-limitow dostawcow danych.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String QUOTES_CACHE = "instrumentQuotes";
    public static final String FX_RATES_CACHE = "fxRates";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(QUOTES_CACHE, FX_RATES_CACHE);
        manager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(2000)
                .expireAfterWrite(15, TimeUnit.MINUTES));
        return manager;
    }
}
