package pl.monter.core.level

import pl.monter.core.model.CircuitSpec
import pl.monter.core.model.CrossSection
import pl.monter.core.model.DeviceSpec
import pl.monter.core.model.Kind
import pl.monter.core.model.Part
import pl.monter.core.model.TermRef
import pl.monter.core.model.Wire
import pl.monter.core.model.WireColor
import pl.monter.core.model.CrossSection as CS
import pl.monter.core.model.WireColor as W

data class Chapter(val number: Int, val title: String, val description: String)

private fun p(
    id: String, kind: Kind, x: Int, y: Int, label: String = kind.title,
    spec: DeviceSpec? = null, options: List<DeviceSpec> = emptyList(), circuit: CircuitSpec? = null,
) = Part(id, kind, x.toFloat(), y.toFloat(), label, spec, options, circuit)

private class Prewire {
    val list = mutableListOf<Wire>()
    fun w(a: String, b: String, color: WireColor, cs: CrossSection) {
        val (pa, ta) = a.split(".", limit = 2); val (pb, tb) = b.split(".", limit = 2)
        list += Wire(-(list.size + 1), TermRef(pa, ta), TermRef(pb, tb), color, cs, fixed = true)
    }
}

private fun prewire(block: Prewire.() -> Unit) = Prewire().apply(block).list

private fun supply(ratingA: Int, label: String) = DeviceSpec("fuse$ratingA", label, ratingA = ratingA)
private fun mcb(a: Int, curve: Char = 'B') = DeviceSpec("$curve$a", "$curve$a", ratingA = a, curve = curve)
private fun rcd(a: Int, ma: Int, type: String) = DeviceSpec("rcd${a}_${ma}_$type", "$a A / $ma mA typ $type", ratingA = a, rcdmA = ma, rcdType = type)

object Levels {

    val chapters = listOf(
        Chapter(1, "Pierwsze kroki", "Bezpieczna praca, żarówka, gniazdko, lampa z łącznikiem."),
        Chapter(2, "Instalacje w domu", "Łączniki świecznikowe, schodowe, krzyżowe, dzwonek i czujnik ruchu."),
        Chapter(3, "Rozdzielnice", "Od rozdzielnicy mieszkaniowej 230 V po pełną rozdzielnicę 400 V."),
    )

    // ================================================================== ROZDZIAŁ 1

    private val bulb = Level(
        id = "1-1", chapter = 1, number = 1,
        title = "Wymiana żarówki",
        subtitle = "Zasady bezpiecznej pracy i dobór źródła światła",
        theory = listOf(
            "Każdą pracę przy instalacji zaczynamy od procedury bezpieczeństwa: wyłącz, zabezpiecz przed załączeniem, sprawdź brak napięcia.",
            "Nawet przy zwykłej wymianie żarówki wyłączamy łącznik ORAZ zabezpieczenie obwodu – łącznik mógł zostać źle podłączony (w przewodzie N) i oprawka wciąż jest pod napięciem.",
            "Żarówkę dobieramy do oprawy: trzonek (E27, E14, GU10…) i maksymalna moc podana na oprawie. Zbyt mocna żarówka przegrzewa oprawkę i przewody.",
        ),
        funFact = "Oznaczenie E27 pochodzi od Thomasa Edisona (E) i średnicy gwintu – 27 mm. E14 to „mały gwint\" – 14 mm.",
        parts = listOf(
            p("sup", Kind.SUPPLY_1P, 60, 460, "Obwód oświetlenia (B10)", spec = supply(10, "B10")),
            p("sw", Kind.SWITCH_1, 330, 440, "Łącznik"),
            p(
                "lamp", Kind.LAMP, 600, 120, "Oprawa E27, max 60 W",
                options = listOf(
                    DeviceSpec("led_e27", "LED E27 8 W"),
                    DeviceSpec("led_e14", "LED E14 5 W"),
                    DeviceSpec("inc_e27_100", "Żarowa E27 100 W"),
                    DeviceSpec("led_gu10", "LED GU10 5 W"),
                ),
            ),
        ),
        prewired = prewire {
            w("sup.L", "sw.L", W.BROWN, CS.S1_5)
            w("sw.P", "lamp.L", W.BLACK, CS.S1_5)
            w("sup.N", "lamp.N", W.BLUE, CS.S1_5)
            w("sup.PE", "lamp.PE", W.GREEN_YELLOW, CS.S1_5)
        },
        goals = listOf(Goal.Follows("lamp", "lampa świeci, gdy łącznik jest włączony") { it.on("sw") }),
        choiceRules = listOf(
            ChoiceRule(
                "lamp", setOf("led_e27"),
                mapOf(
                    "led_e14" to "Trzonek E14 nie pasuje do oprawki E27.",
                    "inc_e27_100" to "Oprawa dopuszcza maks. 60 W. Żarówka 100 W przegrzeje oprawkę – a LED 8 W daje tyle światła co tradycyjna 60 W!",
                    "led_gu10" to "GU10 to trzonek bagnetowy do oczek halogenowych – nie wkręcisz go w E27.",
                ),
            ),
        ),
        basePoints = 60,
        hints = listOf(
            "Dotknij oprawy, aby wybrać żarówkę.",
            "Po wyborze przełącz się w tryb TEST i włącz łącznik.",
        ),
    )

