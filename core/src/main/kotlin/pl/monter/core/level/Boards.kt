package pl.monter.core.level

import pl.monter.core.model.CircuitSpec
import pl.monter.core.model.Kind

/** Rozdziały 4 i 5 – rozdzielnice 230 V i instalacje trójfazowe. */
internal object Boards {

    private fun light(name: String = "Oświetlenie") = CircuitSpec(name, 6, 10, needsRcd = false)
    private fun sockets(name: String = "Gniazda") = CircuitSpec(name, 10, 16, needsRcd = true)

    /** Obwody odbiorcze: przewód fazowy z wyłącznika, N z szyny, PE z szyny. */
    private fun LevelBuilder.circuit(mcb: String, c: String, nbar: String, pe: String, cs: pl.monter.core.model.CrossSection, color: pl.monter.core.model.WireColor = BR) {
        w("$mcb.2", "$c.L", color, cs); w("$nbar", "$c.N", BL, cs); w("$pe", "$c.PE", GY, cs)
    }

    // ================================================================== ROZDZIAŁ 4

    private val flat = level("4-1", 4, 1, "Rozdzielnica mieszkaniowa") {
        subtitle = "Rozłącznik, RCD, wyłączniki nadprądowe, szyny N i PE"
        theory(
            "Kolejność aparatów: rozłącznik główny → wyłącznik różnicowoprądowy (RCD) → wyłączniki nadprądowe obwodów.",
            "Przewód N każdego obwodu chronionego przez RCD MUSI przejść przez ten RCD (szyna N za RCD). Inaczej RCD „zobaczy\" różnicę prądów i wyzwoli.",
            "PE nigdy nie przechodzi przez żaden aparat – idzie bezpośrednio na szynę PE.",
            "Przewody między aparatami chroni tylko wkładka przedlicznikowa (25 A), więc muszą mieć min. 4 mm². Obwody: B10 → 1,5 mm², B16 → 2,5 mm².",
        )
        funFact = "RCD porównuje prąd „wpływający\" fazą z „wypływającym\" przewodem N. Już 30 mA różnicy wyłącza zasilanie w ok. 20–40 ms."
        part("sup", Kind.SUPPLY_1P, 40, 20, "Zasilanie z licznika (wkładka 25 A)", spec = supply(25, "25 A gG"))
        part("main", Kind.MAIN_SWITCH_2P, 60, 150, "Rozłącznik główny 40 A", spec = mainSwitch(40))
        part("rcd", Kind.RCD_2P, 200, 150, "RCD 40 A / 30 mA typ A", spec = rcd(40, 30, "A"))
        part("m1", Kind.MCB_1P, 360, 150, "B10", spec = mcb(10))
        part("m2", Kind.MCB_1P, 430, 150, "B16", spec = mcb(16))
        part("m3", Kind.MCB_1P, 500, 150, "B16", spec = mcb(16))
        part("nbar", Kind.N_BAR, 60, 330, "Szyna N")
        part("pebar", Kind.PE_BAR, 420, 330, "Szyna PE")
        part("c1", Kind.CIRCUIT_1P, 40, 480, "Oświetlenie", circuit = light())
        part("c2", Kind.CIRCUIT_1P, 280, 480, "Gniazda – pokoje", circuit = sockets())
        part("c3", Kind.CIRCUIT_1P, 520, 480, "Łazienka", circuit = sockets("Łazienka"))
        pre("sup.L", "main.1", BR, S10); pre("sup.N", "main.N", BL, S10); pre("sup.PE", "pebar.1", GY, S10)
        w("main.2", "rcd.1", BR, S4); w("main.N'", "rcd.N", BL, S4)
        w("rcd.2", "m1.1", BR, S4); w("m1.1", "m2.1", BR, S4); w("m2.1", "m3.1", BR, S4)
        w("rcd.N'", "nbar.1", BL, S4)
        circuit("m1", "c1", "nbar.2", "pebar.2", S15)
        circuit("m2", "c2", "nbar.3", "pebar.3", S25)
        circuit("m3", "c3", "nbar.4", "pebar.4", S25)
        goal(Goal.Powered("c1"), Goal.Powered("c2"), Goal.Powered("c3"))
        points = 250
        hint(
            "Rozłącznik (2, N′) → RCD (1, N). Z RCD zacisk 2 → wejście B10, a mostki przewodem do kolejnych wyłączników.",
            "RCD N′ → szyna N. Z szyny N i szyny PE prowadź przewody do każdego obwodu.",
        )
    }

