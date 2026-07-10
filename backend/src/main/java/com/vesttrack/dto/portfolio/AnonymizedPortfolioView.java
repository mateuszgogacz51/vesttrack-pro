package com.vesttrack.dto.portfolio;

import java.math.BigDecimal;
import java.util.List;

/**
 * Widok dla roli EMPLOYEE - wylacznie procentowy podzial portfela,
 * bez ujawniania kwot ani konkretnych instrumentow w sposob umozliwiajacy
 * odtworzenie majatku uzytkownika.
 */
public record AnonymizedPortfolioView(
        Long accountId,
        String accountTypeMasked,
        List<SectorWeight> weightsByAssetType
) {
    public record SectorWeight(String assetType, BigDecimal percentage) {}
}
