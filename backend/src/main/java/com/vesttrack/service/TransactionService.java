package com.vesttrack.service;

import com.vesttrack.domain.entity.FinancialInstrument;
import com.vesttrack.domain.entity.InvestmentAccount;
import com.vesttrack.domain.entity.Transaction;
import com.vesttrack.domain.enums.TransactionType;
import com.vesttrack.dto.transaction.CreateTransactionRequest;
import com.vesttrack.dto.transaction.TransactionResponse;
import com.vesttrack.exception.ResourceNotFoundException;
import com.vesttrack.repository.FinancialInstrumentRepository;
import com.vesttrack.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final FinancialInstrumentRepository instrumentRepository;
    private final AccountService accountService;
    private final FifoCalculationService fifoCalculationService;

    @Transactional
    public TransactionResponse recordTransaction(CreateTransactionRequest request) {
        InvestmentAccount account = accountService.getOwnedAccountOrThrow(request.accountId());
        FinancialInstrument instrument = instrumentRepository.findById(request.instrumentId())
                .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono instrumentu"));

        BigDecimal fee = request.fee() != null ? request.fee() : BigDecimal.ZERO;

        Transaction.TransactionBuilder builder = Transaction.builder()
                .account(account)
                .instrument(instrument)
                .transactionType(request.transactionType())
                .quantity(request.quantity())
                .price(request.price())
                .fee(fee)
                .instrumentCurrency(request.instrumentCurrency())
                .exchangeRate(request.exchangeRate())
                .transactionDate(request.transactionDate());

        if (request.transactionType() == TransactionType.SELL) {
            BigDecimal realizedGain = fifoCalculationService.calculateRealizedGain(
                    account.getId(), instrument.getId(),
                    request.quantity(), request.price(), fee, request.exchangeRate());
            builder.realizedGain(realizedGain);
            builder.realizedGainCurrency(account.getCurrency());
        }

        if (request.transactionType() == TransactionType.BUY) {
            BigDecimal grossAmountInAccountCurrency = request.quantity().multiply(request.price())
                    .add(fee).multiply(request.exchangeRate());
            accountService.registerContribution(account, grossAmountInAccountCurrency);
        }

        Transaction saved = transactionRepository.save(builder.build());
        return TransactionResponse.from(saved);
    }

    public List<TransactionResponse> getAccountTransactions(Long accountId) {
        // weryfikacja wlasnosci konta
        accountService.getOwnedAccountOrThrow(accountId);
        return transactionRepository.findByAccountIdWithInstrument(accountId).stream()
                .map(TransactionResponse::from)
                .toList();
    }
}