    private val selection = level("4-2", 4, 2, "Dobór zabezpieczeń") {
        subtitle = "Wyłącznik do przewodu, RCD do obwodu"
        theory(
            "Wyłącznik nadprądowy chroni PRZEWÓD. Jego prąd znamionowy nie może przekroczyć obciążalności przewodu: 1,5 mm² → B10, 2,5 mm² → B16.",
            "Charakterystyka B wyzwala przy 3–5 × In (oświetlenie, gniazda), C przy 5–10 × In (silniki, duże prądy rozruchu).",
            "RCD ma dwa parametry: IΔn (czułość, dla ochrony ludzi ≤ 30 mA) i In – prąd, który może przez niego płynąć (nie mniej niż zabezpieczenie przed nim).",
        )
        funFact = "Oznaczenie B16 znaczy: charakterystyka B, 16 A. Wyłącznik wytrzyma 1,13 × In przez godzinę, a przy 1,45 × In musi wyłączyć w ciągu godziny."
        part("sup", Kind.SUPPLY_1P, 40, 20, "Zasilanie z licznika (wkładka 25 A)", spec = supply(25, "25 A gG"))
        part("main", Kind.MAIN_SWITCH_2P, 60, 150, "Rozłącznik główny 40 A", spec = mainSwitch(40))
        part("rcd", Kind.RCD_2P, 200, 150, "RCD – wybierz", options = listOf(rcd(16, 30, "A"), rcd(40, 30, "A"), rcd(40, 30, "AC"), rcd(40, 100, "A"), rcd(40, 300, "AC")))
        part("m1", Kind.MCB_1P, 360, 150, "Wybierz", options = listOf(mcb(6), mcb(10), mcb(16), mcb(20), mcb(10, 'C')))
        part("m2", Kind.MCB_1P, 430, 150, "Wybierz", options = listOf(mcb(10), mcb(16), mcb(20), mcb(25), mcb(16, 'C')))
        part("m3", Kind.MCB_1P, 500, 150, "Wybierz", options = listOf(mcb(10), mcb(16), mcb(20), mcb(25), mcb(16, 'C')))
        part("nbar", Kind.N_BAR, 60, 330, "Szyna N")
        part("pebar", Kind.PE_BAR, 420, 330, "Szyna PE")
        part("c1", Kind.CIRCUIT_1P, 40, 480, "Oświetlenie (1,5 mm²)", circuit = light())
        part("c2", Kind.CIRCUIT_1P, 280, 480, "Gniazda kuchnia (2,5 mm²)", circuit = CircuitSpec("Gniazda kuchnia", 16, 16, needsRcd = true))
        part("c3", Kind.CIRCUIT_1P, 520, 480, "Pralka (2,5 mm²)", circuit = CircuitSpec("Pralka", 16, 16, needsRcd = true))
        pre("sup.L", "main.1", BR, S10); pre("sup.N", "main.N", BL, S10); pre("sup.PE", "pebar.1", GY, S10)
        pre("main.2", "rcd.1", BR, S10); pre("main.N'", "rcd.N", BL, S10)
        pre("rcd.2", "m1.1", BR, S10); pre("m1.1", "m2.1", BR, S10); pre("m2.1", "m3.1", BR, S10)
        pre("rcd.N'", "nbar.1", BL, S10)
        pre("m1.2", "c1.L", BR, S15); pre("nbar.2", "c1.N", BL, S15); pre("pebar.2", "c1.PE", GY, S15)
        pre("m2.2", "c2.L", BR, S25); pre("nbar.3", "c2.N", BL, S25); pre("pebar.3", "c2.PE", GY, S25)
        pre("m3.2", "c3.L", BR, S25); pre("nbar.4", "c3.N", BL, S25); pre("pebar.4", "c3.PE", GY, S25)
        choose("rcd", "rcd40_30_A"); choose("m1", "B10"); choose("m2", "B16"); choose("m3", "B16")
        goal(Goal.Powered("c1"), Goal.Powered("c2"), Goal.Powered("c3"))
        points = 220
        hint("Dotknij każdego aparatu z napisem „Wybierz\".", "Pamiętaj: przewód 1,5 mm² → maks. 10 A, 2,5 mm² → maks. 16 A.")
    }

