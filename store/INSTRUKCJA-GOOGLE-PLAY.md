# Spięcie! w Google Play – instrukcja krok po kroku

Najwygodniej robić to na komputerze. Nazwy przycisków w Play Console czasem się zmieniają – w nawiasach podane są angielskie odpowiedniki.

## 0. Paczka do wgrania
1. github.com/konradlech17-sys/Monter → **Actions** → najnowszy przebieg **Android CI** z zielonym ✓.
2. Na dole, w **Artifacts**, pobierz **monter-play-aab** i rozpakuj ZIP → plik `app-release.aab`.

Nowy build ręcznie: **Actions → Android CI → Run workflow** → gałąź `claude/android-electrical-panel-game-k8fxuc` → **Run workflow**.

## 1. Polityka prywatności (GitHub Pages, jednorazowo)
1. GitHub → repozytorium → **Settings → Pages**.
2. **Source:** Deploy from a branch → gałąź `claude/android-electrical-panel-game-k8fxuc`, folder **/docs** → **Save**.
3. Po 1–2 min działa: https://konradlech17-sys.github.io/Monter/polityka-prywatnosci.html

## 2. Utworzenie aplikacji (jednorazowo)
play.google.com/console → **Utwórz aplikację** (*Create app*):
- Nazwa: `Spięcie! Gra dla elektryków`
- Język domyślny: polski · **Gra** · **Bezpłatna** → zaznacz oświadczenia → **Utwórz aplikację**

## 3. Karta sklepu
**Rozwój → Obecność w sklepie → Główna strona aplikacji** (*Grow users → Store presence → Main store listing*). Teksty są w `store/opis-sklepu.md`.
- Nazwa, krótki opis, pełny opis – skopiuj
- Ikona: `store/icon-512.png`
- Grafika promocyjna: `store/feature-1024x500.png`
- Zrzuty ekranu telefonu: min. 2 (np. plansza z rozdzielnicą, tryb TEST z iskrami, mapa poziomów, ekran SEP)
- **Zapisz**

**Ustawienia sklepu** (*Store settings*): kategoria **Gry → Edukacyjne** (albo Symulacje), e-mail kontaktowy (będzie publiczny).

## 4. Zawartość aplikacji
**Zasady i programy → Zawartość aplikacji** (*Policy and programs → App content*):

| Sekcja | Odpowiedź |
|---|---|
| Polityka prywatności | link z punktu 1 |
| Dostęp do aplikacji | Wszystkie funkcje dostępne bez specjalnego dostępu |
| Reklamy | Nie, aplikacja nie zawiera reklam |
| Ocena treści | ankieta IARC: kategoria „Gra”, e-mail, wszędzie „Nie” (przemoc, hazard, czat itd.) |
| Grupa docelowa | 13–15, 16–17, 18+ (bez grup dziecięcych < 13) |
| Aplikacje rządowe, finansowe, zdrowotne, informacyjne | Nie |

### Bezpieczeństwo danych (*Data safety*)
Gra sama nic nie wysyła, ale Google Play Gry (logowanie, zapis w chmurze, osiągnięcia) zbiera dane. Uczciwie je deklarujemy:
- Czy aplikacja zbiera lub udostępnia dane? **Tak**
- Czy dane są szyfrowane podczas przesyłania? **Tak**
- Czy użytkownicy mogą tworzyć konto? **Nie** (logowanie to konto Google Play Gry, nie nasze)
- Czy można poprosić o usunięcie danych? **Tak** – w profilu Play Gry / myaccount.google.com
- Typy danych (zbierane, nie udostępniane; opcjonalne, bo logowanie jest dobrowolne):
  - **Informacje osobiste → Identyfikatory użytkownika** (profil gracza) – cel: funkcjonalność aplikacji
  - **Aktywność w aplikacji → Inne działania w aplikacji** (osiągnięcia, wyniki, zapis gry) – cel: funkcjonalność aplikacji
  - **Informacje o aplikacji i wydajności → Dzienniki awarii, Diagnostyka** (SDK Google) – cel: analityka

Źródło: developer.android.com/games/pgs/data-collection – za ostateczne odpowiedzi odpowiada deweloper.

## 5. Testy wewnętrzne
**Testowanie i publikowanie → Testowanie → Testy wewnętrzne** (*Test and release → Testing → Internal testing*):
1. Zakładka **Testerzy** → **Utwórz listę e-mailową** → Twój Gmail z telefonu → zaznacz listę → **Zapisz**.
2. **Utwórz nową wersję** → przy pytaniu o podpis: **Użyj klucza wygenerowanego przez Google** → wgraj `app-release.aab` → **Dalej** → **Zapisz i opublikuj**.
3. **Testerzy → Skopiuj link** → otwórz na telefonie → **Zostań testerem** → **Zainstaluj z Google Play**.
4. Przed pierwszą instalacją ze sklepu odinstaluj starą wersję „Monter – Elektryk” (ten sam identyfikator, inny podpis). „Spięcie! DEV” może zostać.

Pierwsza wersja może pojawić się w sklepie po kilkunastu minutach do kilku godzin.

## 6. Kolejne wersje
1. Pobierz nowe **monter-play-aab** (punkt 0).
2. **Testy wewnętrzne → Utwórz nową wersję** → wgraj → **Zapisz i opublikuj**.
3. Telefon dostaje aktualizację – postępy zostają.

## 7. Zapis w chmurze – Google Play Gry (opcjonalnie)
1. **Testowanie i publikowanie → Integralność aplikacji → Podpisywanie aplikacji** → skopiuj **SHA-1 klucza podpisywania aplikacji**.
2. **Rozwój → Usługi gier Play → Konfiguracja i zarządzanie → Konfiguracja** → „Nie, gra nie korzysta z interfejsów API Google” → nazwa `Spięcie!` → **Utwórz**. Skopiuj **Identyfikator projektu** (numer).
3. **Dane logowania → Dodaj** → Android → skonfiguruj **ekran zgody OAuth** (Zewnętrzny, nazwa, e-maile) → **Utwórz klienta OAuth**: Android, pakiet `pl.monter.elektryk`, SHA-1 z kroku 1 → wróć, **Odśwież**, wybierz klienta → **Zapisz**.
4. **Usługi gier Play → Testerzy** → dodaj swój Gmail.
5. Identyfikator projektu wpisuje się w `app/src/main/res/values/strings.xml` → `game_services_project_id`, potem nowy build i nowa wersja.

## Produkcja (później)
Nowe konta prywatne muszą przed produkcją przejść **testy zamknięte: min. 12 testerów przez 14 dni**.
