package com.vesttrack.repository;

import com.vesttrack.domain.entity.ApiUsageDaily;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ApiUsageDailyRepository extends JpaRepository<ApiUsageDaily, Long> {
    Optional<ApiUsageDaily> findByProviderAndUsageDate(String provider, LocalDate usageDate);
    List<ApiUsageDaily> findByUsageDateOrderByProviderAsc(LocalDate usageDate);
    List<ApiUsageDaily> findByProviderOrderByUsageDateDesc(String provider);
}
