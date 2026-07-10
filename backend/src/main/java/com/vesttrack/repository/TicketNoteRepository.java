package com.vesttrack.repository;

import com.vesttrack.domain.entity.TicketNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketNoteRepository extends JpaRepository<TicketNote, Long> {
    List<TicketNote> findByTicketIdOrderByCreatedAtAsc(Long ticketId);
}
