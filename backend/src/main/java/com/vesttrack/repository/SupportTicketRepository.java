package com.vesttrack.repository;

import com.vesttrack.domain.entity.SupportTicket;
import com.vesttrack.domain.enums.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {
    List<SupportTicket> findByUserId(Long userId);
    List<SupportTicket> findByStatus(TicketStatus status);
    List<SupportTicket> findByAssignedEmployeeId(Long employeeId);
    long countByAssignedEmployeeIdAndStatus(Long employeeId, TicketStatus status);
}
