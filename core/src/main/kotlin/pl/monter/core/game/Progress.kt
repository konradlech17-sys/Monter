package pl.monter.core.game

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import pl.monter.core.level.Level
import pl.monter.core.level.Levels
import kotlin.math.roundToInt

@Serializable
data class LevelResult(val stars: Int, val bestTimeSec: Int? = null, val completions: Int = 1)

/** Pozycja elementu przestawionego przez gracza. */
@Serializable
data class Pos(val x: Float, val y: Float)

@Serializable
data class Stats(
    val checks: Int = 0,
    val shortsCaused: Int = 0,
    val perfectProcedures: Int = 0,
    val purchases: Int = 0,
    val wiresLaid: Int = 0,
)

/** Pełny zapis stanu gracza – przechowywany lokalnie i w chmurze (Google Play Games – Saved Games). */
@Serializable
data class SaveData(
    val version: Int = 1,
    val sparks: Int = 0,
    val totalEarned: Int = 0,
    /** Klucz: "idPoziomu@TRUDNOŚĆ". */
    val results: Map<String, LevelResult> = emptyMap(),
    val hints: Int = 3,
    val owned: Set<String> = setOf("theme_classic", "helmet_yellow"),
    val activeTheme: String = "theme_classic",
    val activeHelmet: String = "helmet_yellow",
    val achievements: Set<String> = emptySet(),
    val stats: Stats = Stats(),
    /** Poziomy, których wprowadzenie teoretyczne gracz już widział. */
    val seenIntro: Set<String> = emptySet(),
    /** Ustawienia: widok 3D planszy, wibracje. */
    val view3d: Boolean = true,
    /** Własny układ elementów na planszy: id poziomu → (id elementu → pozycja). */
    val layouts: Map<String, Map<String, Pos>> = emptyMap(),
    val haptics: Boolean = true,
    val updatedAt: Long = 0,
) {
    fun result(level: Level, d: Difficulty) = results[key(level.id, d)]

    fun bestStars(level: Level): Int = Difficulty.entries.maxOf { result(level, it)?.stars ?: 0 }

    fun completed(level: Level) = Difficulty.entries.any { result(level, it) != null }

    /**
     * Zasady odblokowania: poziom bonusowy – kupiony w sklepie; poziom serwisowy – po ukończeniu poziomu bazowego;
     * pozostałe – po poprzednim poziomie rozdziału. Rozdział otwiera się po ukończeniu 3 poziomów poprzedniego.
     */
    fun isUnlocked(level: Level): Boolean {
        if (level.unlockCost != null) return "level_${level.id}" in owned
        level.requires?.let { req -> return Levels.byId(req)?.let { completed(it) } ?: true }
        val inChapter = Levels.inChapter(level.chapter).filter { it.unlockCost == null }
        val idx = inChapter.indexOf(level)
        if (idx > 0) return completed(inChapter[idx - 1])
        val prevChapter = Levels.previousChapter(level.chapter) ?: return true
        val prev = Levels.inChapter(prevChapter).filter { it.unlockCost == null }
        return prev.count { completed(it) } >= minOf(Levels.CHAPTER_GATE, prev.size)
    }

    fun introSeen(level: Level) = level.id in seenIntro

    fun toJson(): String = json.encodeToString(this)

    companion object {
        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        fun key(levelId: String, d: Difficulty) = "$levelId@${d.name}"
        fun fromJson(s: String): SaveData = json.decodeFromString(serializer(), s)

        /**
         * Łączenie zapisu lokalnego i chmurowego (np. gra na dwóch telefonach).
         * Wyniki i zakupy sumujemy (bierzemy lepsze), a stan „portfela" z nowszego zapisu.
         */
        fun merge(a: SaveData, b: SaveData): SaveData {
            val newer = if (a.updatedAt >= b.updatedAt) a else b
            val results = (a.results.keys + b.results.keys).associateWith { k ->
                val x = a.results[k]; val y = b.results[k]
                when {
                    x == null -> y!!
                    y == null -> x
                    else -> LevelResult(
                        stars = maxOf(x.stars, y.stars),
                        bestTimeSec = listOfNotNull(x.bestTimeSec, y.bestTimeSec).minOrNull(),
                        completions = maxOf(x.completions, y.completions),
                    )
                }
            }
            return newer.copy(
                results = results,
                owned = a.owned + b.owned,
                achievements = a.achievements + b.achievements,
                seenIntro = a.seenIntro + b.seenIntro,
                layouts = (if (newer === a) b.layouts + a.layouts else a.layouts + b.layouts),
                totalEarned = maxOf(a.totalEarned, b.totalEarned),
                stats = Stats(
                    checks = maxOf(a.stats.checks, b.stats.checks),
                    shortsCaused = maxOf(a.stats.shortsCaused, b.stats.shortsCaused),
                    perfectProcedures = maxOf(a.stats.perfectProcedures, b.stats.perfectProcedures),
                    purchases = maxOf(a.stats.purchases, b.stats.purchases),
                    wiresLaid = maxOf(a.stats.wiresLaid, b.stats.wiresLaid),
                ),
                updatedAt = maxOf(a.updatedAt, b.updatedAt),
            )
        }
    }
}

/** Przebieg jednej rozgrywki poziomu – na jego podstawie liczymy gwiazdki i punkty. */
data class Attempt(
    val failedChecks: Int,
    val warnings: Int,
    val procedureMistakes: Int,
    val hintsUsed: Int,
    val seconds: Int,
)

data class Reward(val stars: Int, val sparks: Int, val firstClear: Boolean, val improved: Boolean, val newAchievements: List<Achievement>)

object Scoring {
    fun stars(a: Attempt, d: Difficulty): Int {
        var s = 3
        if (a.failedChecks >= 1) s--
        if (a.failedChecks >= 3) s--
        if (d != Difficulty.EASY && a.warnings > 0) s--
        if (d != Difficulty.EASY && a.procedureMistakes > 0) s--
        if (a.hintsUsed > d.freeHints + 1) s--
        return s.coerceIn(1, 3)
    }

    fun basePoints(level: Level, d: Difficulty, stars: Int): Int = (level.basePoints * d.multiplier * stars / 3.0).roundToInt()

    /** Nalicza nagrodę i zwraca zaktualizowany zapis. */
    fun complete(save: SaveData, level: Level, d: Difficulty, a: Attempt, now: Long): Pair<SaveData, Reward> {
        val stars = stars(a, d)
        val key = SaveData.key(level.id, d)
        val prev = save.results[key]
        val full = basePoints(level, d, stars)
        val sparks = when {
            prev == null -> full + 50
            stars > prev.stars -> full - basePoints(level, d, prev.stars) + full / 5
            else -> full / 5
        }
        val result = LevelResult(
            stars = maxOf(stars, prev?.stars ?: 0),
            bestTimeSec = listOfNotNull(prev?.bestTimeSec, a.seconds).minOrNull(),
            completions = (prev?.completions ?: 0) + 1,
        )
        var next = save.copy(
            sparks = save.sparks + sparks,
            totalEarned = save.totalEarned + sparks,
            results = save.results + (key to result),
            stats = save.stats.copy(perfectProcedures = save.stats.perfectProcedures + if (level.safetyProcedure && a.procedureMistakes == 0) 1 else 0),
            updatedAt = now,
        )
        val ctx = AchievementContext(level, d, stars, a)
        val unlocked = Achievements.newlyUnlocked(next, ctx)
        next = next.copy(achievements = next.achievements + unlocked.map { it.id })
        return next to Reward(stars, sparks, prev == null, prev != null && stars > prev.stars, unlocked)
    }
}