    private val socket = Level(
        id = "1-2", chapter = 1, number = 2,
        title = "Wymiana gniazdka",
        subtitle = "L, N i PE – trzy przewody, trzy kolory",
        theory = listOf(
            "Przewód w ścianie ma trzy żyły: brązową (faza L), niebieską (neutralny N) i zielono-żółtą (ochronny PE).",
            "PE podłączamy do bolca ochronnego gniazda. Przy uszkodzeniu izolacji to on odprowadza prąd, a zabezpieczenie wyłącza obwód.",
            "Gniazda zasila się przewodem 2,5 mm² i zabezpiecza wyłącznikiem B16. Przekrój przewodu musi pasować do zabezpieczenia!",
        ),
        funFact = "Polskie gniazdo (typ E) ma bolec ochronny wystający z gniazda, a niemieckie (typ F, „Schuko\") – boczne blaszki. Wtyczki typu E/F pasują do obu.",
        parts = listOf(
            p("sup", Kind.SUPPLY_1P, 80, 440, "Przewód w puszce (B16)", spec = supply(16, "B16")),
            p("socket", Kind.SOCKET, 560, 150, "Gniazdo 230 V"),
        ),
        goals = listOf(Goal.Powered("socket")),
        basePoints = 80,
        hints = listOf(
            "Połącz L z L, N z N i PE z PE.",
            "Zabezpieczenie B16 wymaga przewodu co najmniej 2,5 mm².",
        ),
    )

    private val lampSwitch = Level(
        id = "1-3", chapter = 1, number = 3,
        title = "Lampa z łącznikiem",
        subtitle = "Łącznik zawsze w przewodzie fazowym",
        theory = listOf(
            "Łącznik jednobiegunowy przerywa tylko jeden przewód – musi to być FAZA (L). Przewód N idzie bezpośrednio do oprawy.",
            "Przewód między łącznikiem a oprawą to „faza łączona\" (L′). Często ma kolor czarny, aby odróżnić ją od fazy stałej.",
            "Oprawa z metalową obudową to urządzenie I klasy ochronności – wymaga podłączenia PE.",
        ),
        funFact = "Wyłączając światło łącznikiem w przewodzie N, żarówka zgaśnie, ale oprawka zostaje pod napięciem 230 V. Tego błędu nie widać „na oko\" – to jeden z najczęstszych grzechów amatorów.",
        parts = listOf(
            p("sup", Kind.SUPPLY_1P, 60, 470, "Obwód oświetlenia (B10)", spec = supply(10, "B10")),
            p("sw", Kind.SWITCH_1, 330, 150, "Łącznik"),
            p("lamp", Kind.LAMP, 680, 110, "Plafon"),
        ),
        goals = listOf(Goal.Follows("lamp", "lampa świeci, gdy łącznik jest włączony") { it.on("sw") }),
        basePoints = 100,
        hints = listOf("L z zasilania → łącznik L; wyjście łącznika ↑ → oprawa L.", "N i PE prowadzimy od zasilania prosto do oprawy."),
    )

