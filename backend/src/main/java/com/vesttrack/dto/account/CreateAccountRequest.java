package com.vesttrack.dto.account;

import com.vesttrack.domain.enums.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateAccountRequest(
        @NotBlank String name,
        @NotNull AccountType accountType,
        @NotBlank String currency,
        java.math.BigDecimal annualContributionLimit
) {}
