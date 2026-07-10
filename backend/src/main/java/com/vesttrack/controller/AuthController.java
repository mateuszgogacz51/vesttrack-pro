package com.vesttrack.controller;

import com.vesttrack.dto.auth.AuthResponse;
import com.vesttrack.dto.auth.ForgotPasswordRequest;
import com.vesttrack.dto.auth.LoginRequest;
import com.vesttrack.dto.auth.LogoutRequest;
import com.vesttrack.dto.auth.RefreshRequest;
import com.vesttrack.dto.auth.RegisterRequest;
import com.vesttrack.dto.auth.ResetPasswordRequest;
import com.vesttrack.service.AuthService;
import com.vesttrack.service.CurrentUserService;
import com.vesttrack.service.PasswordResetService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Autoryzacja", description = "Rejestracja, logowanie, odswiezanie tokenow i reset hasla")
public class AuthController {

    private final AuthService authService;
    private final CurrentUserService currentUserService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Wymienia refresh token na nowa pare tokenow (access + refresh).
     * Endpoint publiczny (nie wymaga JWT access tokenu) - autoryzacja odbywa sie
     * poprzez sam refresh token przekazany w body.
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request.refreshToken()));
    }

    /** Wylogowanie - wymaga aktywnego access tokenu (Authorization: Bearer ...). */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(currentUserService.getCurrentUser());
        return ResponseEntity.noContent().build();
    }

    /**
     * Zadanie resetu hasla - wysyla mailem link resetujacy, jesli e-mail istnieje w bazie.
     * Zawsze zwraca 202 Accepted, niezaleznie od tego czy e-mail istnieje (ochrona przed
     * enumeracja kont - nie ujawniamy, ktore adresy sa zarejestrowane).
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestReset(request.email());
        return ResponseEntity.accepted().build();
    }

    /** Ustawienie nowego hasla na podstawie tokenu otrzymanego mailem. */
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.confirmReset(request.token(), request.newPassword());
        return ResponseEntity.noContent().build();
    }
}
