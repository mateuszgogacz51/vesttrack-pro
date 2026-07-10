package com.vesttrack.service.quotes;

import com.vesttrack.repository.ApiProviderConfigRepository;
import com.vesttrack.service.ApiUsageTrackingService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;

/**
 * Klient Alpha Vantage - dostawca ZAPASOWY, uzywany gdy Yahoo Finance jest
 * niedostepne (circuit breaker w stanie OPEN) lub zwraca blad.
 * Wymaga klucza API (darmowy plan: 25 zapytan/dzien) - klucz odczytywany
 * dynamicznie z tabeli api_provider_config (mozliwosc zmiany "na zywo" z panelu admina,
 * bez restartu aplikacji - patrz AdminController -> /api/v1/admin/api-usage/providers).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AlphaVantageClient {

    private final WebClient alphaVantageWebClient;
    private final ApiUsageTrackingService usageTrackingService;
    private final ApiProviderConfigRepository providerConfigRepository;

    public static final String PROVIDER_NAME = "ALPHA_VANTAGE";

    @CircuitBreaker(name = "alphaVantageQuotes")
    @Retry(name = "alphaVantageQuotes")
    public BigDecimal fetchLastPrice(String ticker) {
        String apiKey = providerConfigRepository.findByProvider(PROVIDER_NAME)
                .map(cfg -> cfg.getApiKey() != null ? cfg.getApiKey() : "demo")
                .orElse("demo");

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = alphaVantageWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/query")
                            .queryParam("function", "GLOBAL_QUOTE")
                            .queryParam("symbol", ticker)
                            .queryParam("apikey", apiKey)
                            .build())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();

            BigDecimal price = extractPrice(response, ticker);
            usageTrackingService.recordCall(PROVIDER_NAME, true);
            return price;
        } catch (Exception ex) {
            usageTrackingService.recordCall(PROVIDER_NAME, false);
            log.warn("Alpha Vantage: blad pobierania notowania dla {}: {}", ticker, ex.getMessage());
            throw ex;
        }
    }

    @SuppressWarnings("unchecked")
    private BigDecimal extractPrice(Map<String, Object> response, String ticker) {
        if (response == null || !response.containsKey("Global Quote")) {
            throw new IllegalStateException("Pusta odpowiedz z Alpha Vantage dla " + ticker);
        }
        Map<String, Object> quote = (Map<String, Object>) response.get("Global Quote");
        Object priceStr = quote.get("05. price");
        if (priceStr == null) {
            throw new IllegalStateException("Brak pola '05. price' w odpowiedzi Alpha Vantage dla " + ticker);
        }
        return new BigDecimal(priceStr.toString());
    }
}
