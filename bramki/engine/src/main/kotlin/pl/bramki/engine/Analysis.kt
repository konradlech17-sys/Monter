package pl.bramki.engine

import kotlin.math.max

object Analysis {
    /**
     * Najlepszy możliwy wynik na trasie (bez wjeżdżania w przeszkody).
     * Ponieważ każda operacja jest niemalejąca względem wartości, wybór zachłanny
     * (większa wartość po bramce) jest wyborem optymalnym – nie trzeba przeszukiwać 2^n ścieżek.
     */
    fun optimalValue(track: Track): Double {
        var v = track.mode.start
        for (g in track.gates) v = max(g.left.applyRounded(v), g.right.applyRounded(v))
        return v
    }

    /** Najgorszy możliwy wynik – do testów i kalibracji progów. */
    fun worstValue(track: Track): Double {
        var v = track.mode.start
        for (g in track.gates) v = kotlin.math.min(g.left.applyRounded(v), g.right.applyRounded(v))
        return v
    }

    /** Wynik gracza, który zawsze wybiera „zieloną” bramkę, a między dwiema tego samego koloru – losowo. */
    fun naiveValue(track: Track, rnd: kotlin.random.Random): Double {
        var v = track.mode.start
        for (g in track.gates) {
            val op = when {
                g.left.positive != g.right.positive -> if (g.left.positive) g.left else g.right
                rnd.nextBoolean() -> g.left
                else -> g.right
            }
            v = op.applyRounded(v)
        }
        return v
    }

    /** Czytelny opis decyzji do ekranu wyniku. */
    fun describe(d: Decision, mode: Mode): String {
        val chosen = d.gate.op(d.chosen).label
        val other = d.gate.other(d.chosen).label
        val u = mode.unit
        return "Bramka ${d.gate.index + 1}: miałeś ${Format.compact(d.before)} $u, wybrałeś „$chosen” (→ ${Format.compact(d.after)}). " +
            "Lepsze było „$other” (→ ${Format.compact(d.alternative)})."
    }
}
