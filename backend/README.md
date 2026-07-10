# VestTrack Pro

Zaawansowana platforma zarządzania i analizy portfela inwestycyjnego multi-asset (Fintech).
Backend: **Java 21 + Spring Boot 3.4 + PostgreSQL 16 + Flyway + JWT (access+refresh) + Caffeine +
WebClient + Resilience4j + OpenPDF**.

---

## 1. Co jest w projekcie

| Moduł | Opis |
|---|---|
| Rejestracja / logowanie | JWT access token + rotujący refresh token |
| Rachunki inwestycyjne | REGULAR / IKE / IKZE, limity wpłat rocznych |
| Transakcje | BUY / SELL / DIVIDEND, silnik **FIFO** do rozliczania zysków |
| Analiza portfela | Strategia **Core & Satellite**, rebalancing, **TWR/MWR** |
| Notowania | **Yahoo Finance** (podstawowy) + **Alpha Vantage** (zapasowy) z **Resilience4j** (Circuit Breaker + Retry) |
| Kursy walut | NBP API z cache Caffeine i fallbackiem 1:1 |
| Raporty | Eksport **CSV** i **PDF** (roczne zestawienie, pomoc do PIT-38) |
| Wsparcie | System zgłoszeń (tickets) z notatkami wewnętrznymi/publicznymi |
| Panel Admina | Zarządzanie zespołem, audyt, **Rate-Limiting Dashboard** dla API |
| RBAC | USER / EMPLOYEE / ADMIN, wymuszane na poziomie URL i `@PreAuthorize` |

Dokument `MIGRATION_TO_MICROSERVICES.md` opisuje krok po kroku, jak i kiedy podzielić
ten modularny monolit na mikroserwisy (bez przepisywania logiki biznesowej).

---

## 2. Wymagania

- **Java 21** (JDK)
- **Maven 3.9+**
- **Docker + Docker Compose** (baza danych, opcjonalnie cała aplikacja, oraz **wymagany do testów** — Testcontainers odpala PostgreSQL w kontenerze)
- (Opcjonalnie) darmowy klucz API **Alpha Vantage** — https://www.alphavantage.co/support/#api-key — działa też bez klucza na planie "demo", ale z bardzo niskim limitem

---

## 3. Instrukcja krok po kroku — najprostszy start (Docker Compose)

```bash
# 1. Wejdź do katalogu projektu
cd vesttrack-pro

# 2. (opcjonalnie) podmień domyślny sekret JWT i klucz Alpha Vantage w docker-compose.yml
#    - JWT_SECRET: dowolny losowy string min. 32 znaki
#    - ALPHAVANTAGE_API_KEY: Twój klucz (albo zostaw "demo")

# 3. Zbuduj i odpal caly stack (PostgreSQL + aplikacja)
docker compose up --build

# 4. Poczekaj aż zobaczysz w logach: "Started VestTrackProApplication"
#    Flyway automatycznie nałoży migracje V1..V7 przy pierwszym starcie.
```

Aplikacja dostępna na **http://localhost:8080**
Swagger UI: **http://localhost:8080/swagger-ui.html**

Zatrzymanie: `docker compose down` (dane w bazie zostają zachowane).
Pełne wyczyszczenie łącznie z danymi: `docker compose down -v`.

---

## 4. Instrukcja krok po kroku — praca lokalna (IDE + baza w Dockerze)

```bash
# 1. Odpal tylko baze danych
docker compose up postgres -d

# 2. Ustaw zmienne środowiskowe (Linux/macOS)
export DB_HOST=localhost
export DB_PORT=5432
export DB_NAME=vesttrack
export DB_USER=vesttrack
export DB_PASSWORD=vesttrack_dev_password
export JWT_SECRET="wpisz-tutaj-losowy-sekret-min-32-znaki-1234567890"
export ALPHAVANTAGE_API_KEY="demo"     # albo Twoj wlasny klucz

# Windows PowerShell - odpowiedniki:
# $env:DB_HOST="localhost"; $env:DB_PORT="5432"; ...

# 3. Zbuduj projekt
mvn clean package -DskipTests

# 4. Odpal aplikacje
java -jar target/vesttrack-pro-1.0.0.jar

# Alternatywnie w trakcie developmentu (hot reload z devtools nieskonfigurowany domyslnie):
mvn spring-boot:run
```

