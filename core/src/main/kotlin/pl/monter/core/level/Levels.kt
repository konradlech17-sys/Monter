package pl.monter.core.level

import pl.monter.core.model.DeviceSpec
import pl.monter.core.model.Kind

data class Chapter(val number: Int, val title: String, val description: String, val icon: String)

object Levels {

    val chapters = listOf(
        Chapter(1, "Pierwsze kroki", "Bezpieczna praca, żarówka, gniazdka, lampy i łączniki.", "💡"),
        Chapter(2, "Łączniki", "Świecznikowy, schodowy, krzyżowy – sterowanie z wielu miejsc.", "🔀"),
        Chapter(3, "Sygnalizacja i automatyka", "Dzwonki, transformator SELV, czujniki ruchu i zmierzchu.", "🔔"),
        Chapter(4, "Rozdzielnice 230 V", "Rozłącznik, RCD, wyłączniki nadprądowe, szyny, ochrona przepięciowa.", "🗄️"),
        Chapter(5, "Trójfaza 400 V", "Gniazda siłowe, silniki, płyta indukcyjna i pełna rozdzielnica domowa.", "⚙️"),
        Chapter(6, "Serwis – znajdź usterkę", "Poprawiasz instalacje po „fachowcach\". Słuchaj objawów zgłoszonych przez klienta!", "🛠️"),
    )

    /** Ile poziomów poprzedniego rozdziału trzeba ukończyć, by otworzyć następny. */
    const val CHAPTER_GATE = 3

    // ================================================================== ROZDZIAŁ 1 – PIERWSZE KROKI

    private val bulb = level("1-1", 1, 1, "Wymiana żarówki") {
        subtitle = "Zasady bezpiecznej pracy i dobór źródła światła"
        theory(
            "Każdą pracę przy instalacji zaczynamy od procedury bezpieczeństwa: wyłącz, zabezpiecz przed załączeniem, sprawdź brak napięcia.",
            "Nawet przy zwykłej wymianie żarówki wyłączamy łącznik ORAZ zabezpieczenie obwodu – łącznik mógł zostać źle podłączony (w przewodzie N) i oprawka wciąż jest pod napięciem.",
            "Żarówkę dobieramy do oprawy: trzonek (E27, E14, GU10…) i maksymalna moc podana na oprawie.",
        )
        funFact = "Oznaczenie E27 pochodzi od Thomasa Edisona (E) i średnicy gwintu – 27 mm. E14 to „mały gwint\" – 14 mm."
        part("sup", Kind.SUPPLY_1P, 60, 460, "Obwód oświetlenia (B10)", spec = supply(10, "B10"))
        part("sw", Kind.SWITCH_1, 330, 440, "Łącznik")
        part(
            "lamp", Kind.LAMP, 600, 120, "Oprawa E27, max 60 W",
            options = listOf(
                DeviceSpec("led_e27", "LED E27 8 W"), DeviceSpec("led_e14", "LED E14 5 W"),
                DeviceSpec("inc_e27_100", "Żarowa E27 100 W"), DeviceSpec("led_gu10", "LED GU10 5 W"),
            ),
        )
        pre("sup.L", "sw.L", BR, S15); pre("sw.P", "lamp.L", BK, S15)
        pre("sup.N", "lamp.N", BL, S15); pre("sup.PE", "lamp.PE", GY, S15)
        goal(Goal.Follows("lamp", "lampa świeci, gdy łącznik jest włączony") { it.on("sw") })
        rule(
            ChoiceRule(
                "lamp", setOf("led_e27"),
                mapOf(
                    "led_e14" to "Trzonek E14 nie pasuje do oprawki E27.",
                    "inc_e27_100" to "Oprawa dopuszcza maks. 60 W. Żarówka 100 W przegrzeje oprawkę – a LED 8 W daje tyle światła co tradycyjna 60 W!",
                    "led_gu10" to "GU10 to trzonek bagnetowy do oczek halogenowych – nie wkręcisz go w E27.",
                ),
            ),
        )
        choose("lamp", "led_e27")
        points = 60
        hint("Dotknij oprawy, aby wybrać żarówkę.", "Po wyborze przełącz się w tryb TEST i włącz łącznik.")
    }

