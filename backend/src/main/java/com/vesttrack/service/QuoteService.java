package com.vesttrack.service;

import com.vesttrack.config.CacheConfig;
import com.vesttrack.domain.entity.FinancialInstrument;
import com.vesttrack.exception.ResourceNotFoundException;
import com.vesttrack.repository.FinancialInstrumentRepository;
import com.vesttrack.service.quotes.AlphaVantageClient;
import com.vesttrack.service.quotes.YahooFinanceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Warstwa pobierania biezacych notowan instrumentow.
 *
 * Strategia odpornosci (resilience chain):
 *   1. Sprobuj Yahoo Finance (PODSTAWOWY dostawca) - zabezpieczone Circuit Breaker + Retry.
 *   2. Jesli Yahoo zawiedzie (wyjatek / obwod OPEN) -> sprobuj Alpha Vantage (ZAPASOWY).
 *   3. Jesli oba zawiedza -> zwroc ostatnia znana cene zapisana w bazie (financial_instruments.last_price).
 *      Dzieki temu calkowita awaria zewnetrznych dostawcow nigdy nie wywala calego systemu -
 *      uzytkownik dostaje (ewentualnie nieco nieaktualna) wycene, a nie blad 500.
 *
 * Wynik cache'owany w Caffeine na 15 minut (per instrumentId), zeby nie odpytywac
 * zewnetrznych API czesciej niz to konieczne (ochrona przed rate-limitami dostawcow).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuoteService {

    private final FinancialInstrumentRepository instrumentRepository;
    private final YahooFinanceClient yahooFinanceClient;
    private final AlphaVantageClient alphaVantageClient;
    private final ApiUsageTrackingService usageTrackingService;

    @Cacheable(value = CacheConfig.QUOTES_CACHE, key = "#instrumentId")
    public BigDecimal getCurrentPrice(Long instrumentId) {
        FinancialInstrument instrument = instrumentRepository.findById(instrumentId)
                .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono instrumentu"));

        if (instrument.isBlocked()) {
            log.warn("Instrument {} jest zablokowany - zwracam ostatnia znana cene", instrument.getTicker());
            return lastKnownPriceOrZero(instrument);
        }

        BigDecimal price = fetchFromYahooWithFallback(instrument);
        if (price != null) {
            updateLastPriceInternal(instrument, price);
            return price;
        }

        log.warn("Wszyscy dostawcy notowan zawiedli dla {}, zwracam ostatnia znana cene z bazy", instrument.getTicker());
        return lastKnownPriceOrZero(instrument);
    }

    private BigDecimal fetchFromYahooWithFallback(FinancialInstrument instrument) {
        String symbol = resolveYahooSymbol(instrument);

        if (usageTrackingService.isWithinDailyLimit(YahooFinanceClient.PROVIDER_NAME)) {
            try {
                return yahooFinanceClient.fetchLastPrice(symbol);
            } catch (Exception ex) {
                log.info("Yahoo Finance niedostepne dla {}, przelaczam na Alpha Vantage. Przyczyna: {}",
                        instrument.getTicker(), ex.getMessage());
            }
        } else {
            log.info("Przekroczono dzienny limit zapytan do Yahoo Finance, przelaczam na Alpha Vantage");
        }

        if (usageTrackingService.isWithinDailyLimit(AlphaVantageClient.PROVIDER_NAME)) {
            try {
                return alphaVantageClient.fetchLastPrice(instrument.getTicker());
            } catch (Exception ex) {
                log.warn("Alpha Vantage rowniez niedostepne dla {}. Przyczyna: {}",
                        instrument.getTicker(), ex.getMessage());
            }
        }

        return null;
    }

    /**
     * Yahoo Finance wymaga sufiksu gieldy dla instrumentow spoza USA, np. spolki z GPW
     * notowane sa jako "TICKER.WA". Dla ETF-ow/akcji US symbol pozostaje bez zmian.
     */
    private String resolveYahooSymbol(FinancialInstrument instrument) {
        if (instrument.getExchange() != null && instrument.getExchange().equalsIgnoreCase("GPW")) {
            return instrument.getTicker() + ".WA";
        }
        return instrument.getTicker();
    }

    private BigDecimal lastKnownPriceOrZero(FinancialInstrument instrument) {
        return instrument.getLastPrice() != null ? instrument.getLastPrice() : BigDecimal.ZERO;
    }

    @Transactional
    public void updateLastPrice(Long instrumentId, BigDecimal price) {
        FinancialInstrument instrument = instrumentRepository.findById(instrumentId)
                .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono instrumentu"));
        updateLastPriceInternal(instrument, price);
    }

    private void updateLastPriceInternal(FinancialInstrument instrument, BigDecimal price) {
        instrument.setLastPrice(price);
        instrument.setLastPriceAt(OffsetDateTime.now());
        instrumentRepository.save(instrument);
    }

    /** Wymuszenie odswiezenia notowania z pominieciem cache (np. przycisk "Odswiez" w UI). */
    @CacheEvict(value = CacheConfig.QUOTES_CACHE, key = "#instrumentId")
    public void evictCache(Long instrumentId) {
        // metoda pusta - adnotacja @CacheEvict wykonuje cala prace
    }
}
