package com.vesttrack.service.quotes;

import com.vesttrack.service.ApiUsageTrackingService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

/**
 * Klient nieoficjalnego, publicznego API Yahoo Finance (endpoint /v8/finance/chart/{ticker}).
 * Traktowany jako PODSTAWOWY dostawca notowan.
 *
 * Zabezpieczony wzorcem Circuit Breaker (Resilience4j): po przekroczeniu progu bledow
 * (50% w oknie 10 wywolan) "obwod" przechodzi w stan OPEN na 30s i zamiast pytac API,
 * od razu zglasza wyjatek (fallback w QuoteService przelacza na Alpha Vantage / ostatnia znana cene).
 * Dodatkowo Retry z exponential backoff amortyzuje chwilowe, pojedyncze bledy sieciowe.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class YahooFinanceClient {

    private final WebClient yahooWebClient;
    private final ApiUsageTrackingService usageTrackingService;

    public static final String PROVIDER_NAME = "YAHOO_FINANCE";

    @CircuitBreaker(name = "yahooQuotes")
    @Retry(name = "yahooQuotes")
    public BigDecimal fetchLastPrice(String ticker) {
        try {
            ChartResponse response = yahooWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v8/finance/chart/{ticker}")
                            .queryParam("interval", "1d")
                            .queryParam("range", "1d")
                            .build(ticker))
                    .retrieve()
                    .bodyToMono(ChartResponse.class)
                    .timeout(Duration.ofSeconds(4))
                    .block();

            BigDecimal price = extractPrice(response);
            usageTrackingService.recordCall(PROVIDER_NAME, true);
            return price;
        } catch (Exception ex) {
            usageTrackingService.recordCall(PROVIDER_NAME, false);
            log.warn("Yahoo Finance: blad pobierania notowania dla {}: {}", ticker, ex.getMessage());
            throw ex; // rzucamy dalej - Resilience4j zdecyduje o retry/otworzeniu obwodu
        }
    }

    private BigDecimal extractPrice(ChartResponse response) {
        if (response == null || response.chart() == null || response.chart().result() == null
                || response.chart().result().isEmpty()) {
            throw new IllegalStateException("Pusta odpowiedz z Yahoo Finance");
        }
        Meta meta = response.chart().result().get(0).meta();
        if (meta == null || meta.regularMarketPrice() == null) {
            throw new IllegalStateException("Brak pola regularMarketPrice w odpowiedzi Yahoo Finance");
        }
        return BigDecimal.valueOf(meta.regularMarketPrice());
    }

    // Minimalny wycinek struktury JSON zwracanej przez Yahoo Finance /v8/finance/chart
    private record ChartResponse(Chart chart) {}
    private record Chart(List<Result> result) {}
    private record Result(Meta meta) {}
    private record Meta(String currency, String symbol, Double regularMarketPrice) {}
}
