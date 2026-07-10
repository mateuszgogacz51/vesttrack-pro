package com.vesttrack.dto.admin;

import com.vesttrack.domain.entity.ApiProviderConfig;

public record ProviderConfigResponse(
        String provider,
        boolean hasApiKey,
        Integer dailyLimit,
        boolean active
) {
    public static ProviderConfigResponse from(ApiProviderConfig config) {
        return new ProviderConfigResponse(
                config.getProvider(),
                config.getApiKey() != null && !config.getApiKey().isBlank(),
                config.getDailyLimit(),
                config.isActive());
    }
}