    private val socket = level("1-2", 1, 2, "Wymiana gniazdka") {
        subtitle = "L, N i PE – trzy przewody, trzy kolory"
        theory(
            "Przewód w ścianie ma trzy żyły: brązową (faza L), niebieską (neutralny N) i zielono-żółtą (ochronny PE).",
            "PE podłączamy do bolca ochronnego gniazda. Przy uszkodzeniu izolacji to on odprowadza prąd, a zabezpieczenie wyłącza obwód.",
            "Gniazda zasila się przewodem 2,5 mm² i zabezpiecza wyłącznikiem B16. Przekrój przewodu musi pasować do zabezpieczenia!",
        )
        funFact = "Polskie gniazdo (typ E) ma bolec ochronny wystający z gniazda, a niemieckie (typ F, „Schuko\") – boczne blaszki. Wtyczki typu E/F pasują do obu."
        part("sup", Kind.SUPPLY_1P, 80, 440, "Przewód w puszce (B16)", spec = supply(16, "B16"))
        part("socket", Kind.SOCKET, 560, 150, "Gniazdo 230 V")
        w("sup.L", "socket.L", BR, S25); w("sup.N", "socket.N", BL, S25); w("sup.PE", "socket.PE", GY, S25)
        goal(Goal.Powered("socket"))
        points = 80
        hint("Przeciągnij palcem od zacisku L zasilania do zacisku L gniazda. Potem N i PE.", "Zabezpieczenie B16 wymaga przewodu co najmniej 2,5 mm².")
    }

    private val lampSwitch = level("1-3", 1, 3, "Lampa z łącznikiem") {
        subtitle = "Łącznik zawsze w przewodzie fazowym"
        theory(
            "Łącznik jednobiegunowy przerywa tylko jeden przewód – musi to być FAZA (L). Przewód N idzie bezpośrednio do oprawy.",
            "Przewód między łącznikiem a oprawą to „faza łączona\" (L′). Często ma kolor czarny, aby odróżnić ją od fazy stałej.",
            "Oprawa z metalową obudową to urządzenie I klasy ochronności – wymaga podłączenia PE.",
        )
        funFact = "Wyłączając światło łącznikiem w przewodzie N, żarówka zgaśnie, ale oprawka zostaje pod napięciem 230 V. Tego błędu nie widać „na oko\"."
        part("sup", Kind.SUPPLY_1P, 60, 470, "Obwód oświetlenia (B10)", spec = supply(10, "B10"))
        part("sw", Kind.SWITCH_1, 330, 150, "Łącznik")
        part("lamp", Kind.LAMP, 680, 110, "Plafon")
        w("sup.L", "sw.L", BR, S15); w("sw.P", "lamp.L", BK, S15); w("sup.N", "lamp.N", BL, S15); w("sup.PE", "lamp.PE", GY, S15)
        goal(Goal.Follows("lamp", "lampa świeci, gdy łącznik jest włączony") { it.on("sw") })
        points = 100
        hint("L z zasilania → łącznik L; wyjście łącznika ↑ → oprawa L.", "N i PE prowadzimy od zasilania prosto do oprawy.")
    }

    private val twoSockets = level("1-4", 1, 4, "Dwa gniazda – przelot") {
        subtitle = "Zasilanie kolejnego gniazda z poprzedniego"
        theory(
            "Gniazda mają podwójne zaciski, które pozwalają „przelotowo\" zasilić następne gniazdo.",
            "W jednym zacisku aparatu można zacisnąć najwyżej dwa przewody. Więcej połączeń robimy w złączkach (np. WAGO).",
            "Przewody przelotowe przewodzą prąd WSZYSTKICH dalszych gniazd – muszą mieć ten sam przekrój co zasilanie (2,5 mm²).",
        )
        funFact = "Złączki WAGO wymyślono w Niemczech w 1951 r. Dziś są standardem w puszkach – połączenie „na skrętkę\" z taśmą jest niedopuszczalne."
        part("sup", Kind.SUPPLY_1P, 60, 460, "Obwód gniazd (B16)", spec = supply(16, "B16"))
        part("s1", Kind.SOCKET, 380, 150, "Gniazdo 1")
        part("s2", Kind.SOCKET, 720, 150, "Gniazdo 2")
        w("sup.L", "s1.L", BR, S25); w("sup.N", "s1.N", BL, S25); w("sup.PE", "s1.PE", GY, S25)
        w("s1.L", "s2.L", BR, S25); w("s1.N", "s2.N", BL, S25); w("s1.PE", "s2.PE", GY, S25)
        goal(Goal.Powered("s1"), Goal.Powered("s2"))
        points = 110
        hint("Zasil gniazdo 1, a z jego drugich zacisków poprowadź przewody do gniazda 2.")
    }

