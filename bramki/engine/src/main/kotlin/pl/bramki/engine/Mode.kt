package pl.bramki.engine

/** Jeden „szczebel" rozwoju postaci: od jakiej części wartości optymalnej go osiągasz, jak wygląda i czego uczy. */
data class Tier(
    val name: String,
    val emoji: String,
    /** Próg jako ułamek wartości optymalnej dla danej trasy (0.0 = start). */
    val fraction: Double,
    /** Kolor nieba/tła w formacie 0xAARRGGBB. */
    val color: Long,
    /** Ciekawostka pokazywana po osiągnięciu tego szczebla. */
    val fact: String,
)

/** Wariant gry: motyw, jednostka, szczeble rozwoju i styl bramek. */
data class Mode(
    val id: String,
    val name: String,
    val emoji: String,
    val tagline: String,
    /** Jednostka wartości, np. „lat", „zł". */
    val unit: String,
    /** Wartość startowa. */
    val start: Double,
    val tiers: List<Tier>,
    /** Czy bramki mnożące opisywać procentami (+50%) zamiast ×1,5. */
    val percentStyle: Boolean = false,
    /** Jeśli ustawione, na mecie czeka „wróg" o sile = ułamek wartości optymalnej (tryb Armia). */
    val enemyFraction: Double? = null,
    /** Liczba par bramek na trasie. */
    val gateCount: Int = 14,
    /** Wskazówki (matematyczne i strategiczne) pokazywane na ekranie wyniku. */
    val tips: List<String>,
) {
    init {
        require(tiers.isNotEmpty())
        require(tiers.first().fraction == 0.0) { "pierwszy szczebel musi zaczynać się od 0" }
        require(tiers.zipWithNext().all { (a, b) -> a.fraction < b.fraction }) { "progi muszą rosnąć" }
    }

    /** Indeks szczebla dla wartości [value] przy wartości optymalnej [optimal]. */
    fun tierIndex(value: Double, optimal: Double): Int {
        if (optimal <= 0.0) return 0
        val ratio = value / optimal
        var idx = 0
        tiers.forEachIndexed { i, t -> if (ratio >= t.fraction) idx = i }
        return idx
    }

    fun tier(value: Double, optimal: Double): Tier = tiers[tierIndex(value, optimal)]

    /** Postęp (0..1) wewnątrz bieżącego szczebla – do paska „do następnej epoki". */
    fun tierProgress(value: Double, optimal: Double): Double {
        val i = tierIndex(value, optimal)
        if (i == tiers.lastIndex || optimal <= 0.0) return 1.0
        val from = tiers[i].fraction * optimal
        val to = tiers[i + 1].fraction * optimal
        return ((value - from) / (to - from)).coerceIn(0.0, 1.0)
    }

    /** Wartość potrzebna do następnego szczebla albo null na szczycie. */
    fun nextTierValue(value: Double, optimal: Double): Double? {
        val i = tierIndex(value, optimal)
        return if (i == tiers.lastIndex) null else tiers[i + 1].fraction * optimal
    }
}
