# Migracja VestTrack Pro na architekturę mikroserwisową

Ten dokument opisuje konkretny, wykonywalny krok po kroku plan podziału monolitu
na mikroserwisy. Kod aplikacji jest już przygotowany pod ten podział — pakiety
`service`/`repository`/`controller` są rozdzielone tematycznie (accounts, transactions,
portfolio, support/tickets, admin), a każdy moduł biznesowy komunikuje się z innymi
wyłącznie przez jasno zdefiniowane serwisy (nie ma "przypadkowych" zależności krzyżowych
na poziomie repozytoriów).

---

## 1. Dlaczego nie robić tego "na starcie"

Monolit modularny (obecna architektura) jest **właściwym punktem wyjścia** dla tej
skali projektu: jedna baza danych z transakcjami ACID jest kluczowa dla poprawności
silnika FIFO i limitów IKE — rozbicie tego na wcześnie za wcześnie wprowadziłoby
rozproszone transakcje bez realnej korzyści skalowania. Migrację na mikroserwisy
warto rozważyć, gdy pojawi się **konkretny driver biznesowy**: inny zespół przejmuje
dany moduł, moduł wymaga innej charakterystyki skalowania (np. `reports` generuje
duże obciążenie CPU przy PDF), albo potrzeba niezależnego release cycle.

---

## 2. Docelowe granice serwisów (bounded contexts)

```
┌─────────────────┐   ┌──────────────────┐   ┌───────────────────┐
│  identity-service │   │ accounts-service │   │ transactions-service │
│  (users, auth,     │   │ (investment_     │   │ (transactions,       │
│   refresh_tokens)  │   │  accounts, limity │   │  silnik FIFO)        │
└────────┬───────────┘   │  IKE/IKZE)        │   └─────────┬─────────┘
         │               └────────┬──────────┘             │
         │                        │                        │
         └──────────┬─────────────┴────────────┬───────────┘
                     │                          │
            ┌────────▼─────────┐      ┌─────────▼──────────┐
            │ portfolio-service │      │  market-data-service │
            │ (Core&Satellite,  │      │  (notowania, kursy   │
            │  TWR/MWR, alokacja)│      │  walut, cache, CB)   │
            └───────────────────┘      └──────────────────────┘

            ┌───────────────────┐      ┌──────────────────────┐
            │  support-service   │      │   admin-service        │
            │  (tickets, notatki)│      │  (audyt, zespol,        │
            │                    │      │   rate-limit dashboard) │
            └───────────────────┘      └──────────────────────┘

            ┌───────────────────┐
            │  reports-service   │  <- NAJLEPSZY KANDYDAT NA PIERWSZĄ MIGRACJĘ
            │  (eksport CSV/PDF) │     (bezstanowy, CPU-heavy, zero zapisu do DB)
            └───────────────────┘
```

### Mapowanie 1:1 z obecnym kodem

| Docelowy mikroserwis | Obecne pakiety/klasy (do przeniesienia bez zmian logiki) |
|---|---|
| `identity-service` | `security/*`, `service/AuthService`, `service/RefreshTokenService`, `domain/entity/User`, `domain/entity/RefreshToken`, tabele `users`, `refresh_tokens` |
| `accounts-service` | `service/AccountService`, `domain/entity/InvestmentAccount`, tabela `investment_accounts` |
| `transactions-service` | `service/TransactionService`, `service/FifoCalculationService`, `domain/entity/Transaction`, tabela `transactions` |
| `portfolio-service` | `service/PortfolioAnalysisService`, `service/PerformanceCalculationService`, `domain/entity/PortfolioAsset` |
| `market-data-service` | `service/QuoteService`, `service/CurrencyService`, `service/quotes/*`, `domain/entity/FinancialInstrument`, `service/ApiUsageTrackingService` |
| `support-service` | `service/TicketService`, `domain/entity/SupportTicket`, `domain/entity/TicketNote` |
| `admin-service` | `service/AdminService`, `domain/entity/AuditLog`, `domain/entity/ApiProviderConfig` |
| `reports-service` | `service/ReportExportService`, `controller/ReportController` |

---

## 3. Plan migracji krok po kroku

### Krok 0 — Przygotowanie (bez zmiany architektury)
1. Zamień wszystkie bezpośrednie wywołania serwisów między modułami na interfejsy
   (już częściowo zrobione — np. `TransactionService` zależy od `AccountService`
   wyłącznie przez publiczne metody, nigdy przez repozytorium konta).
2. Wprowadź `CorrelationId` (nagłówek `X-Correlation-Id`) propagowany przez filtr
   serwletowy — ułatwi to trasowanie żądań między serwisami po podziale.
3. Dodaj Micrometer + OpenTelemetry już w monolicie, żeby mieć bazowe metryki
   PRZED podziałem (punkt odniesienia do porównań po migracji).

### Krok 1 — Wydziel `reports-service` (najniższe ryzyko)
`ReportExportService` jest bezstanowy i tylko odczytuje dane (nie zapisuje nic
do bazy). To najbezpieczniejszy pierwszy krok:
1. Nowy moduł Maven/repozytorium `vesttrack-reports-service`.
2. Zamiast wstrzykiwać `TransactionRepository` bezpośrednio, serwis raportów
   wywołuje REST endpoint `GET /internal/accounts/{id}/transactions` na
   `transactions-service` (na razie wciąż w monolicie).
