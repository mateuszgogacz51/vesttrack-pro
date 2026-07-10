package com.vesttrack.dto.ticket;

import jakarta.validation.constraints.NotBlank;

public record CreateTicketRequest(
        @NotBlank String subject,
        @NotBlank String description
) {}
