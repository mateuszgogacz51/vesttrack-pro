package com.vesttrack.controller;

import com.vesttrack.dto.institution.BrokerageFirmResponse;
import com.vesttrack.service.InstitutionService;
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
 * Slownik instytucji (biur maklerskich / bankow / TFI) - pozwala uzytkownikowi
 * wybrac instytucje z listy przy zakladaniu rachunku zamiast wpisywac jej nazwe
 * recznie (patrz {@link com.vesttrack.controller.AccountController}).
 */
@RestController
@RequestMapping("/api/v1/institutions")
@RequiredArgsConstructor
@Tag(name = "Instytucje", description = "Wyszukiwanie biur maklerskich/bankow/TFI z gotowego slownika")
@PreAuthorize("hasAnyRole('USER', 'EMPLOYEE', 'ADMIN')")
public class InstitutionController {

    private final InstitutionService institutionService;

    @GetMapping("/search")
    public ResponseEntity<List<BrokerageFirmResponse>> search(@RequestParam(required = false) String query) {
        return ResponseEntity.ok(institutionService.search(query));
    }
}
