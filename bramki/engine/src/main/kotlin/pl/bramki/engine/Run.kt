package pl.bramki.engine

import kotlin.math.abs
import kotlin.math.min

/** Zapis jednej decyzji przy bramce – materiał do „powtórki" na ekranie wyniku. */
data class Decision(
    val gate: Item.GatePair,
    val chosen: Side,
    val before: Double,
    val after: Double,
    /** Wartość, gdyby wybrać drugą bramkę. */
    val alternative: Double,
) {
    val optimal: Boolean get() = after >= alternative
    val lost: Double get() = alternative - after
}

/** Zdarzenia dla warstwy graficznej (dźwięk, wibracja, „+320" nad głową). */
sealed class GameEvent {
    data class GatePassed(val op: Op, val before: Double, val after: Double, val side: Side) : GameEvent()
    data class WallHit(val op: Op, val before: Double, val after: Double) : GameEvent()
    data class TierChanged(val from: Int, val to: Int) : GameEvent()
    data class Finished(val result: RunResult) : GameEvent()
}

data class RunResult(
    val mode: Mode,
    val value: Double,
    val optimal: Double,
    val tierIndex: Int,
    val decisions: List<Decision>,
    val wallsHit: Int,
) {
    val tier: Tier get() = mode.tiers[tierIndex]
    val ratio: Double get() = if (optimal > 0) min(1.0, value / optimal) else 0.0
    val stars: Int get() = when {
        ratio >= 0.75 -> 3
        ratio >= 0.4 -> 2
        ratio >= 0.12 -> 1
        else -> 0
    }
    val mistakes: List<Decision> get() = decisions.filter { !it.optimal }.sortedByDescending { it.lost }
    /** Siła wroga na mecie (tylko tryby z [Mode.enemyFraction]). */
    val enemy: Double? get() = mode.enemyFraction?.let { TrackGenerator.nice(optimal * it).toDouble() }
    val battleWon: Boolean? get() = enemy?.let { value >= it }
}

/**
 * Stan jednego przebiegu. Czysta logika – bez Androida – dzięki czemu testy biegną na JVM.
 * Pozycja pozioma [x] jest w zakresie −1..1 (−1 = lewa krawędź toru).
 */
class Run(val track: Track) {
    val mode: Mode get() = track.mode
    val optimal: Double = Analysis.optimalValue(track)

    var x: Double = 0.0
        private set
    var z: Double = 0.0
        private set
    var value: Double = mode.start
        private set
    var speed: Double = BASE_SPEED
        private set
    var finished: Boolean = false
        private set
    /** Prędkość boczna z ostatniego kroku (jednostki toru/s) – do animacji pochylenia postaci. */
    var lateralVelocity: Double = 0.0
        private set
    var result: RunResult? = null
        private set

    private var nextItem = 0
    private val decisions = ArrayList<Decision>()
    private var wallsHit = 0
    private val pending = ArrayDeque<GameEvent>()
    private var targetX = 0.0

    val tierIndex: Int get() = mode.tierIndex(value, optimal)
    val tier: Tier get() = mode.tiers[tierIndex]
    val progress: Double get() = (z / track.length).coerceIn(0.0, 1.0)
    val side: Side get() = if (x < 0) Side.LEFT else Side.RIGHT

    /** Przesunięcie palca: zmienia docelową pozycję (ruch jest wygładzany w [step]). */
    fun nudge(dx: Double) {
        targetX = (targetX + dx).coerceIn(-1.0, 1.0)
    }

    fun setTarget(x: Double) {
        targetX = x.coerceIn(-1.0, 1.0)
    }

    /** Krok symulacji o [dt] sekund. */
    fun step(dt: Double) {
        if (finished || dt <= 0.0) return
        // Wygładzony ruch w bok (ograniczona prędkość boczna, żeby nie „teleportować").
        val maxDx = LATERAL_SPEED * dt
        val diff = targetX - x
        val before = x
        x = if (abs(diff) <= maxDx) targetX else x + maxDx * (if (diff > 0) 1 else -1)
        lateralVelocity = (x - before) / dt

        z += speed * dt
        speed = min(MAX_SPEED, speed + ACCEL * dt)

        while (nextItem < track.items.size && track.items[nextItem].z <= z) {
            resolve(track.items[nextItem])
            nextItem++
        }
        if (z >= track.length) finish()
    }

    private fun resolve(item: Item) {
        val tierBefore = tierIndex
        when (item) {
            is Item.GatePair -> {
                val s = side
                val op = item.op(s)
                val before = value
                val after = op.applyRounded(before)
                val alt = item.other(s).applyRounded(before)
                value = after
                decisions += Decision(item, s, before, after, alt)
                pending += GameEvent.GatePassed(op, before, after, s)
            }
            is Item.Wall -> if (item.side == side) {
                val before = value
                value = item.op.applyRounded(before)
                wallsHit++
                pending += GameEvent.WallHit(item.op, before, value)
            }
        }
        val tierAfter = tierIndex
        if (tierAfter != tierBefore) pending += GameEvent.TierChanged(tierBefore, tierAfter)
    }

    private fun finish() {
        finished = true
        val r = RunResult(mode, value, optimal, tierIndex, decisions.toList(), wallsHit)
        result = r
        pending += GameEvent.Finished(r)
    }

    /** Pobiera i usuwa zdarzenia, które zaszły od ostatniego wywołania. */
    fun drainEvents(): List<GameEvent> {
        if (pending.isEmpty()) return emptyList()
        val out = pending.toList()
        pending.clear()
        return out
    }

    /** Elementy trasy jeszcze przed graczem, w zasięgu [viewDistance] – do narysowania. */
    fun visibleItems(viewDistance: Double): List<Item> {
        val out = ArrayList<Item>()
        var i = nextItem
        while (i < track.items.size && track.items[i].z - z <= viewDistance) {
            out += track.items[i]
            i++
        }
        return out
    }

    companion object {
        const val BASE_SPEED = 9.0
        const val MAX_SPEED = 16.0
        const val ACCEL = 0.09
        const val LATERAL_SPEED = 4.5
    }
}
