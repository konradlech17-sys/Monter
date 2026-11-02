package pl.bramki.engine

import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.random.Random

enum class Side { LEFT, RIGHT }

/** Element trasy na pozycji [z] (jednostki toru; gracz zaczyna na z = 0). */
sealed class Item {
    abstract val z: Double

    /** Para bramek: wybierasz lewą albo prawą. */
    data class GatePair(override val z: Double, val index: Int, val left: Op, val right: Op) : Item() {
        fun op(side: Side) = if (side == Side.LEFT) left else right
        fun other(side: Side) = if (side == Side.LEFT) right else left
    }

    /** Przeszkoda zajmująca połowę toru – wjazd w nią kosztuje [op]. Trzeba ją ominąć. */
    data class Wall(override val z: Double, val side: Side, val op: Op) : Item()
}

data class Track(
    val mode: Mode,
    val seed: Long,
    val items: List<Item>,
    /** Pozycja mety. */
    val length: Double,
) {
    val gates: List<Item.GatePair> get() = items.filterIsInstance<Item.GatePair>()
    val walls: List<Item.Wall> get() = items.filterIsInstance<Item.Wall>()
}

/**
 * Generator tras. Wielkości bramek są dobierane względem wartości, jaką ma gracz idący optymalnie,
 * dlatego trasa jest ciekawa niezależnie od tego, czy masz 50, czy 5 000 000 jednostek.
 */
object TrackGenerator {
    const val GATE_SPACING = 10.0
    const val FIRST_GATE_Z = 14.0
    const val FINISH_MARGIN = 12.0

    fun generate(mode: Mode, seed: Long): Track {
        val rnd = Random(seed)
        val items = ArrayList<Item>()
        var v = mode.start            // wartość na ścieżce optymalnej
        var z = FIRST_GATE_Z
        for (i in 0 until mode.gateCount) {
            val pair = makePair(mode, rnd, i, v)
            val gate = Item.GatePair(z, i, pair.first, pair.second)
            items += gate
            v = max(gate.left.apply(v), gate.right.apply(v))
            // Przeszkoda między bramkami – od trzeciej bramki, coraz częściej.
            if (i >= 2 && i < mode.gateCount - 1 && rnd.nextDouble() < 0.35 + i * 0.02) {
                val side = if (rnd.nextBoolean()) Side.LEFT else Side.RIGHT
                val penalty: Op = if (mode.percentStyle) Op.Pct(-30) else Op.Div(1.5)
                items += Item.Wall(z + GATE_SPACING * 0.5, side, penalty)
            }
            z += GATE_SPACING
        }
        return Track(mode, seed, items, z + FINISH_MARGIN - GATE_SPACING)
    }

    private enum class Kind { EASY, BOTH_POSITIVE, BOTH_NEGATIVE, MUL_VS_DIV }

    private fun makePair(mode: Mode, rnd: Random, index: Int, v: Double): Pair<Op, Op> {
        // Pierwsze dwie bramki są proste – gracz uczy się sterowania.
        val kind = if (index < 2) Kind.EASY else when (rnd.nextDouble()) {
            in 0.0..0.30 -> Kind.EASY
            in 0.30..0.68 -> Kind.BOTH_POSITIVE
            in 0.68..0.85 -> Kind.BOTH_NEGATIVE
            else -> Kind.MUL_VS_DIV
        }
        val pair: Pair<Op, Op> = when (kind) {
            Kind.EASY -> {
                val good = if (rnd.nextDouble() < 0.6) add(rnd, v, 0.4, 1.2) else mul(mode, rnd)
                val bad = if (rnd.nextDouble() < 0.6) sub(rnd, v, 0.2, 0.6) else div(mode, rnd)
                good to bad
            }
            Kind.BOTH_POSITIVE -> {
                // Dodawanie kontra mnożenie: „+N" opłaca się tylko, gdy N > v·(k−1).
                val m = mul(mode, rnd)
                val k = factorOf(m)
                val n = nice(v * (k - 1) * between(rnd, 0.4, 2.2))
                Op.Add(max(1, n)) to m
            }
            Kind.BOTH_NEGATIVE -> {
                val d = div(mode, rnd)
                val k = factorOf(d)
                val n = nice(v * (1 - 1 / k) * between(rnd, 0.4, 2.2))
                Op.Sub(max(1, n)) to d
            }
            Kind.MUL_VS_DIV -> mul(mode, rnd) to div(mode, rnd)
        }
        // Losowa strona, żeby „dobra" bramka nie była zawsze z lewej.
        return if (rnd.nextBoolean()) pair else pair.second to pair.first
    }

    private fun add(rnd: Random, v: Double, lo: Double, hi: Double) = Op.Add(max(1, nice(v * between(rnd, lo, hi))))
    private fun sub(rnd: Random, v: Double, lo: Double, hi: Double) = Op.Sub(max(1, nice(v * between(rnd, lo, hi))))

    private fun mul(mode: Mode, rnd: Random): Op {
        val k = listOf(1.5, 2.0, 2.0, 3.0).random(rnd)
        return if (mode.percentStyle) Op.Pct(((k - 1) * 100).roundToLong().toInt()) else Op.Mul(k)
    }

    private fun div(mode: Mode, rnd: Random): Op {
        val k = listOf(1.5, 2.0, 2.0, 3.0).random(rnd)
        return if (mode.percentStyle) Op.Pct(-((1 - 1 / k) * 100).roundToLong().toInt()) else Op.Div(k)
    }

    /** Współczynnik mnożenia/dzielenia danej bramki (dla Pct liczony z procentu). */
    private fun factorOf(op: Op): Double = when (op) {
        is Op.Mul -> op.k
        is Op.Div -> op.k
        is Op.Pct -> if (op.percent >= 0) 1 + op.percent / 100.0 else 100.0 / (100 + op.percent)
        else -> 1.0
    }

    private fun between(rnd: Random, lo: Double, hi: Double) = lo + rnd.nextDouble() * (hi - lo)

    /** Zaokrąglenie do „ładnej" liczby: 1–2 cyfry znaczące (137 → 140, 2 861 → 2 900, 48 123 → 48 000). */
    fun nice(x: Double): Long {
        if (x < 10) return max(1.0, x).roundToLong()
        val digits = kotlin.math.floor(kotlin.math.log10(x)).toInt()
        val step = 10.0.pow(digits - 1)
        return ((x / step).roundToLong() * step).roundToLong()
    }
}
