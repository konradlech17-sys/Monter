package pl.monter.core.level

import pl.monter.core.model.CircuitSpec
import pl.monter.core.model.DeviceSpec
import pl.monter.core.model.Kind

/** Dodatkowe poziomy rozdziałów 2–3 oraz rozdziały 7 (styczniki) i 8 (projekty). */
internal object Advanced {

    // ================================================================== uzupełnienia rozdziałów 2 i 3

    val dimmer = level("2-8", 2, 8, "Ściemniacz") {
        subtitle = "Płynna regulacja jasności – i dobór właściwej żarówki"
        theory(
            "Ściemniacz podłącza się jak zwykły łącznik: w przewód fazowy, między L a lampę.",
            "Ściemniacz fazowy (z triakiem) „wycina\" część każdej połówki sinusoidy – żarówka dostaje mniejszą moc.",
            "Nie każda żarówka LED to wytrzyma! Zwykła LED będzie migać, brzęczeć albo szybko się zepsuje. Szukaj oznaczenia „dimmable\" / „ściemnialna\".",
        )
        funFact = "Ściemniacz przełącza prąd 100 razy na sekundę (2 × 50 Hz). Oko nie widzi migotania, ale tanie LED-y z prostym zasilaczem potrafią je pokazać w kamerze telefonu."
        part("sup", Kind.SUPPLY_1P, 40, 480, "Obwód oświetlenia (B10)", spec = supply(10, "B10"))
        part("dim", Kind.DIMMER, 300, 150, "Ściemniacz")
        part(
            "lamp", Kind.LAMP, 620, 100, "Lampa salonu",
            options = listOf(
                DeviceSpec("led_dim", "LED E27 ściemnialna 8 W"), DeviceSpec("led", "LED E27 zwykła 8 W"),
                DeviceSpec("cfl", "Świetlówka kompaktowa 11 W"),
            ),
        )
        w("sup.L", "dim.L", BR, S15); w("dim.P", "lamp.L", BK, S15); w("sup.N", "lamp.N", BL, S15); w("sup.PE", "lamp.PE", GY, S15)
        choose("lamp", "led_dim")
        rule(
            ChoiceRule(
                "lamp", setOf("led_dim"),
                mapOf(
                    "led" to "Zwykła LED ma zasilacz nieprzystosowany do ściemniania – będzie migać i brzęczeć.",
                    "cfl" to "Świetlówek kompaktowych nie ściemnia się zwykłym ściemniaczem – elektronika statecznika się uszkodzi.",
                ),
            ),
        )
        goal(Goal.Follows("lamp", "świeci, gdy ściemniacz jest włączony") { it.on("dim") })
        points = 170
        hint("Połącz jak zwykły łącznik, a potem dotknij lampy i wybierz żarówkę ściemnialną.")
    }

    val intercom = level("3-6", 3, 6, "Domofon: dzwonek i furtka") {
        subtitle = "Dwa odbiorniki SELV, dwa przyciski"
        theory(
            "Elektrozaczep w furtce też zasila się napięciem bezpiecznym z transformatora.",
            "Każdy odbiornik ma swój przycisk. Wspólny przewód z transformatora może zasilać oba (zacisk przyjmie 2 przewody).",
        )
        funFact = "Elektrozaczep ma elektromagnes, który po podaniu napięcia zwalnia zapadkę. Charakterystyczne „brzęczenie\" przy otwieraniu to drgania zwory w rytm prądu przemiennego."
        part("sup", Kind.SUPPLY_1P, 40, 480, "Obwód (B10)", spec = supply(10, "B10"))
        part("tr", Kind.TRANSFORMER, 220, 110, "Transformator 230/8 V")
        part("b1", Kind.BUTTON, 430, 330, "Przycisk – dzwonek")
        part("b2", Kind.BUTTON, 600, 330, "Przycisk – furtka")
        part("bell", Kind.BELL, 620, 90, "Dzwonek")
        part("strike", Kind.STRIKE, 820, 110, "Elektrozaczep")
        w("sup.L", "tr.L", BR, S15); w("sup.N", "tr.N", BL, S15)
        w("tr.S1", "bell.A", WH, S05); w("tr.S1", "strike.A", WH, S05)
        w("tr.S2", "b1.1", WH, S05); w("b1.1", "b2.1", WH, S05)
        w("b1.2", "bell.B", WH, S05); w("b2.2", "strike.B", WH, S05)
        goal(
            Goal.Follows("bell", "dzwoni tylko przy przycisku dzwonka") { it.on("b1") },
            Goal.Follows("strike", "otwiera tylko przy przycisku furtki") { it.on("b2") },
        )
        points = 220
        hint("Zacisk 8V → dzwonek i elektrozaczep. Drugi zacisk 8V → oba przyciski. Każdy przycisk → swój odbiornik.")
    }

