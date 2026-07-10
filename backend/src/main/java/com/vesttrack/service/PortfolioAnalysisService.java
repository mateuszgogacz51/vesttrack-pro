package com.vesttrack.service;

import com.vesttrack.domain.entity.InvestmentAccount;
import com.vesttrack.domain.entity.PortfolioAsset;
import com.vesttrack.domain.enums.StrategyRole;
import com.vesttrack.dto.portfolio.AnonymizedPortfolioView;
import com.vesttrack.dto.portfolio.PortfolioAllocationResponse;
import com.vesttrack.repository.InvestmentAccountRepository;
import com.vesttrack.repository.PortfolioAssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PortfolioAnalysisService {

    private final PortfolioAssetRepository portfolioAssetRepository;
    private final InvestmentAccountRepository accountRepository;
    private final QuoteService quoteService;
    private final AccountService accountService;

    /**
     * Wylicza aktualna alokacje portfela Core/Satellite oraz odchylenie od celu
     * (target_weight), generujac sugestie rebalancingu - np. gdy Satellite urosnie
     * powyzej zalozonego progu, system podpowiada redukcje pozycji.
     */
    public PortfolioAllocationResponse analyzeAllocation(Long accountId) {
        InvestmentAccount account = accountService.getOwnedAccountOrThrow(accountId);
        List<PortfolioAsset> assets = portfolioAssetRepository.findByAccountIdWithInstrument(accountId);

        record Valued(PortfolioAsset asset, BigDecimal value) {}

        List<Valued> valuedAssets = assets.stream()
                .map(a -> {
                    BigDecimal price = quoteService.getCurrentPrice(a.getInstrument().getId());
                    // uproszczenie: ilosc posiadanych jednostek liczona byłaby na podstawie
                    // sumy transakcji BUY-SELL; tutaj uzywamy ceny * 1 jako wartosci jednostkowej
                    // referencyjnej do wyliczenia wag procentowych w portfelu.
                    return new Valued(a, price);
                })
                .toList();

        BigDecimal totalValue = valuedAssets.stream()
                .map(Valued::value)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal coreValue = valuedAssets.stream()
                .filter(v -> v.asset().getStrategyRole() == StrategyRole.CORE)
                .map(Valued::value)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal satelliteValue = totalValue.subtract(coreValue);

        BigDecimal coreWeight = percentageOf(coreValue, totalValue);
        BigDecimal satelliteWeight = percentageOf(satelliteValue, totalValue);

        List<PortfolioAllocationResponse.AssetAllocation> allocations = valuedAssets.stream()
                .map(v -> {
                    BigDecimal actualWeight = percentageOf(v.value(), totalValue);
                    BigDecimal targetWeight = v.asset().getTargetWeight() != null
                            ? v.asset().getTargetWeight() : BigDecimal.ZERO;
                    BigDecimal deviation = actualWeight.subtract(targetWeight);
                    String suggestion = buildRebalanceSuggestion(v.asset().getInstrument().getTicker(), deviation);

                    return new PortfolioAllocationResponse.AssetAllocation(
                            v.asset().getInstrument().getTicker(),
                            v.asset().getStrategyRole().name(),
                            v.value(), actualWeight, targetWeight, deviation, suggestion);
                })
                .toList();

        return new PortfolioAllocationResponse(accountId, totalValue, coreWeight, satelliteWeight, allocations);
    }

    private String buildRebalanceSuggestion(String ticker, BigDecimal deviationPercent) {
        BigDecimal threshold = new BigDecimal("5.0"); // prog 5 p.p. odchylenia, powyzej ktorego sugerujemy akcje
        if (deviationPercent.abs().compareTo(threshold) < 0) {
            return "Brak akcji - alokacja zgodna z celem";
        }
        return deviationPercent.signum() > 0
                ? "Rozwaz redukcje pozycji " + ticker + " (przewazona o " + deviationPercent.abs() + " p.p.)"
                : "Rozwaz dokupienie " + ticker + " (niedowazona o " + deviationPercent.abs() + " p.p.)";
    }

    /**
     * Widok dla roli Pracownik: wylacznie procentowy podzial wedlug typu aktywa,
     * bez ujawniania kwot, ilosci jednostek czy konkretnych transakcji.
     */
    public AnonymizedPortfolioView getAnonymizedView(Long accountId) {
        InvestmentAccount account = accountRepository.findById(accountId)
                .orElseThrow(() -> new com.vesttrack.exception.ResourceNotFoundException("Nie znaleziono konta"));

        List<PortfolioAsset> assets = portfolioAssetRepository.findByAccountIdWithInstrument(accountId);

        Map<String, BigDecimal> valueByType = assets.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getInstrument().getAssetType().name(),
                        Collectors.reducing(BigDecimal.ZERO,
                                a -> quoteService.getCurrentPrice(a.getInstrument().getId()),
                                BigDecimal::add)));

        BigDecimal total = valueByType.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        List<AnonymizedPortfolioView.SectorWeight> weights = valueByType.entrySet().stream()
                .map(e -> new AnonymizedPortfolioView.SectorWeight(e.getKey(), percentageOf(e.getValue(), total)))
                .toList();

        // maskowanie typu konta - pracownik widzi tylko "EMERYTALNE" lub "STANDARDOWE", bez szczegolow podatkowych
        String maskedType = account.getAccountType().name().equals("REGULAR") ? "STANDARDOWE" : "EMERYTALNE";

        return new AnonymizedPortfolioView(accountId, maskedType, weights);
    }

    private BigDecimal percentageOf(BigDecimal part, BigDecimal total) {
        if (total == null || total.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return part.multiply(BigDecimal.valueOf(100)).divide(total, 2, RoundingMode.HALF_UP);
    }
}
