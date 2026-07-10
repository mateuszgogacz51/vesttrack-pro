package com.vesttrack.dto.admin;

import com.vesttrack.domain.entity.AuditLog;

import java.time.OffsetDateTime;

public record AuditLogResponse(
        Long id,
        String actorEmail,
        String action,
        String details,
        String ipAddress,
        OffsetDateTime createdAt
) {
    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getActor() != null ? log.getActor().getEmail() : "system",
                log.getAction(), log.getDetails(), log.getIpAddress(), log.getCreatedAt());
    }
}