    private val threeSockets = level("1-5", 1, 5, "Trzy gniazda z puszki") {
        subtitle = "Rozgałęzienie w złączkach WAGO"
        theory(
            "Zamiast łańcucha „gniazdo za gniazdem\" można rozprowadzić obwód w puszce – każda żyła ma swoją złączkę.",
            "Zaleta: odłączenie jednego gniazda nie przerywa zasilania (ani PE!) pozostałych.",
            "Jeden przewód = jedno miejsce w złączce. Złączka 5-torowa wystarczy na zasilanie i cztery odejścia.",
        )
        funFact = "Otwór w złączce WAGO ma tzw. okienko kontrolne – przez przezroczystą obudowę widać, czy żyła weszła do końca."
        part("sup", Kind.SUPPLY_1P, 60, 480, "Przewód zasilający (B16)", spec = supply(16, "B16"))
        part("wl", Kind.WAGO5, 300, 470, "Złączka L")
        part("wn", Kind.WAGO5, 450, 470, "Złączka N")
        part("wpe", Kind.WAGO5, 600, 470, "Złączka PE")
        part("s1", Kind.SOCKET, 180, 120, "Gniazdo – biurko")
        part("s2", Kind.SOCKET, 450, 120, "Gniazdo – TV")
        part("s3", Kind.SOCKET, 720, 120, "Gniazdo – łóżko")
        w("sup.L", "wl.1", BR, S25); w("sup.N", "wn.1", BL, S25); w("sup.PE", "wpe.1", GY, S25)
        for ((i, s) in listOf("s1", "s2", "s3").withIndex()) {
            w("wl.${i + 2}", "$s.L", BR, S25); w("wn.${i + 2}", "$s.N", BL, S25); w("wpe.${i + 2}", "$s.PE", GY, S25)
        }
        goal(Goal.Powered("s1"), Goal.Powered("s2"), Goal.Powered("s3"))
        points = 130
        hint("Każda żyła zasilania trafia do swojej złączki: L, N, PE. Z każdej złączki – przewód do każdego gniazda.")
    }

    private val twoLamps = level("1-6", 1, 6, "Dwie lampy, jeden łącznik") {
        subtitle = "Odbiorniki połączone równolegle"
        theory(
            "Odbiorniki w instalacji łączymy RÓWNOLEGLE – każdy dostaje pełne 230 V.",
            "Faza łączona z łącznika trafia do pierwszej lampy, a z jej drugiego miejsca w zacisku – do drugiej.",
            "N i PE rozprowadzamy złączkami.",
        )
        funFact = "Gdyby połączyć dwie żarówki szeregowo, każda dostałaby tylko ~115 V i świeciłaby słabo. Tak działały kiedyś lampki choinkowe – jedna przepalona gasiła cały łańcuch."
        part("sup", Kind.SUPPLY_1P, 40, 480, "Obwód oświetlenia (B10)", spec = supply(10, "B10"))
        part("sw", Kind.SWITCH_1, 250, 150, "Łącznik")
        part("l1", Kind.LAMP, 500, 90, "Lampa 1")
        part("l2", Kind.LAMP, 760, 90, "Lampa 2")
        part("wn", Kind.WAGO3, 430, 490, "Złączka N")
        part("wpe", Kind.WAGO3, 600, 490, "Złączka PE")
        w("sup.L", "sw.L", BR, S15); w("sw.P", "l1.L", BK, S15); w("l1.L", "l2.L", BK, S15)
        w("sup.N", "wn.1", BL, S15); w("wn.2", "l1.N", BL, S15); w("wn.3", "l2.N", BL, S15)
        w("sup.PE", "wpe.1", GY, S15); w("wpe.2", "l1.PE", GY, S15); w("wpe.3", "l2.PE", GY, S15)
        goal(Goal.Follows("l1", "świeci przy włączonym łączniku") { it.on("sw") }, Goal.Follows("l2", "świeci przy włączonym łączniku") { it.on("sw") })
        points = 130
        hint("Łącznik ↑ → lampa 1 L → lampa 2 L. N i PE przez złączki.")
    }

    private val room = level("1-7", 1, 7, "Pokój: światło i gniazdo") {
        subtitle = "Dwa osobne obwody – dwa różne przekroje"
        theory(
            "Oświetlenie i gniazda to OSOBNE obwody z osobnymi zabezpieczeniami. Awaria czajnika nie zgasi światła.",
            "Obwód oświetlenia: B10 i przewód 1,5 mm². Obwód gniazd: B16 i 2,5 mm².",
        )
        funFact = "Grubszy przewód ma mniejszą rezystancję, więc mniej się nagrzewa. Moc strat rośnie z kwadratem prądu: 2 × większy prąd = 4 × więcej ciepła!"
        part("supL", Kind.SUPPLY_1P, 40, 480, "Obwód oświetlenia (B10)", spec = supply(10, "B10"))
        part("supG", Kind.SUPPLY_1P, 600, 480, "Obwód gniazd (B16)", spec = supply(16, "B16"))
        part("sw", Kind.SWITCH_1, 200, 140, "Łącznik")
        part("lamp", Kind.LAMP, 420, 90, "Lampa")
        part("socket", Kind.SOCKET, 780, 140, "Gniazdo")
        w("supL.L", "sw.L", BR, S15); w("sw.P", "lamp.L", BK, S15); w("supL.N", "lamp.N", BL, S15); w("supL.PE", "lamp.PE", GY, S15)
        w("supG.L", "socket.L", BR, S25); w("supG.N", "socket.N", BL, S25); w("supG.PE", "socket.PE", GY, S25)
        goal(Goal.Follows("lamp", "lampa świeci przy włączonym łączniku") { it.on("sw") }, Goal.Powered("socket"))
        points = 140
        hint("Lampę zasil z obwodu B10 (1,5 mm²), gniazdo z obwodu B16 (2,5 mm²).")
    }

