package com.vesttrack.dto.admin;

public record UpdateProviderConfigRequest(
        String apiKey,
        Integer dailyLimit,
        Boolean active
) {}