    private val twoRcd = level("4-3", 4, 3, "Dwa wyłączniki RCD") {
        subtitle = "Podział obwodów na grupy – każda z własną szyną N"
        theory(
            "Jeden RCD na cały dom oznacza, że każda usterka gasi wszystko. Lepiej podzielić obwody na grupy z osobnymi RCD.",
            "Każdy RCD ma SWOJĄ szynę N. Obwód zasilany fazą przez RCD 1, a N wracający przez RCD 2, wyzwoli oba!",
            "Rozłącznik główny ma w zacisku miejsce na dwa przewody – zasilisz z niego oba RCD.",
        )
        funFact = "Wyłącznik RCD sprawdza się przyciskiem TEST co kilka miesięcy – mechanizm, który nie pracuje latami, potrafi się „zapiec\"."
        width = 1200f; height = 640f
        part("sup", Kind.SUPPLY_1P, 40, 20, "Zasilanie z licznika (wkładka 25 A)", spec = supply(25, "25 A gG"))
        part("main", Kind.MAIN_SWITCH_2P, 60, 150, "Rozłącznik 40 A", spec = mainSwitch(40))
        part("rcd1", Kind.RCD_2P, 200, 150, "RCD 1 – 40 A / 30 mA A", spec = rcd(40, 30, "A"))
        part("m1", Kind.MCB_1P, 310, 150, "B10", spec = mcb(10))
        part("m2", Kind.MCB_1P, 370, 150, "B16", spec = mcb(16))
        part("rcd2", Kind.RCD_2P, 490, 150, "RCD 2 – 40 A / 30 mA A", spec = rcd(40, 30, "A"))
        part("m3", Kind.MCB_1P, 600, 150, "B16", spec = mcb(16))
        part("m4", Kind.MCB_1P, 660, 150, "B16", spec = mcb(16))
        part("nbar1", Kind.N_BAR, 60, 330, "Szyna N – RCD 1")
        part("nbar2", Kind.N_BAR, 400, 330, "Szyna N – RCD 2")
        part("pebar", Kind.PE_BAR, 760, 330, "Szyna PE")
        part("c1", Kind.CIRCUIT_1P, 40, 510, "Oświetlenie", circuit = light())
        part("c2", Kind.CIRCUIT_1P, 230, 510, "Gniazda – pokoje", circuit = sockets())
        part("c3", Kind.CIRCUIT_1P, 420, 510, "Łazienka", circuit = sockets("Łazienka"))
        part("c4", Kind.CIRCUIT_1P, 610, 510, "Kuchnia", circuit = sockets("Kuchnia"))
        pre("sup.L", "main.1", BR, S10); pre("sup.N", "main.N", BL, S10); pre("sup.PE", "pebar.1", GY, S10)
        w("main.2", "rcd1.1", BR, S4); w("main.2", "rcd2.1", BR, S4); w("main.N'", "rcd1.N", BL, S4); w("main.N'", "rcd2.N", BL, S4)
        w("rcd1.2", "m1.1", BR, S4); w("m1.1", "m2.1", BR, S4)
        w("rcd2.2", "m3.1", BR, S4); w("m3.1", "m4.1", BR, S4)
        w("rcd1.N'", "nbar1.1", BL, S4); w("rcd2.N'", "nbar2.1", BL, S4)
        circuit("m1", "c1", "nbar1.2", "pebar.2", S15)
        circuit("m2", "c2", "nbar1.3", "pebar.3", S25)
        circuit("m3", "c3", "nbar2.2", "pebar.4", S25)
        circuit("m4", "c4", "nbar2.3", "pebar.5", S25)
        goal(Goal.Powered("c1"), Goal.Powered("c2"), Goal.Powered("c3"), Goal.Powered("c4"))
        points = 300
        hint("Obwody B10 i pierwszego B16 biorą N z szyny RCD 1, a dwa ostatnie – z szyny RCD 2.")
    }

    private val stove = level("4-4", 4, 4, "Kuchenka elektryczna") {
        subtitle = "Obwód dużej mocy – dobór wyłącznika i przekroju"
        theory(
            "Kuchenka jednofazowa pobiera ok. 4–5 kW, czyli ~20 A. Potrzebuje własnego obwodu: B20 i przewód 4 mm².",
            "Wkładki 32 A przed rozdzielnicą → przewody między aparatami min. 6 mm².",
        )
        funFact = "Moc P = U × I. Przy 230 V każdy amper to ok. 230 W – czajnik 2 kW pobiera prawie 9 A!"
        part("sup", Kind.SUPPLY_1P, 40, 20, "Zasilanie (wkładka 32 A)", spec = supply(32, "32 A gG"))
        part("main", Kind.MAIN_SWITCH_2P, 60, 150, "Rozłącznik 40 A", spec = mainSwitch(40))
        part("rcd", Kind.RCD_2P, 200, 150, "RCD 40 A / 30 mA typ A", spec = rcd(40, 30, "A"))
        part("m1", Kind.MCB_1P, 360, 150, "B10", spec = mcb(10))
        part("m2", Kind.MCB_1P, 430, 150, "B16", spec = mcb(16))
        part("m3", Kind.MCB_1P, 500, 150, "Wybierz", options = listOf(mcb(16), mcb(20), mcb(32), mcb(40, 'C')))
        part("nbar", Kind.N_BAR, 60, 330, "Szyna N")
        part("pebar", Kind.PE_BAR, 420, 330, "Szyna PE")
        part("c1", Kind.CIRCUIT_1P, 40, 480, "Oświetlenie", circuit = light())
        part("c2", Kind.CIRCUIT_1P, 280, 480, "Gniazda", circuit = sockets())
        part("c3", Kind.CIRCUIT_1P, 520, 480, "Kuchenka 1F", circuit = CircuitSpec("Kuchenka", 20, 25, needsRcd = true))
        pre("sup.L", "main.1", BR, S10); pre("sup.N", "main.N", BL, S10); pre("sup.PE", "pebar.1", GY, S10)
        w("main.2", "rcd.1", BR, S6); w("main.N'", "rcd.N", BL, S6)
        w("rcd.2", "m1.1", BR, S6); w("m1.1", "m2.1", BR, S6); w("m2.1", "m3.1", BR, S6)
        w("rcd.N'", "nbar.1", BL, S6)
        circuit("m1", "c1", "nbar.2", "pebar.2", S15)
        circuit("m2", "c2", "nbar.3", "pebar.3", S25)
        circuit("m3", "c3", "nbar.4", "pebar.4", S4)
        choose("m3", "B20")
        goal(Goal.Powered("c1"), Goal.Powered("c2"), Goal.Powered("c3"))
        points = 280
        hint("Kuchenka: B20 i 4 mm² (także N i PE). Przewody między aparatami – 6 mm².")
    }

