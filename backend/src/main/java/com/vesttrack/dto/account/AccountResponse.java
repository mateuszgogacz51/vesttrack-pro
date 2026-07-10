package com.vesttrack.dto.account;

import com.vesttrack.domain.entity.InvestmentAccount;

import java.math.BigDecimal;

public record AccountResponse(
        Long id,
        String name,
        String accountType,
        String currency,
        BigDecimal annualContributionLimit,
        BigDecimal contributedThisYear,
        boolean active
) {
    public static AccountResponse from(InvestmentAccount a) {
        return new AccountResponse(
                a.getId(), a.getName(), a.getAccountType().name(), a.getCurrency(),
                a.getAnnualContributionLimit(), a.getContributedThisYear(), a.isActive());
    }
}
