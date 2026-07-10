package com.vesttrack.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "api_provider_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiProviderConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String provider;

    @Column(name = "api_key")
    private String apiKey;

    @Column(name = "daily_limit")
    private Integer dailyLimit;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
