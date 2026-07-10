package com.vesttrack.repository;

import com.vesttrack.domain.entity.ApiProviderConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApiProviderConfigRepository extends JpaRepository<ApiProviderConfig, Long> {
    Optional<ApiProviderConfig> findByProvider(String provider);
    List<ApiProviderConfig> findAllByOrderByProviderAsc();
}
