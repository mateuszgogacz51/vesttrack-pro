package com.vesttrack.service;

import com.vesttrack.domain.entity.PasswordResetToken;
import com.vesttrack.domain.entity.User;
import com.vesttrack.exception.ApiException;
import com.vesttrack.repository.PasswordResetTokenRepository;
import com.vesttrack.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Optional;

/**
 * Obsluguje przeplyw "zapomnialem hasla":
 *  1. requestReset(email) - generuje jednorazowy token, wysyla mailem link resetujacy.
 *     Ze wzgledow bezpieczenstwa (ochrona przed enumeracja kont) ZAWSZE konczy sie
 *     sukcesem z punktu widzenia API, niezaleznie czy e-mail istnieje w bazie czy nie.
 *  2. confirmReset(token, newPassword) - waliduje token (jednorazowy, z czasem wygasniecia),
 *     ustawia nowe haslo i uniewaznia WSZYSTKIE dotychczasowe refresh tokeny uzytkownika
 *     (wymusza ponowne zalogowanie wszedzie - standardowa praktyka bezpieczenstwa po zmianie hasla).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final RefreshTokenService refreshTokenService;
    private final AuditService auditService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.password-reset.token-expiration-minutes}")
    private long tokenExpirationMinutes;

    @Transactional
    public void requestReset(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            // Celowo nic nie robimy i nie zglaszamy bledu - patrz komentarz klasy.
            log.info("Zadanie resetu hasla dla nieistniejacego e-maila: {}", email);
            return;
        }

        User user = userOpt.get();
        String rawToken = generateRawToken();

        PasswordResetToken entity = PasswordResetToken.builder()
                .user(user)
                .tokenHash(hash(rawToken))
                .expiresAt(OffsetDateTime.now().plusMinutes(tokenExpirationMinutes))
                .used(false)
                .build();
        resetTokenRepository.save(entity);

        emailService.sendPasswordResetEmail(user.getEmail(), rawToken);
        auditService.log(user, "PASSWORD_RESET_REQUESTED", "Wygenerowano token resetu hasla");
    }

    @Transactional
    public void confirmReset(String rawToken, String newPassword) {
        PasswordResetToken token = resetTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new ApiException("Nieprawidłowy lub nieznany token resetujący", HttpStatus.BAD_REQUEST));

        if (token.isUsed()) {
            throw new ApiException("Ten link resetujący został już wykorzystany", HttpStatus.BAD_REQUEST);
        }
        if (token.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new ApiException("Link resetujący wygasł, poproś o nowy", HttpStatus.BAD_REQUEST);
        }

        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        token.setUsed(true);
        resetTokenRepository.save(token);

        // Wylogowanie ze wszystkich urzadzen po zmianie hasla - standardowa praktyka bezpieczenstwa.
        refreshTokenService.revokeAllForUser(user.getId());

        auditService.log(user, "PASSWORD_RESET_COMPLETED", "Haslo zostalo zmienione przez reset");
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 niedostepny w tym srodowisku JVM", e);
        }
    }
}
