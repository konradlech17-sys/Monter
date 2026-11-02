package pl.bramki.engine

/**
 * Wszystkie warianty gry. Progi szczebli są ułamkami wartości OPTYMALNEJ dla danej trasy
 * (patrz [Analysis.optimalValue]) – dzięki temu każda losowa trasa jest „do przejścia” w całości,
 * a najwyższy szczebel oznacza: „wybierałeś prawie zawsze dobrze”.
 */
object Modes {
    /** Wspólne progi: 8 szczebli. Skalibrowane symulacją (patrz testy). */
    private val F = listOf(0.0, 0.03, 0.08, 0.16, 0.28, 0.45, 0.65, 0.9)

    private val commonTips = listOf(
        "Każda bramka to funkcja rosnąca: kto ma więcej przed bramką, ma więcej i po niej. " +
            "Dlatego najlepsza strategia to zawsze wybrać bramkę, która daje więcej TERAZ – nie trzeba planować naprzód.",
        "„×2” wygrywa z „+N” dopiero, gdy masz więcej niż N. Z 80 punktami „+100” daje 180, a „×2” tylko 160.",
        "„÷2” to strata 50%, ale „−N” przy małej wartości może zabrać wszystko. Gdy masz mało, procenty bolą mniej niż liczby.",
        "Po „−50%” i „+50%” NIE wracasz do punktu wyjścia: 100 → 50 → 75. Straty procentowe są droższe, niż wyglądają.",
    )

    val EPOKI = Mode(
        id = "epoki",
        name = "Epoki",
        emoji = "🏛️",
        tagline = "Z jaskini do gwiazd. Zbieraj lata postępu i zmieniaj epokę.",
        unit = "lat",
        start = 10.0,
        gateCount = 24,
        tiers = listOf(
            Tier("Epoka kamienia", "🪨", F[0], 0xFF3E2F23,
                "Epoka kamienia to ponad 99% historii człowieka – trwała ok. 3 mln lat. Wszystko, co nazywamy „cywilizacją”, zmieściło się w ostatnim 1%."),
            Tier("Epoka brązu", "🗿", F[1], 0xFF5A3E1B,
                "Brąz to stop miedzi i cyny. Cyna była rzadka, więc już 4000 lat temu istniały szlaki handlowe długości tysięcy kilometrów."),
            Tier("Starożytność", "🏛️", F[2], 0xFF8A6A2B,
                "Rzymski beton przetrwał 2000 lat, bo woda morska reaguje z popiołem wulkanicznym i… naprawia pęknięcia."),
            Tier("Średniowiecze", "🏰", F[3], 0xFF2E4A5C,
                "W średniowieczu nie wierzono, że Ziemia jest płaska – to mit z XIX wieku. Uczeni znali obliczenia Eratostenesa."),
            Tier("Renesans", "🎨", F[4], 0xFF7A3B5E,
                "Druk Gutenberga obniżył cenę książki ok. 300 razy w ciągu 50 lat. To była pierwsza „rewolucja informacyjna”."),
            Tier("Era przemysłowa", "🏭", F[5], 0xFF4F4F57,
                "Maszyna parowa Watta miała sprawność ok. 3%. Dzisiejsze elektrownie gazowe – ponad 60%."),
            Tier("Współczesność", "📱", F[6], 0xFF1F5F8B,
                "Twój telefon ma ponad 100 000 razy więcej mocy obliczeniowej niż komputer, który zaprowadził Apollo 11 na Księżyc."),
            Tier("Przyszłość", "🚀", F[7], 0xFF2B1F6B,
                "Sonda Voyager 1 leci z 1977 roku i wciąż nadaje. Sygnał z niej biegnie do Ziemi ponad 22 godziny."),
        ),
        tips = commonTips + "Epoki w grze są umowne – prawdziwe trwały od milionów lat (kamień) do kilkudziesięciu (współczesność). Postęp przyspiesza wykładniczo.",
    )

