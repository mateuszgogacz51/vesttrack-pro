package com.vesttrack.dto.admin;

import com.vesttrack.domain.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateEmployeeRequest(
        @NotBlank @Email String email,
        @NotBlank String temporaryPassword,
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotNull Role role
) {}
