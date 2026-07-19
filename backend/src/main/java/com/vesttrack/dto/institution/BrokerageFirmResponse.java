package com.vesttrack.dto.institution;

import com.vesttrack.domain.entity.BrokerageFirm;

public record BrokerageFirmResponse(
        Long id,
        String name,
        String category,
        String website
) {
    public static BrokerageFirmResponse from(BrokerageFirm firm) {
        return new BrokerageFirmResponse(firm.getId(), firm.getName(), firm.getCategory().name(), firm.getWebsite());
    }
}
