package com.vesttrack.dto.admin;

import com.vesttrack.domain.entity.ApiUsageDaily;

import java.time.LocalDate;

public record ApiUsageResponse(
        String provider,
        LocalDate usageDate,
        int callCount,
        int errorCount,
        Integer dailyLimit,
        Double usagePercentage
) {
    public static ApiUsageResponse from(ApiUsageDaily usage, Integer dailyLimit) {
        Double percentage = (dailyLimit != null && dailyLimit > 0)
                ? Math.round((usage.getCallCount() * 10000.0) / dailyLimit) / 100.0
                : null;
        return new ApiUsageResponse(usage.getProvider(), usage.getUsageDate(),
                usage.getCallCount(), usage.getErrorCount(), dailyLimit, percentage);
    }
}