    private val surge1 = level("4-5", 4, 5, "Ochrona przepięciowa 230 V") {
        subtitle = "Ogranicznik przepięć przed RCD"
        theory(
            "Ogranicznik przepięć (SPD) odprowadza do PE udary z sieci (np. od wyładowań atmosferycznych).",
            "Montujemy go ZA rozłącznikiem, a PRZED wyłącznikiem RCD. Przewód do szyny PE – jak najkrótszy.",
        )
        funFact = "Warystor w ograniczniku przy normalnym napięciu prawie nie przewodzi, a przy przepięciu jego rezystancja spada tysiące razy w nanosekundy."
        part("sup", Kind.SUPPLY_1P, 40, 20, "Zasilanie (wkładka 25 A)", spec = supply(25, "25 A gG"))
        part("main", Kind.MAIN_SWITCH_2P, 60, 150, "Rozłącznik 40 A", spec = mainSwitch(40))
        part("spd", Kind.SPD_2P, 170, 150, "SPD T1+T2")
        part("rcd", Kind.RCD_2P, 280, 150, "RCD 40 A / 30 mA A", spec = rcd(40, 30, "A"))
        part("m1", Kind.MCB_1P, 400, 150, "B10", spec = mcb(10))
        part("m2", Kind.MCB_1P, 460, 150, "B16", spec = mcb(16))
        part("nbar", Kind.N_BAR, 60, 330, "Szyna N")
        part("pebar", Kind.PE_BAR, 420, 330, "Szyna PE")
        part("c1", Kind.CIRCUIT_1P, 40, 480, "Oświetlenie", circuit = light())
        part("c2", Kind.CIRCUIT_1P, 280, 480, "Gniazda", circuit = sockets())
        pre("sup.L", "main.1", BR, S10); pre("sup.N", "main.N", BL, S10); pre("sup.PE", "pebar.1", GY, S10)
        w("main.2", "spd.L", BR, S4); w("main.N'", "spd.N", BL, S4); w("spd.PE", "pebar.2", GY, S4)
        w("main.2", "rcd.1", BR, S4); w("main.N'", "rcd.N", BL, S4)
        w("rcd.2", "m1.1", BR, S4); w("m1.1", "m2.1", BR, S4); w("rcd.N'", "nbar.1", BL, S4)
        circuit("m1", "c1", "nbar.2", "pebar.3", S15)
        circuit("m2", "c2", "nbar.3", "pebar.4", S25)
        goal(Goal.Powered("spd"), Goal.Powered("c1"), Goal.Powered("c2"))
        points = 260
        hint("Z rozłącznika zasil i SPD (L, N), i RCD. SPD zaciskiem PE → szyna PE.")
    }

