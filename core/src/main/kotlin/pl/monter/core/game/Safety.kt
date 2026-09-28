package pl.monter.core.game

data class SafetyStep(val id: String, val text: String, val why: String, val correct: Boolean)

/**
 * „5 zasad bezpieczeństwa" (PN-EN 50110-1) w kolejności ich wykonywania, plus kilka
 * fałszywych kart, które gracz powinien odrzucić.
 */
object Safety {
    val steps = listOf(
        SafetyStep("off", "Wyłącz zasilanie obwodu", "Wyłączamy zabezpieczenie obwodu (lub rozłącznik), a nie tylko łącznik światła.", true),
        SafetyStep("lock", "Zabezpiecz przed ponownym załączeniem", "Blokada dźwigienki lub tabliczka „Nie załączać – pracują ludzie\". Ktoś mógłby włączyć prąd, nie wiedząc o Tobie.", true),
        SafetyStep("verify", "Sprawdź brak napięcia", "Dwubiegunowym wskaźnikiem napięcia – sprawdzonym na znanym źródle przed i po pomiarze.", true),
        SafetyStep("earth", "Uziem i zewrzyj (jeśli wymagane)", "W sieciach wysokiego napięcia obowiązkowe; w instalacji domowej zwykle pomijane, ale warto znać tę zasadę.", true),
        SafetyStep("cover", "Osłoń sąsiednie części pod napięciem", "Np. w rozdzielnicy – izolacyjną osłoną zakryj aparaty innych obwodów.", true),
    )

    val decoys = listOf(
        SafetyStep("finger", "Sprawdź napięcie palcem, szybko", "NIGDY! Nawet krótkie dotknięcie może spowodować migotanie komór serca.", false),
        SafetyStep("screwdriver", "Sprawdź „próbnikiem-śrubokrętem\"", "Neonowy śrubokręt bywa zawodny (np. przy słabym kontakcie z ciałem) – do sprawdzenia braku napięcia używamy wskaźnika dwubiegunowego.", false),
        SafetyStep("wet", "Pracuj szybko – mokre ręce nie przeszkadzają", "Wilgotna skóra ma wielokrotnie mniejszą rezystancję – prąd rażenia rośnie.", false),
    )

    fun deck(seed: Int): List<SafetyStep> = (steps + decoys.shuffled(kotlin.random.Random(seed)).take(2)).shuffled(kotlin.random.Random(seed * 31 + 7))

    /** Ocena kliknięcia karty [step], gdy poprawnie wykonano już [done] kroków. */
    fun isNext(done: Int, step: SafetyStep): Boolean = step.correct && steps.indexOfFirst { it.id == step.id } == done
}