    private val twoSockets = Level(
        id = "1-4", chapter = 1, number = 4,
        title = "Dwa gniazda – przelot",
        subtitle = "Zasilanie kolejnego gniazda z poprzedniego",
        theory = listOf(
            "Gniazda mają podwójne zaciski, które pozwalają „przelotowo\" zasilić następne gniazdo.",
            "W jednym zacisku śrubowym aparatu można zacisnąć najwyżej dwa przewody. Więcej połączeń robimy w złączkach (np. WAGO).",
            "Uwaga: ciągłość PE przy przelocie – odłączenie pierwszego gniazda nie może przerwać PE dla następnych (dlatego coraz częściej stosuje się złączki w puszce).",
        ),
        funFact = "Złączki WAGO wymyślono w Niemczech w 1951 r. Dziś są standardem w puszkach instalacyjnych – połączenie „na skrętkę\" z taśmą jest niedopuszczalne.",
        parts = listOf(
            p("sup", Kind.SUPPLY_1P, 60, 460, "Obwód gniazd (B16)", spec = supply(16, "B16")),
            p("s1", Kind.SOCKET, 380, 150, "Gniazdo 1"),
            p("s2", Kind.SOCKET, 720, 150, "Gniazdo 2"),
        ),
        goals = listOf(Goal.Powered("s1"), Goal.Powered("s2")),
        basePoints = 110,
        hints = listOf("Zasil gniazdo 1, a z jego drugich zacisków poprowadź przewody do gniazda 2."),
    )

    // ================================================================== ROZDZIAŁ 2

    private val chandelier = Level(
        id = "2-1", chapter = 2, number = 1,
        title = "Łącznik świecznikowy",
        subtitle = "Dwie grupy żarówek żyrandola",
        theory = listOf(
            "Łącznik świecznikowy ma jeden zacisk wejściowy L i dwa wyjścia (↑1, ↑2). Każdy klawisz załącza inną grupę żarówek.",
            "Do żyrandola biegnie przewód 4-żyłowy: dwie fazy łączone, N i PE.",
            "Gdy z jednego zacisku zasilania musi wyjść kilka przewodów – używamy złączki.",
        ),
        funFact = "Nazwa „świecznikowy\" pochodzi od żyrandoli świecowych, w których zapalano osobno kilka kręgów świec.",
        parts = listOf(
            p("sup", Kind.SUPPLY_1P, 40, 480, "Obwód oświetlenia (B10)", spec = supply(10, "B10")),
            p("sw", Kind.SWITCH_2, 250, 140, "Łącznik świecznikowy"),
            p("la", Kind.LAMP, 530, 90, "Żyrandol – grupa 1"),
            p("lb", Kind.LAMP, 780, 90, "Żyrandol – grupa 2"),
            p("wn", Kind.WAGO3, 430, 500, "Złączka N"),
            p("wpe", Kind.WAGO3, 600, 500, "Złączka PE"),
        ),
        goals = listOf(
            Goal.OneOf(
                listOf(
                    listOf(Goal.Follows("la", "klawisz 1 → grupa 1") { it.bit("sw", 0) }, Goal.Follows("lb", "klawisz 2 → grupa 2") { it.bit("sw", 1) }),
                    listOf(Goal.Follows("la", "klawisz 2 → grupa 1") { it.bit("sw", 1) }, Goal.Follows("lb", "klawisz 1 → grupa 2") { it.bit("sw", 0) }),
                ),
            ),
        ),
        basePoints = 140,
        hints = listOf("Zasilanie ma po jednym miejscu na przewód – N i PE rozprowadź przez złączki.", "Wyjście ↑1 do grupy 1, ↑2 do grupy 2."),
    )

