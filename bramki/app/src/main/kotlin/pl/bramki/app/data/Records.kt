package pl.bramki.app.data

import android.content.Context
import pl.bramki.engine.RunResult

/** Rekordy gracza – zwykłe SharedPreferences, bez konta i bez chmury. */
class Records(context: Context) {
    private val prefs = context.getSharedPreferences("bramki_records", Context.MODE_PRIVATE)

    data class Best(
        val tier: Int,
        val ratio: Float,
        val plays: Int,
        val wins: Int,
    ) {
        val played: Boolean get() = plays > 0
    }

    fun get(modeId: String): Best = Best(
        tier = prefs.getInt("tier_$modeId", -1),
        ratio = prefs.getFloat("ratio_$modeId", 0f),
        plays = prefs.getInt("plays_$modeId", 0),
        wins = prefs.getInt("wins_$modeId", 0),
    )

    /** Zapisuje wynik; zwraca `true`, gdy to nowy rekord (wyższy szczebel albo lepszy procent). */
    fun record(result: RunResult): Boolean {
        val id = result.mode.id
        val old = get(id)
        val ratio = result.ratio.toFloat()
        val isRecord = result.tierIndex > old.tier || (result.tierIndex == old.tier && ratio > old.ratio)
        prefs.edit()
            .putInt("plays_$id", old.plays + 1)
            .putInt("wins_$id", old.wins + if (result.battleWon == true) 1 else 0)
            .putInt("tier_$id", maxOf(old.tier, result.tierIndex))
            .putFloat("ratio_$id", maxOf(old.ratio, ratio))
            .apply()
        return isRecord
    }
}
