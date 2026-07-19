package com.vesttrack.service;

import com.vesttrack.domain.entity.BrokerageFirm;
import com.vesttrack.domain.entity.InvestmentAccount;
import com.vesttrack.domain.entity.User;
import com.vesttrack.domain.enums.AccountType;
import com.vesttrack.dto.account.AccountResponse;
import com.vesttrack.dto.account.CreateAccountRequest;
import com.vesttrack.exception.BusinessRuleException;
import com.vesttrack.exception.ResourceNotFoundException;
import com.vesttrack.repository.BrokerageFirmRepository;
import com.vesttrack.repository.InvestmentAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Year;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final InvestmentAccountRepository accountRepository;
    private final BrokerageFirmRepository brokerageFirmRepository;
    private final CurrentUserService currentUserService;

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        User user = currentUserService.getCurrentUser();

        // Jesli podano brokerageFirmId, uzytkownik wybral instytucje z gotowego slownika -
        // nie musi juz recznie wpisywac jej nazwy. "name" pozostaje wtedy opcjonalnym
        // wlasnym pseudonimem konta (np. "IKE na emeryture"); jesli go nie poda, uzywamy
        // wprost nazwy instytucji.
        BrokerageFirm firm = null;
        if (request.brokerageFirmId() != null) {
            firm = brokerageFirmRepository.findById(request.brokerageFirmId())
                    .filter(BrokerageFirm::isActive)
                    .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono wybranej instytucji"));
        }

        String resolvedName = (request.name() != null && !request.name().isBlank())
                ? request.name().trim()
                : (firm != null ? firm.getName() : null);

        if (resolvedName == null) {
            throw new BusinessRuleException("Podaj nazwe konta lub wybierz instytucje z listy");
        }

        InvestmentAccount account = InvestmentAccount.builder()
                .user(user)
                .name(resolvedName)
                .brokerageFirm(firm)
                .accountType(request.accountType())
                .currency(request.currency())
                .annualContributionLimit(request.annualContributionLimit())
                .contributedThisYear(BigDecimal.ZERO)
                .contributionYear(Year.now().getValue())
                .active(true)
                .build();

        return AccountResponse.from(accountRepository.save(account));
    }

    public List<AccountResponse> getMyAccounts() {
        User user = currentUserService.getCurrentUser();
        return accountRepository.findByUserId(user.getId()).stream()
                .map(AccountResponse::from)
                .toList();
    }

    public InvestmentAccount getOwnedAccountOrThrow(Long accountId) {
        User user = currentUserService.getCurrentUser();
        return accountRepository.findByIdAndUserId(accountId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Nie znaleziono konta lub brak dostepu"));
    }

    /**
     * Rejestruje wplate na konto IKE/IKZE i weryfikuje, czy nie przekracza
     * ona rocznego ustawowego limitu wplat - zgodnie z regulami podatkowymi
     * dla kont emerytalnych.
     */
    @Transactional
    public void registerContribution(InvestmentAccount account, BigDecimal amountInAccountCurrency) {
        if (account.getAccountType() == AccountType.REGULAR) {
            return; // limit dotyczy wylacznie kont IKE/IKZE
        }
        int currentYear = Year.now().getValue();
        if (account.getContributionYear() == null || account.getContributionYear() != currentYear) {
            account.setContributionYear(currentYear);
            account.setContributedThisYear(BigDecimal.ZERO);
        }

        BigDecimal newTotal = account.getContributedThisYear().add(amountInAccountCurrency);
        if (account.getAnnualContributionLimit() != null
                && newTotal.compareTo(account.getAnnualContributionLimit()) > 0) {
            throw new BusinessRuleException(String.format(
                    "Wplata przekracza roczny limit wplat na konto %s (limit: %s, juz wplacono: %s)",
                    account.getAccountType(), account.getAnnualContributionLimit(), account.getContributedThisYear()));
        }

        account.setContributedThisYear(newTotal);
        accountRepository.save(account);
    }
}