    private val stairs = Level(
        id = "2-2", chapter = 2, number = 2,
        title = "Łącznik schodowy",
        subtitle = "Światło sterowane z dwóch miejsc",
        theory = listOf(
            "Łącznik schodowy (przełącznik) łączy zacisk wspólny C z wyjściem ↑1 albo ↑2.",
            "Dwa łączniki schodowe łączymy dwoma przewodami korespondencyjnymi: ↑1–↑1 i ↑2–↑2. Faza wchodzi na C pierwszego łącznika, a z C drugiego wychodzi do lampy.",
            "Dzięki temu każda zmiana pozycji dowolnego łącznika zmienia stan światła.",
        ),
        funFact = "W Anglii łącznik schodowy nazywa się „two-way switch\", a w USA – „3-way switch\" (od liczby zacisków). Ten sam aparat, dwie nazwy!",
        parts = listOf(
            p("sup", Kind.SUPPLY_1P, 40, 480, "Obwód oświetlenia (B10)", spec = supply(10, "B10")),
            p("s1", Kind.SWITCH_STAIR, 180, 140, "Łącznik – parter"),
            p("s2", Kind.SWITCH_STAIR, 760, 140, "Łącznik – piętro"),
            p("lamp", Kind.LAMP, 460, 90, "Lampa na schodach"),
        ),
        goals = listOf(Goal.Toggles("lamp", listOf("s1", "s2"))),
        basePoints = 160,
        hints = listOf("L → C łącznika 1. Przewody korespondencyjne: ↑1–↑1, ↑2–↑2. C łącznika 2 → lampa L."),
    )

    private val cross = Level(
        id = "2-3", chapter = 2, number = 3,
        title = "Łącznik krzyżowy",
        subtitle = "Długi korytarz – trzy miejsca sterowania",
        theory = listOf(
            "Łącznik krzyżowy wstawia się między dwa łączniki schodowe, w przewody korespondencyjne.",
            "W jednej pozycji łączy 1–3 i 2–4 (na wprost), w drugiej 1–4 i 2–3 (na krzyż).",
            "Łączników krzyżowych może być dowolnie wiele – każdy dodaje kolejne miejsce sterowania.",
        ),
        funFact = "Układ schodowo-krzyżowy to w istocie bramka XOR – ta sama operacja logiczna, której używają procesory!",
        parts = listOf(
            p("sup", Kind.SUPPLY_1P, 40, 480, "Obwód oświetlenia (B10)", spec = supply(10, "B10")),
            p("s1", Kind.SWITCH_STAIR, 150, 150, "Łącznik – wejście"),
            p("x", Kind.SWITCH_CROSS, 440, 300, "Łącznik krzyżowy – środek"),
            p("s2", Kind.SWITCH_STAIR, 780, 150, "Łącznik – koniec"),
            p("lamp", Kind.LAMP, 450, 60, "Lampy korytarza"),
        ),
        goals = listOf(Goal.Toggles("lamp", listOf("s1", "x", "s2"))),
        basePoints = 190,
        hints = listOf("↑1 i ↑2 łącznika 1 → zaciski 1 i 2 krzyżowego. Zaciski 3 i 4 krzyżowego → ↑1 i ↑2 łącznika 2."),
    )

    private val bell = Level(
        id = "2-4", chapter = 2, number = 4,
        title = "Dzwonek do drzwi",
        subtitle = "Transformator i obwód SELV",
        theory = listOf(
            "Dzwonek zasilamy bardzo niskim napięciem bezpiecznym (SELV), np. 8 V z transformatora dzwonkowego.",
            "Strona pierwotna (230 V) to L i N. Strona wtórna (8 V) jest galwanicznie oddzielona od sieci – dlatego przycisk przy drzwiach jest bezpieczny nawet w deszczu.",
            "Przycisk wpina się w obwód wtórny, szeregowo z dzwonkiem. Obwody SELV można wykonać cieńszym przewodem (np. 0,5 mm²).",
        ),
        funFact = "SELV to „Safety Extra-Low Voltage\". Granica bezpieczeństwa to 50 V AC w warunkach normalnych, a w łazience czy basenie – tylko 12–25 V.",
        parts = listOf(
            p("sup", Kind.SUPPLY_1P, 40, 480, "Obwód (B10)", spec = supply(10, "B10")),
            p("tr", Kind.TRANSFORMER, 240, 110, "Transformator 230/8 V"),
            p("btn", Kind.BUTTON, 520, 330, "Przycisk przy drzwiach"),
            p("bell", Kind.BELL, 770, 110, "Dzwonek 8 V"),
        ),
        goals = listOf(Goal.Follows("bell", "dzwonek dzwoni tylko przy wciśniętym przycisku") { it.on("btn") }),
        basePoints = 180,
        hints = listOf("L i N zasilania → strona 230 V transformatora.", "8 V: jeden zacisk → dzwonek, drugi → przycisk → dzwonek."),
    )

