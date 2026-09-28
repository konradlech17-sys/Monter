package pl.monter.core

import pl.monter.core.game.BuildSession
import pl.monter.core.game.Difficulty
import pl.monter.core.level.Levels
import pl.monter.core.model.CrossSection
import pl.monter.core.model.TermRef
import pl.monter.core.model.WireColor
import pl.monter.core.rules.Report
import pl.monter.core.rules.Severity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ValidatorTest {

    private fun BuildSession.check(d: Difficulty = Difficulty.HARD): Report = validate(d)

    private fun Report.codes() = issues.map { it.code }.toSet()

    private fun Report.dump() = issues.joinToString("\n") { "${it.severity} ${it.code}: ${it.title}" }

    @Test
    fun `every level has a reference solution that passes on HARD without warnings`() {
        assertEquals(Levels.all.map { it.id }.toSet(), Solutions.all.keys)
        for ((id, s) in Solutions.all) {
            val r = s.check(Difficulty.HARD)
            assertTrue(r.issues.isEmpty(), "Poziom $id:\n${r.dump()}")
        }
    }

    @Test
    fun `empty board fails`() {
        for (level in Levels.all.filter { it.prewired.isEmpty() }) {
            assertFalse(BuildSession(level).check().passed, level.id)
        }
    }

    @Test
    fun `wrong bulb is rejected with explanation`() {
        val r = solve("1-1") { choose("lamp", "inc_e27_100") }.check()
        assertTrue("choice.wrong" in r.codes(), r.dump())
        assertTrue(r.errors.first { it.code == "choice.wrong" }.detail.contains("60 W"))
    }

    @Test
    fun `socket wire too thin for B16`() {
        val r = Solutions.socket(cs = CrossSection.S1_5).check()
        assertTrue("cs.phase" in r.codes(), r.dump())
    }

    @Test
    fun `socket without PE`() {
        val r = Solutions.socket(withPe = false).check()
        assertTrue("load.nope" in r.codes(), r.dump())
    }

    @Test
    fun `short circuit trips the breaker`() {
        val s = Solutions.socket().let {
            it.connect(TermRef("socket", "L"), TermRef("socket", "N"), WireColor.BROWN, CrossSection.S2_5)
        }
        val r = s.check()
        assertTrue("fault.trip" in r.codes(), r.dump())
        val sim = s.simulate(emptyMap())
        assertEquals(setOf("sup"), sim.tripped)
    }

    @Test
    fun `switch in neutral is an error even though the lamp works`() {
        val s = Solutions.lampSwitch(switchInNeutral = true)
        val sim = s.simulate(mapOf("sw" to 1))
        assertTrue(sim.loads.getValue("lamp").on, "lampa świeci – błąd jest „niewidoczny\"")
        val r = s.check()
        assertTrue("switch.neutral" in r.codes(), r.dump())
    }

    @Test
    fun `staircase must toggle from both places`() {
        val bad = Solutions.stairs(crossed = true).check()
        assertFalse(bad.passed, bad.dump())
        val good = Solutions.stairs().simulate(mapOf("s1" to 0, "s2" to 0)).loads.getValue("lamp").on
        val flipped = Solutions.stairs().simulate(mapOf("s1" to 1, "s2" to 0)).loads.getValue("lamp").on
        assertTrue(good != flipped)
    }

    @Test
    fun `bell button on mains side is rejected`() {
        val r = Solutions.bell(buttonOnMains = true).check()
        assertTrue("button.mains" in r.codes(), r.dump())
    }

    @Test
    fun `bell rings only with button pressed`() {
        val s = Solutions.bell(false)
        assertTrue(s.simulate(mapOf("btn" to 1)).loads.getValue("bell").on)
        assertFalse(s.simulate(mapOf("btn" to 0)).loads.getValue("bell").on)
    }

    @Test
    fun `neutral bypassing RCD trips it`() {
        val s = Solutions.flat(neutralBypassesRcd = true)
        val sim = s.simulate(emptyMap())
        assertTrue("rcd" in sim.tripped, sim.trips.toString())
        assertFalse(s.check().passed)
    }

    @Test
    fun `feeder protected only by 25A fuse needs 4 mm2`() {
        val r = Solutions.flat(feed = CrossSection.S2_5).check()
        assertTrue("cs.phase" in r.codes(), r.dump())
    }

    @Test
    fun `breaker selection`() {
        val r = solve("3-2") { choose("rcd", "rcd40_100_A"); choose("m1", "B16"); choose("m2", "B16"); choose("m3", "B20") }.check()
        val codes = r.codes()
        assertTrue("prot.rcd" in codes, r.dump())
        assertTrue("prot.rating" in codes, r.dump())
        assertTrue("cs.phase" in codes, r.dump())
        val small = solve("3-2") { choose("rcd", "rcd16_30_A"); choose("m1", "B10"); choose("m2", "B16"); choose("m3", "B16") }.check()
        assertTrue("prot.rcd.in" in small.codes(), small.dump())
        val ac = solve("3-2") { choose("rcd", "rcd40_30_AC"); choose("m1", "B10"); choose("m2", "B16"); choose("m3", "B16") }
        assertTrue(ac.check(Difficulty.MEDIUM).passed)
        val acIssues = ac.check(Difficulty.MEDIUM).issues
        assertEquals(2, acIssues.size) // oba obwody gniazd
        assertTrue(acIssues.all { it.severity == Severity.WARNING && it.code == "prot.rcd.type" })
        assertFalse(ac.check(Difficulty.HARD).passed)
    }

    @Test
    fun `phase rotation is a warning on medium and an error on hard`() {
        val s = Solutions.power400(swapL2L3 = true)
        val medium = s.check(Difficulty.MEDIUM)
        assertTrue(medium.passed, medium.dump())
        assertTrue("load.rotation" in medium.codes())
        assertFalse(s.check(Difficulty.HARD).passed)
    }

    @Test
    fun `three phase socket needs 3P breaker of right rating`() {
        val r = Solutions.power400(mcb = "C25").check()
        assertTrue("prot.rating" in r.codes(), r.dump())
    }

    @Test
    fun `surge arrester must be before RCD`() {
        val r = Solutions.house(spdAfterRcd = true).check(Difficulty.MEDIUM)
        assertTrue("prot.spd" in r.codes(), r.dump())
    }

    @Test
    fun `phase balance`() {
        val r = Solutions.house(allOnL1 = true).check(Difficulty.MEDIUM)
        assertTrue("balance" in r.codes(), r.dump())
    }

    @Test
    fun `green-yellow used as phase`() {
        val s = solve("1-3") {
            w("sup.L", "sw.L", WireColor.GREEN_YELLOW, CrossSection.S1_5); w("sw.P", "lamp.L", WireColor.BLACK, CrossSection.S1_5)
            w("sup.N", "lamp.N", WireColor.BLUE, CrossSection.S1_5); w("sup.PE", "lamp.PE", WireColor.GREEN_YELLOW, CrossSection.S1_5)
        }
        assertTrue("color.gy" in s.check().codes())
    }

    @Test
    fun `terminal capacity is enforced`() {
        val s = Solutions.socket()
        assertTrue(s.connectProblem(TermRef("sup", "L"), TermRef("socket", "N")) != null)
    }
}
