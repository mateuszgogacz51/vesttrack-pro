package com.vesttrack.dto.admin;

import com.vesttrack.domain.enums.AssetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateInstrumentRequest(
        @NotBlank String ticker,
        @NotBlank String name,
        @NotNull AssetType assetType,
        String isin,
        String exchange,
        @NotBlank String quoteCurrency,
        Boolean accumulating
) {}
