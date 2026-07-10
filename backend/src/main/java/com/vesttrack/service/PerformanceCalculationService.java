package com.vesttrack.service;

import com.vesttrack.domain.entity.InvestmentAccount;
import com.vesttrack.domain.entity.Transaction;
import com.vesttrack.domain.enums.TransactionType;
import com.vesttrack.dto.portfolio.PerformanceResponse;
import com.vesttrack.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.solvers.BrentSolver;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Wylicza dwa standardowe wskazniki stopy zwrotu portfela:
 *
 * 1) TWR (Time-Weighted Return) - metoda Modified Dietz.
 *    Prawdziwy TWR wymaga wyceny portfela w KAZDYM momencie wpływu/wyplywu kapitalu,
 *    czego nie przechowujemy (brak tabeli codziennych wycen historycznych).
 *    Modified Dietz jest powszechnie stosowanym w praktyce (m.in. przez CFA Institute
 *    jako dopuszczalna metoda GIPS) przyblizeniem TWR, wymagajacym jedynie:
 *      - wartosci portfela na poczatku i koncu okresu,
 *      - dat i kwot przepływow kapitalu w tym okresie.
 *    Wzor: R = (EV - BV - CF) / (BV + sum(CF_i * w_i))
 *      gdzie w_i = (CD - D_i) / CD  (waga zalezna od liczby dni pozostajacych do konca okresu)
 *
 * 2) MWR (Money-Weighted Return) - rzeczywista wewnetrzna stopa zwrotu (IRR/XIRR)
 *    z uwzglednieniem RZECZYWISTYCH dat przepływow (a nie regularnych okresow).
 *    Rozwiazywane numerycznie metoda Brenta (Apache Commons Math) dla rownania:
 *      sum( CF_i / (1+r)^(dni_i/365) ) = 0
 *
 * Uwaga/ograniczenie: dywidendy sa traktowane jako dochod portfela (nie jako
 * zewnetrzny wplyw/wyplyw kapitalu), zgodnie ze standardowa praktyka liczenia
 * stopy zwrotu z inwestycji.
 */
@Service
@RequiredArgsConstructor
public class PerformanceCalculationService {

    private final QuoteService quoteService;
    private final AccountService accountService;
    private final TransactionRepository transactionRepository;

    public PerformanceResponse calculatePerformance(Long accountId) {
        InvestmentAccount account = accountService.getOwnedAccountOrThrow(accountId);
        List<Transaction> transactions = transactionRepository.findByAccountIdWithInstrument(accountId);

        if (transactions.isEmpty()) {
            return new PerformanceResponse(accountId, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    "Brak transakcji na koncie - nie mozna wyliczyc stopy zwrotu");
        }

        LocalDate periodStart = transactions.stream()
                .map(Transaction::getTransactionDate)
                .min(LocalDate::compareTo)
                .orElseThrow();
        LocalDate today = LocalDate.now();

        BigDecimal currentMarketValue = calculateCurrentMarketValue(accountId, transactions);

        // Zewnetrzne przepływy kapitalu: BUY = kapital WPLYWA do portfela (+),
        // SELL = kapital WYPLYWA z portfela (-). Dywidendy sa wylaczone (dochod, nie wplyw kapitalu).
        List<CashFlow> capitalFlows = transactions.stream()
                .filter(t -> t.getTransactionType() == TransactionType.BUY || t.getTransactionType() == TransactionType.SELL)
                .map(t -> {
                    BigDecimal grossAmount = t.getQuantity().multiply(t.getPrice()).multiply(t.getExchangeRate());
                    BigDecimal signedAmount = t.getTransactionType() == TransactionType.BUY ? grossAmount : grossAmount.negate();
                    return new CashFlow(t.getTransactionDate(), signedAmount);
                })
                .collect(Collectors.toList());

        BigDecimal twr = calculateModifiedDietz(BigDecimal.ZERO, currentMarketValue, capitalFlows, periodStart, today);
        BigDecimal mwr = calculateXirr(capitalFlows, currentMarketValue, today);

        String methodology = "TWR: metoda Modified Dietz (przyblizenie Time-Weighted Return bez historii dziennych wycen). "
                + "MWR: rzeczywisty IRR (XIRR) na podstawie dat i kwot faktycznych przepływow kapitalu.";

        return new PerformanceResponse(accountId, twr, mwr, currentMarketValue, methodology);
    }

