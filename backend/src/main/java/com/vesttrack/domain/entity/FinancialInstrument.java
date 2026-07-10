package com.vesttrack.domain.entity;

import com.vesttrack.domain.enums.AssetType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "financial_instruments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialInstrument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String ticker;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false)
    private AssetType assetType;

    @Column(unique = true)
    private String isin;

    private String exchange;

    @Column(name = "quote_currency", nullable = false)
    private String quoteCurrency;

    @Column(name = "is_accumulating")
    private Boolean accumulating; // true=Acc, false=Dist, null=nie dotyczy (np. akcje)

    @Column(name = "is_blocked", nullable = false)
    private boolean blocked = false;

    @Column(name = "last_price", precision = 18, scale = 6)
    private BigDecimal lastPrice;

    @Column(name = "last_price_at")
    private OffsetDateTime lastPriceAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
