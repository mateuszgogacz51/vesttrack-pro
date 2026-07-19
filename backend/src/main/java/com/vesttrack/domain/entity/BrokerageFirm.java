package com.vesttrack.domain.entity;

import com.vesttrack.domain.enums.FirmCategory;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

/**
 * Slownik instytucji (biur maklerskich / bankow / TFI) dostepnych do wyboru
 * przy zakladaniu rachunku inwestycyjnego - patrz {@link com.vesttrack.service.AccountService}.
 */
@Entity
@Table(name = "brokerage_firms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BrokerageFirm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FirmCategory category;

    private String website;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