    private val house230 = level("4-6", 4, 6, "Dom 230 V – pięć obwodów") {
        subtitle = "Pełna rozdzielnica jednofazowa (bonus)"
        theory(
            "Połącz wszystko, czego się nauczyłeś: SPD, dwa RCD z osobnymi szynami N, pięć obwodów.",
            "Rozłącznik ma miejsce na 2 przewody – drugi RCD zasil mostkiem z pierwszego (zaciski 1 i N).",
        )
        funFact = "Nowe instalacje w Polsce muszą mieć ochronę przepięciową – wymaga tego norma PN-HD 60364-4-443 od 2019 r."
        width = 1200f; height = 640f
        part("sup", Kind.SUPPLY_1P, 40, 20, "Zasilanie (wkładka 32 A)", spec = supply(32, "32 A gG"))
        part("main", Kind.MAIN_SWITCH_2P, 40, 150, "Rozłącznik 40 A", spec = mainSwitch(40))
        part("spd", Kind.SPD_2P, 150, 150, "SPD T1+T2")
        part("rcd1", Kind.RCD_2P, 260, 150, "RCD 1", spec = rcd(40, 30, "A"))
        part("m1", Kind.MCB_1P, 370, 150, "B10", spec = mcb(10))
        part("m2", Kind.MCB_1P, 430, 150, "B16", spec = mcb(16))
        part("m3", Kind.MCB_1P, 490, 150, "B16", spec = mcb(16))
        part("rcd2", Kind.RCD_2P, 580, 150, "RCD 2", spec = rcd(40, 30, "A"))
        part("m4", Kind.MCB_1P, 690, 150, "B16", spec = mcb(16))
        part("m5", Kind.MCB_1P, 750, 150, "B10", spec = mcb(10))
        part("nbar1", Kind.N_BAR, 40, 330, "Szyna N – RCD 1")
        part("nbar2", Kind.N_BAR, 360, 330, "Szyna N – RCD 2")
        part("pebar", Kind.PE_BAR, 700, 330, "Szyna PE")
        part("c1", Kind.CIRCUIT_1P, 20, 510, "Oświetlenie dom", circuit = light())
        part("c2", Kind.CIRCUIT_1P, 190, 510, "Gniazda pokoje", circuit = sockets())
        part("c3", Kind.CIRCUIT_1P, 360, 510, "Gniazda kuchnia", circuit = sockets())
        part("c4", Kind.CIRCUIT_1P, 530, 510, "Łazienka", circuit = sockets("Łazienka"))
        part("c5", Kind.CIRCUIT_1P, 700, 510, "Oświetlenie ogród", circuit = light())
        pre("sup.L", "main.1", BR, S10); pre("sup.N", "main.N", BL, S10); pre("sup.PE", "pebar.1", GY, S16)
        w("main.2", "spd.L", BR, S6); w("main.N'", "spd.N", BL, S6); w("spd.PE", "pebar.2", GY, S6)
        w("main.2", "rcd1.1", BR, S6); w("main.N'", "rcd1.N", BL, S6)
        w("rcd1.1", "rcd2.1", BR, S6); w("rcd1.N", "rcd2.N", BL, S6)
        w("rcd1.2", "m1.1", BR, S6); w("m1.1", "m2.1", BR, S6); w("m2.1", "m3.1", BR, S6)
        w("rcd2.2", "m4.1", BR, S6); w("m4.1", "m5.1", BR, S6)
        w("rcd1.N'", "nbar1.1", BL, S6); w("rcd2.N'", "nbar2.1", BL, S6)
        circuit("m1", "c1", "nbar1.2", "pebar.3", S15)
        circuit("m2", "c2", "nbar1.3", "pebar.4", S25)
        circuit("m3", "c3", "nbar1.4", "pebar.5", S25)
        circuit("m4", "c4", "nbar2.2", "pebar.6", S25)
        circuit("m5", "c5", "nbar2.3", "pebar.7", S15)
        goal(Goal.Powered("spd"), Goal.Powered("c1"), Goal.Powered("c2"), Goal.Powered("c3"), Goal.Powered("c4"), Goal.Powered("c5"))
        points = 400
        unlock = 400
        hint("Kolejność: rozłącznik → SPD i RCD 1 → (mostek) RCD 2 → wyłączniki. Pilnuj szyn N!")
    }

    val chapter4 = listOf(flat, selection, twoRcd, stove, surge1, house230)

    // ================================================================== ROZDZIAŁ 5

    private val power400 = level("5-1", 5, 1, "Gniazdo siłowe 400 V") {
        subtitle = "Trzy fazy, kolejność faz i wyłącznik 3P"
        theory(
            "W sieci 3×400/230 V napięcie między fazą a N wynosi 230 V, a między dwiema fazami – 400 V (230 × √3).",
            "Gniazdo 5-biegunowe (CEE, czerwone) ma zaciski L1, L2, L3, N, PE. Zachowaj kolejność faz – od niej zależy kierunek obrotów silnika.",
            "Odbiornik trójfazowy zabezpieczamy wyłącznikiem 3P, który odłącza wszystkie fazy naraz.",
        )
        funFact = "Czerwony kolor gniazda CEE oznacza 400 V, niebieski 230 V, a żółty 110 V. Położenie bolca PE też zależy od napięcia – nie da się pomylić wtyczek!"
        threePhase = true
        part("sup", Kind.SUPPLY_3P, 40, 20, "Zasilanie 3×400 V (wkładki 25 A)", spec = supply(25, "25 A gG"))
        part("rcd", Kind.RCD_4P, 80, 190, "RCD 4P 40 A / 30 mA typ A", spec = rcd(40, 30, "A"))
        part("mcb", Kind.MCB_3P, 360, 190, "Wybierz", options = listOf(mcb(10, 'C'), mcb(16, 'C'), mcb(16), mcb(25, 'C')))
        part("gn", Kind.SOCKET_400, 680, 420, "Gniazdo 16 A 5p", circuit = CircuitSpec("Gniazdo 400 V 16 A", 16, 16, needsRcd = true))
        w("sup.L1", "rcd.1", BR, S4); w("sup.L2", "rcd.3", BK, S4); w("sup.L3", "rcd.5", GR, S4); w("sup.N", "rcd.N", BL, S4)
        w("rcd.2", "mcb.1", BR, S4); w("rcd.4", "mcb.3", BK, S4); w("rcd.6", "mcb.5", GR, S4)
        w("mcb.2", "gn.L1", BR, S25); w("mcb.4", "gn.L2", BK, S25); w("mcb.6", "gn.L3", GR, S25)
        w("rcd.N'", "gn.N", BL, S25); w("sup.PE", "gn.PE", GY, S25)
        choose("mcb", "C16")
        goal(Goal.Powered("gn"))
        points = 280
        hint("Zasilanie → RCD (1, 3, 5, N). RCD (2, 4, 6) → wyłącznik 3P → gniazdo L1, L2, L3.", "N z RCD (N′) prosto do gniazda, PE z zasilania prosto do gniazda.")
    }