    // ================================================================== ROZDZIAŁ 7 – STYCZNIKI

    private val contactorLight = level("7-1", 7, 1, "Stycznik – pierwsze kroki") {
        subtitle = "Obwód sterowania i obwód główny"
        theory(
            "Stycznik to przekaźnik dużej mocy: napięcie na cewce A1–A2 zamyka styki główne 1-2, 3-4, 5-6.",
            "Obwód sterowania (łącznik → A1, A2 → N) płynie małym prądem. Obwód główny (przez styki) może zasilać duże odbiorniki.",
            "Faza musi trafić i do łącznika, i na styk 1 – potrzebna złączka L.",
        )
        funFact = "Stycznik „klika\" przy załączaniu – to zwora przyciągana przez elektromagnes. W dużych stycznikach słychać to wyraźnie nawet przez drzwi rozdzielni."
        part("sup", Kind.SUPPLY_1P, 40, 480, "Obwód hali (B10)", spec = supply(10, "B10"))
        part("sw", Kind.SWITCH_1, 230, 140, "Łącznik sterujący")
        part("k", Kind.CONTACTOR, 480, 150, "Stycznik")
        part("lamp", Kind.LAMP, 760, 100, "Naświetlacz hali")
        part("wl", Kind.WAGO3, 240, 490, "Złączka L")
        part("wn", Kind.WAGO3, 400, 490, "Złączka N")
        w("sup.L", "wl.1", BR, S15); w("wl.2", "sw.L", BR, S15); w("wl.3", "k.1", BR, S15)
        w("sw.P", "k.A1", BK, S15); w("k.2", "lamp.L", BK, S15)
        w("sup.N", "wn.1", BL, S15); w("wn.2", "k.A2", BL, S15); w("wn.3", "lamp.N", BL, S15)
        w("sup.PE", "lamp.PE", GY, S15)
        goal(Goal.Follows("lamp", "świeci, gdy łącznik steruje cewką") { it.on("sw") })
        points = 230
        hint("Sterowanie: L → łącznik → A1, A2 → N. Obwód główny: L → styk 1, styk 2 → lampa.")
    }

    private val boiler = level("7-2", 7, 2, "Bojler w taryfie nocnej") {
        subtitle = "Zegar steruje stycznikiem, stycznik – grzałką"
        theory(
            "W taryfie G12 prąd jest tańszy w nocy i w części dnia. Zegar sterujący włącza bojler tylko w tanich godzinach.",
            "Styki zegara są słabe (kilka amperów), więc zegar steruje cewką stycznika, a grzałkę 2–3 kW zasila stycznik.",
            "Cały obwód bojlera: B16 i przewód 2,5 mm².",
        )
        funFact = "Podgrzanie 80 litrów wody o 40 °C wymaga ok. 3,7 kWh energii – tyle, ile laptop zużywa przez tydzień pracy."
        part("sup", Kind.SUPPLY_1P, 40, 480, "Obwód bojlera (B16)", spec = supply(16, "B16"))
        part("timer", Kind.MOTION_SENSOR, 220, 130, "Zegar sterujący G12")
        part("k", Kind.CONTACTOR, 440, 150, "Stycznik")
        part("boiler", Kind.BOILER, 760, 120, "Bojler 80 l")
        part("wl", Kind.WAGO3, 230, 490, "Złączka L")
        part("wn", Kind.WAGO5, 390, 490, "Złączka N")
        w("sup.L", "wl.1", BR, S25); w("wl.2", "timer.L", BR, S25); w("wl.3", "k.1", BR, S25)
        w("timer.OUT", "k.A1", BK, S25); w("k.2", "boiler.L", BK, S25)
        w("sup.N", "wn.1", BL, S25); w("wn.2", "timer.N", BL, S25); w("wn.3", "k.A2", BL, S25); w("wn.4", "boiler.N", BL, S25)
        w("sup.PE", "boiler.PE", GY, S25)
        goal(Goal.Follows("boiler", "grzeje tylko w taniej taryfie") { it.on("timer") })
        points = 250
        hint("Zegar: L i N. Wyjście zegara → A1 stycznika. Styk 1 z fazy, styk 2 → bojler.")
    }

