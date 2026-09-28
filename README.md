# ⚡ Monter – gra o budowaniu instalacji elektrycznych

Gra na Androida, w której uczysz się montażu instalacji – od wymiany żarówki, przez łączniki schodowe
i dzwonek, aż po pełną rozdzielnicę 3×400 V. Układasz przewody na planszy, a silnik gry **symuluje obwód**
i sprawdza go według zasad z polskich norm (PN-HD 60364, PN-EN 60445, PN-EN 50110-1).

## Co jest w grze

| Rozdział | Poziomy |
|---|---|
| 1. Pierwsze kroki | wymiana żarówki (dobór trzonka i mocy), wymiana gniazdka, lampa z łącznikiem, gniazda przelotowe |
| 2. Instalacje w domu | łącznik świecznikowy, schodowy, krzyżowy, dzwonek z transformatorem SELV, czujnik ruchu (bonus za punkty) |
| 3. Rozdzielnice | rozdzielnica mieszkaniowa, dobór zabezpieczeń, gniazdo siłowe 400 V, rozdzielnica domowa 400 V z SPD |

- **3 poziomy trudności**: Uczeń (podpowiedzi ról zacisków, auto-kolor), Czeladnik, Mistrz (dobre praktyki obowiązkowe).
- **Procedura BHP** przed pracą – „5 zasad bezpieczeństwa" z fałszywymi kartami.
- **Tryb TEST** – przełączasz łączniki, widzisz przepływ prądu, świecące żarówki, dzwoniący dzwonek,
  a przy zwarciu iskry i zadziałanie właściwego zabezpieczenia (MCB, RCD, wkładka).
- **Gratyfikacja**: gwiazdki, iskry ⚡ (waluta), 13 osiągnięć, sklep (podpowiedzi, miernik, wskaźnik napięcia,
  kamera termowizyjna, motywy tablicy, kaski, poziom bonusowy).
- **Chmura**: logowanie przez Google Play Gry + zapis postępów (Saved Games), osiągnięcia i ranking.
  Bez logowania gra działa offline z zapisem lokalnym.

### Co sprawdza walidator (z wyjaśnieniem „dlaczego")
kolory żył (PE wyłącznie zielono-żółty, N niebieski, fazy brąz/czerń/szary, L1-L2-L3 w rozdzielnicy 3F) •
przekrój vs prąd zabezpieczenia (B10→1,5 mm², B16→2,5 mm², wkładka 25 A→4 mm²…) • N i PE nie cieńsze od fazy •
łącznik w przewodzie fazowym • RCD ≤30 mA dla gniazd i łazienki, typ A zamiast AC • N za RCD (inaczej RCD wyzwala) •
prąd znamionowy RCD ≥ zabezpieczenie przed nim • wyłącznik 3P dla odbiorników 3-fazowych • kolejność faz •
SPD przed RCD • symetria obciążenia faz • przycisk dzwonka w obwodzie SELV • PE podłączony do odbiorników I klasy •
obudowa pod napięciem • maks. 2 przewody w zacisku.

## Struktura projektu

```
core/   – czysty Kotlin (bez Androida): symulator, walidator, poziomy, punkty, sklep, zapis + testy
app/    – aplikacja Android (Jetpack Compose, Play Games Services v2)
```

Silnik jest oddzielony od Androida, dzięki czemu testy uruchomisz na dowolnym komputerze:

```bash
./gradlew :core:test          # testy silnika (każdy poziom ma wzorcowe rozwiązanie + testy typowych błędów)
./gradlew :app:assembleDebug  # APK do zainstalowania na telefonie
```

GitHub Actions (`.github/workflows/android.yml`) buduje APK przy każdym pushu – gotowy plik znajdziesz
w zakładce **Actions → Android CI → Artifacts → monter-debug-apk**.

## Konfiguracja Google Play (logowanie i zapis w chmurze)

1. **Play Console** → Utwórz aplikację (nazwa pakietu: `pl.monter.elektryk`, można zmienić w `app/build.gradle.kts`).
2. **Rozwój → Usługi gier Play → Konfiguracja**: utwórz projekt usług gier.
   - Włącz **Zapisane gry (Saved Games)**.
   - Dodaj **dane logowania Android**: nazwa pakietu + odcisk SHA-1 klucza podpisywania
     (`keytool -list -v -keystore twoj.jks` oraz osobno klucz z „Podpisywania aplikacji przez Google Play").
3. Skopiuj **identyfikator projektu** (sam numer) do `app/src/main/res/values/strings.xml` → `game_services_project_id`.
4. (Opcjonalnie) Utwórz osiągnięcia i ranking „Iskry", a ich identyfikatory wklej do `pgs_ach_*` / `pgs_leaderboard_sparks`
   w tym samym pliku. Puste wartości są po prostu pomijane.
5. Dodaj swoje konto jako **testera** usług gier, dopóki projekt nie jest opublikowany.

> Ciekawostka: Play Games Services v2 loguje gracza automatycznie przy starcie gry – nie trzeba już osobnego
> przycisku „Zaloguj przez Google". Przycisk w menu służy tylko do ponownej próby.

## Wydanie w sklepie

```bash
./gradlew :app:bundleRelease   # plik .aab do wgrania w Play Console
```
Przed wydaniem skonfiguruj podpisywanie (`signingConfigs` w `app/build.gradle.kts`, klucz trzymaj poza repozytorium).
Aplikacja targetuje API 36 (wymóg Google Play od sierpnia 2026).

## Pomysły na rozwój
- kolejne poziomy: przekaźnik bistabilny, ściemniacz, sterowanie rolet, instalacja w łazience (strefy 0–2), pomiary (impedancja pętli zwarcia, rezystancja izolacji),
- tryb „Znajdź usterkę" – gotowa instalacja z ukrytym błędem,
- dźwięki (klik łącznika, bzyczenie przy zwarciu).