    private val pir = Level(
        id = "2-5", chapter = 2, number = 5,
        title = "Czujnik ruchu (bonus)",
        subtitle = "Automatyczne światło w garażu",
        theory = listOf(
            "Czujnik ruchu (PIR) to łącznik z własną elektroniką – dlatego potrzebuje zasilania: L i N.",
            "Wyjście L′ podaje fazę do lampy, gdy czujnik wykryje ruch.",
            "N musi trafić i do czujnika, i do lampy – użyj złączki.",
        ),
        funFact = "PIR (Passive InfraRed) nie wysyła żadnych promieni – „widzi\" zmiany promieniowania cieplnego, które emituje ciało człowieka (ok. 10 µm długości fali).",
        parts = listOf(
            p("sup", Kind.SUPPLY_1P, 40, 480, "Obwód oświetlenia (B10)", spec = supply(10, "B10")),
            p("pir", Kind.MOTION_SENSOR, 280, 130, "Czujnik ruchu"),
            p("lamp", Kind.LAMP, 680, 100, "Lampa garażowa"),
            p("wn", Kind.WAGO3, 420, 490, "Złączka N"),
        ),
        goals = listOf(Goal.Follows("lamp", "lampa świeci przy wykryciu ruchu") { it.on("pir") }),
        basePoints = 200,
        unlockCost = 300,
        hints = listOf("Czujnik: L z zasilania, N przez złączkę. Wyjście L′ → lampa L."),
    )

    // ================================================================== ROZDZIAŁ 3

    private val flatBoardParts = listOf(
        p("sup", Kind.SUPPLY_1P, 40, 20, "Zasilanie z licznika (wkładka 25 A)", spec = supply(25, "25 A gG")),
        p("main", Kind.MAIN_SWITCH_2P, 60, 150, "Rozłącznik główny 40 A", spec = DeviceSpec("fr40", "40 A", ratingA = 40)),
        p("rcd", Kind.RCD_2P, 200, 150, "RCD 40 A / 30 mA typ A", spec = rcd(40, 30, "A")),
        p("m1", Kind.MCB_1P, 360, 150, "B10", spec = mcb(10)),
        p("m2", Kind.MCB_1P, 430, 150, "B16", spec = mcb(16)),
        p("m3", Kind.MCB_1P, 500, 150, "B16", spec = mcb(16)),
        p("nbar", Kind.N_BAR, 60, 330, "Szyna N"),
        p("pebar", Kind.PE_BAR, 420, 330, "Szyna PE"),
        p("c1", Kind.CIRCUIT_1P, 40, 480, "Oświetlenie", circuit = CircuitSpec("Oświetlenie", 6, 10, needsRcd = false)),
        p("c2", Kind.CIRCUIT_1P, 280, 480, "Gniazda – pokoje", circuit = CircuitSpec("Gniazda", 10, 16, needsRcd = true)),
        p("c3", Kind.CIRCUIT_1P, 520, 480, "Łazienka", circuit = CircuitSpec("Łazienka", 10, 16, needsRcd = true)),
    )

    private val flatBoard = Level(
        id = "3-1", chapter = 3, number = 1,
        title = "Rozdzielnica mieszkaniowa",
        subtitle = "Rozłącznik, RCD, wyłączniki nadprądowe, szyny N i PE",
        theory = listOf(
            "Kolejność aparatów: rozłącznik główny → wyłącznik różnicowoprądowy (RCD) → wyłączniki nadprądowe obwodów.",
            "Przewód N każdego obwodu chronionego przez RCD MUSI przejść przez ten RCD (przez szynę N za RCD). Inaczej RCD „zobaczy\" różnicę prądów i wyzwoli.",
            "PE nigdy nie przechodzi przez żaden aparat – idzie bezpośrednio na szynę PE.",
            "Przewody między aparatami chroni tylko wkładka przedlicznikowa (25 A), więc muszą mieć min. 4 mm². Obwody: B10 → 1,5 mm², B16 → 2,5 mm².",
        ),
        funFact = "RCD porównuje prąd „wpływający\" fazą z „wypływającym\" przewodem N. Już 30 mA różnicy (tyle, co może płynąć przez ciało człowieka) wyłącza zasilanie w ok. 20–40 ms.",
        parts = flatBoardParts,
        prewired = prewire {
            w("sup.L", "main.1", W.BROWN, CS.S10)
            w("sup.N", "main.N", W.BLUE, CS.S10)
            w("sup.PE", "pebar.1", W.GREEN_YELLOW, CS.S10)
        },
        goals = listOf(Goal.Powered("c1"), Goal.Powered("c2"), Goal.Powered("c3")),
        safetyProcedure = true,
        basePoints = 250,
        hints = listOf(
            "Rozłącznik (2, N′) → RCD (1, N). Z RCD zacisk 2 → wejście B10, a mostki przewodem do kolejnych wyłączników.",
            "RCD N′ → szyna N. Z szyny N i szyny PE prowadź przewody do każdego obwodu.",
        ),
    )