    private val motorContactor = level("7-3", 7, 3, "Silnik ze stycznikiem") {
        subtitle = "Załączanie silnika trójfazowego łącznikiem"
        theory(
            "Silnika 3-fazowego nie włączymy zwykłym łącznikiem – robi to stycznik 3P.",
            "Cewkę 230 V zasilamy z jednej fazy (za wyłącznikiem silnika) i przewodu N.",
            "Przewody sterowania na fazie L1 też powinny mieć kolor tej fazy.",
        )
        funFact = "Układ „start-stop\" z podtrzymaniem wykorzystuje styk pomocniczy stycznika – po puszczeniu przycisku START stycznik sam trzyma swoją cewkę pod napięciem."
        threePhase = true
        part("sup", Kind.SUPPLY_3P, 40, 20, "Zasilanie 3×400 V (wkładki 25 A)", spec = supply(25, "25 A gG"))
        part("mcb", Kind.MCB_3P, 60, 180, "C10 3P", spec = mcb(10, 'C'))
        part("k", Kind.CONTACTOR, 260, 180, "Stycznik")
        part("sw", Kind.SWITCH_1, 460, 170, "Start / stop")
        part("motor", Kind.MOTOR, 680, 420, "Wentylator hali 3~", circuit = CircuitSpec("Wentylator", 10, 16, needsRcd = false))
        w("sup.L1", "mcb.1", BR, S4); w("sup.L2", "mcb.3", BK, S4); w("sup.L3", "mcb.5", GR, S4)
        w("mcb.2", "k.1", BR, S15); w("mcb.4", "k.3", BK, S15); w("mcb.6", "k.5", GR, S15)
        w("mcb.2", "sw.L", BR, S15); w("sw.P", "k.A1", BR, S15); w("k.A2", "sup.N", BL, S15)
        w("k.2", "motor.L1", BR, S15); w("k.4", "motor.L2", BK, S15); w("k.6", "motor.L3", GR, S15)
        w("sup.PE", "motor.PE", GY, S15)
        goal(Goal.Follows("motor", "kręci się po włączeniu łącznika") { it.on("sw") })
        points = 280
        hint("Wyłącznik → styki 1, 3, 5. Z zacisku 2 wyłącznika faza do łącznika → A1. A2 → N zasilania.")
    }

    private val motorTwoPlaces = level("7-4", 7, 4, "Wentylator dachowy z dwóch miejsc") {
        subtitle = "Łączniki schodowe w obwodzie sterowania"
        theory(
            "Obwód sterowania to „zwykła\" instalacja – możesz w nim użyć łączników schodowych.",
            "Łączniki sterują tylko cewką, więc nawet silnik 10 kW włączysz z dwóch miejsc cienkim przewodem sterowniczym.",
        )
        funFact = "W automatyce przemysłowej obwody sterowania często mają 24 V – bezpieczniejsze dla operatora przy panelu."
        threePhase = true
        part("sup", Kind.SUPPLY_3P, 40, 20, "Zasilanie 3×400 V (wkładki 25 A)", spec = supply(25, "25 A gG"))
        part("mcb", Kind.MCB_3P, 60, 180, "C10 3P", spec = mcb(10, 'C'))
        part("k", Kind.CONTACTOR, 260, 180, "Stycznik")
        part("s1", Kind.SWITCH_STAIR, 440, 160, "Łącznik – hala")
        part("s2", Kind.SWITCH_STAIR, 600, 160, "Łącznik – biuro")
        part("motor", Kind.MOTOR, 700, 420, "Wentylator dachowy 3~", circuit = CircuitSpec("Wentylator", 10, 16, needsRcd = false))
        w("sup.L1", "mcb.1", BR, S4); w("sup.L2", "mcb.3", BK, S4); w("sup.L3", "mcb.5", GR, S4)
        w("mcb.2", "k.1", BR, S15); w("mcb.4", "k.3", BK, S15); w("mcb.6", "k.5", GR, S15)
        w("mcb.2", "s1.C", BR, S15); w("s1.P1", "s2.P1", BR, S15); w("s1.P2", "s2.P2", BR, S15); w("s2.C", "k.A1", BR, S15)
        w("k.A2", "sup.N", BL, S15)
        w("k.2", "motor.L1", BR, S15); w("k.4", "motor.L2", BK, S15); w("k.6", "motor.L3", GR, S15)
        w("sup.PE", "motor.PE", GY, S15)
        goal(Goal.Toggles("motor", listOf("s1", "s2")))
        points = 300
        hint("Zrób układ schodowy, ale zamiast lampy – cewka A1 stycznika.")
    }

