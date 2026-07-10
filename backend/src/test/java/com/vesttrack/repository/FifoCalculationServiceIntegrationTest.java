package com.vesttrack.repository;

import com.vesttrack.domain.entity.*;
import com.vesttrack.domain.enums.*;
import com.vesttrack.service.FifoCalculationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test integracyjny weryfikujacy poprawnosc algorytmu FIFO na prawdziwej bazie
 * PostgreSQL (Testcontainers) - kluczowy element demonstrujacy jakosc silnika
 * obliczeniowego przy transzowych zakupach i czesciowej sprzedazy akcji.
 */
@Transactional
class FifoCalculationServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired private FifoCalculationService fifoCalculationService;
    @Autowired private UserRepository userRepository;
    @Autowired private InvestmentAccountRepository accountRepository;
    @Autowired private FinancialInstrumentRepository instrumentRepository;
    @Autowired private TransactionRepository transactionRepository;

    private InvestmentAccount account;
    private FinancialInstrument instrument;

    @BeforeEach
    void setUp() {
        User user = userRepository.save(User.builder()
                .email("investor@example.com")
                .passwordHash("hash")
                .firstName("Jan")
                .lastName("Kowalski")
                .role(Role.USER)
                .enabled(true)
                .baseCurrency("PLN")
                .build());

        account = accountRepository.save(InvestmentAccount.builder()
                .user(user)
                .name("Konto maklerskie")
                .accountType(AccountType.REGULAR)
                .currency("PLN")
                .contributedThisYear(BigDecimal.ZERO)
                .active(true)
                .build());

        instrument = instrumentRepository.save(FinancialInstrument.builder()
                .ticker("XTB")
                .name("XTB S.A.")
                .assetType(AssetType.STOCK)
                .quoteCurrency("PLN")
                .blocked(false)
                .build());
    }

    @Test
    void shouldCalculateRealizedGainAcrossMultipleLotsUsingFifo() {
        // Transza 1: kupno 10 akcji po 50 PLN
        saveBuy(new BigDecimal("10"), new BigDecimal("50.00"), LocalDate.of(2025, 1, 10));
        // Transza 2: kupno 10 akcji po 60 PLN
        saveBuy(new BigDecimal("10"), new BigDecimal("60.00"), LocalDate.of(2025, 3, 5));

        // Sprzedaz 15 akcji po 80 PLN -> FIFO: 10 z transzy1 (koszt 50) + 5 z transzy2 (koszt 60)
        BigDecimal realizedGain = fifoCalculationService.calculateRealizedGain(
                account.getId(), instrument.getId(),
                new BigDecimal("15"), new BigDecimal("80.00"),
                BigDecimal.ZERO, BigDecimal.ONE);

        // Koszt bazowy: 10*50 + 5*60 = 500 + 300 = 800
        // Przychod: 15*80 = 1200
        // Zysk: 1200 - 800 = 400
        assertThat(realizedGain).isEqualByComparingTo("400.00");
    }

    @Test
    void shouldThrowWhenSellingMoreThanOwned() {
        saveBuy(new BigDecimal("5"), new BigDecimal("100.00"), LocalDate.of(2025, 1, 10));

        org.junit.jupiter.api.Assertions.assertThrows(
                com.vesttrack.exception.BusinessRuleException.class,
                () -> fifoCalculationService.calculateRealizedGain(
                        account.getId(), instrument.getId(),
                        new BigDecimal("10"), new BigDecimal("120.00"),
                        BigDecimal.ZERO, BigDecimal.ONE));
    }

    private void saveBuy(BigDecimal qty, BigDecimal price, LocalDate date) {
        transactionRepository.save(Transaction.builder()
                .account(account)
                .instrument(instrument)
                .transactionType(TransactionType.BUY)
                .quantity(qty)
                .price(price)
                .fee(BigDecimal.ZERO)
                .instrumentCurrency("PLN")
                .exchangeRate(BigDecimal.ONE)
                .transactionDate(date)
                .build());
    }
}