    private val selection = Level(
        id = "3-2", chapter = 3, number = 2,
        title = "Dobór zabezpieczeń",
        subtitle = "Wyłącznik do przewodu, RCD do obwodu",
        theory = listOf(
            "Wyłącznik nadprądowy chroni PRZEWÓD. Jego prąd znamionowy nie może przekroczyć obciążalności przewodu: 1,5 mm² → B10, 2,5 mm² → B16.",
            "Charakterystyka B wyzwala przy 3–5 × In (oświetlenie, gniazda), C przy 5–10 × In (silniki, duże prądy rozruchu).",
            "RCD ma dwa parametry: IΔn (czułość, dla ochrony ludzi ≤ 30 mA) i In – prąd, który może przez niego płynąć (nie mniej niż zabezpieczenie przed nim).",
        ),
        funFact = "Oznaczenie B16 znaczy: charakterystyka B, 16 A. Wyłącznik wytrzyma 1,13 × In przez godzinę, a przy 1,45 × In musi wyłączyć w ciągu godziny.",
        parts = listOf(
            p("sup", Kind.SUPPLY_1P, 40, 20, "Zasilanie z licznika (wkładka 25 A)", spec = supply(25, "25 A gG")),
            p("main", Kind.MAIN_SWITCH_2P, 60, 150, "Rozłącznik główny 40 A", spec = DeviceSpec("fr40", "40 A", ratingA = 40)),
            p(
                "rcd", Kind.RCD_2P, 200, 150, "RCD – wybierz",
                options = listOf(rcd(16, 30, "A"), rcd(40, 30, "A"), rcd(40, 30, "AC"), rcd(40, 100, "A"), rcd(40, 300, "AC")),
            ),
            p("m1", Kind.MCB_1P, 360, 150, "Wybierz", options = listOf(mcb(6), mcb(10), mcb(16), mcb(20), mcb(10, 'C'))),
            p("m2", Kind.MCB_1P, 430, 150, "Wybierz", options = listOf(mcb(10), mcb(16), mcb(20), mcb(25), mcb(16, 'C'))),
            p("m3", Kind.MCB_1P, 500, 150, "Wybierz", options = listOf(mcb(10), mcb(16), mcb(20), mcb(25), mcb(16, 'C'))),
            p("nbar", Kind.N_BAR, 60, 330, "Szyna N"),
            p("pebar", Kind.PE_BAR, 420, 330, "Szyna PE"),
            p("c1", Kind.CIRCUIT_1P, 40, 480, "Oświetlenie (1,5 mm²)", circuit = CircuitSpec("Oświetlenie", 6, 10, needsRcd = false)),
            p("c2", Kind.CIRCUIT_1P, 280, 480, "Gniazda kuchnia (2,5 mm²)", circuit = CircuitSpec("Gniazda kuchnia", 16, 16, needsRcd = true)),
            p("c3", Kind.CIRCUIT_1P, 520, 480, "Pralka (2,5 mm²)", circuit = CircuitSpec("Pralka", 16, 16, needsRcd = true)),
        ),
        prewired = prewire {
            w("sup.L", "main.1", W.BROWN, CS.S10); w("sup.N", "main.N", W.BLUE, CS.S10)
            w("sup.PE", "pebar.1", W.GREEN_YELLOW, CS.S10)
            w("main.2", "rcd.1", W.BROWN, CS.S10); w("main.N'", "rcd.N", W.BLUE, CS.S10)
            w("rcd.2", "m1.1", W.BROWN, CS.S10); w("m1.1", "m2.1", W.BROWN, CS.S10); w("m2.1", "m3.1", W.BROWN, CS.S10)
            w("rcd.N'", "nbar.1", W.BLUE, CS.S10)
            w("m1.2", "c1.L", W.BROWN, CS.S1_5); w("nbar.2", "c1.N", W.BLUE, CS.S1_5); w("pebar.2", "c1.PE", W.GREEN_YELLOW, CS.S1_5)
            w("m2.2", "c2.L", W.BROWN, CS.S2_5); w("nbar.3", "c2.N", W.BLUE, CS.S2_5); w("pebar.3", "c2.PE", W.GREEN_YELLOW, CS.S2_5)
            w("m3.2", "c3.L", W.BROWN, CS.S2_5); w("nbar.4", "c3.N", W.BLUE, CS.S2_5); w("pebar.4", "c3.PE", W.GREEN_YELLOW, CS.S2_5)
        },
        goals = listOf(Goal.Powered("c1"), Goal.Powered("c2"), Goal.Powered("c3")),
        basePoints = 220,
        hints = listOf("Dotknij każdego aparatu z napisem „Wybierz\".", "Pamiętaj: przewód 1,5 mm² → maks. 10 A, 2,5 mm² → maks. 16 A."),
    )

