package com.vesttrack.dto.transaction;

import com.vesttrack.domain.entity.Transaction;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionResponse(
        Long id,
        Long accountId,
        String instrumentTicker,
        String transactionType,
        BigDecimal quantity,
        BigDecimal price,
        BigDecimal fee,
        String instrumentCurrency,
        BigDecimal exchangeRate,
        LocalDate transactionDate,
        BigDecimal realizedGain,
        String realizedGainCurrency
) {
    public static TransactionResponse from(Transaction t) {
        return new TransactionResponse(
                t.getId(), t.getAccount().getId(), t.getInstrument().getTicker(),
                t.getTransactionType().name(), t.getQuantity(), t.getPrice(), t.getFee(),
                t.getInstrumentCurrency(), t.getExchangeRate(), t.getTransactionDate(),
                t.getRealizedGain(), t.getRealizedGainCurrency());
    }
}
