package pl.monter.core.level

/** Rozdział 6 – poziomy serwisowe „znajdź usterkę", budowane z poprawnych rozwiązań innych poziomów. */
internal object Service {
    fun levels(base: Map<String, Level>): List<Level> = listOf(
        fault(
            base.getValue("1-3"), "6-1", 1, "Kopnięcie przy wymianie żarówki",
            story = "Klient: „Zgasiłem światło, a przy wymianie żarówki mnie kopnęło!\"",
            why = "Pomyśl, który przewód przerywa łącznik. Tryb TEST z miernikiem pokaże, gdzie jest faza przy wyłączonym świetle.",
            hint = "Łącznik musi przerywać fazę L, a nie przewód N.",
        ) {
            replace("sup.L", "sw.L", "sup.N", "sw.L", BL)
            replace("sw.P", "lamp.L", "sw.P", "lamp.N", BL)
            replace("sup.N", "lamp.N", "sup.L", "lamp.L", BR)
        },
        fault(
            base.getValue("1-2"), "6-2", 2, "Czajnik „kopie\"",
            story = "Klient: „Metalowy czajnik lekko mrowi przy dotknięciu. Gniazdo wymieniał szwagier.\"",
            why = "Metalowa obudowa czajnika łączy się z bolcem ochronnym gniazda. Co musi być do niego podłączone?",
            hint = "Sprawdź, czy bolec ochronny (PE) gniazda jest podłączony.",
        ) { remove("sup.PE", "socket.PE") },
        fault(
            base.getValue("1-4"), "6-3", 3, "Ciepłe gniazdko",
            story = "Klient: „Gniazdo przy biurku robi się ciepłe i czuć zapach plastiku.\"",
            why = "Przewód przelotowy przewodzi prąd dalszych gniazd. Czy jego przekrój pasuje do zabezpieczenia B16?",
            hint = "Przewody między gniazdami są za cienkie (1,5 mm² przy B16).",
        ) {
            cs("s1.L", "s2.L", S15); cs("s1.N", "s2.N", S15); cs("s1.PE", "s2.PE", S15)
        },
        fault(
            base.getValue("2-2"), "6-4", 4, "Schody sterowane z jednej strony",
            story = "Klient: „Światło na schodach da się zgasić tylko z parteru. Z góry nic się nie dzieje.\"",
            why = "Sprawdź w trybie TEST wszystkie pozycje łączników. Który przewód nie jest przewodem korespondencyjnym?",
            hint = "Zacisk C łącznika na piętrze musi iść do lampy, a ↑1/↑2 – do łącznika na parterze.",
        ) {
            replace("s1.P2", "s2.P2", "s1.P2", "s2.C")
            replace("s2.C", "lamp.L", "s2.P2", "lamp.L")
        },
        fault(
            base.getValue("3-1"), "6-5", 5, "Dzwonek dzwoni bez przerwy",
            story = "Klient: „Dzwonek dzwoni cały czas, nawet gdy nikt nie naciska przycisku!\"",
            why = "Coś zwiera przycisk – szukaj zbędnego przewodu w obwodzie 8 V.",
            hint = "Jeden z przewodów omija przycisk. Przetnij go ✂️.",
        ) { add("tr.S2", "bell.B", WH, S05) },
        fault(
            base.getValue("4-1"), "6-6", 6, "RCD wybija od razu",
            story = "Klient: „Po remoncie rozdzielnicy RCD wyłącza się, gdy tylko coś włączę.\"",
            why = "RCD porównuje prąd w fazie i w N. Skąd szyna N dostaje zasilanie – sprzed czy zza wyłącznika RCD?",
            hint = "Szyna N musi być zasilana z zacisku N′ wyłącznika RCD.",
        ) { replace("rcd.N'", "nbar.1", "main.N'", "nbar.1") },
        fault(
            base.getValue("5-2"), "6-7", 7, "Pompa nie tłoczy wody",
            story = "Klient: „Po wymianie przewodu pompa hałasuje, ale woda nie leci.\"",
            why = "Silnik działa, ale… w którą stronę się kręci? Zobacz w trybie TEST.",
            hint = "Zamienione dwie fazy na zaciskach silnika – odwrócony kierunek obrotów.",
        ) {
            replace("mcb.4", "motor.L2", "mcb.4", "motor.L3")
            replace("mcb.6", "motor.L3", "mcb.6", "motor.L2")
        },
        fault(
            base.getValue("5-4"), "6-8", 8, "Łazienka: RCD i napięcie na obudowie",
            story = "Klient: „W łazience wybija RCD przy suszarce, a elektryk z miernikiem znalazł napięcie na obudowie lampy.\"",
            why = "Przewody N i PE w układzie TN-S nie mogą się łączyć za rozdzielnicą. Sprawdź, skąd wychodzą przewody obwodu łazienki.",
            hint = "PE łazienki podłączono do szyny N zamiast do szyny PE.",
        ) { replace("pebar.7", "c5.PE", "nbar.8", "c5.PE") },
        fault(
            base.getValue("1-6"), "6-9", 9, "Druga lampa nie świeci",
            story = "Klient: „W salonie świeci tylko jedna lampa. Żarówkę w drugiej wymieniłem – nic.\"",
            why = "Żarówka jest dobra. Czego brakuje drugiej lampie, żeby prąd mógł przez nią popłynąć?",
            hint = "Druga lampa nie ma podłączonego przewodu N.",
        ) { remove("wn.3", "l2.N") },
        fault(
            base.getValue("2-1"), "6-10", 10, "Żyrandol: jedna grupa martwa",
            story = "Klient: „Drugi klawisz zapala te same żarówki co pierwszy, a druga grupa nigdy się nie świeci.\"",
            why = "Sprawdź, dokąd biegną przewody z wyjść ↑1 i ↑2 łącznika.",
            hint = "Oba wyjścia łącznika podłączono do tej samej grupy żarówek.",
        ) { replace("sw.P2", "lb.L", "sw.P2", "la.L") },
        fault(
            base.getValue("3-3"), "6-11", 11, "Czujnik ruchu nie reaguje",
            story = "Klient: „Czujnik ruchu w garażu w ogóle nie włącza światła, nawet w nocy.\"",
            why = "Czujnik ma własną elektronikę. Czego potrzebuje, żeby w ogóle działać?",
            hint = "Czujnik nie ma podłączonego N – jego elektronika nie jest zasilana.",
        ) { remove("wn.2", "pir.N") },
        fault(
            base.getValue("4-3"), "6-12", 12, "Wybijają oba RCD",
            story = "Klient: „Po podłączeniu nowej łazienki przy każdym włączeniu światła wybijają OBA wyłączniki różnicowoprądowe.\"",
            why = "Faza obwodu łazienki idzie przez RCD 2. A przez który RCD wraca jej przewód N?",
            hint = "N łazienki podpięto do szyny N pierwszego RCD.",
        ) { replace("nbar2.2", "c3.N", "nbar1.4", "c3.N") },
        fault(
            base.getValue("5-3"), "6-13", 13, "Płyta indukcyjna „mrowi\"",
            story = "Klient: „Przy dotknięciu metalowej ramki płyty czuję mrowienie.\"",
            why = "Metalowa obudowa odbiornika I klasy musi być połączona z…?",
            hint = "Brak przewodu PE do płyty indukcyjnej.",
        ) { remove("pebar.2", "c.PE") },
        fault(
            base.getValue("7-3"), "6-14", 14, "Silnik nie rusza",
            story = "Klient: „Łącznik klika, ale wentylator na hali stoi. Stycznik nawet nie drgnie.\"",
            why = "Cewka stycznika potrzebuje 230 V między A1 i A2. Dokąd podłączono A2?",
            hint = "Zacisk A2 cewki podłączono do PE zamiast do N.",
        ) { replace("k.A2", "sup.N", "k.A2", "sup.PE") },
    )
}