    // ================================================================== ROZDZIAŁ 2 – ŁĄCZNIKI

    private val chandelier = level("2-1", 2, 1, "Łącznik świecznikowy") {
        subtitle = "Dwie grupy żarówek żyrandola"
        theory(
            "Łącznik świecznikowy ma jeden zacisk wejściowy L i dwa wyjścia (↑1, ↑2). Każdy klawisz załącza inną grupę żarówek.",
            "Do żyrandola biegnie przewód 4-żyłowy: dwie fazy łączone, N i PE.",
        )
        funFact = "Nazwa „świecznikowy\" pochodzi od żyrandoli świecowych, w których zapalano osobno kilka kręgów świec."
        part("sup", Kind.SUPPLY_1P, 40, 480, "Obwód oświetlenia (B10)", spec = supply(10, "B10"))
        part("sw", Kind.SWITCH_2, 250, 140, "Łącznik świecznikowy")
        part("la", Kind.LAMP, 530, 90, "Żyrandol – grupa 1")
        part("lb", Kind.LAMP, 780, 90, "Żyrandol – grupa 2")
        part("wn", Kind.WAGO3, 430, 500, "Złączka N")
        part("wpe", Kind.WAGO3, 600, 500, "Złączka PE")
        w("sup.L", "sw.L", BR, S15); w("sw.P1", "la.L", BK, S15); w("sw.P2", "lb.L", BK, S15)
        w("sup.N", "wn.1", BL, S15); w("wn.2", "la.N", BL, S15); w("wn.3", "lb.N", BL, S15)
        w("sup.PE", "wpe.1", GY, S15); w("wpe.2", "la.PE", GY, S15); w("wpe.3", "lb.PE", GY, S15)
        goal(twoKeys("sw", "la", "lb"))
        points = 140
        hint("Zasilanie ma po jednym miejscu na przewód – N i PE rozprowadź przez złączki.", "Wyjście ↑1 do grupy 1, ↑2 do grupy 2.")
    }

    private val stairs = level("2-2", 2, 2, "Łącznik schodowy") {
        subtitle = "Światło sterowane z dwóch miejsc"
        theory(
            "Łącznik schodowy (przełącznik) łączy zacisk wspólny C z wyjściem ↑1 albo ↑2.",
            "Dwa łączniki schodowe łączymy dwoma przewodami korespondencyjnymi: ↑1–↑1 i ↑2–↑2. Faza wchodzi na C pierwszego łącznika, a z C drugiego wychodzi do lampy.",
        )
        funFact = "W Anglii łącznik schodowy to „two-way switch\", a w USA – „3-way switch\" (od liczby zacisków). Ten sam aparat, dwie nazwy!"
        part("sup", Kind.SUPPLY_1P, 40, 480, "Obwód oświetlenia (B10)", spec = supply(10, "B10"))
        part("s1", Kind.SWITCH_STAIR, 180, 140, "Łącznik – parter")
        part("s2", Kind.SWITCH_STAIR, 760, 140, "Łącznik – piętro")
        part("lamp", Kind.LAMP, 460, 90, "Lampa na schodach")
        w("sup.L", "s1.C", BR, S15); w("s1.P1", "s2.P1", BK, S15); w("s1.P2", "s2.P2", BK, S15)
        w("s2.C", "lamp.L", BK, S15); w("sup.N", "lamp.N", BL, S15); w("sup.PE", "lamp.PE", GY, S15)
        goal(Goal.Toggles("lamp", listOf("s1", "s2")))
        points = 160
        hint("L → C łącznika 1. Przewody korespondencyjne: ↑1–↑1, ↑2–↑2. C łącznika 2 → lampa L.")
    }

