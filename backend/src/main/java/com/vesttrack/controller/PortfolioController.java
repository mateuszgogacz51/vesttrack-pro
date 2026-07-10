package com.vesttrack.controller;

import com.vesttrack.dto.portfolio.PerformanceResponse;
import com.vesttrack.dto.portfolio.PortfolioAllocationResponse;
import com.vesttrack.service.PerformanceCalculationService;
import com.vesttrack.service.PortfolioAnalysisService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/portfolio")
@RequiredArgsConstructor
@Tag(name = "Analiza portfela", description = "Strategia Core & Satellite, rebalancing, wielowalutowosc, TWR/MWR")
@PreAuthorize("hasRole('USER')")
public class PortfolioController {

    private final PortfolioAnalysisService portfolioAnalysisService;
    private final PerformanceCalculationService performanceCalculationService;

    @GetMapping("/account/{accountId}/allocation")
    public ResponseEntity<PortfolioAllocationResponse> getAllocation(@PathVariable Long accountId) {
        return ResponseEntity.ok(portfolioAnalysisService.analyzeAllocation(accountId));
    }

    @GetMapping("/account/{accountId}/performance")
    public ResponseEntity<PerformanceResponse> getPerformance(@PathVariable Long accountId) {
        return ResponseEntity.ok(performanceCalculationService.calculatePerformance(accountId));
    }
}
