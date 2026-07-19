package com.vesttrack.service;

import com.vesttrack.domain.entity.FinancialInstrument;
import com.vesttrack.domain.entity.User;
import com.vesttrack.domain.enums.AssetType;
import com.vesttrack.dto.admin.CreateInstrumentRequest;
import com.vesttrack.exception.ApiException;
import com.vesttrack.exception.ResourceNotFoundException;
import com.vesttrack.repository.FinancialInstrumentRepository;
import com.vesttrack.service.quotes.YahooFinanceClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Zarzadzanie globalnym slownikiem instrumentow finansowych.
 *
 * Wyszukiwanie (search) jest dostepne dla kazdego uzytkownika (USER) i dziala
 * w trybie "znajdz-lub-utworz": jesli zapytanie nie trafia w nic co juz jest
 * w lokalnej bazie, system automatycznie pyta publiczna wyszukiwarke Yahoo
 * Finance i dodaje pasujace instrumenty do slownika. Dzieki temu admin nie
 * musi recznie dodawac kazdej spolki/ETF-u z gieldy - robi to tylko w wyjatkowych
 * przypadkach (np. instrument spoza pokrycia Yahoo, lub potrzeba
 * ustawienia dodatkowych metadanych typu ISIN).
 */
@Service
@RequiredArgsConstructor
public class InstrumentService {

    private final FinancialInstrumentRepository instrumentRepository;
    private final AuditService auditService;
    private final YahooFinanceClient yahooFinanceClient;

    @Transactional
    public List<FinancialInstrument> search(String query) {
        List<FinancialInstrument> local = instrumentRepository
                .findByTickerContainingIgnoreCaseOrNameContainingIgnoreCase(query, query);

        if (!local.isEmpty()) {
            return local;
        }

        // Brak lokalnie -> zapytaj Yahoo Finance i dodaj znalezione instrumenty do slownika
        List<YahooFinanceClient.SymbolMatch> matches = yahooFinanceClient.searchSymbols(query);

        return matches.stream()
                .filter(m -> m.symbol() != null && !m.symbol().isBlank())
                .filter(m -> instrumentRepository.findByTickerContainingIgnoreCaseOrNameContainingIgnoreCase(
                        m.symbol(), m.symbol()).isEmpty())
                .map(m -> instrumentRepository.save(FinancialInstrument.builder()
                        .ticker(m.symbol())
                        .name(m.longname() != null ? m.longname() : (m.shortname() != null ? m.shortname() : m.symbol()))
                        .assetType(mapQuoteType(m.quoteType()))
                        .exchange(m.exchange())
                        .quoteCurrency(m.currency() != null ? m.currency() : "USD")
                        .blocked(false)
                        .build()))
                .toList();
    }

    private AssetType mapQuoteType(String yahooQuoteType) {
        if (yahooQuoteType == null) {
            return AssetType.OTHER;
        }
        return switch (yahooQuoteType.toUpperCase()) {
            case "ETF" -> AssetType.ETF;
            case "EQUITY" -> AssetType.STOCK;
            case "BOND", "MUTUALFUND" -> AssetType.BOND;
            default -> AssetType.OTHER;
        };
    }

    @Transactional
    public FinancialInstrument createInstrument(CreateInstrumentRequest request, User actor) {
        if (request.isin() != null && instrumentRepository.findByIsin(request.isin()).isPresent()) {
            throw new ApiException("Instrument z tym kodem ISIN juz istnieje w slowniku", HttpStatus.CONFLICT);
        }

        FinancialInstrument instrument = FinancialInstrument.builder()
                .ticker(request.ticker())
                .name(request.name())
                .assetType(request.assetType())
                .isin(request.isin())
                .exchange(request.exchange())
                .quoteCurrency(request.quoteCurrency())
                .accumulating(request.accumulating())
                .blocked(false)
                .build();

        FinancialInstrument saved = instrumentRepository.save(instrument);
        auditService.log(actor, "INSTRUMENT_CREATED", "Dodano instrument do slownika: " + saved.getTicker());
        return saved;
    }

    @Transactional
    public void blockInstrument(Long instrumentId, User actor, String reason) {
        FinancialInstrument instrument = instrumentRepository.findById(instrumentId)
                .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono instrumentu"));
        instrument.setBlocked(true);
        instrumentRepository.save(instrument);
        auditService.log(actor, "INSTRUMENT_BLOCKED",
                "Zablokowano instrument " + instrument.getTicker() + ". Powod: " + reason);
    }
}