---

## 5. Pierwsze kroki po starcie aplikacji

### 5.1. Utworzenie pierwszego konta administratora

Rejestracja publiczna (`POST /api/v1/auth/register`) **zawsze** tworzy rolę `USER`
(świadome ograniczenie bezpieczeństwa — nikt z zewnątrz nie nadaje sobie sam uprawnień).
Pierwsze konto ADMIN nadajesz ręcznie w bazie:

```bash
# Zaloguj sie do kontenera bazy danych
docker exec -it vesttrack-postgres psql -U vesttrack -d vesttrack

# W konsoli psql:
UPDATE users SET role = 'ADMIN' WHERE email = 'twoj-email@example.com';
\q
```

Kolejnych pracowników/adminów twórz już przez panel: `POST /api/v1/admin/employees`
(wymaga zalogowania jako ADMIN, zwraca token przy logowaniu z rolą ADMIN w JWT).

### 5.2. Przykładowy przepływ (curl)

```bash
BASE=http://localhost:8080/api/v1

# Rejestracja
curl -s -X POST $BASE/auth/register -H "Content-Type: application/json" \
  -d '{"email":"jan@example.com","password":"HasloBezpieczne123","firstName":"Jan","lastName":"Kowalski"}'

# Logowanie -> otrzymujesz accessToken + refreshToken
curl -s -X POST $BASE/auth/login -H "Content-Type: application/json" \
  -d '{"email":"jan@example.com","password":"HasloBezpieczne123"}'

# Podstaw accessToken z odpowiedzi logowania:
TOKEN="wklej_accessToken_tutaj"
REFRESH="wklej_refreshToken_tutaj"

# Odswiezenie access tokenu (rotacja - stary refresh token zostaje uniewazniony)
curl -s -X POST $BASE/auth/refresh -H "Content-Type: application/json" \
  -d "{\"refreshToken\":\"$REFRESH\"}"

# Utworzenie konta IKE
curl -s -X POST $BASE/accounts -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"name":"Moje IKE","accountType":"IKE","currency":"PLN","annualContributionLimit":26019.00}'

# Wylogowanie (uniewaznia wszystkie refresh tokeny uzytkownika)
curl -s -X POST $BASE/auth/logout -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" -d "{\"refreshToken\":\"$REFRESH\"}"
```

### 5.3. Eksport raportów

```bash
# CSV - wszystkie transakcje konta
curl -s -H "Authorization: Bearer $TOKEN" \
  "$BASE/reports/account/1/transactions.csv" -o transakcje.csv

# CSV filtrowane po roku
curl -s -H "Authorization: Bearer $TOKEN" \
  "$BASE/reports/account/1/transactions.csv?year=2025" -o transakcje_2025.csv

# PDF - roczne podsumowanie z szacowanym podatkiem Belki (pomoc do PIT-38)
curl -s -H "Authorization: Bearer $TOKEN" \
  "$BASE/reports/account/1/annual-summary.pdf?year=2025" -o zestawienie_2025.pdf
```

### 5.4. Wskaźniki TWR / MWR

```bash
curl -s -H "Authorization: Bearer $TOKEN" \
  "$BASE/portfolio/account/1/performance"
```

### 5.5. Panel administratora — Rate-Limiting Dashboard

```bash
ADMIN_TOKEN="token_zalogowanego_admina"

# Zuzycie limitow API dzisiaj (Yahoo Finance, Alpha Vantage, NBP)
curl -s -H "Authorization: Bearer $ADMIN_TOKEN" "$BASE/admin/api-usage"

# Aktualna konfiguracja dostawcow (bez ujawniania samego klucza API)
curl -s -H "Authorization: Bearer $ADMIN_TOKEN" "$BASE/admin/api-usage/providers"

# Zmiana klucza API "na zywo", bez restartu aplikacji
curl -s -X PATCH -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
  "$BASE/admin/api-usage/providers/ALPHA_VANTAGE" \
  -d '{"apiKey":"TWOJ_NOWY_KLUCZ","dailyLimit":500,"active":true}'
```

