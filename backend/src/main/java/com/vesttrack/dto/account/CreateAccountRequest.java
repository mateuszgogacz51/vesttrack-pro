package com.vesttrack.dto.account;

import com.vesttrack.domain.enums.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * @param name             Opcjonalna wlasna nazwa/pseudonim konta (np. "IKE na emeryture").
 *                         Jesli puste, zostanie wygenerowana na podstawie wybranej instytucji
 *                         (patrz brokerageFirmId).
 * @param brokerageFirmId  Opcjonalne ID instytucji ze slownika (GET /api/v1/institutions/search).
 *                         Dzieki temu uzytkownik wybiera biuro maklerskie/bank/TFI z listy
 *                         zamiast wpisywac jego nazwe recznie za kazdym razem. Jedno z pol
 *                         name / brokerageFirmId musi byc podane - walidowane w AccountService.
 */
public record CreateAccountRequest(
        String name,
        Long brokerageFirmId,
        @NotNull AccountType accountType,
        @NotBlank String currency,
        java.math.BigDecimal annualContributionLimit
) {}
