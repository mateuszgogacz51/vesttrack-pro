package com.vesttrack.controller;

import com.vesttrack.domain.entity.User;
import com.vesttrack.dto.ticket.AddNoteRequest;
import com.vesttrack.dto.ticket.CreateTicketRequest;
import com.vesttrack.dto.ticket.TicketResponse;
import com.vesttrack.service.CurrentUserService;
import com.vesttrack.service.TicketService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
@Tag(name = "Zgloszenia wsparcia", description = "System zgloszen uzytkownikow do Pracownikow/Admina")
public class TicketController {

    private final TicketService ticketService;
    private final CurrentUserService currentUserService;

    @PostMapping
    public ResponseEntity<TicketResponse> create(@Valid @RequestBody CreateTicketRequest request) {
        User user = currentUserService.getCurrentUser();
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketService.createTicket(user, request));
    }

    @GetMapping("/my")
    public ResponseEntity<List<TicketResponse>> myTickets() {
        return ResponseEntity.ok(ticketService.getMyTickets(currentUserService.getCurrentUser()));
    }

    @PostMapping("/{ticketId}/notes")
    public ResponseEntity<Void> addNote(@PathVariable Long ticketId, @Valid @RequestBody AddNoteRequest request) {
        ticketService.addNote(ticketId, currentUserService.getCurrentUser(), request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
