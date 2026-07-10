package com.vesttrack.service;

import com.vesttrack.domain.entity.RefreshToken;
import com.vesttrack.domain.entity.User;
import com.vesttrack.exception.ApiException;
import com.vesttrack.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;

/**
 * Zarzadza refresh tokenami wg wzorca "rotating refresh token":
 *  - Token przekazywany klientowi jest losowym, nieprzewidywalnym ciagiem (256 bit),
 *    NIGDY nie jest przechowywany w bazie w postaci jawnej - tylko jego hash SHA-256.
 *  - Kazde uzycie refresh tokenu do odswiezenia access tokenu UNIEWAZNIA go
 *    i wydaje NOWY refresh token (rotacja) - ogranicza to skutki ewentualnej kradziezy tokenu:
 *    jesli ktos uzyje juz zuzytego/uniewaznionego tokenu, jest to sygnal potencjalnego wycieku.
 *  - Access token (JWT, krotki czas zycia - domyslnie 60 min) sluzy do autoryzacji requestow.
 *  - Refresh token (dlugi czas zycia - domyslnie 7 dni) sluzy WYLACZNIE do wymiany na nowy access token.
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${jwt.refresh-expiration-days}")
    private long refreshExpirationDays;

    @Transactional
    public String issueNewRefreshToken(User user) {
        String rawToken = generateRawToken();
        RefreshToken entity = RefreshToken.builder()
                .user(user)
                .tokenHash(hash(rawToken))
                .expiresAt(OffsetDateTime.now().plusDays(refreshExpirationDays))
                .revoked(false)
                .build();
        refreshTokenRepository.save(entity);
        return rawToken;
    }

    /**
     * Waliduje refresh token, uniewaznia go i wydaje nowy (rotacja).
     * Zwraca uzytkownika oraz nowy, surowy refresh token do przekazania klientowi.
     */
    @Transactional
    public RotationResult rotateToken(String rawToken) {
        RefreshToken existing = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new ApiException("Nieprawidlowy refresh token", HttpStatus.UNAUTHORIZED));

        if (existing.isRevoked()) {
            // Token juz uzyty/uniewazniony - potencjalny sygnal kradziezy tokenu.
            // Zgodnie z dobra praktyka, uniewazniamy WSZYSTKIE tokeny tego uzytkownika.
            refreshTokenRepository.revokeAllActiveForUser(existing.getUser().getId());
            throw new ApiException(
                    "Refresh token zostal juz uzyty. Ze wzgledow bezpieczenstwa wszystkie sesje zostaly zakonczone - zaloguj sie ponownie.",
                    HttpStatus.UNAUTHORIZED);
        }

        if (existing.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new ApiException("Refresh token wygasl, zaloguj sie ponownie", HttpStatus.UNAUTHORIZED);
        }

        existing.setRevoked(true);

        String newRawToken = generateRawToken();
        RefreshToken newToken = RefreshToken.builder()
                .user(existing.getUser())
                .tokenHash(hash(newRawToken))
                .expiresAt(OffsetDateTime.now().plusDays(refreshExpirationDays))
                .revoked(false)
                .build();
        refreshTokenRepository.save(newToken);

        existing.setReplacedBy(newToken);
        refreshTokenRepository.save(existing);

        return new RotationResult(existing.getUser(), newRawToken);
    }

    @Transactional
    public void revokeAllForUser(Long userId) {
        refreshTokenRepository.revokeAllActiveForUser(userId);
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32]; // 256 bit
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

    public record RotationResult(User user, String newRawRefreshToken) {}
}
