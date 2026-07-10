package com.vesttrack.dto.portfolio;

import java.math.BigDecimal;
import java.util.List;

public record PortfolioAllocationResponse(
        Long accountId,
        BigDecimal totalValue,
        BigDecimal coreActualWeight,
        BigDecimal satelliteActualWeight,
        List<AssetAllocation> assets
) {
    public record AssetAllocation(
            String ticker,
            String strategyRole,
            BigDecimal currentValue,
            BigDecimal actualWeight,
            BigDecimal targetWeight,
            BigDecimal deviation,
            String rebalanceSuggestion
    ) {}
}