---

## 6. Dokumentacja API (Swagger / OpenAPI)

- Swagger UI: **http://localhost:8080/swagger-ui.html**
- JSON: **http://localhost:8080/v3/api-docs**

W Swaggerze kliknij "Authorize" i wklej **accessToken** (bez słowa `Bearer` — Swagger doda je sam).

---

## 7. Testy (Testcontainers)

```bash
# Wymaga dzialajacego lokalnie Dockera - kontener PostgreSQL jest tworzony
# i usuwany automatycznie, nic nie trzeba konfigurowac recznie.
mvn test
```

Kluczowy test: `FifoCalculationServiceIntegrationTest` — weryfikuje rozliczenie zysku
z transzowych zakupów i sprzedaży częściowej metodą FIFO na prawdziwej bazie PostgreSQL,
oraz poprawne odrzucenie próby sprzedaży większej liczby jednostek niż posiadane.

---

## 8. Architektura i kluczowa logika biznesowa

### 8.1. Warstwy
```
Controller  →  Service  →  Repository  →  PostgreSQL
```
- **controller** – REST, tylko mapowanie DTO ↔ serwis, `@PreAuthorize`
- **service** – logika biznesowa (FIFO, limity IKE, TWR/MWR, circuit breaker, audyt)
- **repository** – Spring Data JPA + zapytania natywne (agregacje SQL)
- **domain.entity** – encje JPA 1:1 z migracjami Flyway
- **security** – JWT (access + refresh), `UserDetailsService`, filtr autoryzacyjny
- **config** – Security, Cache (Caffeine), WebClient, Resilience4j, OpenAPI

### 8.2. RBAC
| Rola | Dostęp |
|---|---|
| `USER` | `/api/v1/accounts/**`, `/api/v1/transactions/**`, `/api/v1/portfolio/**`, `/api/v1/tickets/**`, `/api/v1/reports/**` |
| `EMPLOYEE` | `/api/v1/employee/**` |
| `ADMIN` | `/api/v1/admin/**` + wszystko co EMPLOYEE |

### 8.3. Model danych (PostgreSQL, migracje V1–V7)
```
users ──1:N── investment_accounts ──1:N── portfolio_assets ──N:1── financial_instruments
   │                    │                                              │
   │                    └──────────────1:N── transactions ─────────────┘
   │
   ├──1:N── refresh_tokens
   ├──1:N── support_tickets ──1:N── ticket_notes
   └──1:N── audit_log

financial_instruments ──1:N── api_usage_daily (per dostawca/dzien)
api_provider_config (klucze API, limity, mozliwe do zmiany "na zywo")
```
Wyłącznie `NUMERIC`/`DECIMAL` dla wartości pieniężnych. Schemat wersjonowany Flyway
(`ddl-auto: validate` — appka nigdy sama nie modyfikuje schematu na produkcji).

### 8.4. Silnik FIFO (`FifoCalculationService`)
Każda transakcja BUY tworzy "lot" w kolejce `Deque`. SELL zdejmuje jednostki od
najstarszej transzy: zysk = przychód ze sprzedaży − koszt nabycia najstarszych jednostek.

### 8.5. Notowania — Circuit Breaker (`QuoteService` + `service/quotes/*`)
```
getCurrentPrice(instrumentId)
   │
   ├─ 1. Yahoo Finance (CircuitBreaker "yahooQuotes" + Retry z backoff)
   │      └─ sukces? → zapisz jako last_price, zwroc
   │
   ├─ 2. porazka/obwod OPEN → Alpha Vantage (CircuitBreaker "alphaVantageQuotes")
   │      └─ sukces? → zapisz jako last_price, zwroc
   │
   └─ 3. oba zawiodly → zwroc ostatnia znana cene z bazy (financial_instruments.last_price)
          (system NIGDY nie wywraca sie z powodu awarii zewnetrznego dostawcy)
```
Wynik cache'owany w Caffeine 15 minut. Każde wywołanie liczone do dziennego limitu
(`api_usage_daily`) widocznego w panelu admina.

