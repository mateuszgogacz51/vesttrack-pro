package com.vesttrack.dto.admin;

public record EmployeeStatsResponse(
        Long employeeId,
        String email,
        long resolvedTickets,
        long inProgressTickets
) {}
