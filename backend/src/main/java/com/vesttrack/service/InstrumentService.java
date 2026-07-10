package com.vesttrack.service;

import com.vesttrack.domain.entity.FinancialInstrument;
import com.vesttrack.domain.entity.User;
import com.vesttrack.dto.admin.CreateInstrumentRequest;
import com.vesttrack.exception.ApiException;
import com.vesttrack.exception.ResourceNotFoundException;
import com.vesttrack.repository.FinancialInstrumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Zarzadzanie globalnym slownikiem instrumentow finansowych.
 * Dostepne dla roli EMPLOYEE/ADMIN - odpowiada na zgloszenia uzytkownikow
 * o brakujace tickery (np. nowo zadebiutowana spolka na GPW).
 */
@Service
@RequiredArgsConstructor
public class InstrumentService {

    private final FinancialInstrumentRepository instrumentRepository;
    private final AuditService auditService;

    public List<FinancialInstrument> search(String query) {
        return instrumentRepository.findByTickerContainingIgnoreCaseOrNameContainingIgnoreCase(query, query);
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
