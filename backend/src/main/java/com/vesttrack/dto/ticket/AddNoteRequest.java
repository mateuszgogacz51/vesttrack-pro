package com.vesttrack.dto.ticket;

import jakarta.validation.constraints.NotBlank;

public record AddNoteRequest(@NotBlank String note, boolean internal) {}
