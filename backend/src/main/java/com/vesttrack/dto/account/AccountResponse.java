package com.vesttrack.dto.account;

import com.vesttrack.domain.entity.InvestmentAccount;

import java.math.BigDecimal;

public record AccountResponse(
        Long id,
        String name,
        Long brokerageFirmId,
        String brokerageFirmName,
        String accountType,
        String currency,
        BigDecimal annualContributionLimit,
        BigDecimal contributedThisYear,
        boolean active
) {
    public static AccountResponse from(InvestmentAccount a) {
        return new AccountResponse(
                a.getId(),
                a.getName(),
                a.getBrokerageFirm() != null ? a.getBrokerageFirm().getId() : null,
                a.getBrokerageFirm() != null ? a.getBrokerageFirm().getName() : null,
                a.getAccountType().name(), a.getCurrency(),
                a.getAnnualContributionLimit(), a.getContributedThisYear(), a.isActive());
    }
}
