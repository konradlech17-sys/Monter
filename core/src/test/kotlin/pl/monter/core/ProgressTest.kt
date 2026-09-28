package pl.monter.core

import pl.monter.core.game.Attempt
import pl.monter.core.game.Difficulty
import pl.monter.core.game.LevelResult
import pl.monter.core.game.PurchaseResult
import pl.monter.core.game.SaveData
import pl.monter.core.game.Safety
import pl.monter.core.game.Scoring
import pl.monter.core.game.Shop
import pl.monter.core.level.Levels
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ProgressTest {
    private val perfect = Attempt(failedChecks = 0, warnings = 0, procedureMistakes = 0, hintsUsed = 0, seconds = 60)

    @Test
    fun `stars and points`() {
        assertEquals(3, Scoring.stars(perfect, Difficulty.HARD))
        assertEquals(1, Scoring.stars(perfect.copy(failedChecks = 4, warnings = 1), Difficulty.MEDIUM))
        assertEquals(3, Scoring.stars(perfect.copy(warnings = 2), Difficulty.EASY))
        val level = Levels.all.first()
        val (save, reward) = Scoring.complete(SaveData(), level, Difficulty.HARD, perfect, now = 10)
        assertTrue(reward.firstClear)
        assertEquals((level.basePoints * 2.5).toInt() + 50, reward.sparks)
        assertEquals(reward.sparks, save.sparks)
        assertTrue("first_light" in save.achievements)
        // Powtórka daje tylko 20%.
        val (_, again) = Scoring.complete(save, level, Difficulty.HARD, perfect, now = 20)
        assertEquals(reward.sparks.minus(50) / 5, again.sparks)
    }

    @Test
    fun `campaign unlocking`() {
        val s = SaveData()
        assertTrue(s.isUnlocked(Levels.campaign[0]))
        assertFalse(s.isUnlocked(Levels.campaign[1]))
        val done = s.copy(results = mapOf(SaveData.key(Levels.campaign[0].id, Difficulty.EASY) to LevelResult(1)))
        assertTrue(done.isUnlocked(Levels.campaign[1]))
        val bonus = Levels.all.first { it.unlockCost != null }
        assertFalse(done.isUnlocked(bonus))
    }

    @Test
    fun `shop purchases`() {
        val s = SaveData(sparks = 500)
        assertIs<PurchaseResult.NotEnough>(Shop.buy(SaveData(sparks = 10), "hint1", 1))
        val r = Shop.buy(s, "hint5", 1) as PurchaseResult.Ok
        assertEquals(300, r.save.sparks)
        assertEquals(s.hints + 5, r.save.hints)
        val t = Shop.buy(r.save, "theme_wood", 2) as PurchaseResult.Ok
        assertTrue("theme_wood" in t.save.owned)
        assertIs<PurchaseResult.AlreadyOwned>(Shop.buy(t.save.copy(sparks = 1000), "theme_wood", 3))
        val bonus = Shop.buy(SaveData(sparks = 1000), "level_2-5", 1) as PurchaseResult.Ok
        assertTrue(bonus.save.isUnlocked(Levels.byId("2-5")!!))
    }

    @Test
    fun `cloud merge keeps best of both`() {
        val a = SaveData(sparks = 100, results = mapOf("1-1@EASY" to LevelResult(2, 50, 1)), owned = setOf("theme_wood"), updatedAt = 5)
        val b = SaveData(sparks = 40, results = mapOf("1-1@EASY" to LevelResult(3, 70, 2), "1-2@EASY" to LevelResult(1)), owned = setOf("tool_meter"), updatedAt = 9)
        val m = SaveData.merge(a, b)
        assertEquals(40, m.sparks) // portfel z nowszego zapisu
        assertEquals(LevelResult(3, 50, 2), m.results["1-1@EASY"])
        assertTrue(m.results.containsKey("1-2@EASY"))
        assertEquals(setOf("theme_wood", "tool_meter"), m.owned)
        assertEquals(SaveData.merge(a, b), SaveData.merge(b, a))
    }

    @Test
    fun `json roundtrip`() {
        val s = SaveData(sparks = 123, results = mapOf("1-1@HARD" to LevelResult(3)), achievements = setOf("first_light"))
        assertEquals(s, SaveData.fromJson(s.toJson()))
        // Odporność na przyszłe pola.
        assertEquals(7, SaveData.fromJson("""{"sparks":7,"future":"x"}""").sparks)
    }

    @Test
    fun `safety procedure order`() {
        val deck = Safety.deck(3)
        assertEquals(Safety.steps.size + 2, deck.size)
        assertTrue(Safety.isNext(0, Safety.steps[0]))
        assertFalse(Safety.isNext(0, Safety.steps[2]))
        assertFalse(Safety.isNext(0, Safety.decoys[0]))
    }
}
