package com.vesttrack.service;

import com.vesttrack.domain.entity.AuditLog;
import com.vesttrack.domain.entity.User;
import com.vesttrack.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Zapisuje zdarzenia wrazliwe do tabeli audit_log:
 * logowania, zmiany uprawnien, dostepy administracyjne, proby nieautoryzowanego dostepu.
 * Uzywa wlasnej, niezaleznej transakcji (REQUIRES_NEW), aby wpis audytowy
 * powstal nawet jesli glowna operacja zostanie wycofana (rollback).
 */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(User actor, String action, String details) {
        AuditLog entry = AuditLog.builder()
                .actor(actor)
                .action(action)
                .details(details)
                .build();
        auditLogRepository.save(entry);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logSystem(String action, String details) {
        log(null, action, details);
    }
}
