package com.vesttrack.dto.auth;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        String email,
        String role
) {
    public AuthResponse(String accessToken, String refreshToken, String email, String role) {
        this(accessToken, refreshToken, "Bearer", email, role);
    }
}
