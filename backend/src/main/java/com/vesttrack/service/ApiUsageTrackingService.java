package com.vesttrack.service;

import com.vesttrack.domain.entity.ApiProviderConfig;
import com.vesttrack.domain.entity.ApiUsageDaily;
import com.vesttrack.repository.ApiProviderConfigRepository;
import com.vesttrack.repository.ApiUsageDailyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Zlicza wywolania i bledy zewnetrznych API (Yahoo Finance, Alpha Vantage, NBP)
 * w rozbiciu dzien/dostawca - zasila panel "Rate-Limiting Dashboard" administratora
 * (AdminController -> GET /api/v1/admin/api-usage).
 *
 * Wlasna transakcja REQUIRES_NEW: zapis licznika nie moze zostac wycofany nawet jesli
 * wywolanie zewnetrznego API zakonczy sie bledem i "opakowujaca" transakcja zrobi rollback.
 */
@Service
@RequiredArgsConstructor
public class ApiUsageTrackingService {

    private final ApiUsageDailyRepository usageRepository;
    private final ApiProviderConfigRepository providerConfigRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordCall(String provider, boolean success) {
        LocalDate today = LocalDate.now();
        ApiUsageDaily usage = usageRepository.findByProviderAndUsageDate(provider, today)
                .orElseGet(() -> ApiUsageDaily.builder()
                        .provider(provider)
                        .usageDate(today)
                        .callCount(0)
                        .errorCount(0)
                        .build());

        usage.setCallCount(usage.getCallCount() + 1);
        if (!success) {
            usage.setErrorCount(usage.getErrorCount() + 1);
        }
        usageRepository.save(usage);
    }

    /** Sprawdza, czy dostawca nie przekroczyl dziennego limitu zapytan skonfigurowanego przez admina. */
    public boolean isWithinDailyLimit(String provider) {
        Optional<ApiProviderConfig> config = providerConfigRepository.findByProvider(provider);
        if (config.isEmpty() || config.get().getDailyLimit() == null) {
            return true; // brak skonfigurowanego limitu = bez ograniczen
        }
        int usedToday = usageRepository.findByProviderAndUsageDate(provider, LocalDate.now())
                .map(ApiUsageDaily::getCallCount)
                .orElse(0);
        return usedToday < config.get().getDailyLimit();
    }

    public List<ApiUsageDaily> getUsageForDate(LocalDate date) {
        return usageRepository.findByUsageDateOrderByProviderAsc(date);
    }

    public List<ApiProviderConfig> getAllProviderConfigs() {
        return providerConfigRepository.findAllByOrderByProviderAsc();
    }

    @Transactional
    public void updateProviderKey(String provider, String newApiKey, Integer newDailyLimit, Boolean active) {
        ApiProviderConfig config = providerConfigRepository.findByProvider(provider)
                .orElseThrow(() -> new com.vesttrack.exception.ResourceNotFoundException(
                        "Nieznany dostawca API: " + provider));
        if (newApiKey != null) config.setApiKey(newApiKey);
        if (newDailyLimit != null) config.setDailyLimit(newDailyLimit);
        if (active != null) config.setActive(active);
        providerConfigRepository.save(config);
    }
}
