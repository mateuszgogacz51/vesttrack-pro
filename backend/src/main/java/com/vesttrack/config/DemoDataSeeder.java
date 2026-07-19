package com.vesttrack.config;

import com.vesttrack.domain.entity.BrokerageFirm;
import com.vesttrack.domain.entity.InvestmentAccount;
import com.vesttrack.domain.entity.User;
import com.vesttrack.domain.enums.AccountType;
import com.vesttrack.domain.enums.Role;
import com.vesttrack.repository.BrokerageFirmRepository;
import com.vesttrack.repository.InvestmentAccountRepository;
import com.vesttrack.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Year;
import java.util.Optional;

/**
 * Zaklada konta testowe (po jednym na kazda role) przy pierwszym starcie aplikacji,
 * zeby moc od razu recznie przetestowac appke bez rejestrowania sie recznie.
 * Dziala idempotentnie - jesli konto o danym e-mailu juz istnieje, nic nie robi.
 *
 * Konta testowe (haslo dla wszystkich: Test1234!):
 *  - user.demo@vesttrack.pro      (rola USER)
 *  - pracownik.demo@vesttrack.pro (rola EMPLOYEE)
 *  - admin.demo@vesttrack.pro     (rola ADMIN)
 *
 * UWAGA: to jest dane demonstracyjne/testowe do uzytku lokalnego/QA. Przed wdrozeniem
 * na produkcje nalezy usunac ten komponent albo ograniczyc go profilem Springa (np.
 * @Profile("dev")), zeby konta z powszechnie znanym haslem nie trafily na produkcje.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(100)
public class DemoDataSeeder implements ApplicationRunner {

    private static final String DEMO_PASSWORD = "Test1234!";

    private final UserRepository userRepository;
    private final InvestmentAccountRepository accountRepository;
    private final BrokerageFirmRepository brokerageFirmRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        User user = seedUser("user.demo@vesttrack.pro", "Jan", "Kowalski", Role.USER);
        seedUser("pracownik.demo@vesttrack.pro", "Anna", "Nowak", Role.EMPLOYEE);
        seedUser("admin.demo@vesttrack.pro", "Piotr", "Zielinski", Role.ADMIN);

        if (user != null) {
            seedDemoAccounts(user);
        }
    }

    /**
     * Zwraca nowo utworzonego uzytkownika, albo null jesli juz istnial wczesniej
     * (zeby nie duplikowac tez powiazanych z nim danych demo przy kolejnych startach).
     */
    private User seedUser(String email, String firstName, String lastName, Role role) {
        if (userRepository.existsByEmail(email)) {
            return null;
        }
        User saved = userRepository.save(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(DEMO_PASSWORD))
                .firstName(firstName)
                .lastName(lastName)
                .role(role)
                .enabled(true)
                .baseCurrency("PLN")
                .build());
        log.info("Utworzono konto testowe {} (rola {})", email, role);
        return saved;
    }

    private void seedDemoAccounts(User user) {
        Optional<BrokerageFirm> xtb = brokerageFirmRepository.findByActiveTrueAndNameContainingIgnoreCaseOrderByNameAsc("XTB")
                .stream().findFirst();
        Optional<BrokerageFirm> mbank = brokerageFirmRepository.findByActiveTrueAndNameContainingIgnoreCaseOrderByNameAsc("mBank")
                .stream().findFirst();

        accountRepository.save(InvestmentAccount.builder()
                .user(user)
                .name(xtb.map(BrokerageFirm::getName).orElse("Konto maklerskie testowe"))
                .brokerageFirm(xtb.orElse(null))
                .accountType(AccountType.REGULAR)
                .currency("PLN")
                .contributedThisYear(BigDecimal.ZERO)
                .contributionYear(Year.now().getValue())
                .active(true)
                .build());

        accountRepository.save(InvestmentAccount.builder()
                .user(user)
                .name(mbank.map(f -> "IKE " + f.getName()).orElse("IKE testowe"))
                .brokerageFirm(mbank.orElse(null))
                .accountType(AccountType.IKE)
                .currency("PLN")
                .annualContributionLimit(new BigDecimal("26019.00"))
                .contributedThisYear(BigDecimal.ZERO)
                .contributionYear(Year.now().getValue())
                .active(true)
                .build());

        log.info("Dodano przykladowe konta inwestycyjne dla {}", user.getEmail());
    }
}
