package com.vesttrack.dto.transaction;

import com.vesttrack.domain.enums.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateTransactionRequest(
        @NotNull Long accountId,
        @NotNull Long instrumentId,
        @NotNull TransactionType transactionType,
        @NotNull @DecimalMin(value = "0.000001") BigDecimal quantity,
        @NotNull @DecimalMin(value = "0") BigDecimal price,
        BigDecimal fee,
        @NotNull String instrumentCurrency,
        @NotNull @DecimalMin(value = "0.000001") BigDecimal exchangeRate,
        @NotNull LocalDate transactionDate
) {}
