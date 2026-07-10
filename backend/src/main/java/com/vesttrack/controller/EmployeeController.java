package com.vesttrack.controller;

import com.vesttrack.domain.entity.FinancialInstrument;
import com.vesttrack.domain.entity.User;
import com.vesttrack.domain.enums.TicketStatus;
import com.vesttrack.dto.admin.CreateInstrumentRequest;
import com.vesttrack.dto.portfolio.AnonymizedPortfolioView;
import com.vesttrack.dto.ticket.TicketResponse;
import com.vesttrack.dto.ticket.UpdateTicketStatusRequest;
import com.vesttrack.service.CurrentUserService;
import com.vesttrack.service.InstrumentService;
import com.vesttrack.service.PortfolioAnalysisService;
import com.vesttrack.service.TicketService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Panel Pracownika (Analityk/Support).
 * Bezpieczenstwo dostepu do tej ścieżki (/api/v1/employee/**) wymuszone globalnie
 * w SecurityConfig: hasAnyRole("EMPLOYEE", "ADMIN").
 */
@RestController
@RequestMapping("/api/v1/employee")
@RequiredArgsConstructor
@Tag(name = "Panel Pracownika", description = "Zarzadzanie slownikiem instrumentow i zgloszeniami")
public class EmployeeController {

    private final InstrumentService instrumentService;
    private final TicketService ticketService;
    private final PortfolioAnalysisService portfolioAnalysisService;
    private final CurrentUserService currentUserService;

    @PostMapping("/instruments")
    public ResponseEntity<FinancialInstrument> createInstrument(@Valid @RequestBody CreateInstrumentRequest request) {
        User actor = currentUserService.getCurrentUser();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(instrumentService.createInstrument(request, actor));
    }

    @GetMapping("/instruments/search")
    public ResponseEntity<List<FinancialInstrument>> searchInstruments(@RequestParam String query) {
        return ResponseEntity.ok(instrumentService.search(query));
    }

    @GetMapping("/tickets")
    public ResponseEntity<List<TicketResponse>> getTickets(
            @RequestParam(required = false) TicketStatus status) {
        List<TicketResponse> tickets = status != null
                ? ticketService.getTicketsByStatus(status)
                : ticketService.getAllTickets();
        return ResponseEntity.ok(tickets);
    }

    @PostMapping("/tickets/{ticketId}/assign-to-me")
    public ResponseEntity<TicketResponse> assignToMe(@PathVariable Long ticketId) {
        User employee = currentUserService.getCurrentUser();
        return ResponseEntity.ok(ticketService.assignToSelf(ticketId, employee));
    }

    @PatchMapping("/tickets/{ticketId}/status")
    public ResponseEntity<TicketResponse> updateStatus(@PathVariable Long ticketId,
                                                         @Valid @RequestBody UpdateTicketStatusRequest request) {
        User actor = currentUserService.getCurrentUser();
        return ResponseEntity.ok(ticketService.updateStatus(ticketId, request.status(), actor));
    }

    /**
     * Anonimowy przeglad struktury portfela klienta - wylacznie procentowy podzial,
     * bez ujawniania kwot (patrz PortfolioAnalysisService.getAnonymizedView).
     */
    @GetMapping("/accounts/{accountId}/anonymized-view")
    public ResponseEntity<AnonymizedPortfolioView> getAnonymizedView(@PathVariable Long accountId) {
        return ResponseEntity.ok(portfolioAnalysisService.getAnonymizedView(accountId));
    }
}
