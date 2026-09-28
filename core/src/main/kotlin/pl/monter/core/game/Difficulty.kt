package pl.monter.core.game

import kotlinx.serialization.Serializable

@Serializable
enum class Difficulty(
    val title: String,
    val description: String,
    val multiplier: Double,
    /** Kolorowe podpowiedzi ról zacisków i automatyczny dobór koloru przewodu. */
    val roleHints: Boolean,
    /** Ostrzeżenia (dobre praktyki) traktowane jako błędy. */
    val strict: Boolean,
    val freeHints: Int,
) {
    EASY(
        "Uczeń", "Podpowiedzi ról zacisków, automatyczny kolor przewodu, 3 darmowe podpowiedzi.",
        1.0, roleHints = true, strict = false, freeHints = 3,
    ),
    MEDIUM(
        "Czeladnik", "Bez automatycznych kolorów. Ostrzeżenia odbierają gwiazdki.",
        1.5, roleHints = false, strict = false, freeHints = 1,
    ),
    HARD(
        "Mistrz", "Wszystkie zasady dobrej praktyki są obowiązkowe: kolejność faz, kolory L1/L2/L3, polaryzacja.",
        2.5, roleHints = false, strict = true, freeHints = 0,
    ),
}