### 8.6. Refresh tokeny (`RefreshTokenService`)
Wzorzec **rotating refresh token**: refresh token przechowywany w bazie tylko jako
hash SHA-256, każde użycie unieważnia go i wydaje nowy. Ponowne użycie już zużytego
tokenu unieważnia WSZYSTKIE tokeny użytkownika (sygnał potencjalnej kradzieży).

### 8.7. TWR / MWR (`PerformanceCalculationService`)
- **TWR** – metoda **Modified Dietz** (uznawana metoda GIPS/CFA Institute jako
  przybliżenie Time-Weighted Return bez potrzeby codziennych wycen historycznych).
- **MWR** – rzeczywisty **IRR/XIRR** liczony numerycznie metodą Brenta (`commons-math3`)
  na podstawie faktycznych dat przepływów kapitału.

### 8.8. Raporty (`ReportExportService`)
CSV (UTF-8 z BOM dla Excela) oraz PDF (biblioteka **OpenPDF**, licencja LGPL/MPL)
z szacowanym podatkiem od dochodów kapitałowych (19%, "podatek Belki") — materiał
pomocniczy do samodzielnego rozliczenia PIT-38 (nie zastępuje księgowego).

### 8.9. Rate-Limiting Dashboard (`ApiUsageTrackingService` + `AdminController`)
Każde wywołanie zewnętrznego API zwiększa licznik w `api_usage_daily`. Admin widzi
zużycie na dziś per dostawca i może zmienić klucz API / dzienny limit / status
aktywności "na żywo" (tabela `api_provider_config`), bez restartu aplikacji.

---

## 9. Struktura katalogów

```
vesttrack-pro/
├── pom.xml
├── docker-compose.yml
├── Dockerfile
├── README.md
├── MIGRATION_TO_MICROSERVICES.md
└── src/
    ├── main/
    │   ├── java/com/vesttrack/
    │   │   ├── VestTrackProApplication.java
    │   │   ├── config/                 # Security, Cache, WebClient, OpenAPI
    │   │   ├── security/                # JWT, UserDetails, filtr
    │   │   ├── domain/
    │   │   │   ├── entity/              # encje JPA
    │   │   │   └── enums/
    │   │   ├── repository/              # Spring Data JPA
    │   │   ├── dto/                     # rekordy request/response per modul
    │   │   ├── service/
    │   │   │   └── quotes/              # klienci Yahoo Finance / Alpha Vantage
    │   │   ├── controller/              # REST endpoints
    │   │   └── exception/               # globalny handler bledow
    │   └── resources/
    │       ├── application.yml
    │       └── db/migration/            # V1..V7 Flyway SQL
    └── test/
        ├── java/com/vesttrack/repository/   # testy z Testcontainers
        └── resources/application-test.yml
```

---

## 10. Zmienne środowiskowe (podsumowanie)

| Zmienna | Domyślna wartość | Opis |
|---|---|---|
| `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USER` / `DB_PASSWORD` | `postgres`/`5432`/`vesttrack`/`vesttrack`/`vesttrack_dev_password` | Połączenie z PostgreSQL |
| `JWT_SECRET` | *(dev only, zmień na produkcji!)* | Sekret do podpisywania access tokenów |
| `NBP_API_BASE_URL` | `https://api.nbp.pl/api` | API kursów walut NBP |
| `YAHOO_API_BASE_URL` | `https://query1.finance.yahoo.com` | Podstawowy dostawca notowań |
| `ALPHAVANTAGE_API_BASE_URL` | `https://www.alphavantage.co` | Zapasowy dostawca notowań |
| `ALPHAVANTAGE_API_KEY` | `demo` | Klucz API (zarejestruj własny dla wyższych limitów) |

---

## 11. Dalsza roadmapa

Zobacz `MIGRATION_TO_MICROSERVICES.md` po szczegółowy plan podziału na mikroserwisy.
Inne pomysły na rozwój: powiadomienia e-mail (support-service), WebSocket dla notowań
live, aplikacja mobilna/frontend SPA (React/Angular), wieloetapowa autoryzacja (2FA).
