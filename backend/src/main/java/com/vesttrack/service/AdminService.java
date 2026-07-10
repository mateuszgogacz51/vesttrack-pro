package com.vesttrack.service;

import com.vesttrack.domain.entity.User;
import com.vesttrack.domain.enums.Role;
import com.vesttrack.domain.enums.TicketStatus;
import com.vesttrack.dto.admin.ApiUsageResponse;
import com.vesttrack.dto.admin.AuditLogResponse;
import com.vesttrack.dto.admin.CreateEmployeeRequest;
import com.vesttrack.dto.admin.EmployeeStatsResponse;
import com.vesttrack.dto.admin.ProviderConfigResponse;
import com.vesttrack.dto.admin.UpdateProviderConfigRequest;
import com.vesttrack.exception.ApiException;
import com.vesttrack.exception.ResourceNotFoundException;
import com.vesttrack.repository.AuditLogRepository;
import com.vesttrack.repository.SupportTicketRepository;
import com.vesttrack.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Panel administratora: zarzadzanie zespolem pracownikow, statystyki wydajnosci
 * (ile zgloszen zamknal dany pracownik) oraz dostep do dziennika audytu.
 */
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SupportTicketRepository ticketRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuditService auditService;
    private final ApiUsageTrackingService apiUsageTrackingService;

    @Transactional
    public void createEmployeeAccount(CreateEmployeeRequest request, User actor) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ApiException("Konto z tym adresem e-mail juz istnieje", HttpStatus.CONFLICT);
        }
        if (request.role() == Role.USER) {
            throw new ApiException("Ten endpoint slusza wylacznie do tworzenia kont EMPLOYEE/ADMIN", HttpStatus.BAD_REQUEST);
        }

        User employee = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.temporaryPassword()))
                .firstName(request.firstName())
                .lastName(request.lastName())
                .role(request.role())
                .enabled(true)
                .baseCurrency("PLN")
                .build();
        userRepository.save(employee);

        auditService.log(actor, "EMPLOYEE_ACCOUNT_CREATED",
                "Utworzono konto " + request.role() + ": " + request.email());
    }

    @Transactional
    public void setUserEnabled(Long userId, boolean enabled, User actor) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono uzytkownika"));
        user.setEnabled(enabled);
        userRepository.save(user);
        auditService.log(actor, enabled ? "USER_ENABLED" : "USER_DISABLED",
                "Konto " + user.getEmail() + " -> enabled=" + enabled);
    }

    @Transactional
    public void changeUserRole(Long userId, Role newRole, User actor) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono uzytkownika"));
        Role oldRole = user.getRole();
        user.setRole(newRole);
        userRepository.save(user);
        auditService.log(actor, "USER_ROLE_CHANGED",
                "Zmieniono role " + user.getEmail() + ": " + oldRole + " -> " + newRole);
    }

    public List<EmployeeStatsResponse> getEmployeePerformanceStats() {
        return userRepository.findByRole(Role.EMPLOYEE).stream()
                .map(emp -> new EmployeeStatsResponse(
                        emp.getId(),
                        emp.getEmail(),
                        ticketRepository.countByAssignedEmployeeIdAndStatus(emp.getId(), TicketStatus.RESOLVED)
                                + ticketRepository.countByAssignedEmployeeIdAndStatus(emp.getId(), TicketStatus.CLOSED),
                        ticketRepository.countByAssignedEmployeeIdAndStatus(emp.getId(), TicketStatus.IN_PROGRESS)
                ))
                .toList();
    }

    public List<AuditLogResponse> getRecentAuditLog(int page, int size) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size)).stream()
                .map(AuditLogResponse::from)
                .toList();
    }

    // ---------------------------------------------------------------------
    // Rate-Limiting Dashboard: monitorowanie zuzycia zewnetrznych API
    // ---------------------------------------------------------------------

    /** Zuzycie limitow zewnetrznych API (Yahoo Finance, Alpha Vantage, NBP) dla dzisiejszego dnia. */
    public List<ApiUsageResponse> getTodayApiUsage() {
        var configs = apiUsageTrackingService.getAllProviderConfigs();
        var usageToday = apiUsageTrackingService.getUsageForDate(LocalDate.now());

        return usageToday.stream()
                .map(usage -> {
                    Integer limit = configs.stream()
                            .filter(c -> c.getProvider().equals(usage.getProvider()))
                            .findFirst()
                            .map(com.vesttrack.domain.entity.ApiProviderConfig::getDailyLimit)
                            .orElse(null);
                    return ApiUsageResponse.from(usage, limit);
                })
                .toList();
    }

    public List<ProviderConfigResponse> getProviderConfigs() {
        return apiUsageTrackingService.getAllProviderConfigs().stream()
                .map(ProviderConfigResponse::from)
                .toList();
    }

    /** Dynamiczna zmiana klucza API / limitu / statusu dostawcy "na zywo", bez restartu aplikacji. */
    @Transactional
    public void updateProviderConfig(String provider, UpdateProviderConfigRequest request, User actor) {
        apiUsageTrackingService.updateProviderKey(
                provider, request.apiKey(), request.dailyLimit(), request.active());
        auditService.log(actor, "API_PROVIDER_CONFIG_UPDATED",
                "Zaktualizowano konfiguracje dostawcy " + provider);
    }
}