    private val cross = level("2-3", 2, 3, "Łącznik krzyżowy") {
        subtitle = "Długi korytarz – trzy miejsca sterowania"
        theory(
            "Łącznik krzyżowy wstawia się między dwa łączniki schodowe, w przewody korespondencyjne.",
            "W jednej pozycji łączy 1–3 i 2–4 (na wprost), w drugiej 1–4 i 2–3 (na krzyż).",
        )
        funFact = "Układ schodowo-krzyżowy to w istocie bramka XOR – ta sama operacja logiczna, której używają procesory!"
        part("sup", Kind.SUPPLY_1P, 40, 480, "Obwód oświetlenia (B10)", spec = supply(10, "B10"))
        part("s1", Kind.SWITCH_STAIR, 150, 150, "Łącznik – wejście")
        part("x", Kind.SWITCH_CROSS, 440, 300, "Łącznik krzyżowy")
        part("s2", Kind.SWITCH_STAIR, 780, 150, "Łącznik – koniec")
        part("lamp", Kind.LAMP, 450, 60, "Lampy korytarza")
        w("sup.L", "s1.C", BR, S15); w("s1.P1", "x.1", BK, S15); w("s1.P2", "x.2", BK, S15)
        w("x.3", "s2.P1", BK, S15); w("x.4", "s2.P2", BK, S15)
        w("s2.C", "lamp.L", BK, S15); w("sup.N", "lamp.N", BL, S15); w("sup.PE", "lamp.PE", GY, S15)
        goal(Goal.Toggles("lamp", listOf("s1", "x", "s2")))
        points = 190
        hint("↑1 i ↑2 łącznika 1 → zaciski 1 i 2 krzyżowego. Zaciski 3 i 4 krzyżowego → ↑1 i ↑2 łącznika 2.")
    }

    private val fourPlaces = level("2-4", 2, 4, "Cztery miejsca sterowania") {
        subtitle = "Dwa łączniki krzyżowe w jednym korytarzu"
        theory(
            "Łączników krzyżowych można wstawić dowolnie wiele – zawsze między dwoma schodowymi.",
            "Przewody korespondencyjne biegną „sztafetą\": schodowy → krzyżowy → krzyżowy → schodowy.",
        )
        funFact = "Przy n miejscach sterowania potrzebujesz 2 schodowych i (n−2) krzyżowych. W hotelowym korytarzu bywa ich kilkanaście!"
        part("sup", Kind.SUPPLY_1P, 40, 480, "Obwód oświetlenia (B10)", spec = supply(10, "B10"))
        part("s1", Kind.SWITCH_STAIR, 60, 140, "Schodowy – wejście")
        part("x1", Kind.SWITCH_CROSS, 290, 300, "Krzyżowy 1")
        part("x2", Kind.SWITCH_CROSS, 560, 300, "Krzyżowy 2")
        part("s2", Kind.SWITCH_STAIR, 830, 140, "Schodowy – taras")
        part("lamp", Kind.LAMP, 440, 60, "Lampy korytarza")
        w("sup.L", "s1.C", BR, S15)
        w("s1.P1", "x1.1", BK, S15); w("s1.P2", "x1.2", BK, S15)
        w("x1.3", "x2.1", BK, S15); w("x1.4", "x2.2", BK, S15)
        w("x2.3", "s2.P1", BK, S15); w("x2.4", "s2.P2", BK, S15)
        w("s2.C", "lamp.L", BK, S15); w("sup.N", "lamp.N", BL, S15); w("sup.PE", "lamp.PE", GY, S15)
        goal(Goal.Toggles("lamp", listOf("s1", "x1", "x2", "s2")))
        points = 220
        hint("Schodowy 1 → krzyżowy 1 (zaciski 1, 2). Krzyżowy 1 (3, 4) → krzyżowy 2 (1, 2). Krzyżowy 2 (3, 4) → schodowy 2.")
    }

    private val bathroom = level("2-5", 2, 5, "Łazienka: światło i wentylator") {
        subtitle = "Łącznik świecznikowy steruje dwoma różnymi odbiornikami"
        theory(
            "Jeden klawisz – światło, drugi – wentylator. To też zastosowanie łącznika świecznikowego.",
            "Wentylator łazienkowy ma zwykle II klasę ochronności (symbol ⧈ – podwójna izolacja), więc nie ma zacisku PE.",
            "W łazience liczą się strefy: w strefie 0–1 tylko urządzenia SELV lub o wysokim stopniu IP; łączniki montuje się poza strefą 2.",
        )
        funFact = "IP44 oznacza ochronę przed ciałami stałymi > 1 mm (pierwsza cyfra) i przed bryzgami wody z każdej strony (druga cyfra)."
        part("sup", Kind.SUPPLY_1P, 40, 480, "Obwód łazienki (B10)", spec = supply(10, "B10"))
        part("sw", Kind.SWITCH_2, 250, 140, "Łącznik świecznikowy")
        part("lamp", Kind.LAMP, 520, 90, "Oprawa IP44")
        part("fan", Kind.FAN, 780, 100, "Wentylator")
        part("wn", Kind.WAGO3, 430, 500, "Złączka N")
        w("sup.L", "sw.L", BR, S15); w("sw.P1", "lamp.L", BK, S15); w("sw.P2", "fan.L", BK, S15)
        w("sup.N", "wn.1", BL, S15); w("wn.2", "lamp.N", BL, S15); w("wn.3", "fan.N", BL, S15)
        w("sup.PE", "lamp.PE", GY, S15)
        goal(twoKeys("sw", "lamp", "fan"))
        points = 170
        hint("Wentylator nie ma PE – to urządzenie II klasy. PE prowadzisz tylko do oprawy.")
    }