    private val pump = level("5-2", 5, 2, "Silnik pompy") {
        subtitle = "Silnik trójfazowy – bez N, ale z kierunkiem obrotów"
        theory(
            "Silnik trójfazowy obciąża fazy równo, więc nie potrzebuje przewodu N. Zaciski uzwojeń: U1, V1, W1.",
            "Kolejność L1→U1, L2→V1, L3→W1 daje obroty w prawo. Zamiana dowolnych dwóch faz – obroty w lewo.",
            "Silniki mają duży prąd rozruchu (5–7 × In), dlatego stosujemy charakterystykę C.",
        )
        funFact = "Trzy przesunięte w czasie prądy tworzą w silniku wirujące pole magnetyczne – odkrył to Nikola Tesla w 1888 r. Silnik indukcyjny nie ma szczotek i może pracować dziesiątki lat."
        threePhase = true
        part("sup", Kind.SUPPLY_3P, 40, 20, "Zasilanie 3×400 V (wkładki 25 A)", spec = supply(25, "25 A gG"))
        part("mcb", Kind.MCB_3P, 300, 200, "Wybierz", options = listOf(mcb(10, 'C'), mcb(25, 'C'), mcb(6), mcb(32, 'C')))
        part("motor", Kind.MOTOR, 650, 400, "Pompa wodna 3~", circuit = CircuitSpec("Silnik pompy", 10, 16, needsRcd = false))
        w("sup.L1", "mcb.1", BR, S4); w("sup.L2", "mcb.3", BK, S4); w("sup.L3", "mcb.5", GR, S4)
        w("mcb.2", "motor.L1", BR, S15); w("mcb.4", "motor.L2", BK, S15); w("mcb.6", "motor.L3", GR, S15)
        w("sup.PE", "motor.PE", GY, S15)
        choose("mcb", "C10")
        goal(Goal.Powered("motor"))
        points = 260
        hint("Silnik: L1→U1, L2→V1, L3→W1 i PE. N nie jest potrzebny.")
    }

    private val hob = level("5-3", 5, 3, "Płyta indukcyjna") {
        subtitle = "Obwód trójfazowy z rozdzielnicy"
        theory(
            "Płyta indukcyjna 7 kW najczęściej zasilana jest z trzech faz (lub dwóch) – każde pole grzejne z innej fazy.",
            "Obwód: wyłącznik B16 3P, przewód 5×2,5 mm² (L1, L2, L3, N, PE), ochrona RCD 30 mA.",
        )
        funFact = "Płyta indukcyjna grzeje samo naczynie – zmienne pole magnetyczne indukuje w dnie garnka prądy wirowe. Dlatego garnek musi być ferromagnetyczny."
        threePhase = true
        part("sup", Kind.SUPPLY_3P, 40, 20, "Zasilanie 3×400 V (wkładki 25 A)", spec = supply(25, "25 A gG"))
        part("main", Kind.MAIN_SWITCH_4P, 40, 160, "Rozłącznik 4P 63 A", spec = mainSwitch(63))
        part("rcd", Kind.RCD_4P, 250, 160, "RCD 4P 40 A / 30 mA A", spec = rcd(40, 30, "A"))
        part("mcb", Kind.MCB_3P, 460, 160, "B16 3P", spec = mcb(16))
        part("pebar", Kind.PE_BAR, 40, 360, "Szyna PE")
        part("c", Kind.CIRCUIT_3P, 520, 490, "Płyta indukcyjna", circuit = CircuitSpec("Płyta indukcyjna", 16, 16, needsRcd = true))
        pre("sup.L1", "main.1", BR, S10); pre("sup.L2", "main.3", BK, S10); pre("sup.L3", "main.5", GR, S10); pre("sup.N", "main.N", BL, S10)
        pre("sup.PE", "pebar.1", GY, S10)
        w("main.2", "rcd.1", BR, S4); w("main.4", "rcd.3", BK, S4); w("main.6", "rcd.5", GR, S4); w("main.N'", "rcd.N", BL, S4)
        w("rcd.2", "mcb.1", BR, S4); w("rcd.4", "mcb.3", BK, S4); w("rcd.6", "mcb.5", GR, S4)
        w("mcb.2", "c.L1", BR, S25); w("mcb.4", "c.L2", BK, S25); w("mcb.6", "c.L3", GR, S25)
        w("rcd.N'", "c.N", BL, S25); w("pebar.2", "c.PE", GY, S25)
        goal(Goal.Powered("c"))
        points = 290
        hint("Rozłącznik → RCD → B16 3P → płyta. N z RCD, PE z szyny.")
    }

