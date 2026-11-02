# 🚪 Bramki! – lewo czy prawo?

Gra na Androida w stylu „biegnie ludzik, po lewej +100 lat, po prawej −50 lat" – ale **bez reklam,
bez mikropłatności i bez internetu**. Pięć wariantów, jeden silnik, a po każdej trasie krótka lekcja:
gdzie uciekły punkty i dlaczego.

Projekt jest **całkowicie samodzielny** (własny Gradle, własny pakiet `pl.bramki.gra`). Można go
skopiować do osobnego repozytorium 1:1 – dołączony jest też gotowy workflow GitHub Actions.

## Warianty

| Tryb | Co zbierasz | Szczeble (8) |
|---|---|---|
| 🏛️ **Epoki** | lata postępu | epoka kamienia → brąz → starożytność → średniowiecze → renesans → era przemysłowa → współczesność → przyszłość |
| 🧬 **Ewolucja** | punkty DNA | komórka → ryba → płaz → gad → ssak → naczelny → człowiek → postczłowiek |
| 💰 **Fortuna** | złotówki | pusta kieszeń → skarbonka → … → jacht → rakieta; bramki opisane **procentami** (+50%, −30%) |
| ⚔️ **Armia** | wojów | samotny woj → drużyna → … → horda → imperium; **na mecie bitwa** z wrogiem o sile ½ najlepszej możliwej armii |
| 🪐 **Kosmos** | masę (t) | pyłek → asteroida → księżyc → planeta → gazowy olbrzym → gwiazda → nadolbrzym → czarna dziura |

Każdy szczebel ma swoją **ciekawostkę** (historia, biologia, finanse, fizyka), a ekran wyniku pokazuje
**powtórkę decyzji**: „Bramka 7: miałeś 240, wybrałeś +100 (→ 340), lepsze było ×2 (→ 480)".
Trasa to 24 pary bramek (ok. 1,5–2 min gry), tempo rośnie w miarę jazdy.

## Grafika i animacje

40 postaci (po jednej na szczebel) i 5 teł wygenerował **Gemini** (`tools/gen_art.py`, model
`gemini-3.1-flash-image`). Postacie powstają na jednolicie zielonym tle, a skrypt kluczuje zieleń na
przezroczystość **zalewaniem od brzegu obrazu**: kluczowane są tylko zielone piksele połączone z krawędzią
(plus zamknięte „dziury" z czystą zielenią tła), więc zielona żaba czy jaszczurka zostają nietknięte.
Surowe obrazy leżą w `art/raw/`, gotowe WebP w `app/src/main/res/drawable-nodpi/` (łącznie ~2,4 MB).

Animacje są **proceduralne** (liczone z czasu, nie z klatek) – jeden sprite na szczebel wystarcza:
- bieg: podskok i kołysanie w rytmie prędkości,
- pochylenie w stronę ruchu (jak motocyklista w zakręcie), z prędkości bocznej z silnika,
- **squash & stretch** przy bramce: dobra rozciąga postać w górę, zła spłaszcza,
- wstrząs przy wjeździe w ścianę,
- **przemiana** przy awansie: stara postać znika, nowa „wyskakuje" z poświatą i iskrami; przy spadku czerwony błysk,
- tło trybu z lekką paralaksą przy ruchu w bok, przyciemniane kolorem bieżącego szczebla.

## Jak to działa (i czego uczy)

- **Bramki**: `+N`, `−N`, `×k`, `÷k` (w Fortunie: `+p%`, `−p%`). Zielona = dobra, czerwona = zła, ale często
  wybierasz między dwiema dobrymi (`+500` czy `×2`?) albo dwiema złymi (`−300` czy `÷2`?).
- **Ściany** 🧱 zajmują pół toru i zabierają część wartości – trzeba je omijać.
- **Matematyka pod spodem**: każda operacja jest funkcją *niemalejącą* względem wartości. Dlatego strategia
  zachłanna („wybierz to, co daje więcej teraz") jest **optymalna** na całej trasie – nie trzeba planować
  naprzód. Silnik liczy tak wartość optymalną, a test `AnalysisTest` sprawdza to brutalnie na wszystkich 2ⁿ ścieżkach.
- **Progi szczebli to ułamki wartości optymalnej** danej trasy, więc każda losowa trasa jest „do przejścia",
  a szczyt (≥ 90% optimum) oznacza niemal bezbłędną grę. Kalibracja symulacją: gracz wybierający „zielone bez
  myślenia" kończy w medianie na ok. 76% (6. szczebel), losowe wybory – ok. 25% (4. szczebel).
- Wielkości bramek skalują się z wartością na ścieżce optymalnej – trasa jest ciekawa niezależnie od tego,
  czy masz 50 czy 5 000 000 jednostek.

## Struktura

```
engine/  – czysty Kotlin (bez Androida): operacje, generator tras, symulacja przebiegu, analiza + testy
app/     – aplikacja Android (Jetpack Compose, pseudo-3D rysowane na Canvas, sprite'y z Gemini)
tools/   – generator grafik (Gemini) · art/raw/ – surowe obrazy
```

Pseudo-3D to ten sam trik co w grach wyścigowych z lat 80.: punkt w odległości `dz` rzutujemy skalą
`s = f / (f + dz)` – bez macierzy, bez OpenGL.

## Budowanie

```bash
./gradlew :engine:test          # testy silnika (JVM, bez Androida)
./gradlew :app:assembleDebug    # APK testowe „Bramki DEV" (pl.bramki.gra.debug)
./gradlew :app:bundleRelease    # .aab do Google Play
```

CI (`.github/workflows/android.yml`) buduje przy każdym pushu i wystawia artefakty **bramki-debug-apk** oraz
**bramki-play-aab**. APK debug jest podpisane stałym kluczem `app/debug.keystore`, więc kolejne wersje
instalują się jako aktualizacja i rekordy zostają. `versionCode` = numer przebiegu CI.

Sekrety do podpisu wersji sklepowej: `BRAMKI_KEYSTORE_B64` (plik `.jks` w base64, alias `upload`)
i `BRAMKI_KEYSTORE_PASSWORD`.

## Sterowanie
Przesuń palcem w lewo/prawo albo stuknij połowę ekranu. Bramka po Twojej stronie jest podświetlona.

## Pomysły na rozwój
- dźwięki i muzyka, tryb „wyścig z czasem", trasy dzienne (ten sam seed dla wszystkich),
- kolejne warianty: temperatura (lód → plazma), pierwiastki (wodór → uran), język (gaworzenie → poezja),
- klatkowe animacje biegu (sprite sheet) zamiast proceduralnych.