    private val stairsTwoLamps = level("2-6", 2, 6, "Klatka schodowa – dwie lampy") {
        subtitle = "Układ schodowy z odbiornikami równoległymi"
        theory(
            "Łączniki sterują fazą, a lampy za nimi łączymy równolegle – tak jak przy jednym łączniku.",
            "Wyjście C drugiego łącznika zasila pierwszą lampę, a z niej przelotowo drugą.",
        )
        funFact = "W blokach często zamiast łączników schodowych stosuje się przyciski i automat schodowy, który gasi światło po np. 2 minutach."
        part("sup", Kind.SUPPLY_1P, 40, 480, "Obwód oświetlenia (B10)", spec = supply(10, "B10"))
        part("s1", Kind.SWITCH_STAIR, 150, 150, "Łącznik – dół")
        part("s2", Kind.SWITCH_STAIR, 780, 150, "Łącznik – góra")
        part("l1", Kind.LAMP, 340, 60, "Lampa – półpiętro")
        part("l2", Kind.LAMP, 560, 60, "Lampa – piętro")
        part("wn", Kind.WAGO3, 400, 500, "Złączka N")
        part("wpe", Kind.WAGO3, 560, 500, "Złączka PE")
        w("sup.L", "s1.C", BR, S15); w("s1.P1", "s2.P1", BK, S15); w("s1.P2", "s2.P2", BK, S15)
        w("s2.C", "l2.L", BK, S15); w("l2.L", "l1.L", BK, S15)
        w("sup.N", "wn.1", BL, S15); w("wn.2", "l1.N", BL, S15); w("wn.3", "l2.N", BL, S15)
        w("sup.PE", "wpe.1", GY, S15); w("wpe.2", "l1.PE", GY, S15); w("wpe.3", "l2.PE", GY, S15)
        goal(Goal.Toggles("l1", listOf("s1", "s2")), Goal.Toggles("l2", listOf("s1", "s2")))
        points = 190
        hint("Najpierw zrób zwykły układ schodowy do jednej lampy, potem dołóż drugą równolegle.")
    }

    private val hallway = level("2-7", 2, 7, "Przedpokój: schody i gniazdo") {
        subtitle = "Dwa obwody w jednym pomieszczeniu"
        theory(
            "Układ schodowy dla światła (B10, 1,5 mm²) i osobny obwód gniazda (B16, 2,5 mm²).",
            "Nie łącz przewodów N dwóch obwodów – przy ochronie RCD każdy obwód musi mieć „swój\" N.",
        )
        funFact = "Łącznik schodowy możesz rozpoznać po tym, że nie ma na nim oznaczeń „0\" i „I\" – obie pozycje są równoprawne."
        part("supL", Kind.SUPPLY_1P, 40, 480, "Obwód oświetlenia (B10)", spec = supply(10, "B10"))
        part("supG", Kind.SUPPLY_1P, 620, 480, "Obwód gniazd (B16)", spec = supply(16, "B16"))
        part("s1", Kind.SWITCH_STAIR, 140, 150, "Łącznik – drzwi")
        part("s2", Kind.SWITCH_STAIR, 520, 150, "Łącznik – salon")
        part("lamp", Kind.LAMP, 310, 40, "Lampa")
        part("socket", Kind.SOCKET, 810, 150, "Gniazdo")
        w("supL.L", "s1.C", BR, S15); w("s1.P1", "s2.P1", BK, S15); w("s1.P2", "s2.P2", BK, S15)
        w("s2.C", "lamp.L", BK, S15); w("supL.N", "lamp.N", BL, S15); w("supL.PE", "lamp.PE", GY, S15)
        w("supG.L", "socket.L", BR, S25); w("supG.N", "socket.N", BL, S25); w("supG.PE", "socket.PE", GY, S25)
        goal(Goal.Toggles("lamp", listOf("s1", "s2")), Goal.Powered("socket"))
        points = 200
        hint("Światło z obwodu B10 przez dwa łączniki schodowe, gniazdo z obwodu B16.")
    }

    // ================================================================== ROZDZIAŁ 3 – SYGNALIZACJA I AUTOMATYKA

