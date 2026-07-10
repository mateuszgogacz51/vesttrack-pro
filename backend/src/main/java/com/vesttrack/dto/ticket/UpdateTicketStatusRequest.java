package com.vesttrack.dto.ticket;

import com.vesttrack.domain.enums.TicketStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTicketStatusRequest(@NotNull TicketStatus status) {}