    private val wallbox = level("7-5", 7, 5, "Ładowarka samochodu elektrycznego") {
        subtitle = "Wallbox 11 kW: dobór RCD i wyłącznika"
        theory(
            "Wallbox 11 kW pobiera ok. 16 A na fazę. Obwód: wyłącznik 3P B16–B20 i przewód 5×4 mm² (kabel bywa długi – liczymy spadek napięcia).",
            "Samochód może „wpuścić\" do instalacji prąd upływowy stały. Zwykły RCD typu A wtedy „ślepnie\". Ładowarki mają wbudowany czujnik DC 6 mA – wtedy wystarczy RCD typu A, inaczej potrzebny jest typ B.",
        )
        funFact = "11 kW przez 8 godzin nocnego ładowania to ok. 88 kWh – wystarczy na ponad 500 km jazdy elektrykiem."
        threePhase = true
        part("sup", Kind.SUPPLY_3P, 40, 20, "Złącze 3×400 V (wkładki 32 A)", spec = supply(32, "32 A gG"))
        part("main", Kind.MAIN_SWITCH_4P, 40, 160, "Rozłącznik 4P 63 A", spec = mainSwitch(63))
        part("rcd", Kind.RCD_4P, 250, 160, "RCD – wybierz", options = listOf(rcd(40, 30, "A"), rcd(40, 30, "AC"), rcd(40, 300, "A"), rcd(25, 30, "A")))
        part("mcb", Kind.MCB_3P, 460, 160, "Wybierz", options = listOf(mcb(20), mcb(32), mcb(10), mcb(40, 'C')))
        part("pebar", Kind.PE_BAR, 40, 360, "Szyna PE")
        part("ev", Kind.WALLBOX, 560, 440, "Wallbox 11 kW", circuit = CircuitSpec("Ładowarka EV", 16, 20, needsRcd = true))
        pre("sup.L1", "main.1", BR, S10); pre("sup.L2", "main.3", BK, S10); pre("sup.L3", "main.5", GR, S10); pre("sup.N", "main.N", BL, S10)
        pre("sup.PE", "pebar.1", GY, S10)
        w("main.2", "rcd.1", BR, S6); w("main.4", "rcd.3", BK, S6); w("main.6", "rcd.5", GR, S6); w("main.N'", "rcd.N", BL, S6)
        w("rcd.2", "mcb.1", BR, S6); w("rcd.4", "mcb.3", BK, S6); w("rcd.6", "mcb.5", GR, S6)
        w("mcb.2", "ev.L1", BR, S4); w("mcb.4", "ev.L2", BK, S4); w("mcb.6", "ev.L3", GR, S4)
        w("rcd.N'", "ev.N", BL, S4); w("pebar.2", "ev.PE", GY, S4)
        choose("rcd", "rcd40_30_A"); choose("mcb", "B20")
        goal(Goal.Powered("ev"))
        points = 320
        hint("RCD: 30 mA typ A, prąd znamionowy ≥ 32 A. Wyłącznik B20 3P i przewody 4 mm².")
    }

    private val hall = level("7-6", 7, 6, "Hala: światło na trzech fazach") {
        subtitle = "Stycznik 3P załącza lampy rozłożone na L1, L2, L3"
        theory(
            "Duże oświetlenie rozkładamy na trzy fazy – obciążenie jest równe, a stycznik 3P włącza wszystko jednym łącznikiem.",
            "Każda lampa ma N i PE ze złączek.",
        )
        funFact = "Lampy na różnych fazach ograniczają efekt stroboskopowy – przy migotaniu z jednej fazy wirująca tarcza piły mogłaby wyglądać, jakby stała!"
        threePhase = true
        width = 1200f; height = 640f
        part("sup", Kind.SUPPLY_3P, 40, 20, "Obwód oświetlenia hali (B10 3P)", spec = supply(10, "B10 3P"))
        part("sw", Kind.SWITCH_1, 280, 160, "Łącznik hali")
        part("k", Kind.CONTACTOR, 480, 180, "Stycznik")
        part("l1", Kind.LAMP, 660, 40, "Lampa – L1")
        part("l2", Kind.LAMP, 820, 40, "Lampa – L2")
        part("l3", Kind.LAMP, 980, 40, "Lampa – L3")
        part("w1", Kind.WAGO3, 300, 540, "Złączka L1")
        part("wn", Kind.WAGO5, 440, 540, "Złączka N")
        part("wpe", Kind.WAGO5, 600, 540, "Złączka PE")
        w("sup.L1", "w1.1", BR, S15); w("w1.2", "sw.L", BR, S15); w("w1.3", "k.1", BR, S15)
        w("sup.L2", "k.3", BK, S15); w("sup.L3", "k.5", GR, S15)
        w("sw.P", "k.A1", BR, S15)
        w("k.2", "l1.L", BR, S15); w("k.4", "l2.L", BK, S15); w("k.6", "l3.L", GR, S15)
        w("sup.N", "wn.1", BL, S15); w("wn.2", "k.A2", BL, S15)
        w("wn.3", "l1.N", BL, S15); w("wn.4", "l2.N", BL, S15); w("wn.5", "l3.N", BL, S15)
        w("sup.PE", "wpe.1", GY, S15); w("wpe.2", "l1.PE", GY, S15); w("wpe.3", "l2.PE", GY, S15); w("wpe.4", "l3.PE", GY, S15)
        for (l in listOf("l1", "l2", "l3")) goal(Goal.Follows(l, "świeci po włączeniu łącznika") { it.on("sw") })
        points = 300
        hint("L1 przez złączkę do łącznika i styku 1. L2 i L3 prosto na styki 3 i 5. Każdy styk wyjściowy → inna lampa.")
    }

    val chapter7 = listOf(contactorLight, boiler, motorContactor, motorTwoPlaces, wallbox, hall)

