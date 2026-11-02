package pl.bramki.app.game

/**
 * Znaczniki czasu ostatnich zdarzeń – z nich renderer wylicza animacje
 * (squash & stretch przy bramce, wstrząs przy ścianie, przemiana przy awansie).
 * Wszystko liczone z czasu, więc animacje nie zależą od liczby klatek.
 */
class Fx {
    var gateMs = Long.MIN_VALUE / 2
    var gatePositive = true
    var wallMs = Long.MIN_VALUE / 2
    var tierMs = Long.MIN_VALUE / 2
    var tierUp = true
    var previousTier = 0

    companion object {
        const val GATE_DURATION = 380L
        const val WALL_DURATION = 450L
        const val TIER_DURATION = 850L
    }
}
