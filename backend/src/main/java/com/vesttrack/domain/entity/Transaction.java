package com.vesttrack.domain.entity;

import com.vesttrack.domain.enums.TransactionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private InvestmentAccount account;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instrument_id", nullable = false)
    private FinancialInstrument instrument;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private TransactionType transactionType;

    @Column(precision = 18, scale = 6, nullable = false)
    private BigDecimal quantity;

    @Column(precision = 18, scale = 6, nullable = false)
    private BigDecimal price;

    @Column(precision = 18, scale = 6, nullable = false)
    private BigDecimal fee = BigDecimal.ZERO;

    @Column(name = "instrument_currency", nullable = false)
    private String instrumentCurrency;

    @Column(name = "exchange_rate", precision = 18, scale = 6, nullable = false)
    private BigDecimal exchangeRate = BigDecimal.ONE;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "realized_gain", precision = 18, scale = 2)
    private BigDecimal realizedGain;

    @Column(name = "realized_gain_currency")
    private String realizedGainCurrency;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
