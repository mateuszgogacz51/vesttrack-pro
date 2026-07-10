package com.vesttrack.service;

import com.vesttrack.domain.entity.SupportTicket;
import com.vesttrack.domain.entity.TicketNote;
import com.vesttrack.domain.entity.User;
import com.vesttrack.domain.enums.Role;
import com.vesttrack.domain.enums.TicketStatus;
import com.vesttrack.dto.ticket.AddNoteRequest;
import com.vesttrack.dto.ticket.CreateTicketRequest;
import com.vesttrack.dto.ticket.TicketResponse;
import com.vesttrack.exception.ApiException;
import com.vesttrack.exception.ResourceNotFoundException;
import com.vesttrack.repository.SupportTicketRepository;
import com.vesttrack.repository.TicketNoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Obsluga zgloszen od uzytkownikow (np. "brak instrumentu w bazie", "blad splitu akcji").
 * Pracownik moze przypisac zgloszenie do siebie, zmienic status i dodac notatke wewnetrzna.
 * Notatki oznaczone jako internal=true NIE sa nigdy zwracane uzytkownikowi (rola USER).
 */
@Service
@RequiredArgsConstructor
public class TicketService {

    private final SupportTicketRepository ticketRepository;
    private final TicketNoteRepository ticketNoteRepository;
    private final AuditService auditService;

    @Transactional
    public TicketResponse createTicket(User user, CreateTicketRequest request) {
        SupportTicket ticket = SupportTicket.builder()
                .user(user)
                .subject(request.subject())
                .description(request.description())
                .status(TicketStatus.OPEN)
                .build();
        return TicketResponse.from(ticketRepository.save(ticket));
    }

    public List<TicketResponse> getMyTickets(User user) {
        return ticketRepository.findByUserId(user.getId()).stream()
                .map(TicketResponse::from)
                .toList();
    }

    public List<TicketResponse> getAllTickets() {
        return ticketRepository.findAll().stream().map(TicketResponse::from).toList();
    }

    public List<TicketResponse> getTicketsByStatus(TicketStatus status) {
        return ticketRepository.findByStatus(status).stream().map(TicketResponse::from).toList();
    }

    @Transactional
    public TicketResponse assignToSelf(Long ticketId, User employee) {
        SupportTicket ticket = getTicketOrThrow(ticketId);
        ticket.setAssignedEmployee(employee);
        if (ticket.getStatus() == TicketStatus.OPEN) {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
        }
        return TicketResponse.from(ticketRepository.save(ticket));
    }

    @Transactional
    public TicketResponse updateStatus(Long ticketId, TicketStatus newStatus, User actor) {
        SupportTicket ticket = getTicketOrThrow(ticketId);
        ticket.setStatus(newStatus);
        if (newStatus == TicketStatus.RESOLVED || newStatus == TicketStatus.CLOSED) {
            ticket.setResolvedAt(OffsetDateTime.now());
        }
        SupportTicket saved = ticketRepository.save(ticket);
        auditService.log(actor, "TICKET_STATUS_CHANGED",
                "Zgloszenie #" + ticketId + " -> " + newStatus);
        return TicketResponse.from(saved);
    }

    @Transactional
    public void addNote(Long ticketId, User author, AddNoteRequest request) {
        SupportTicket ticket = getTicketOrThrow(ticketId);

        // uzytkownik (USER) moze dodawac tylko notatki publiczne (internal=false),
        // nigdy notatek wewnetrznych widocznych dla zespolu
        boolean internal = request.internal();
        if (author.getRole() == Role.USER && internal) {
            throw new ApiException("Uzytkownik nie moze dodawac notatek wewnetrznych", HttpStatus.FORBIDDEN);
        }

        TicketNote note = TicketNote.builder()
                .ticket(ticket)
                .author(author)
                .note(request.note())
                .internal(internal)
                .build();
        ticketNoteRepository.save(note);
    }

    /** Notatki widoczne dla danego odbiorcy - USER nigdy nie widzi notatek internal=true. */
    public List<TicketNote> getVisibleNotes(Long ticketId, User requester) {
        List<TicketNote> all = ticketNoteRepository.findByTicketIdOrderByCreatedAtAsc(ticketId);
        if (requester.getRole() == Role.USER) {
            return all.stream().filter(n -> !n.isInternal()).toList();
        }
        return all;
    }

    private SupportTicket getTicketOrThrow(Long ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono zgloszenia #" + ticketId));
    }
}
