package com.vesttrack.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, message = "Haslo musi miec co najmniej 8 znakow") String password,
        @NotBlank String firstName,
        @NotBlank String lastName
) {}