    private BigDecimal calculateCurrentMarketValue(Long accountId, List<Transaction> transactions) {
        Map<Long, BigDecimal> netQuantityByInstrument = transactions.stream()
                .filter(t -> t.getTransactionType() == TransactionType.BUY || t.getTransactionType() == TransactionType.SELL)
                .collect(Collectors.groupingBy(
                        t -> t.getInstrument().getId(),
                        Collectors.reducing(BigDecimal.ZERO,
                                t -> t.getTransactionType() == TransactionType.BUY ? t.getQuantity() : t.getQuantity().negate(),
                                BigDecimal::add)));

        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Long, BigDecimal> entry : netQuantityByInstrument.entrySet()) {
            if (entry.getValue().compareTo(BigDecimal.ZERO) <= 0) {
                continue; // pozycja calkowicie zamknieta
            }
            BigDecimal price = quoteService.getCurrentPrice(entry.getKey());
            total = total.add(entry.getValue().multiply(price));
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateModifiedDietz(BigDecimal beginningValue, BigDecimal endingValue,
                                               List<CashFlow> flows, LocalDate periodStart, LocalDate periodEnd) {
        long totalDays = ChronoUnit.DAYS.between(periodStart, periodEnd);
        if (totalDays <= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal sumFlows = BigDecimal.ZERO;
        BigDecimal sumWeightedFlows = BigDecimal.ZERO;

        for (CashFlow flow : flows) {
            long daysFromStart = ChronoUnit.DAYS.between(periodStart, flow.date());
            BigDecimal weight = BigDecimal.valueOf(totalDays - daysFromStart)
                    .divide(BigDecimal.valueOf(totalDays), 10, RoundingMode.HALF_UP);
            sumFlows = sumFlows.add(flow.amount());
            sumWeightedFlows = sumWeightedFlows.add(flow.amount().multiply(weight));
        }

        BigDecimal denominator = beginningValue.add(sumWeightedFlows);
        if (denominator.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal numerator = endingValue.subtract(beginningValue).subtract(sumFlows);
        return numerator.divide(denominator, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * XIRR - rozwiazuje rownanie NPV(r) = 0 metoda Brenta.
     * Przepływy z perspektywy INWESTORA: BUY to wydatek (-), SELL to wplyw (+),
     * a biezaca wartosc rynkowa portfela na "dzis" to fikcyjny, koncowy wplyw (+)
     * jakby portfel zostal spienizony w calosci.
     */
    private BigDecimal calculateXirr(List<CashFlow> capitalFlows, BigDecimal currentMarketValue, LocalDate today) {
        if (capitalFlows.isEmpty()) {
            return BigDecimal.ZERO;
        }
        LocalDate baseDate = capitalFlows.get(0).date();

        List<CashFlow> investorFlows = capitalFlows.stream()
                .map(f -> new CashFlow(f.date(), f.amount().negate())) // BUY(+) z perspektywy portfela -> wydatek(-) inwestora
                .collect(Collectors.toList());
        investorFlows.add(new CashFlow(today, currentMarketValue)); // "sprzedaz" calego portfela dzisiaj

        UnivariateFunction npv = rate -> investorFlows.stream()
                .mapToDouble(f -> {
                    long days = ChronoUnit.DAYS.between(baseDate, f.date());
                    double years = days / 365.0;
                    return f.amount().doubleValue() / Math.pow(1 + rate, years);
                })
                .sum();

        try {
            BrentSolver solver = new BrentSolver(1e-7);
            double rate = solver.solve(1000, npv, -0.99, 10.0); // szukamy rozwiazania w przedziale (-99%, +1000%)
            return BigDecimal.valueOf(rate * 100).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception ex) {
            // brak zbiegajacego sie rozwiazania (np. wszystkie przepływy jednego znaku) - nie da sie wyliczyc IRR
            return null;
        }
    }

    private record CashFlow(LocalDate date, BigDecimal amount) {}
}
