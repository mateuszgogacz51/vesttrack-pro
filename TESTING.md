# VestTrack Pro — scenariusze testowe

Konta testowe są tworzone automatycznie przy starcie backendu (`DemoDataSeeder`),
nie trzeba nic rejestrować ręcznie. Hasło dla wszystkich: **Test1234!**

| Rola     | E-mail                          |
|----------|----------------------------------|
| USER     | user.demo@vesttrack.pro         |
| EMPLOYEE | pracownik.demo@vesttrack.pro    |
| ADMIN    | admin.demo@vesttrack.pro        |

Konto `user.demo@vesttrack.pro` ma od razu założone 2 przykładowe rachunki
(REGULAR w XTB i IKE w mBanku), żeby dashboard nie był pusty przy pierwszym logowaniu.

> Uwaga: to konta testowe ze znanym hasłem — przed wystawieniem appki na produkcję
> usuń `DemoDataSeeder` albo ogranicz go do profilu deweloperskiego.

---

## Scenariusz 1 — logowanie na konto testowe

1. Otwórz stronę logowania.
2. Zaloguj się jako `user.demo@vesttrack.pro` / `Test1234!`.
3. **Sprawdź:** trafiasz na Pulpit i widzisz 2 gotowe konta (XTB, IKE mBank) bez konieczności ich tworzenia.

---

## Scenariusz 2 — nowe konto z wyborem instytucji z listy (główna nowa funkcja)

1. Na Pulpicie kliknij **„+ Nowe konto”**.
2. Kliknij pole „Instytucja” — powinna od razu rozwinąć się lista instytucji (bez wpisywania czegokolwiek).
3. Wpisz w wyszukiwarce np. „santander” i poczekaj chwilę.
4. **Sprawdź:** lista zawęża się do pozycji zawierających „Santander”, bez potrzeby ręcznego wpisywania pełnej nazwy firmy.
5. Wybierz instytucję z listy.
6. Zostaw pole „Własna nazwa konta” puste, wybierz typ konta i walutę, kliknij **„Utwórz konto”**.
7. **Sprawdź:** nowe konto pojawia się na Pulpicie z nazwą instytucji jako nazwą konta — nie trzeba było jej wpisywać ręcznie.

---

## Scenariusz 3 — nowe konto z własnym pseudonimem + wybraną instytucją

1. Powtórz kroki 1–5 ze scenariusza 2, wybierając inną instytucję (np. XTB).
2. Tym razem wypełnij pole **„Własna nazwa konta”**, np. „XTB — spekulacyjne”.
3. Utwórz konto.
4. **Sprawdź:** na karcie konta widać Twój własny pseudonim jako główny tytuł, a pod spodem nazwę instytucji (XTB) i walutę.

---

## Scenariusz 4 — instytucja spoza listy (custom fallback)

1. Otwórz **„+ Nowe konto”**, kliknij pole „Instytucja”.
2. Kliknij **„Nie widzę swojej instytucji — wpiszę nazwę ręcznie”**.
3. Wpisz dowolną nazwę, np. „Bank Spółdzielczy Testowy”.
4. Utwórz konto.
5. **Sprawdź:** konto zostaje utworzone z podaną ręcznie nazwą — stara ścieżka (wpisywanie z ręki) nadal działa dla przypadków spoza słownika.

---

## Scenariusz 5 — walidacja: brak nazwy i brak instytucji

1. Otwórz „+ Nowe konto”, nie wybieraj żadnej instytucji ani nie przełączaj się na tryb ręczny.
2. Spróbuj kliknąć „Utwórz konto” bez wyboru.
3. **Sprawdź:** formularz pokazuje komunikat o konieczności wyboru instytucji lub wpisania nazwy — konto nie zostaje utworzone „puste”.

---

## Scenariusz 6 — limit wpłat na IKE/IKZE

1. Utwórz konto typu IKE z instytucji z listy, ustaw „Roczny limit wpłat” np. na 1000.
2. Wejdź w szczegóły konta, dodaj transakcję/wpłatę przekraczającą limit (jeśli formularz na to pozwala) albo sprawdź pasek wykorzystania limitu na Pulpicie.
3. **Sprawdź:** pasek wykorzystania limitu na karcie konta pokazuje poprawny procent, a próba przekroczenia limitu jest blokowana z czytelnym komunikatem.

---

## Scenariusz 7 — dodanie transakcji (instrument picker, dla porównania z nowym Institution picker)

1. Wejdź w szczegóły dowolnego konta, kliknij „+ Nowa transakcja”.
2. Wyszukaj instrument po tickerze (np. „AAPL”).
3. **Sprawdź:** instrumenty spoza lokalnej bazy są automatycznie dociągane (Yahoo Finance) — to już działający mechanizm, na którym wzorowany jest nowy wybór instytucji.

---

## Scenariusz 8 — zgłoszenie (ticket) jako USER, obsługa jako EMPLOYEE

1. Zalogowany jako `user.demo@vesttrack.pro`, utwórz nowe zgłoszenie w sekcji zgłoszeń.
2. Wyloguj się, zaloguj jako `pracownik.demo@vesttrack.pro`.
3. Znajdź zgłoszenie na liście, zmień jego status / dodaj notatkę.
4. **Sprawdź:** zgłoszenie zmienia status, a notatka wewnętrzna nie jest widoczna dla roli USER.