    private val house400 = level("5-4", 5, 4, "Rozdzielnica domowa 400 V") {
        subtitle = "Pełna rozdzielnica: ochrona przepięciowa, RCD, 6 obwodów"
        theory(
            "Kolejność: rozłącznik główny → ogranicznik przepięć T1+T2 → RCD 4P → wyłączniki nadprądowe.",
            "Obwody jednofazowe rozdziel równomiernie na L1, L2 i L3. Przewody fazowe: L1 brązowy, L2 czarny, L3 szary.",
            "Wkładki 32 A → przewody między aparatami min. 6 mm².",
        )
        funFact = "Uderzenie pioruna w linię napowietrzną może wprowadzić do instalacji udar o wartości dziesiątek kA. Ogranicznik T1 odprowadza go do ziemi w ciągu mikrosekund."
        width = 1400f; height = 780f
        threePhase = true; balance = true
        part("sup", Kind.SUPPLY_3P, 40, 10, "Złącze 3×400 V (wkładki 32 A)", spec = supply(32, "32 A gG"))
        part("main", Kind.MAIN_SWITCH_4P, 40, 150, "Rozłącznik 4P 63 A", spec = mainSwitch(63))
        part("spd", Kind.SPD_4P, 250, 150, "Ogranicznik przepięć T1+T2")
        part("rcd", Kind.RCD_4P, 460, 150, "RCD 4P 40 A / 30 mA typ A", spec = rcd(40, 30, "A"))
        part("m1", Kind.MCB_1P, 40, 370, "B10", spec = mcb(10))
        part("m2", Kind.MCB_1P, 100, 370, "B10", spec = mcb(10))
        part("m3", Kind.MCB_1P, 160, 370, "B16", spec = mcb(16))
        part("m4", Kind.MCB_1P, 220, 370, "B16", spec = mcb(16))
        part("m5", Kind.MCB_1P, 280, 370, "B16", spec = mcb(16))
        part("m6", Kind.MCB_3P, 360, 370, "B16 3P", spec = mcb(16))
        part("nbar", Kind.N_BAR, 760, 180, "Szyna N")
        part("pebar", Kind.PE_BAR, 760, 300, "Szyna PE")
        part("c1", Kind.CIRCUIT_1P, 20, 650, "Oświetlenie parter", circuit = light())
        part("c2", Kind.CIRCUIT_1P, 190, 650, "Oświetlenie piętro", circuit = light())
        part("c3", Kind.CIRCUIT_1P, 360, 650, "Gniazda kuchnia", circuit = sockets())
        part("c4", Kind.CIRCUIT_1P, 530, 650, "Gniazda salon", circuit = sockets())
        part("c5", Kind.CIRCUIT_1P, 700, 650, "Łazienka", circuit = sockets("Łazienka"))
        part("c6", Kind.CIRCUIT_3P, 900, 650, "Płyta indukcyjna 3F", circuit = CircuitSpec("Płyta indukcyjna", 16, 16, needsRcd = true))
        pre("sup.L1", "main.1", BR, S10); pre("sup.L2", "main.3", BK, S10); pre("sup.L3", "main.5", GR, S10); pre("sup.N", "main.N", BL, S10)
        pre("sup.PE", "pebar.1", GY, S16)
        w("main.2", "spd.L1", BR, S6); w("main.4", "spd.L2", BK, S6); w("main.6", "spd.L3", GR, S6); w("main.N'", "spd.N", BL, S6)
        w("spd.PE", "pebar.2", GY, S6)
        w("main.2", "rcd.1", BR, S6); w("main.4", "rcd.3", BK, S6); w("main.6", "rcd.5", GR, S6); w("main.N'", "rcd.N", BL, S6)
        w("rcd.2", "m1.1", BR, S6); w("rcd.4", "m2.1", BK, S6); w("rcd.6", "m3.1", GR, S6)
        w("rcd.2", "m6.1", BR, S6); w("rcd.4", "m6.3", BK, S6); w("rcd.6", "m6.5", GR, S6)
        w("m1.1", "m4.1", BR, S6); w("m2.1", "m5.1", BK, S6)
        w("rcd.N'", "nbar.1", BL, S6)
        circuit("m1", "c1", "nbar.2", "pebar.3", S15, BR)
        circuit("m2", "c2", "nbar.3", "pebar.4", S15, BK)
        circuit("m3", "c3", "nbar.4", "pebar.5", S25, GR)
        circuit("m4", "c4", "nbar.5", "pebar.6", S25, BR)
        circuit("m5", "c5", "nbar.6", "pebar.7", S25, BK)
        w("m6.2", "c6.L1", BR, S25); w("m6.4", "c6.L2", BK, S25); w("m6.6", "c6.L3", GR, S25)
        w("nbar.7", "c6.N", BL, S25); w("pebar.8", "c6.PE", GY, S25)
        goal(Goal.Powered("spd"), Goal.Powered("c1"), Goal.Powered("c2"), Goal.Powered("c3"), Goal.Powered("c4"), Goal.Powered("c5"), Goal.Powered("c6"))
        points = 500
        hint(
            "Z wyjść rozłącznika (2, 4, 6, N′) zasil i ogranicznik, i RCD – każdy zacisk przyjmie 2 przewody.",
            "Wyłączniki 1P mostkuj przewodem od sąsiada podłączonego do tej samej fazy.",
            "Rozdziel obwody: np. L1 – oświetlenie parter + gniazda salon, L2 – oświetlenie piętro + łazienka, L3 – kuchnia.",
        )
    }

