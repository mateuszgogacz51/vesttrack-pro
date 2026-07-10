package com.vesttrack.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient nbpWebClient(@Value("${external-api.nbp.base-url}") String baseUrl) {
        return WebClient.builder().baseUrl(baseUrl).build();
    }

    /** Podstawowy dostawca notowan - publiczne (nieoficjalne) endpointy Yahoo Finance. */
    @Bean
    public WebClient yahooWebClient(@Value("${external-api.quotes.yahoo-base-url}") String baseUrl) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("User-Agent", "Mozilla/5.0 (VestTrackPro/1.0)")
                .build();
    }

    /** Dostawca zapasowy - Alpha Vantage, uzywany gdy Yahoo Finance jest niedostepne (circuit breaker OPEN). */
    @Bean
    public WebClient alphaVantageWebClient(@Value("${external-api.quotes.alphavantage-base-url}") String baseUrl) {
        return WebClient.builder().baseUrl(baseUrl).build();
    }
}
