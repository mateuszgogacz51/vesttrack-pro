package com.vesttrack.controller;

import com.vesttrack.domain.entity.FinancialInstrument;
import com.vesttrack.service.InstrumentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Wyszukiwanie instrumentow dostepne dla zwyklego uzytkownika (USER) - potrzebne
 * przy dodawaniu transakcji (wybor tickera). Pelne zarzadzanie slownikiem
 * (tworzenie/blokowanie instrumentow) pozostaje wylacznie w EmployeeController/AdminController.
 */
@RestController
@RequestMapping("/api/v1/instruments")
@RequiredArgsConstructor
@Tag(name = "Instrumenty", description = "Wyszukiwanie instrumentow finansowych")
@PreAuthorize("hasRole('USER')")
public class InstrumentController {

    private final InstrumentService instrumentService;

    @GetMapping("/search")
    public ResponseEntity<List<FinancialInstrument>> search(@RequestParam String query) {
        return ResponseEntity.ok(instrumentService.search(query));
    }
}
