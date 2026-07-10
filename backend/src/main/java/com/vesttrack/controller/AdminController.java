package com.vesttrack.controller;

import com.vesttrack.domain.entity.User;
import com.vesttrack.domain.enums.Role;
import com.vesttrack.dto.admin.ApiUsageResponse;
import com.vesttrack.dto.admin.AuditLogResponse;
import com.vesttrack.dto.admin.CreateEmployeeRequest;
import com.vesttrack.dto.admin.EmployeeStatsResponse;
import com.vesttrack.dto.admin.ProviderConfigResponse;
import com.vesttrack.dto.admin.UpdateProviderConfigRequest;
import com.vesttrack.service.AdminService;
import com.vesttrack.service.CurrentUserService;
import com.vesttrack.service.InstrumentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Panel Administratora. Dostep wymuszony globalnie w SecurityConfig: hasRole("ADMIN").
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Panel Administratora", description = "Zarzadzanie zespolem, audyt, statystyki wydajnosci")
public class AdminController {

    private final AdminService adminService;
    private final InstrumentService instrumentService;
    private final CurrentUserService currentUserService;

    @PostMapping("/employees")
    public ResponseEntity<Void> createEmployee(@Valid @RequestBody CreateEmployeeRequest request) {
        adminService.createEmployeeAccount(request, currentUserService.getCurrentUser());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PatchMapping("/users/{userId}/enabled")
    public ResponseEntity<Void> setEnabled(@PathVariable Long userId, @RequestParam boolean enabled) {
        adminService.setUserEnabled(userId, enabled, currentUserService.getCurrentUser());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/users/{userId}/role")
    public ResponseEntity<Void> changeRole(@PathVariable Long userId, @RequestParam Role role) {
        adminService.changeUserRole(userId, role, currentUserService.getCurrentUser());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/instruments/{instrumentId}/block")
    public ResponseEntity<Void> blockInstrument(@PathVariable Long instrumentId, @RequestParam String reason) {
        instrumentService.blockInstrument(instrumentId, currentUserService.getCurrentUser(), reason);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats/employees")
    public ResponseEntity<List<EmployeeStatsResponse>> employeeStats() {
        return ResponseEntity.ok(adminService.getEmployeePerformanceStats());
    }

    @GetMapping("/audit-log")
    public ResponseEntity<List<AuditLogResponse>> auditLog(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(adminService.getRecentAuditLog(page, size));
    }

    // -----------------------------------------------------------------
    // Rate-Limiting Dashboard - monitorowanie i zarzadzanie zewnetrznymi API
    // -----------------------------------------------------------------

    @GetMapping("/api-usage")
    public ResponseEntity<List<ApiUsageResponse>> apiUsageToday() {
        return ResponseEntity.ok(adminService.getTodayApiUsage());
    }

    @GetMapping("/api-usage/providers")
    public ResponseEntity<List<ProviderConfigResponse>> providerConfigs() {
        return ResponseEntity.ok(adminService.getProviderConfigs());
    }

    @PatchMapping("/api-usage/providers/{provider}")
    public ResponseEntity<Void> updateProviderConfig(@PathVariable String provider,
                                                       @RequestBody UpdateProviderConfigRequest request) {
        adminService.updateProviderConfig(provider, request, currentUserService.getCurrentUser());
        return ResponseEntity.noContent().build();
    }
}