3. `ReportController` w monolicie zamienia się na klienta HTTP (Feign/WebClient)
   wołającego nowy serwis.
4. Wdrażasz `reports-service` niezależnie, skaluje się osobno (np. więcej replik
   pod obciążeniem generowania PDF w styczniu/kwietniu — sezon PIT-38).

### Krok 2 — Wydziel `market-data-service`
Moduł notowań ma inny profil obciążenia (dużo zapytań co 15 min, zewnętrzne
zależności Yahoo/Alpha Vantage z circuit breakerami) niż resztę systemu:
1. Przenieś `QuoteService`, `CurrencyService`, `service/quotes/*`,
   `ApiUsageTrackingService`, tabele `financial_instruments`, `api_usage_daily`,
   `api_provider_config` do własnej bazy danych `market_data_db`.
2. `portfolio-service`/`transactions-service` przestają czytać `financial_instruments`
   bezpośrednio z SQL — komunikują się przez REST (`GET /internal/instruments/{id}/price`)
   lub, lepiej, przez zdarzenia (patrz Krok 4).
3. Instrumenty finansowe (`financial_instruments`) stają się "master data" replikowane
   do innych serwisów przez event (np. `InstrumentCreatedEvent`, `InstrumentPriceUpdatedEvent`).

### Krok 3 — Wydziel `identity-service`
1. Przenieś `users`, `refresh_tokens`, cały `security/*` i `AuthService`.
2. Inne serwisy przestają mieć dostęp do tabeli `users` — walidują JWT lokalnie
   (asymetryczny JWT, RS256, `identity-service` ma klucz prywatny do podpisywania,
   pozostałe serwisy mają tylko klucz publiczny do weryfikacji — bez potrzeby
   odpytywania `identity-service` przy kazdym requeście).
3. Wprowadź API Gateway (Spring Cloud Gateway / Kong) jako jedyny punkt wejścia,
   który weryfikuje JWT i routes'uje do właściwego serwisu.

### Krok 4 — Komunikacja asynchroniczna (Kafka/RabbitMQ)
Zdarzenia domenowe zamiast synchronicznych wywołań REST tam, gdzie nie potrzeba
natychmiastowej odpowiedzi:
- `TransactionRecordedEvent` (transactions-service → portfolio-service): przelicz
  alokację Core&Satellite w tle, nie blokując requestu użytkownika.
- `InstrumentPriceUpdatedEvent` (market-data-service → wszyscy): odśwież cache lokalny.
- `TicketStatusChangedEvent` (support-service → notifications-service, jeśli powstanie):
  wyślij e-mail do użytkownika.

### Krok 5 — Rozbij bazę danych
Docelowo **każdy serwis ma własną bazę** (database-per-service):
1. Zacznij od logicznego rozdzielenia schematów w tej samej instancji PostgreSQL
   (`identity.users`, `market_data.financial_instruments`, itd.) — mniejsze ryzyko.
2. Migruj każdy schemat do osobnej instancji PostgreSQL, gdy pojawi się potrzeba
   niezależnego skalowania/backupu.
3. Zapytania, które wcześniej robiły JOIN między tabelami różnych przyszłych serwisów
   (np. `transactions` JOIN `financial_instruments`), muszą zostać zastąpione:
   - lokalną kopią potrzebnych danych (denormalizacja, np. `transactions` przechowuje
     `instrument_ticker` jako kopię w momencie transakcji — historycznie i tak poprawne,
     bo cena/ticker w dniu transakcji nie powinny się zmieniać retroaktywnie),
   - albo zapytaniem do API drugiego serwisu i złożeniem wyniku w warstwie aplikacji
     (API Composition Pattern).

### Krok 6 — Obserwowalność i odporność rozproszona
1. Rozproszone śledzenie żądań: OpenTelemetry + Jaeger/Tempo.
2. Circuit breaker (już mamy Resilience4j w `market-data-service`) — analogicznie
   zabezpiecz wszystkie wywołania REST między serwisami.
3. Distributed tracing dashboardy + alerty (Grafana) per serwis.
4. Kontrakt API: OpenAPI + Pact (consumer-driven contract testing) między serwisami,
   żeby zmiana w jednym serwisie nie psuła cichych zależności w innym.

---

## 4. Co NIE migrować (przynajmniej na start)

- `accounts-service` i `transactions-service` mają bardzo silną spójność transakcyjną
  (limit wpłat IKE musi być liczony atomowo z zapisem transakcji BUY) — rozważ
  scalenie ich w jeden serwis `investing-service`, żeby uniknąć rozproszonych transakcji
  (SAGA pattern) w najbardziej krytycznej ścieżce biznesowej, dopóki nie będzie
  to absolutnie konieczne.

---

## 5. Podsumowanie — kolejność wdrożenia (od najmniej do najbardziej ryzykownego)

1. ✅ `reports-service` (bezstanowy, brak zapisu, łatwy rollback)
2. ✅ `market-data-service` (inny profil skalowania, jasna granica danych)
3. ⚠️ `identity-service` (wymaga wdrożenia API Gateway + JWT RS256)
4. ⚠️ `support-service` / `admin-service` (niski priorytet biznesowy, ale proste)
5. 🔴 `accounts-service` + `transactions-service` (dopiero gdy będzie realna potrzeba —
   wymaga wzorca SAGA/rozproszonych transakcji dla limitów IKE)