    val EWOLUCJA = Mode(
        id = "ewolucja",
        name = "Ewolucja",
        emoji = "🧬",
        tagline = "Od jednej komórki do człowieka. Zbieraj punkty DNA.",
        unit = "DNA",
        start = 10.0,
        gateCount = 24,
        tiers = listOf(
            Tier("Komórka", "🦠", F[0], 0xFF1E3A2F,
                "Twoje mitochondria mają własne DNA, bo 2 mld lat temu były osobnymi bakteriami, które „zamieszkały” w innej komórce."),
            Tier("Ryba", "🐟", F[1], 0xFF1B4E6B,
                "Masz czkawkę po rybach: to odruch z czasów, gdy nasi przodkowie zamykali skrzela przy przełykaniu wody."),
            Tier("Płaz", "🐸", F[2], 0xFF2F5E2A,
                "Palce u rąk pochodzą z promieni płetw. Zarodek ryby i człowieka na wczesnym etapie wyglądają niemal identycznie."),
            Tier("Gad", "🦎", F[3], 0xFF5C5A24,
                "Jajo z twardą skorupką to wynalazek gadów – pozwoliło rozmnażać się daleko od wody i zasiedlić suchy ląd."),
            Tier("Ssak", "🐀", F[4], 0xFF5B3F2E,
                "Kości naszego ucha środkowego (młoteczek, kowadełko) to dawne kości żuchwy gadów. Ewolucja niczego nie wyrzuca, tylko przerabia."),
            Tier("Naczelny", "🐒", F[5], 0xFF4A3A2B,
                "Widzimy kolory lepiej niż większość ssaków, bo naczelne odzyskały trzeci rodzaj czopków – przydatny do wypatrywania dojrzałych owoców."),
            Tier("Człowiek", "🧑", F[6], 0xFF2C4C7A,
                "Z szympansem dzielimy ok. 98,8% DNA. Różnica 1,2% to wciąż ok. 35 mln „liter” genomu."),
            Tier("Postczłowiek", "🤖", F[7], 0xFF3B2A6B,
                "CRISPR to „nożyczki” do DNA podpatrzone u bakterii, które w ten sposób wycinają wirusy ze swojego genomu."),
        ),
        tips = commonTips + "Ewolucja nie ma celu ani „wyższych” form – w grze szczeble są umowne. Bakterie radzą sobie świetnie od 3,5 mld lat.",
    )

    val FORTUNA = Mode(
        id = "fortuna",
        name = "Fortuna",
        emoji = "💰",
        tagline = "Procenty kontra gotówka. Kiedy +50% jest lepsze niż +500 zł?",
        unit = "zł",
        start = 100.0,
        gateCount = 24,
        percentStyle = true,
        tiers = listOf(
            Tier("Pusta kieszeń", "🪙", F[0], 0xFF3A3A3A,
                "Reguła 72: podziel 72 przez roczny procent, a dostaniesz, po ilu latach pieniądze się podwoją. 6% → ok. 12 lat."),
            Tier("Skarbonka", "🐷", F[1], 0xFF6B3A52,
                "Inflacja 5% rocznie zabiera połowę siły nabywczej w ok. 14 lat. Pieniądze „pod materacem” topnieją."),
            Tier("Portfel", "👛", F[2], 0xFF5A4A2A,
                "Procent składany: 1000 zł na 7% rocznie to po 10 latach 1967 zł, po 30 latach – 7612 zł. Czas robi więcej niż stopa."),
            Tier("Konto", "💳", F[3], 0xFF1F4F6B,
                "Strata 50% wymaga zysku 100%, żeby wrócić do zera. Dlatego unikanie dużych strat liczy się bardziej niż gonienie zysków."),
            Tier("Sejf", "🔐", F[4], 0xFF4A4A5A,
                "Karta kredytowa z RRSO 20% podwaja dług w niecałe 4 lata, jeśli nie spłacasz. Ta sama matematyka, która mnoży oszczędności, mnoży długi."),
            Tier("Rezydencja", "🏠", F[5], 0xFF2E5A3A,
                "Dywersyfikacja: 10 niezależnych inwestycji po 10% ma dużo mniejszą zmienność niż jedna – to matematyka, nie opinia."),
            Tier("Jacht", "🛥️", F[6], 0xFF1F5F8B,
                "„Średni zwrot” oszukuje: +50% i −50% to średnio 0%, ale naprawdę tracisz 25%. Liczy się średnia geometryczna."),
            Tier("Rakieta", "🚀", F[7], 0xFF2B1F6B,
                "W „Monopoly” też lepiej mnożyć niż dodawać: to gry o procencie składanym. W realnym świecie dodaj jeszcze podatki i ryzyko."),
        ),
        tips = commonTips + "„+50%” to to samo co „×1,5”. Porównaj: przy 1000 zł „+50%” daje 1500 zł, a „+400 zł” tylko 1400 zł – ale przy 500 zł wygrywa gotówka.",
    )

