package pl.bramki.engine

import kotlin.math.max
import kotlin.math.roundToLong

/**
 * Operacja, którą bramka wykonuje na wartości gracza.
 *
 * Ważna własność matematyczna: KAŻDA z tych operacji jest funkcją niemalejącą
 * względem wartości wejściowej (jeśli masz więcej, po bramce też będziesz mieć więcej).
 * Dzięki temu strategia zachłanna – „zawsze wybieraj bramkę, która daje więcej TERAZ" –
 * jest optymalna na całej trasie. To właśnie liczy [Analysis.optimalValue].
 */
sealed class Op {
    /** Zastosuj operację; wynik nigdy nie spada poniżej zera. */
    abstract fun apply(value: Double): Double

    /** Napis na bramce, np. „+320", „×2", „−30%". */
    abstract val label: String

    /** Czy bramka jest „dobra" (zielona)? Dla każdej operacji znak jest niezależny od wartości. */
    abstract val positive: Boolean

    data class Add(val n: Long) : Op() {
        override fun apply(value: Double) = value + n
        override val label get() = "+" + Format.compact(n.toDouble())
        override val positive get() = true
    }

    data class Sub(val n: Long) : Op() {
        override fun apply(value: Double) = max(0.0, value - n)
        override val label get() = "−" + Format.compact(n.toDouble())
        override val positive get() = false
    }

    /** Mnożenie przez k > 1 (×2, ×3, ×1,5). */
    data class Mul(val k: Double) : Op() {
        override fun apply(value: Double) = value * k
        override val label get() = "×" + Format.factor(k)
        override val positive get() = true
    }

    /** Dzielenie przez k > 1 (÷2, ÷3). */
    data class Div(val k: Double) : Op() {
        override fun apply(value: Double) = value / k
        override val label get() = "÷" + Format.factor(k)
        override val positive get() = false
    }

    /** Zmiana procentowa, np. +50% lub −30% – to samo co mnożenie, ale inaczej opisane. */
    data class Pct(val percent: Int) : Op() {
        override fun apply(value: Double) = max(0.0, value * (100 + percent) / 100.0)
        override val label get() = (if (percent >= 0) "+" else "−") + kotlin.math.abs(percent) + "%"
        override val positive get() = percent >= 0
    }

    /** Wartość po bramce zaokrąglona do pełnych jednostek (do wyświetlania). */
    fun applyRounded(value: Double): Double = apply(value).roundToLong().toDouble()
}