    private val workshop = level("5-5", 5, 5, "Warsztat 400 V") {
        subtitle = "Gniazdo CEE, piła tarczowa i obwody 230 V"
        theory(
            "W warsztacie łączymy wszystko: obwody jednofazowe rozłożone na L1–L3, gniazdo siłowe i silnik piły.",
            "Wyłącznik silnika (C10 3P) zasilisz mostkami z wejść wyłącznika gniazda CEE.",
            "Silnik nie potrzebuje N, gniazdo CEE – tak.",
        )
        funFact = "Piły i szlifierki mają hamulec elektrodynamiczny – po wyłączeniu przez uzwojenie płynie chwilowo prąd stały, który zatrzymuje tarczę w kilka sekund."
        width = 1400f; height = 780f
        threePhase = true; balance = true
        part("sup", Kind.SUPPLY_3P, 40, 10, "Złącze 3×400 V (wkładki 32 A)", spec = supply(32, "32 A gG"))
        part("main", Kind.MAIN_SWITCH_4P, 40, 150, "Rozłącznik 4P 63 A", spec = mainSwitch(63))
        part("rcd", Kind.RCD_4P, 260, 150, "RCD 4P 40 A / 30 mA A", spec = rcd(40, 30, "A"))
        part("m1", Kind.MCB_1P, 40, 370, "B10", spec = mcb(10))
        part("m2", Kind.MCB_1P, 100, 370, "B16", spec = mcb(16))
        part("m3", Kind.MCB_1P, 160, 370, "B16", spec = mcb(16))
        part("m4", Kind.MCB_3P, 240, 370, "C16 3P", spec = mcb(16, 'C'))
        part("m5", Kind.MCB_3P, 400, 370, "C10 3P", spec = mcb(10, 'C'))
        part("nbar", Kind.N_BAR, 700, 180, "Szyna N")
        part("pebar", Kind.PE_BAR, 700, 300, "Szyna PE")
        part("c1", Kind.CIRCUIT_1P, 20, 640, "Oświetlenie", circuit = light())
        part("c2", Kind.CIRCUIT_1P, 190, 640, "Gniazda – stół", circuit = sockets())
        part("c3", Kind.CIRCUIT_1P, 360, 640, "Gniazda – ściana", circuit = sockets())
        part("gn", Kind.SOCKET_400, 560, 600, "Gniazdo CEE 16 A", circuit = CircuitSpec("Gniazdo CEE", 16, 16, needsRcd = true))
        part("motor", Kind.MOTOR, 780, 600, "Piła tarczowa 3~", circuit = CircuitSpec("Piła", 10, 16, needsRcd = false))
        pre("sup.L1", "main.1", BR, S10); pre("sup.L2", "main.3", BK, S10); pre("sup.L3", "main.5", GR, S10); pre("sup.N", "main.N", BL, S10)
        pre("sup.PE", "pebar.1", GY, S16)
        w("main.2", "rcd.1", BR, S6); w("main.4", "rcd.3", BK, S6); w("main.6", "rcd.5", GR, S6); w("main.N'", "rcd.N", BL, S6)
        w("rcd.2", "m1.1", BR, S6); w("rcd.4", "m2.1", BK, S6); w("rcd.6", "m3.1", GR, S6)
        w("rcd.2", "m4.1", BR, S6); w("rcd.4", "m4.3", BK, S6); w("rcd.6", "m4.5", GR, S6)
        w("m4.1", "m5.1", BR, S6); w("m4.3", "m5.3", BK, S6); w("m4.5", "m5.5", GR, S6)
        w("rcd.N'", "nbar.1", BL, S6)
        circuit("m1", "c1", "nbar.2", "pebar.2", S15, BR)
        circuit("m2", "c2", "nbar.3", "pebar.3", S25, BK)
        circuit("m3", "c3", "nbar.4", "pebar.4", S25, GR)
        w("m4.2", "gn.L1", BR, S25); w("m4.4", "gn.L2", BK, S25); w("m4.6", "gn.L3", GR, S25)
        w("nbar.5", "gn.N", BL, S25); w("pebar.5", "gn.PE", GY, S25)
        w("m5.2", "motor.L1", BR, S15); w("m5.4", "motor.L2", BK, S15); w("m5.6", "motor.L3", GR, S15)
        w("pebar.6", "motor.PE", GY, S15)
        goal(Goal.Powered("c1"), Goal.Powered("c2"), Goal.Powered("c3"), Goal.Powered("gn"), Goal.Powered("motor"))
        points = 450
        hint("Każdy obwód 1F na innej fazie. Wyłącznik piły zasil mostkami z wejść wyłącznika CEE.")
    }

    val chapter5 = listOf(power400, pump, hob, house400, workshop)
}