    private val power400 = Level(
        id = "3-3", chapter = 3, number = 3,
        title = "Gniazdo siłowe 400 V",
        subtitle = "Trzy fazy, kolejność faz i wyłącznik 3P",
        theory = listOf(
            "W sieci 3×400/230 V napięcie między fazą a N wynosi 230 V, a między dwiema fazami – 400 V (230 × √3).",
            "Gniazdo 5-biegunowe (CEE, czerwone) ma zaciski L1, L2, L3, N, PE. Zachowaj kolejność faz – od niej zależy kierunek obrotów silnika.",
            "Odbiornik trójfazowy zabezpieczamy wyłącznikiem 3P, który odłącza wszystkie fazy naraz.",
        ),
        funFact = "Czerwony kolor gniazda CEE oznacza 400 V, niebieski 230 V, a żółty 110 V (np. na budowach w Wielkiej Brytanii). Położenie bolca PE też zależy od napięcia – nie da się pomylić wtyczek!",
        parts = listOf(
            p("sup", Kind.SUPPLY_3P, 40, 20, "Zasilanie 3×400 V (wkładki 25 A)", spec = supply(25, "25 A gG")),
            p("rcd", Kind.RCD_4P, 80, 190, "RCD 4P 40 A / 30 mA typ A", spec = rcd(40, 30, "A")),
            p("mcb", Kind.MCB_3P, 360, 190, "Wybierz", options = listOf(mcb(10, 'C'), mcb(16, 'C'), mcb(16), mcb(25, 'C'))),
            p(
                "gn", Kind.SOCKET_400, 680, 420, "Gniazdo 16 A 5p",
                circuit = CircuitSpec("Gniazdo 400 V 16 A", 16, 16, needsRcd = true),
            ),
        ),
        goals = listOf(Goal.Powered("gn")),
        threePhase = true,
        basePoints = 280,
        hints = listOf(
            "Zasilanie → RCD (1, 3, 5, N). RCD (2, 4, 6) → wyłącznik 3P → gniazdo L1, L2, L3.",
            "N z RCD (N′) prosto do gniazda, PE z zasilania prosto do gniazda.",
        ),
    )

