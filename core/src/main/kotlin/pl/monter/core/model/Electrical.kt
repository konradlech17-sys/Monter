package pl.monter.core.model

import kotlinx.serialization.Serializable

/**
 * Kolory izolacji przewodów wg PN-EN 60445.
 * - zielono-żółty: WYŁĄCZNIE przewód ochronny PE (lub PEN),
 * - niebieski: przewód neutralny N,
 * - brązowy / czarny / szary: przewody fazowe (L1 / L2 / L3).
 */
@Serializable
enum class WireColor(val pl: String) {
    BROWN("brązowy"),
    BLACK("czarny"),
    GREY("szary"),
    BLUE("niebieski"),
    GREEN_YELLOW("zielono-żółty"),
    WHITE("biały"),
    RED("czerwony");

    val isPhaseColor get() = this == BROWN || this == BLACK || this == GREY
}

/** Przekroje żył miedzianych. */
@Serializable
enum class CrossSection(val mm2: Double, val label: String) {
    S0_5(0.5, "0,5 mm²"),
    S1_5(1.5, "1,5 mm²"),
    S2_5(2.5, "2,5 mm²"),
    S4(4.0, "4 mm²"),
    S6(6.0, "6 mm²"),
    S10(10.0, "10 mm²"),
    S16(16.0, "16 mm²");

    companion object {
        /**
         * Minimalny przekrój przewodu dla danego prądu znamionowego zabezpieczenia
         * (uproszczenie tabel obciążalności z PN-HD 60364-5-52 dla typowego ułożenia w tynku / rurce).
         */
        fun minimumFor(ratingA: Int): CrossSection = when {
            ratingA <= 10 -> S1_5
            ratingA <= 16 -> S2_5
            ratingA <= 25 -> S4
            ratingA <= 32 -> S6
            ratingA <= 40 -> S10
            else -> S16
        }
    }
}

/** Potencjały, jakie może „nieść" sieć połączeń. */
enum class Potential(val label: String) {
    L1("L1"), L2("L2"), L3("L3"), N("N"), PE("PE"),
    /** Dwa zaciski strony wtórnej transformatora bezpiecznego (SELV, np. 8 V). */
    LV_A("~8V"), LV_B("~8V");

    val isPhase get() = this == L1 || this == L2 || this == L3
    val isLowVoltage get() = this == LV_A || this == LV_B
}

/** Rola zacisku – podpowiedź dla gracza i dla trybu „łatwy" (automatyczny dobór koloru). */
enum class Role(val label: String) {
    L("L"), N("N"), PE("PE"), L1("L1"), L2("L2"), L3("L3"),
    SWITCHED("L′"), TRAVELER("korespondencja"), LV("SELV"), ANY("");

    fun suggestedColor(): WireColor = when (this) {
        L, L1 -> WireColor.BROWN
        L2, SWITCHED, TRAVELER -> WireColor.BLACK
        L3 -> WireColor.GREY
        N -> WireColor.BLUE
        PE -> WireColor.GREEN_YELLOW
        LV -> WireColor.WHITE
        ANY -> WireColor.BROWN
    }
}

@Serializable
data class TermRef(val part: String, val terminal: String) {
    override fun toString() = "$part.$terminal"
}

@Serializable
data class Wire(
    val id: Int,
    val a: TermRef,
    val b: TermRef,
    val color: WireColor,
    val cs: CrossSection,
    /** Przewód założony przez autora poziomu – nie da się go usunąć. */
    val fixed: Boolean = false,
) {
    fun touches(t: TermRef) = a == t || b == t
    fun other(t: TermRef) = if (a == t) b else a
}

/** Parametry aparatu / odbiornika – stałe albo wybierane przez gracza z listy. */
@Serializable
data class DeviceSpec(
    val id: String,
    val label: String,
    /** Prąd znamionowy [A] – dla wyłączników nadprądowych, RCD, rozłączników, wkładek. */
    val ratingA: Int? = null,
    /** Charakterystyka wyłącznika nadprądowego: B, C, D. */
    val curve: Char? = null,
    /** Prąd różnicowy [mA] wyłącznika RCD. */
    val rcdmA: Int? = null,
    /** Typ RCD: AC, A, F, B. */
    val rcdType: String? = null,
    val note: String? = null,
)
