package com.vesttrack.dto.portfolio;

import java.math.BigDecimal;

public record PerformanceResponse(
        Long accountId,
        BigDecimal twrPercentage,
        BigDecimal mwrPercentage,
        BigDecimal currentMarketValue,
        String methodologyNote
) {}