    private val house400 = Level(
        id = "3-4", chapter = 3, number = 4,
        title = "Rozdzielnica domowa 400 V",
        subtitle = "Pełna rozdzielnica: ochrona przepięciowa, RCD, 6 obwodów",
        theory = listOf(
            "Kolejność: rozłącznik główny → ogranicznik przepięć T1+T2 → RCD 4P → wyłączniki nadprądowe.",
            "Ogranicznik przepięć (SPD) montujemy PRZED RCD i łączymy z szyną PE możliwie krótkim przewodem.",
            "Obwody jednofazowe rozdziel równomiernie na L1, L2 i L3. Przewody fazowe: L1 brązowy, L2 czarny, L3 szary.",
            "Wkładki 32 A → przewody między aparatami min. 6 mm².",
        ),
        funFact = "Uderzenie pioruna w linię napowietrzną może wprowadzić do instalacji udar o wartości dziesiątek kA. Ogranicznik T1 odprowadza go do ziemi w ciągu mikrosekund.",
        width = 1400f, height = 780f,
        parts = listOf(
            p("sup", Kind.SUPPLY_3P, 40, 10, "Złącze 3×400 V (wkładki 32 A)", spec = supply(32, "32 A gG")),
            p("main", Kind.MAIN_SWITCH_4P, 40, 150, "Rozłącznik 4P 63 A", spec = DeviceSpec("fr63", "63 A", ratingA = 63)),
            p("spd", Kind.SPD_4P, 250, 150, "Ogranicznik przepięć T1+T2"),
            p("rcd", Kind.RCD_4P, 460, 150, "RCD 4P 40 A / 30 mA typ A", spec = rcd(40, 30, "A")),
            p("m1", Kind.MCB_1P, 40, 370, "B10", spec = mcb(10)),
            p("m2", Kind.MCB_1P, 100, 370, "B10", spec = mcb(10)),
            p("m3", Kind.MCB_1P, 160, 370, "B16", spec = mcb(16)),
            p("m4", Kind.MCB_1P, 220, 370, "B16", spec = mcb(16)),
            p("m5", Kind.MCB_1P, 280, 370, "B16", spec = mcb(16)),
            p("m6", Kind.MCB_3P, 360, 370, "B16 3P", spec = mcb(16)),
            p("nbar", Kind.N_BAR, 760, 180, "Szyna N"),
            p("pebar", Kind.PE_BAR, 760, 300, "Szyna PE"),
            p("c1", Kind.CIRCUIT_1P, 20, 650, "Oświetlenie parter", circuit = CircuitSpec("Oświetlenie", 6, 10, needsRcd = false)),
            p("c2", Kind.CIRCUIT_1P, 190, 650, "Oświetlenie piętro", circuit = CircuitSpec("Oświetlenie", 6, 10, needsRcd = false)),
            p("c3", Kind.CIRCUIT_1P, 360, 650, "Gniazda kuchnia", circuit = CircuitSpec("Gniazda", 10, 16, needsRcd = true)),
            p("c4", Kind.CIRCUIT_1P, 530, 650, "Gniazda salon", circuit = CircuitSpec("Gniazda", 10, 16, needsRcd = true)),
            p("c5", Kind.CIRCUIT_1P, 700, 650, "Łazienka", circuit = CircuitSpec("Łazienka", 10, 16, needsRcd = true)),
            p("c6", Kind.CIRCUIT_3P, 900, 650, "Płyta indukcyjna 3F", circuit = CircuitSpec("Płyta indukcyjna", 16, 16, needsRcd = true)),
        ),
        prewired = prewire {
            w("sup.L1", "main.1", W.BROWN, CS.S10); w("sup.L2", "main.3", W.BLACK, CS.S10)
            w("sup.L3", "main.5", W.GREY, CS.S10); w("sup.N", "main.N", W.BLUE, CS.S10)
            w("sup.PE", "pebar.1", W.GREEN_YELLOW, CS.S16)
        },
        goals = listOf(
            Goal.Powered("spd"), Goal.Powered("c1"), Goal.Powered("c2"), Goal.Powered("c3"),
            Goal.Powered("c4"), Goal.Powered("c5"), Goal.Powered("c6"),
        ),
        threePhase = true,
        balancePhases = true,
        basePoints = 500,
        hints = listOf(
            "Z wyjść rozłącznika (2, 4, 6, N′) zasil i ogranicznik, i RCD – każdy zacisk przyjmie 2 przewody.",
            "Wyłączniki 1P mostkuj przewodem od sąsiada podłączonego do tej samej fazy.",
            "Rozdziel obwody: np. L1 – oświetlenie parter + gniazda salon, L2 – oświetlenie piętro + łazienka, L3 – kuchnia.",
        ),
    )

    val all: List<Level> = listOf(bulb, socket, lampSwitch, twoSockets, chandelier, stairs, cross, bell, pir, flatBoard, selection, power400, house400)

    fun byId(id: String): Level? = all.firstOrNull { it.id == id }

    /** Poziomy w kolejności kampanii (bez bonusowych). */
    val campaign: List<Level> get() = all.filter { it.unlockCost == null }
}
