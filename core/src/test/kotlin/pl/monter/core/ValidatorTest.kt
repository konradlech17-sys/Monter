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
        assertTrue(Levels.all.size >= 35, "Poziomów: ${Levels.all.size}")
        assertEquals(Levels.all.size, Levels.all.map { it.id }.toSet().size, "Zdublowane id poziomów")
        for ((id, s) in Solutions.all) {
            val r = s.check(Difficulty.HARD)
            assertTrue(r.issues.isEmpty(), "Poziom $id:\n${r.dump()}")
        }
    }

    @Test
    fun `starting board never passes`() {
        for (level in Levels.all.filter { it.solution.wires.isNotEmpty() || it.story != null }) {
            val r = BuildSession(level).check(Difficulty.EASY)
            assertFalse(r.passed, "Poziom ${level.id} zalicza się bez żadnej pracy")
        }
    }

    @Test
    fun `service levels start broken and explain the fault`() {
        val service = Levels.all.filter { it.story != null }
        assertEquals(8, service.size)
        for (level in service) {
            val r = BuildSession(level).check(Difficulty.EASY)
            assertTrue(r.errors.isNotEmpty(), level.id)
            assertTrue(Solutions.all.getValue(level.id).check(Difficulty.HARD).passed, level.id)
        }
    }

    @Test
    fun `every part is reachable on the board and inside bounds`() {
        for (level in Levels.all) for (p in level.parts) {
            assertTrue(p.x >= 0 && p.y >= 0 && p.x + p.kind.w <= level.width + 1 && p.y + p.kind.h <= level.height + 1, "${level.id}/${p.id} poza planszą")
        }
    }

    @Test
    fun `parts do not overlap`() {
        for (level in Levels.all) {
            val ps = level.parts
            for (i in ps.indices) for (j in i + 1 until ps.size) {
                val a = ps[i]; val b = ps[j]
                val overlap = a.x < b.x + b.kind.w && b.x < a.x + a.kind.w && a.y < b.y + b.kind.h && b.y < a.y + a.kind.h
                assertFalse(overlap, "${level.id}: ${a.id} nachodzi na ${b.id}")
            }
        }
    }

    @Test
    fun `motor without N works and wrong rotation is an error`() {
        val sim = Solutions.all.getValue("5-2").simulate(emptyMap())
        assertTrue(sim.loads.getValue("motor").on)
        val broken = BuildSession(Levels.byId("6-7")!!).check(Difficulty.EASY)
        assertTrue("load.rotation" in broken.codes(), broken.dump())
    }

    @Test
    fun `two RCDs need separate neutral bars`() {
        val s = solve("4-3") {
            // wzorcowe przewody, ale obwód łazienki bierze N z szyny pierwszego RCD
            for (w in Levels.byId("4-3")!!.solution.wires) {
                val b = if (w.b.part == "c3" && w.b.terminal == "N") pl.monter.core.model.TermRef("nbar1", "4") else w.b
                s = s.connect(w.a, b, w.color, w.cs)
            }
        }
        val sim = s.simulate(emptyMap())
        assertTrue(sim.tripped.any { it.startsWith("rcd") }, sim.trips.toString())
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
        val r = solve("4-2") { choose("rcd", "rcd40_100_A"); choose("m1", "B16"); choose("m2", "B16"); choose("m3", "B20") }.check()
        val codes = r.codes()
        assertTrue("prot.rcd" in codes, r.dump())
        assertTrue("prot.rating" in codes, r.dump())
        assertTrue("cs.phase" in codes, r.dump())
        val small = solve("4-2") { choose("rcd", "rcd16_30_A"); choose("m1", "B10"); choose("m2", "B16"); choose("m3", "B16") }.check()
        assertTrue("prot.rcd.in" in small.codes(), small.dump())
        val ac = solve("4-2") { choose("rcd", "rcd40_30_AC"); choose("m1", "B10"); choose("m2", "B16"); choose("m3", "B16") }
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
