package com.vesttrack.service;

import com.vesttrack.dto.institution.BrokerageFirmResponse;
import com.vesttrack.repository.BrokerageFirmRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Slownik instytucji (biur maklerskich / bankow / TFI) do wyboru przy zakladaniu
 * rachunku - patrz {@link com.vesttrack.controller.InstitutionController}.
 *
 * W odroznieniu od {@link InstrumentService} (ktory dociaga nowe instrumenty na biezaco
 * z Yahoo Finance) nie istnieje wygodne publiczne API z pelna, aktualna liste polskich
 * biur maklerskich/bankow oferujacych rachunki IKE/IKZE - dlatego slownik jest
 * utrzymywany lokalnie (seed w migracji V9) i rozszerzany recznie przez admina/pracownika
 * w razie potrzeby (TODO: ewentualny wpis nowej instytucji przez AdminController).
 */
@Service
@RequiredArgsConstructor
public class InstitutionService {

    private final BrokerageFirmRepository brokerageFirmRepository;

    public List<BrokerageFirmResponse> search(String query) {
        List<com.vesttrack.domain.entity.BrokerageFirm> results = (query == null || query.isBlank())
                ? brokerageFirmRepository.findByActiveTrueOrderByNameAsc()
                : brokerageFirmRepository.findByActiveTrueAndNameContainingIgnoreCaseOrderByNameAsc(query.trim());

        return results.stream().map(BrokerageFirmResponse::from).toList();
    }
}