    // ================================================================== ROZDZIAŁ 8 – PROJEKTY

    private val kitchen = level("8-1", 8, 1, "Kuchnia") {
        subtitle = "Światło i okap na świeczniku, trzy gniazda nad blatem"
        theory(
            "Kuchnia to najwięcej odbiorników dużej mocy – gniazda nad blatem dostają osobny obwód B16.",
            "Okap i oświetlenie sterujemy łącznikiem świecznikowym z obwodu oświetlenia.",
        )
        funFact = "Czajnik (2 kW) i toster (1 kW) na jednym obwodzie to już 13 A. Dołóż ekspres do kawy – i B16 zadziała. Dlatego w kuchni robi się nawet 2–3 obwody gniazd!"
        width = 1200f; height = 640f
        part("supL", Kind.SUPPLY_1P, 40, 500, "Oświetlenie (B10)", spec = supply(10, "B10"))
        part("supG", Kind.SUPPLY_1P, 1000, 500, "Gniazda kuchnia (B16)", spec = supply(16, "B16"))
        part("sw", Kind.SWITCH_2, 60, 150, "Łącznik: światło / okap")
        part("lamp", Kind.LAMP, 260, 60, "Oświetlenie")
        part("hood", Kind.FAN, 440, 70, "Okap")
        part("wn", Kind.WAGO3, 240, 510, "Złączka N")
        part("s1", Kind.SOCKET, 620, 150, "Gniazdo – czajnik")
        part("s2", Kind.SOCKET, 770, 150, "Gniazdo – toster")
        part("s3", Kind.SOCKET, 920, 150, "Gniazdo – ekspres")
        part("wl", Kind.WAGO5, 620, 420, "Złączka L")
        part("wgn", Kind.WAGO5, 760, 420, "Złączka N")
        part("wpe", Kind.WAGO5, 900, 420, "Złączka PE")
        w("supL.L", "sw.L", BR, S15); w("sw.P1", "lamp.L", BK, S15); w("sw.P2", "hood.L", BK, S15)
        w("supL.N", "wn.1", BL, S15); w("wn.2", "lamp.N", BL, S15); w("wn.3", "hood.N", BL, S15); w("supL.PE", "lamp.PE", GY, S15)
        w("supG.L", "wl.1", BR, S25); w("supG.N", "wgn.1", BL, S25); w("supG.PE", "wpe.1", GY, S25)
        for ((i, s) in listOf("s1", "s2", "s3").withIndex()) {
            w("wl.${i + 2}", "$s.L", BR, S25); w("wgn.${i + 2}", "$s.N", BL, S25); w("wpe.${i + 2}", "$s.PE", GY, S25)
        }
        goal(twoKeys("sw", "lamp", "hood"), Goal.Powered("s1"), Goal.Powered("s2"), Goal.Powered("s3"))
        points = 300
        hint("Lewa strona: świecznikowy z obwodu B10. Prawa: trzy gniazda z obwodu B16 przez złączki.")
    }

    private val bedroom = level("8-2", 8, 2, "Sypialnia") {
        subtitle = "Światło z drzwi i z łóżka, gniazda przy łóżku i biurku"
        theory(
            "Lampę w sypialni wygodnie jest gasić z łóżka – to klasyczny układ schodowy.",
            "Gniazda przy łóżku i biurku łączymy przelotowo z obwodu B16.",
        )
        funFact = "Gniazda przy łóżku montuje się zwykle 60–80 cm nad podłogą, a łączniki – ok. 110 cm (tak, by łatwo sięgnąć ręką)."
        width = 1200f; height = 640f
        part("supL", Kind.SUPPLY_1P, 40, 500, "Oświetlenie (B10)", spec = supply(10, "B10"))
        part("supG", Kind.SUPPLY_1P, 860, 500, "Gniazda (B16)", spec = supply(16, "B16"))
        part("s1", Kind.SWITCH_STAIR, 60, 150, "Łącznik – drzwi")
        part("s2", Kind.SWITCH_STAIR, 500, 150, "Łącznik – łóżko")
        part("lamp", Kind.LAMP, 280, 50, "Lampa sufitowa")
        part("g1", Kind.SOCKET, 700, 150, "Gniazdo – lewa szafka")
        part("g2", Kind.SOCKET, 870, 150, "Gniazdo – prawa szafka")
        part("g3", Kind.SOCKET, 1040, 150, "Gniazdo – biurko")
        w("supL.L", "s1.C", BR, S15); w("s1.P1", "s2.P1", BK, S15); w("s1.P2", "s2.P2", BK, S15)
        w("s2.C", "lamp.L", BK, S15); w("supL.N", "lamp.N", BL, S15); w("supL.PE", "lamp.PE", GY, S15)
        w("supG.L", "g1.L", BR, S25); w("supG.N", "g1.N", BL, S25); w("supG.PE", "g1.PE", GY, S25)
        w("g1.L", "g2.L", BR, S25); w("g1.N", "g2.N", BL, S25); w("g1.PE", "g2.PE", GY, S25)
        w("g2.L", "g3.L", BR, S25); w("g2.N", "g3.N", BL, S25); w("g2.PE", "g3.PE", GY, S25)
        goal(Goal.Toggles("lamp", listOf("s1", "s2")), Goal.Powered("g1"), Goal.Powered("g2"), Goal.Powered("g3"))
        points = 300
        hint("Schodowy: drzwi ↔ łóżko. Gniazda: zasilanie → gniazdo 1 → 2 → 3.")
    }