---

## Scenariusz 9 — panel admina

1. Zaloguj się jako `admin.demo@vesttrack.pro`.
2. Wejdź w panel zespołu / dziennik audytu.
3. **Sprawdź:** widoczne są wpisy audytowe z poprzednich scenariuszy (logowania, utworzone konta) oraz statystyki pracownika obsługującego zgłoszenie ze Scenariusza 8.

---

## Scenariusz 10 — API instytucji bezpośrednio (dla osoby technicznej / Swagger)

1. Otwórz `/swagger-ui.html`, zaloguj się (Bearer token z `/api/v1/auth/login`).
2. Wywołaj `GET /api/v1/institutions/search` bez parametru `query`.
3. **Sprawdź:** zwracana jest pełna lista ok. 20 instytucji z migracji seedującej.
4. Wywołaj `GET /api/v1/institutions/search?query=xyz123` (nieistniejąca fraza).
5. **Sprawdź:** zwracana jest pusta lista `[]`, a nie błąd 500.

---

## Scenariusz 11 — reset hasła

> Uwaga: e-mail resetujący naprawdę wychodzi tylko jeśli w `application.yml`/zmiennych
> środowiskowych skonfigurowany jest realny SMTP (np. Mailtrap.io do testów — patrz
> `backend/README.md`, sekcja o konfiguracji poczty). Jeśli poczta nie jest skonfigurowana,
> wysyłka po prostu nie powiedzie się po cichu (błąd tylko w logu) — API i tak odpowiada
> sukcesem, żeby nie ujawniać, czy dany e-mail istnieje w bazie.

1. Na stronie logowania kliknij „Nie pamiętasz hasła?” i podaj e-mail `user.demo@vesttrack.pro`.
2. **Sprawdź:** niezależnie od tego, czy e-mail istnieje, formularz zawsze pokazuje ten sam komunikat sukcesu (ochrona przed enumeracją kont) — spróbuj też z nieistniejącym adresem i porównaj odpowiedź.
3. Odbierz e-mail (w skrzynce Mailtrap, jeśli tak skonfigurowane) i skopiuj link resetujący (`/reset-password?token=...`).
4. Otwórz link, ustaw nowe hasło.
5. **Sprawdź:** logowanie starym hasłem już nie działa, nowym — działa.
6. Zaloguj się ponownie na innym urządzeniu/przeglądarce (jeśli wcześniej było tam aktywne zalogowanie) — token dostępowy sprzed resetu.
7. **Sprawdź:** stare sesje/refresh tokeny są unieważnione (użytkownik musi zalogować się od nowa wszędzie) — to celowe zachowanie po zmianie hasła.
8. Spróbuj użyć tego samego linku resetującego drugi raz.
9. **Sprawdź:** system odrzuca go komunikatem, że link został już wykorzystany.
10. Poczekaj (albo sprawdź w bazie/README ile trwa ważność — domyślnie 30 minut) i spróbuj użyć przeterminowanego linku.
11. **Sprawdź:** system odrzuca go komunikatem o wygaśnięciu, z sugestią, by poprosić o nowy.

---

## Scenariusz 12 — panel admina: Rate-Limiting Dashboard (zużycie zewnętrznych API)

1. Zaloguj się jako `admin.demo@vesttrack.pro`, wejdź w sekcję zużycia API (Rate-Limiting Dashboard).
2. **Sprawdź:** widoczne są 3 wpisy dostawców (`YAHOO_FINANCE`, `ALPHA_VANTAGE`, `NBP`) z dzisiejszym licznikiem wywołań/błędów.
3. Zaloguj się jako `user.demo@vesttrack.pro`, wyszukaj kilka nowych instrumentów (Scenariusz 7) tak, aby część zapytań trafiła do Yahoo Finance.
4. Wróć na konto admina i odśwież dashboard.
5. **Sprawdź:** licznik wywołań dla `YAHOO_FINANCE` wzrósł o liczbę wykonanych wyszukiwań.
6. W panelu konfiguracji dostawców (`/api/v1/admin/api-usage/providers` lub odpowiadający mu widok) obniż `dailyLimit` dla `YAHOO_FINANCE` do wartości niższej niż dzisiejsze zużycie (np. 1).
7. Zapisz zmianę.
8. Jako `user.demo@vesttrack.pro` spróbuj wyszukać kolejny nowy instrument (którego nie ma jeszcze lokalnie w bazie).
9. **Sprawdź:** wyszukiwanie zewnętrzne jest zablokowane po przekroczeniu limitu (`isWithinDailyLimit` zwraca `false`) — aplikacja nie powinna się wysypać błędem 500, tylko obsłużyć to łagodnie (np. brakiem nowych wyników).
10. Wróć jako admin, ustaw `active: false` dla dostawcy `YAHOO_FINANCE`.
11. **Sprawdź:** dashboard pokazuje dostawcę jako nieaktywnego, a próby wyszukiwania nowych instrumentów nie próbują już go odpytywać.
12. Przywróć oryginalny limit i `active: true` po teście, żeby nie zostawić środowiska w stanie zablokowanym.