    val ARMIA = Mode(
        id = "armia",
        name = "Armia",
        emoji = "⚔️",
        tagline = "Zbieraj wojsko po drodze. Na mecie czeka bitwa.",
        unit = "wojów",
        start = 5.0,
        gateCount = 24,
        enemyFraction = 0.5,
        tiers = listOf(
            Tier("Samotny woj", "🧍", F[0], 0xFF3A3A2A,
                "Najmniejsza jednostka rzymskiej armii to contubernium – 8 żołnierzy, którzy dzielili jeden namiot i jedno ognisko."),
            Tier("Drużyna", "👥", F[1], 0xFF4A3A2A,
                "Rzymska centuria wbrew nazwie liczyła ok. 80 ludzi, nie 100. Nazwa została z czasów, gdy było ich więcej."),
            Tier("Oddział", "🛡️", F[2], 0xFF5A4A2A,
                "Falanga grecka działała tylko dzięki temu, że każdy osłaniał tarczą sąsiada z lewej. Pojedynczy hoplita był bezbronny."),
            Tier("Kompania", "⚔️", F[3], 0xFF2E4A5C,
                "Prawo Lanchestera: w walce na dystans siła armii rośnie z KWADRATEM liczebności. Dwa razy więcej wojska = cztery razy większa siła."),
            Tier("Batalion", "🏹", F[4], 0xFF4F2F2F,
                "Łucznik angielski w XIV w. strzelał 10–12 razy na minutę. Kusznik – 2 razy. Pod Crécy to zdecydowało o bitwie."),
            Tier("Legion", "🦅", F[5], 0xFF5A3A1B,
                "Legion to ok. 5000 ludzi, ale połowę z nich stanowili saperzy, kowale i logistyka. Rzym wygrywał drogami i mostami."),
            Tier("Horda", "🐎", F[6], 0xFF4A4A2A,
                "Mongołowie mieli po 3–5 koni na jeźdźca i zmieniali je w galopie – dlatego pokonywali 100 km dziennie."),
            Tier("Imperium", "👑", F[7], 0xFF2B1F6B,
                "Armia Aleksandra Wielkiego nigdy nie przekroczyła ok. 50 tys. ludzi. Logistyka (żywność, woda) ograniczała ją bardziej niż wróg."),
        ),
        tips = commonTips + "Wróg na mecie ma siłę równą połowie najlepszej możliwej armii. Wystarczy więc nie popełnić zbyt wielu błędów – nie musisz być idealny.",
    )

    val KOSMOS = Mode(
        id = "kosmos",
        name = "Kosmos",
        emoji = "🪐",
        tagline = "Zbieraj masę. Z pyłku wyrośnij na gwiazdę, a potem w czarną dziurę.",
        unit = "t",
        start = 10.0,
        gateCount = 24,
        tiers = listOf(
            Tier("Pyłek", "✨", F[0], 0xFF1A1A2E,
                "Każdego dnia na Ziemię spada ok. 100 ton kosmicznego pyłu. Trochę z niego ląduje na twoim parapecie."),
            Tier("Asteroida", "☄️", F[1], 0xFF2A2A3E,
                "Grawitacja zaczyna „zaokrąglać” ciało niebieskie dopiero od ok. 400 km średnicy. Mniejsze asteroidy wyglądają jak ziemniaki."),
            Tier("Księżyc", "🌙", F[2], 0xFF2E2E4A,
                "Księżyc oddala się od Ziemi o 3,8 cm rocznie – tyle samo, ile rosną paznokcie. Doba na Ziemi przez to się wydłuża."),
            Tier("Planeta", "🌍", F[3], 0xFF1F4F6B,
                "Ziemia nie jest idealną kulą: przez obrót jest „spłaszczona” o 43 km na równiku. Na biegunie ważysz o 0,5% więcej."),
            Tier("Gazowy olbrzym", "🪐", F[4], 0xFF6B4A2A,
                "Jowisz ma 318 mas Ziemi, ale gdyby dodać mu masy, nie urósłby – tylko by się ścisnął. Potrzeba ok. 80 Jowiszów, by zapalić gwiazdę."),
            Tier("Gwiazda", "☀️", F[5], 0xFF8B5A1F,
                "Słońce co sekundę zamienia 4 mln ton materii w energię (E = mc²). Mimo to starczy mu paliwa na kolejne 5 mld lat."),
            Tier("Nadolbrzym", "🌟", F[6], 0xFF8B2F2F,
                "Betelgeza jest tak duża, że na miejscu Słońca sięgałaby za orbitę Jowisza. Kiedyś wybuchnie jako supernowa widoczna w dzień."),
            Tier("Czarna dziura", "🕳️", F[7], 0xFF0B0B1A,
                "Żeby Ziemia stała się czarną dziurą, trzeba by ją ścisnąć do rozmiaru orzecha (ok. 9 mm). Masa się nie zmienia – tylko gęstość."),
        ),
        tips = commonTips + "W kosmosie liczy się masa, nie rozmiar: Jowisz jest 11 razy szerszy od Ziemi, ale 318 razy cięższy. Objętość rośnie z sześcianem średnicy.",
    )

    val ALL: List<Mode> = listOf(EPOKI, EWOLUCJA, FORTUNA, ARMIA, KOSMOS)

    fun byId(id: String): Mode = ALL.first { it.id == id }
}
