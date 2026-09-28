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
    )
}