    private val bathroomFull = level("8-3", 8, 3, "Łazienka – komplet") {
        subtitle = "Plafon, wentylator, kinkiet, gniazdo i bojler"
        theory(
            "Łazienka: oświetlenie z obwodu B10, gniazdo i bojler z obwodu B16 z RCD 30 mA.",
            "Kinkiet nad lustrem ma osobny łącznik. Plafon i wentylator – świecznikowy.",
            "Złączki: osobno L, N, PE dla każdego obwodu – nie mieszamy N różnych obwodów!",
        )
        funFact = "Strefa 0 to wnętrze wanny, strefa 1 – nad wanną do 2,25 m, strefa 2 – 60 cm dookoła. Gniazdo wolno zamontować dopiero poza strefą 2."
        width = 1200f; height = 640f
        part("supL", Kind.SUPPLY_1P, 40, 500, "Oświetlenie (B10)", spec = supply(10, "B10"))
        part("supG", Kind.SUPPLY_1P, 1000, 520, "Łazienka (B16)", spec = supply(16, "B16"))
        part("sw2", Kind.SWITCH_2, 60, 150, "Łącznik: plafon / wentylator")
        part("lamp", Kind.LAMP, 260, 60, "Plafon IP44")
        part("fan", Kind.FAN, 430, 70, "Wentylator")
        part("sw1", Kind.SWITCH_1, 600, 150, "Łącznik – lustro")
        part("mirror", Kind.LAMP, 760, 60, "Kinkiet nad lustrem")
        part("socket", Kind.SOCKET, 920, 300, "Gniazdo IP44")
        part("boiler", Kind.BOILER, 1060, 100, "Bojler")
        part("wl", Kind.WAGO3, 240, 510, "Złączka L")
        part("wn", Kind.WAGO5, 400, 510, "Złączka N")
        part("wpe", Kind.WAGO3, 560, 510, "Złączka PE")
        part("wg", Kind.WAGO3, 700, 440, "Złączka L – B16")
        part("wgn", Kind.WAGO3, 790, 440, "Złączka N – B16")
        part("wgpe", Kind.WAGO3, 880, 440, "Złączka PE – B16")
        w("supL.L", "wl.1", BR, S15); w("wl.2", "sw2.L", BR, S15); w("wl.3", "sw1.L", BR, S15)
        w("sw2.P1", "lamp.L", BK, S15); w("sw2.P2", "fan.L", BK, S15); w("sw1.P", "mirror.L", BK, S15)
        w("supL.N", "wn.1", BL, S15); w("wn.2", "lamp.N", BL, S15); w("wn.3", "fan.N", BL, S15); w("wn.4", "mirror.N", BL, S15)
        w("supL.PE", "wpe.1", GY, S15); w("wpe.2", "lamp.PE", GY, S15); w("wpe.3", "mirror.PE", GY, S15)
        w("supG.L", "wg.1", BR, S25); w("wg.2", "socket.L", BR, S25); w("wg.3", "boiler.L", BR, S25)
        w("supG.N", "wgn.1", BL, S25); w("wgn.2", "socket.N", BL, S25); w("wgn.3", "boiler.N", BL, S25)
        w("supG.PE", "wgpe.1", GY, S25); w("wgpe.2", "socket.PE", GY, S25); w("wgpe.3", "boiler.PE", GY, S25)
        goal(twoKeys("sw2", "lamp", "fan"), Goal.Follows("mirror", "świeci z łącznika przy lustrze") { it.on("sw1") }, Goal.Powered("socket"), Goal.Powered("boiler"))
        points = 340
        hint("Obwód B10: złączki L, N, PE po lewej. Obwód B16: trzy złączki po prawej – do gniazda i bojlera.")
    }

