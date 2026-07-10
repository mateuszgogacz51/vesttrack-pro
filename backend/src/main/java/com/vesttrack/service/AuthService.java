package com.vesttrack.service;

import com.vesttrack.domain.entity.User;
import com.vesttrack.domain.enums.Role;
import com.vesttrack.dto.auth.AuthResponse;
import com.vesttrack.dto.auth.LoginRequest;
import com.vesttrack.dto.auth.RegisterRequest;
import com.vesttrack.exception.ApiException;
import com.vesttrack.repository.UserRepository;
import com.vesttrack.security.JwtService;
import com.vesttrack.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final AuditService auditService;
    private final EmailService emailService;

    @Transactional
    public User createUserRecord(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ApiException("Konto z tym adresem e-mail juz istnieje", HttpStatus.CONFLICT);
        }

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .firstName(request.firstName())
                .lastName(request.lastName())
                .role(Role.USER)
                .enabled(true)
                .baseCurrency("PLN")
                .build();
        return userRepository.save(user);
    }

    public AuthResponse register(RegisterRequest request) {
        User user = createUserRecord(request);

        auditService.log(user, "USER_REGISTERED", "Nowa rejestracja: " + user.getEmail());
        emailService.sendWelcomeEmail(user.getEmail(), user.getFirstName());

        return issueTokenPair(user);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ApiException("Nieprawidlowy email lub haslo", HttpStatus.UNAUTHORIZED));

        auditService.log(user, "USER_LOGIN", "Zalogowano pomyslnie");
        return issueTokenPair(user);
    }

    /**
     * Wymienia poprawny, nieuzyty jeszcze refresh token na nowa pare tokenow
     * (nowy access token + nowy refresh token - rotacja, patrz RefreshTokenService).
     */
    @Transactional
    public AuthResponse refresh(String rawRefreshToken) {
        RefreshTokenService.RotationResult result = refreshTokenService.rotateToken(rawRefreshToken);
        User user = result.user();

        UserPrincipal principal = new UserPrincipal(user);
        String newAccessToken = jwtService.generateToken(principal);

        return new AuthResponse(newAccessToken, result.newRawRefreshToken(), user.getEmail(), user.getRole().name());
    }

    /** Wylogowanie - uniewaznia WSZYSTKIE aktywne refresh tokeny uzytkownika (wylogowanie ze wszystkich urzadzen). */
    @Transactional
    public void logout(User user) {
        refreshTokenService.revokeAllForUser(user.getId());
        auditService.log(user, "USER_LOGOUT", "Uniewazniono wszystkie refresh tokeny");
    }

    private AuthResponse issueTokenPair(User user) {
        UserPrincipal principal = new UserPrincipal(user);
        String accessToken = jwtService.generateToken(principal);
        String refreshToken = refreshTokenService.issueNewRefreshToken(user);
        return new AuthResponse(accessToken, refreshToken, user.getEmail(), user.getRole().name());
    }
}
