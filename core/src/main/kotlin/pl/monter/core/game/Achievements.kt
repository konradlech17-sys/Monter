package pl.monter.core.game

import pl.monter.core.level.Level
import pl.monter.core.level.Levels

data class Achievement(val id: String, val title: String, val description: String, val icon: String)

data class AchievementContext(val level: Level, val difficulty: Difficulty, val stars: Int, val attempt: Attempt)

object Achievements {
    val all = listOf(
        Achievement("first_light", "Pierwsze światło", "Ukończ pierwszy poziom.", "💡"),
        Achievement("flawless", "Bez pudła", "Ukończ poziom bez żadnego błędu przy pierwszym sprawdzeniu.", "🎯"),
        Achievement("stairs", "Schodami w górę", "Podłącz łącznik schodowy.", "🪜"),
        Achievement("xor", "Bramka XOR", "Podłącz układ z łącznikiem krzyżowym.", "✖️"),
        Achievement("sparks", "Iskry lecą!", "Spowoduj zwarcie w trybie TEST (tylko w grze!).", "⚡"),
        Achievement("safety10", "Bezpieczeństwo przede wszystkim", "10 razy bezbłędnie wykonaj procedurę bezpieczeństwa.", "🦺"),
        Achievement("chapter1", "Absolwent", "Ukończ rozdział 1.", "🎓"),
        Achievement("chapter2", "Instalator", "Ukończ rozdział 2.", "🔧"),
        Achievement("chapter3", "Automatyk", "Ukończ rozdział 3.", "🔔"),
        Achievement("chapter4", "Rozdzielnicowy", "Ukończ rozdział 4.", "🗄️"),
        Achievement("chapter5", "Siłowiec", "Ukończ rozdział 5.", "⚙️"),
        Achievement("chapter6", "Serwisant", "Napraw wszystkie usterki z rozdziału 6.", "🛠️"),
        Achievement("master", "Mistrz rozdzielnic", "Ukończ rozdzielnicę domową 400 V na poziomie Mistrz.", "👑"),
        Achievement("collector", "Kolekcjoner", "Dokonaj 3 zakupów w sklepie.", "🛒"),
        Achievement("rich", "Iskrowy milioner", "Zdobądź łącznie 5000 iskier.", "💰"),
        Achievement("perfectionist", "Perfekcjonista", "Zdobądź 3 gwiazdki na każdym poziomie kampanii.", "⭐"),
    )

    fun byId(id: String) = all.first { it.id == id }

    private fun chapterDone(save: SaveData, ch: Int) = Levels.campaign.filter { it.chapter == ch }.all { save.completed(it) }

    fun newlyUnlocked(save: SaveData, ctx: AchievementContext?): List<Achievement> {
        val earned = mutableSetOf<String>()
        if (Levels.all.any { save.completed(it) }) earned += "first_light"
        if (ctx != null && ctx.attempt.failedChecks == 0 && ctx.attempt.warnings == 0) earned += "flawless"
        if (Levels.byId("2-2")?.let { save.completed(it) } == true) earned += "stairs"
        if (Levels.byId("2-3")?.let { save.completed(it) } == true) earned += "xor"
        if (save.stats.shortsCaused > 0) earned += "sparks"
        if (save.stats.perfectProcedures >= 10) earned += "safety10"
        for (ch in 1..6) if (chapterDone(save, ch)) earned += "chapter$ch"
        if (Levels.byId("5-4")?.let { save.result(it, Difficulty.HARD) } != null) earned += "master"
        if (save.stats.purchases >= 3) earned += "collector"
        if (save.totalEarned >= 5000) earned += "rich"
        if (Levels.campaign.all { save.bestStars(it) == 3 }) earned += "perfectionist"
        return (earned - save.achievements).map { byId(it) }
    }
}
