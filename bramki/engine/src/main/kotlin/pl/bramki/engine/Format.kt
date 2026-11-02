package pl.bramki.engine

import kotlin.math.abs
import kotlin.math.roundToLong

/** Formatowanie liczb po polsku: 1234 → „1234", 12 345 → „12,3 tys.", 4 500 000 → „4,5 mln". */
object Format {
    fun compact(value: Double): String {
        val v = abs(value)
        return when {
            v < 10_000 -> value.roundToLong().toString()
            v < 1_000_000 -> scaled(value, 1_000.0, "tys.")
            v < 1_000_000_000 -> scaled(value, 1_000_000.0, "mln")
            v < 1_000_000_000_000 -> scaled(value, 1_000_000_000.0, "mld")
            else -> scaled(value, 1_000_000_000_000.0, "bln")
        }
    }

    private fun scaled(value: Double, div: Double, suffix: String): String {
        val x = value / div
        val txt = if (abs(x) >= 100) x.roundToLong().toString() else oneDecimal(x)
        return "$txt $suffix"
    }

    /** 1,5 → „1,5", 2,0 → „2". */
    fun factor(k: Double): String = oneDecimal(k)

    private fun oneDecimal(x: Double): String {
        val tenths = (x * 10).roundToLong()
        val whole = tenths / 10
        val frac = abs(tenths % 10)
        return if (frac == 0L) whole.toString() else "$whole,$frac"
    }

    /** Pełna liczba z separatorem tysięcy (spacja), np. 1 234 567. */
    fun full(value: Double): String {
        val n = value.roundToLong()
        val s = abs(n).toString()
        val sb = StringBuilder()
        s.forEachIndexed { i, c ->
            if (i > 0 && (s.length - i) % 3 == 0) sb.append(' ')
            sb.append(c)
        }
        return (if (n < 0) "−" else "") + sb.toString()
    }

    fun percent(ratio: Double): String = (ratio * 100).roundToLong().toString() + "%"
}