    private val garage = level("8-4", 8, 4, "Garaż z podrozdzielnicą") {
        subtitle = "Kabel z domu, własny RCD i trzy obwody"
        theory(
            "Budynek gospodarczy zasila się kablem z domu (np. ziemnym YKY 3×4 mm²) do podrozdzielnicy.",
            "Podrozdzielnica ma własny RCD i wyłączniki obwodów – awaria w garażu nie wyłączy domu.",
            "Zabezpieczenie w domu (B25) musi być większe niż w garażu – to selektywność.",
        )
        funFact = "Kabel ziemny układa się na głębokości min. 70 cm, na warstwie piasku, a 25 cm nad nim – niebieską lub czerwoną folię ostrzegawczą."
        part("sup", Kind.SUPPLY_1P, 40, 20, "Kabel z domu (B25)", spec = supply(25, "B25"))
        part("rcd", Kind.RCD_2P, 60, 150, "RCD 25 A / 30 mA A", spec = rcd(25, 30, "A"))
        part("m1", Kind.MCB_1P, 200, 150, "B10", spec = mcb(10))
        part("m2", Kind.MCB_1P, 260, 150, "B16", spec = mcb(16))
        part("m3", Kind.MCB_1P, 320, 150, "B10", spec = mcb(10))
        part("nbar", Kind.N_BAR, 60, 330, "Szyna N")
        part("pebar", Kind.PE_BAR, 420, 330, "Szyna PE")
        part("c1", Kind.CIRCUIT_1P, 40, 480, "Oświetlenie garażu", circuit = CircuitSpec("Oświetlenie", 6, 10, needsRcd = false))
        part("c2", Kind.CIRCUIT_1P, 280, 480, "Gniazda warsztatowe", circuit = CircuitSpec("Gniazda", 10, 16, needsRcd = true))
        part("c3", Kind.CIRCUIT_1P, 520, 480, "Napęd bramy", circuit = CircuitSpec("Brama", 6, 10, needsRcd = false))
        w("sup.L", "rcd.1", BR, S4); w("sup.N", "rcd.N", BL, S4); w("sup.PE", "pebar.1", GY, S4)
        w("rcd.2", "m1.1", BR, S4); w("m1.1", "m2.1", BR, S4); w("m2.1", "m3.1", BR, S4)
        w("rcd.N'", "nbar.1", BL, S4)
        w("m1.2", "c1.L", BR, S15); w("nbar.2", "c1.N", BL, S15); w("pebar.2", "c1.PE", GY, S15)
        w("m2.2", "c2.L", BR, S25); w("nbar.3", "c2.N", BL, S25); w("pebar.3", "c2.PE", GY, S25)
        w("m3.2", "c3.L", BR, S15); w("nbar.4", "c3.N", BL, S15); w("pebar.4", "c3.PE", GY, S15)
        goal(Goal.Powered("c1"), Goal.Powered("c2"), Goal.Powered("c3"))
        points = 300
        hint("Kabel → RCD (1, N). RCD → wyłączniki (mostki). N′ → szyna N, PE kabla → szyna PE.")
    }

    private val garden = level("8-5", 8, 5, "Ogród i taras") {
        subtitle = "Puszka z RCD, lampy na zmierzch i gniazdo IP44"
        theory(
            "Gniazda zewnętrzne MUSZĄ mieć ochronę RCD 30 mA – na zewnątrz łatwo o wilgoć i uszkodzenie przewodu kosiarką.",
            "W puszce ogrodowej: RCD, wyłącznik B10 dla lamp (przez czujnik zmierzchowy) i B16 dla gniazda.",
        )
        funFact = "Stopień ochrony IP44 chroni przed bryzgami, ale gniazdo przy basenie czy fontannie wymaga IP65–IP67 i klapki zamykanej także przy włożonej wtyczce."
        part("sup", Kind.SUPPLY_1P, 40, 20, "Obwód ogrodu z domu (B16)", spec = supply(16, "B16"))
        part("rcd", Kind.RCD_2P, 60, 150, "RCD 25 A / 30 mA A", spec = rcd(25, 30, "A"))
        part("m1", Kind.MCB_1P, 180, 150, "B10", spec = mcb(10))
        part("m2", Kind.MCB_1P, 240, 150, "B16", spec = mcb(16))
        part("dusk", Kind.MOTION_SENSOR, 380, 150, "Czujnik zmierzchowy")
        part("l1", Kind.LAMP, 560, 110, "Lampa – ścieżka")
        part("l2", Kind.LAMP, 720, 110, "Lampa – taras")
        part("socket", Kind.SOCKET, 880, 150, "Gniazdo IP44", circuit = CircuitSpec("Gniazdo ogrodowe", 10, 16, needsRcd = true))
        part("wn", Kind.WAGO5, 420, 500, "Złączka N")
        part("wpe", Kind.WAGO5, 600, 500, "Złączka PE")
        w("sup.L", "rcd.1", BR, S25); w("sup.N", "rcd.N", BL, S25)
        w("rcd.2", "m1.1", BR, S25); w("m1.1", "m2.1", BR, S25)
        w("m1.2", "dusk.L", BR, S15); w("dusk.OUT", "l1.L", BK, S15); w("l1.L", "l2.L", BK, S15)
        w("m2.2", "socket.L", BR, S25)
        w("rcd.N'", "wn.1", BL, S25); w("wn.2", "dusk.N", BL, S15); w("wn.3", "l1.N", BL, S15); w("wn.4", "l2.N", BL, S15); w("wn.5", "socket.N", BL, S25)
        w("sup.PE", "wpe.1", GY, S25); w("wpe.2", "l1.PE", GY, S15); w("wpe.3", "l2.PE", GY, S15); w("wpe.4", "socket.PE", GY, S25)
        goal(
            Goal.Follows("l1", "świeci po zmroku") { it.on("dusk") },
            Goal.Follows("l2", "świeci po zmroku") { it.on("dusk") },
            Goal.Powered("socket"),
        )
        points = 320
        hint("Zasilanie → RCD → B10 i B16. N z RCD (N′) przez złączkę do wszystkich odbiorników.")
    }

