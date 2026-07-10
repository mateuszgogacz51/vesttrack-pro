package com.vesttrack.dto.ticket;

import com.vesttrack.domain.entity.SupportTicket;

import java.time.OffsetDateTime;

public record TicketResponse(
        Long id,
        String subject,
        String description,
        String status,
        String assignedEmployeeEmail,
        OffsetDateTime createdAt
) {
    public static TicketResponse from(SupportTicket t) {
        return new TicketResponse(
                t.getId(), t.getSubject(), t.getDescription(), t.getStatus().name(),
                t.getAssignedEmployee() != null ? t.getAssignedEmployee().getEmail() : null,
                t.getCreatedAt());
    }
}