    private val bell = level("3-1", 3, 1, "Dzwonek do drzwi") {
        subtitle = "Transformator i obwód SELV"
        theory(
            "Dzwonek zasilamy bardzo niskim napięciem bezpiecznym (SELV), np. 8 V z transformatora dzwonkowego.",
            "Strona pierwotna (230 V) to L i N. Strona wtórna (8 V) jest galwanicznie oddzielona od sieci – dlatego przycisk przy drzwiach jest bezpieczny nawet w deszczu.",
            "Przycisk wpina się w obwód wtórny, szeregowo z dzwonkiem. Obwody SELV można wykonać cieńszym przewodem (0,5 mm²).",
        )
        funFact = "SELV to „Safety Extra-Low Voltage\". Granica bezpieczeństwa to 50 V AC w warunkach normalnych, a w łazience czy basenie – tylko 12–25 V."
        part("sup", Kind.SUPPLY_1P, 40, 480, "Obwód (B10)", spec = supply(10, "B10"))
        part("tr", Kind.TRANSFORMER, 240, 110, "Transformator 230/8 V")
        part("btn", Kind.BUTTON, 520, 330, "Przycisk przy drzwiach")
        part("bell", Kind.BELL, 770, 110, "Dzwonek 8 V")
        w("sup.L", "tr.L", BR, S15); w("sup.N", "tr.N", BL, S15)
        w("tr.S1", "bell.A", WH, S05); w("tr.S2", "btn.1", WH, S05); w("btn.2", "bell.B", WH, S05)
        goal(Goal.Follows("bell", "dzwonek dzwoni tylko przy wciśniętym przycisku") { it.on("btn") })
        points = 180
        hint("L i N zasilania → strona 230 V transformatora.", "8 V: jeden zacisk → dzwonek, drugi → przycisk → dzwonek.")
    }

    private val twoButtons = level("3-2", 3, 2, "Dzwonek z dwóch wejść") {
        subtitle = "Przyciski połączone równolegle"
        theory(
            "Dwa przyciski (drzwi i furtka) łączymy RÓWNOLEGLE – wciśnięcie dowolnego zamyka obwód.",
            "Połączenie szeregowe wymagałoby wciśnięcia obu naraz – tak działają np. prasy z oburęcznym sterowaniem dla bezpieczeństwa operatora.",
        )
        funFact = "Równoległe styki to logiczne OR, szeregowe – AND. Automatyka przemysłowa (PLC) do dziś rysuje programy jako „drabinki\" styków!"
        part("sup", Kind.SUPPLY_1P, 40, 480, "Obwód (B10)", spec = supply(10, "B10"))
        part("tr", Kind.TRANSFORMER, 240, 110, "Transformator 230/8 V")
        part("b1", Kind.BUTTON, 460, 330, "Przycisk – drzwi")
        part("b2", Kind.BUTTON, 650, 330, "Przycisk – furtka")
        part("bell", Kind.BELL, 800, 110, "Dzwonek 8 V")
        w("sup.L", "tr.L", BR, S15); w("sup.N", "tr.N", BL, S15)
        w("tr.S1", "bell.A", WH, S05); w("tr.S2", "b1.1", WH, S05); w("b1.1", "b2.1", WH, S05)
        w("b1.2", "bell.B", WH, S05); w("b2.2", "bell.B", WH, S05)
        goal(Goal.Follows("bell", "dzwoni po wciśnięciu dowolnego przycisku") { it.on("b1") || it.on("b2") })
        points = 200
        hint("Połącz zaciski „1\" obu przycisków ze sobą i z transformatorem, a zaciski „2\" – z dzwonkiem.")
    }

    private val pir = level("3-3", 3, 3, "Czujnik ruchu") {
        subtitle = "Automatyczne światło w garażu"
        theory(
            "Czujnik ruchu (PIR) to łącznik z własną elektroniką – dlatego potrzebuje zasilania: L i N.",
            "Wyjście L′ podaje fazę do lampy, gdy czujnik wykryje ruch.",
        )
        funFact = "PIR (Passive InfraRed) nie wysyła żadnych promieni – „widzi\" zmiany promieniowania cieplnego ciała człowieka (ok. 10 µm długości fali)."
        part("sup", Kind.SUPPLY_1P, 40, 480, "Obwód oświetlenia (B10)", spec = supply(10, "B10"))
        part("pir", Kind.MOTION_SENSOR, 280, 130, "Czujnik ruchu")
        part("lamp", Kind.LAMP, 680, 100, "Lampa garażowa")
        part("wn", Kind.WAGO3, 420, 490, "Złączka N")
        w("sup.L", "pir.L", BR, S15); w("sup.N", "wn.1", BL, S15); w("wn.2", "pir.N", BL, S15); w("wn.3", "lamp.N", BL, S15)
        w("pir.OUT", "lamp.L", BK, S15); w("sup.PE", "lamp.PE", GY, S15)
        goal(Goal.Follows("lamp", "lampa świeci przy wykryciu ruchu") { it.on("pir") })
        points = 200
        hint("Czujnik: L z zasilania, N przez złączkę. Wyjście L′ → lampa L.")
    }

