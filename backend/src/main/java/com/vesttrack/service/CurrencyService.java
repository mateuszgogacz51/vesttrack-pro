package com.vesttrack.service;

import com.vesttrack.config.CacheConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;

/**
 * Pobiera biezace kursy walut z publicznego API NBP (Narodowy Bank Polski).
 * Wyniki sa cache'owane (Caffeine, 15 min), aby nie przeciazac zewnetrznego API
 * przy kazdym przeliczeniu wartosci portfela.
 * Zabezpieczone retry/backoff (odpornosc na chwilowa niedostepnosc dostawcy) -
 * w razie calkowitej awaria API spada na kurs awaryjny 1:1, zeby nie wywalic calego systemu.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CurrencyService {

    private final WebClient nbpWebClient;

    private static final Map<String, String> SUPPORTED = Map.of(
            "USD", "usd", "EUR", "eur", "GBP", "gbp", "CHF", "chf"
    );

    @Cacheable(value = CacheConfig.FX_RATES_CACHE, key = "#currencyCode")
    public BigDecimal getMidRateToPln(String currencyCode) {
        if ("PLN".equalsIgnoreCase(currencyCode)) {
            return BigDecimal.ONE;
        }
        String code = currencyCode.toLowerCase();

        try {
            NbpResponse[] response = nbpWebClient.get()
                    .uri("/exchangerates/rates/a/{code}/", code)
                    .retrieve()
                    .bodyToMono(NbpResponse[].class)
                    .retryWhen(Retry.backoff(3, Duration.ofMillis(300)))
                    .timeout(Duration.ofSeconds(5))
                    .onErrorResume(ex -> {
                        log.warn("Nie udalo sie pobrac kursu {} z NBP, uzywam awaryjnego 1:1. Przyczyna: {}",
                                currencyCode, ex.getMessage());
                        return Mono.empty();
                    })
                    .blockOptional()
                    .orElse(null);

            if (response == null || response.length == 0 || response[0].rates() == null
                    || response[0].rates().isEmpty()) {
                return BigDecimal.ONE;
            }
            return response[0].rates().get(0).mid();
        } catch (Exception ex) {
            log.error("Blad pobierania kursu waluty {}: {}", currencyCode, ex.getMessage());
            return BigDecimal.ONE; // fallback, system nie powinien sie wywracac przez niedostepnosc NBP
        }
    }

    public BigDecimal convertToPln(BigDecimal amount, String currencyCode) {
        return amount.multiply(getMidRateToPln(currencyCode));
    }

    // DTO mapujace odpowiedz API NBP: https://api.nbp.pl/api/exchangerates/rates/a/usd/
    private record NbpResponse(String table, String currency, String code, java.util.List<Rate> rates) {}
    private record Rate(String no, String effectiveDate, BigDecimal mid) {}
}