    private val apartment = level("8-6", 8, 6, "Mieszkanie – od rozdzielnicy do gniazdka") {
        subtitle = "Rozdzielnica i prawdziwe odbiorniki: lampy, łączniki, gniazda"
        theory(
            "Tym razem obwody nie kończą się „skrzynką\" – prowadzisz je aż do łączników, lamp i gniazd.",
            "Z wyjścia B10 zasilisz dwa łączniki (dwa miejsca w zacisku). N i PE lamp – przelotowo.",
            "Gniazda za RCD 30 mA na obwodzie B16.",
        )
        funFact = "W typowym mieszkaniu 50 m² jest ok. 8–12 obwodów i nawet 300 m przewodów ukrytych w ścianach!"
        width = 1400f; height = 780f
        part("sup", Kind.SUPPLY_1P, 40, 10, "Zasilanie z licznika (wkładka 25 A)", spec = supply(25, "25 A gG"))
        part("main", Kind.MAIN_SWITCH_2P, 40, 140, "Rozłącznik 40 A", spec = mainSwitch(40))
        part("rcd", Kind.RCD_2P, 160, 140, "RCD 40 A / 30 mA A", spec = rcd(40, 30, "A"))
        part("m1", Kind.MCB_1P, 280, 140, "B10", spec = mcb(10))
        part("m2", Kind.MCB_1P, 340, 140, "B16", spec = mcb(16))
        part("nbar", Kind.N_BAR, 40, 320, "Szyna N")
        part("pebar", Kind.PE_BAR, 360, 320, "Szyna PE")
        part("sw", Kind.SWITCH_1, 720, 100, "Łącznik – pokój")
        part("lamp", Kind.LAMP, 900, 60, "Lampa – pokój")
        part("sw2", Kind.SWITCH_1, 1080, 100, "Łącznik – kuchnia")
        part("lamp2", Kind.LAMP, 1240, 60, "Lampa – kuchnia")
        part("g1", Kind.SOCKET, 720, 420, "Gniazdo – pokój", circuit = CircuitSpec("Gniazda", 10, 16, needsRcd = true))
        part("g2", Kind.SOCKET, 900, 420, "Gniazdo – kuchnia", circuit = CircuitSpec("Gniazda", 10, 16, needsRcd = true))
        pre("sup.L", "main.1", BR, S10); pre("sup.N", "main.N", BL, S10); pre("sup.PE", "pebar.1", GY, S10)
        w("main.2", "rcd.1", BR, S4); w("main.N'", "rcd.N", BL, S4)
        w("rcd.2", "m1.1", BR, S4); w("m1.1", "m2.1", BR, S4); w("rcd.N'", "nbar.1", BL, S4)
        w("m1.2", "sw.L", BR, S15); w("m1.2", "sw2.L", BR, S15)
        w("sw.P", "lamp.L", BK, S15); w("sw2.P", "lamp2.L", BK, S15)
        w("nbar.2", "lamp.N", BL, S15); w("lamp.N", "lamp2.N", BL, S15)
        w("pebar.2", "lamp.PE", GY, S15); w("lamp.PE", "lamp2.PE", GY, S15)
        w("m2.2", "g1.L", BR, S25); w("g1.L", "g2.L", BR, S25)
        w("nbar.3", "g1.N", BL, S25); w("g1.N", "g2.N", BL, S25)
        w("pebar.3", "g1.PE", GY, S25); w("g1.PE", "g2.PE", GY, S25)
        goal(
            Goal.Follows("lamp", "świeci z łącznika w pokoju") { it.on("sw") },
            Goal.Follows("lamp2", "świeci z łącznika w kuchni") { it.on("sw2") },
            Goal.Powered("g1"), Goal.Powered("g2"),
        )
        points = 450
        hint("Rozdzielnica jak zwykle. Z B10 dwa przewody do łączników, z B16 – do pierwszego gniazda i dalej przelotowo.")
    }

    val chapter8 = listOf(kitchen, bedroom, bathroomFull, garage, garden, apartment)
}