    private val pirOverride = level("3-4", 3, 4, "Czujnik z wymuszeniem") {
        subtitle = "Łącznik równolegle do czujnika"
        theory(
            "Czasem chcemy włączyć światło na stałe – np. przy pracy w garażu. Łącznik łączymy RÓWNOLEGLE z wyjściem czujnika.",
            "Faza musi dotrzeć i do czujnika, i do łącznika – potrzebna złączka L.",
        )
        funFact = "Czujniki ruchu mają zwykle pokrętło LUX – działają tylko, gdy jest ciemno. Nie włączą światła w słoneczne południe."
        part("sup", Kind.SUPPLY_1P, 40, 480, "Obwód oświetlenia (B10)", spec = supply(10, "B10"))
        part("pir", Kind.MOTION_SENSOR, 230, 130, "Czujnik ruchu")
        part("sw", Kind.SWITCH_1, 460, 130, "Łącznik – wymuszenie")
        part("lamp", Kind.LAMP, 730, 100, "Lampa garażowa")
        part("wl", Kind.WAGO3, 250, 490, "Złączka L")
        part("wn", Kind.WAGO3, 420, 490, "Złączka N")
        w("sup.L", "wl.1", BR, S15); w("wl.2", "pir.L", BR, S15); w("wl.3", "sw.L", BR, S15)
        w("sup.N", "wn.1", BL, S15); w("wn.2", "pir.N", BL, S15); w("wn.3", "lamp.N", BL, S15)
        w("pir.OUT", "lamp.L", BK, S15); w("sw.P", "lamp.L", BK, S15); w("sup.PE", "lamp.PE", GY, S15)
        goal(Goal.Follows("lamp", "świeci przy ruchu LUB włączonym łączniku") { it.on("pir") || it.on("sw") })
        points = 220
        hint("Faza → złączka L → czujnik i łącznik. Wyjścia obu (L′ i ↑) → lampa L.")
    }

    private val dusk = level("3-5", 3, 5, "Ogród – czujnik zmierzchowy") {
        subtitle = "Dwie lampy włączane po zmroku (bonus)"
        theory(
            "Czujnik zmierzchowy podłącza się tak samo jak czujnik ruchu: L, N i wyjście L′.",
            "Lampy ogrodowe muszą mieć PE i odpowiedni stopień IP (min. IP44, przy gruncie IP65).",
        )
        funFact = "Czujnik zmierzchowy ma histerezę – włącza się przy innym natężeniu światła niż wyłącza, żeby lampy nie „mrugały\" o zmierzchu."
        part("sup", Kind.SUPPLY_1P, 40, 480, "Obwód ogrodu (B10)", spec = supply(10, "B10"))
        part("pir", Kind.MOTION_SENSOR, 240, 130, "Czujnik zmierzchowy")
        part("l1", Kind.LAMP, 520, 90, "Lampa – ścieżka")
        part("l2", Kind.LAMP, 780, 90, "Lampa – taras")
        part("wn", Kind.WAGO3, 420, 500, "Złączka N")
        part("wpe", Kind.WAGO3, 600, 500, "Złączka PE")
        w("sup.L", "pir.L", BR, S15); w("pir.OUT", "l1.L", BK, S15); w("l1.L", "l2.L", BK, S15)
        w("sup.N", "wn.1", BL, S15); w("wn.2", "pir.N", BL, S15); w("wn.3", "l1.N", BL, S15); w("l1.N", "l2.N", BL, S15)
        w("sup.PE", "wpe.1", GY, S15); w("wpe.2", "l1.PE", GY, S15); w("wpe.3", "l2.PE", GY, S15)
        goal(
            Goal.Follows("l1", "świeci po zmroku") { it.on("pir") },
            Goal.Follows("l2", "świeci po zmroku") { it.on("pir") },
        )
        points = 230
        unlock = 300
        hint("Jak czujnik ruchu – tylko do wyjścia L′ podłącz dwie lampy równolegle.")
    }

    private fun twoKeys(sw: String, a: String, b: String) = Goal.OneOf(
        listOf(
            listOf(Goal.Follows(a, "klawisz 1 → pierwszy odbiornik") { it.bit(sw, 0) }, Goal.Follows(b, "klawisz 2 → drugi odbiornik") { it.bit(sw, 1) }),
            listOf(Goal.Follows(a, "klawisz 2 → pierwszy odbiornik") { it.bit(sw, 1) }, Goal.Follows(b, "klawisz 1 → drugi odbiornik") { it.bit(sw, 0) }),
        ),
    )

    val all: List<Level> by lazy {
        val base = listOf(
            bulb, socket, lampSwitch, twoSockets, threeSockets, twoLamps, room,
            chandelier, stairs, cross, fourPlaces, bathroom, stairsTwoLamps, hallway,
            bell, twoButtons, pir, pirOverride, dusk,
        ) + Boards.chapter4 + Boards.chapter5
        base + Service.levels(base.associateBy { it.id })
    }

    fun byId(id: String): Level? = all.firstOrNull { it.id == id }

    /** Poziomy kampanii (bez bonusowych). */
    val campaign: List<Level> get() = all.filter { it.unlockCost == null }

    fun inChapter(ch: Int) = all.filter { it.chapter == ch }
}
