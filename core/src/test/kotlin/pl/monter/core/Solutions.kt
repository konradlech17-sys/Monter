package pl.monter.core

import pl.monter.core.game.BuildSession
import pl.monter.core.level.Level
import pl.monter.core.level.Levels
import pl.monter.core.model.CrossSection
import pl.monter.core.model.CrossSection.S0_5
import pl.monter.core.model.CrossSection.S1_5
import pl.monter.core.model.CrossSection.S2_5
import pl.monter.core.model.CrossSection.S4
import pl.monter.core.model.CrossSection.S6
import pl.monter.core.model.TermRef
import pl.monter.core.model.WireColor
import pl.monter.core.model.WireColor.BLACK
import pl.monter.core.model.WireColor.BLUE
import pl.monter.core.model.WireColor.BROWN
import pl.monter.core.model.WireColor.GREEN_YELLOW
import pl.monter.core.model.WireColor.GREY
import pl.monter.core.model.WireColor.WHITE

/** DSL do budowania rozwiązań w testach. */
class Sol(level: Level) {
    var s = BuildSession(level)
    fun w(a: String, b: String, c: WireColor, cs: CrossSection) {
        val (pa, ta) = a.split(".", limit = 2); val (pb, tb) = b.split(".", limit = 2)
        val before = s
        s = s.connect(TermRef(pa, ta), TermRef(pb, tb), c, cs)
        require(s !== before) { "Nie udało się połączyć $a – $b: ${before.connectProblem(TermRef(pa, ta), TermRef(pb, tb))}" }
    }
    fun choose(part: String, option: String) { s = s.choose(part, option) }
}

fun solve(id: String, block: Sol.() -> Unit): BuildSession = Sol(Levels.byId(id)!!).apply(block).s

/** Wzorcowe rozwiązania wszystkich poziomów. */
object Solutions {
    val all: Map<String, BuildSession> by lazy {
        mapOf(
            "1-1" to solve("1-1") { choose("lamp", "led_e27") },
            "1-2" to socket(),
            "1-3" to lampSwitch(),
            "1-4" to solve("1-4") {
                w("sup.L", "s1.L", BROWN, S2_5); w("sup.N", "s1.N", BLUE, S2_5); w("sup.PE", "s1.PE", GREEN_YELLOW, S2_5)
                w("s1.L", "s2.L", BROWN, S2_5); w("s1.N", "s2.N", BLUE, S2_5); w("s1.PE", "s2.PE", GREEN_YELLOW, S2_5)
            },
            "2-1" to solve("2-1") {
                w("sup.L", "sw.L", BROWN, S1_5); w("sw.P1", "la.L", BLACK, S1_5); w("sw.P2", "lb.L", BLACK, S1_5)
                w("sup.N", "wn.1", BLUE, S1_5); w("wn.2", "la.N", BLUE, S1_5); w("wn.3", "lb.N", BLUE, S1_5)
                w("sup.PE", "wpe.1", GREEN_YELLOW, S1_5); w("wpe.2", "la.PE", GREEN_YELLOW, S1_5); w("wpe.3", "lb.PE", GREEN_YELLOW, S1_5)
            },
            "2-2" to stairs(),
            "2-3" to solve("2-3") {
                w("sup.L", "s1.C", BROWN, S1_5)
                w("s1.P1", "x.1", BLACK, S1_5); w("s1.P2", "x.2", BLACK, S1_5)
                w("x.3", "s2.P1", BLACK, S1_5); w("x.4", "s2.P2", BLACK, S1_5)
                w("s2.C", "lamp.L", BLACK, S1_5); w("sup.N", "lamp.N", BLUE, S1_5); w("sup.PE", "lamp.PE", GREEN_YELLOW, S1_5)
            },
            "2-4" to bell(buttonOnMains = false),
            "2-5" to solve("2-5") {
                w("sup.L", "pir.L", BROWN, S1_5); w("sup.N", "wn.1", BLUE, S1_5)
                w("wn.2", "pir.N", BLUE, S1_5); w("wn.3", "lamp.N", BLUE, S1_5)
                w("pir.OUT", "lamp.L", BLACK, S1_5); w("sup.PE", "lamp.PE", GREEN_YELLOW, S1_5)
            },
            "3-1" to flat(),
            "3-2" to solve("3-2") {
                choose("rcd", "rcd40_30_A"); choose("m1", "B10"); choose("m2", "B16"); choose("m3", "B16")
            },
            "3-3" to power400(),
            "3-4" to house(),
        )
    }

    fun socket(cs: CrossSection = S2_5, withPe: Boolean = true) = solve("1-2") {
        w("sup.L", "socket.L", BROWN, cs); w("sup.N", "socket.N", BLUE, cs)
        if (withPe) w("sup.PE", "socket.PE", GREEN_YELLOW, cs)
    }

    fun lampSwitch(switchInNeutral: Boolean = false) = solve("1-3") {
        if (!switchInNeutral) {
            w("sup.L", "sw.L", BROWN, S1_5); w("sw.P", "lamp.L", BLACK, S1_5); w("sup.N", "lamp.N", BLUE, S1_5)
        } else {
            w("sup.N", "sw.L", BLUE, S1_5); w("sw.P", "lamp.N", BLUE, S1_5); w("sup.L", "lamp.L", BROWN, S1_5)
        }
        w("sup.PE", "lamp.PE", GREEN_YELLOW, S1_5)
    }

    fun stairs(crossed: Boolean = false) = solve("2-2") {
        w("sup.L", "s1.C", BROWN, S1_5)
        w("s1.P1", "s2.P1", BLACK, S1_5)
        if (!crossed) w("s1.P2", "s2.P2", BLACK, S1_5) else w("s1.P2", "s2.C", BLACK, S1_5)
        w(if (crossed) "s2.P2" else "s2.C", "lamp.L", BLACK, S1_5)
        w("sup.N", "lamp.N", BLUE, S1_5); w("sup.PE", "lamp.PE", GREEN_YELLOW, S1_5)
    }

    fun bell(buttonOnMains: Boolean) = solve("2-4") {
        if (!buttonOnMains) {
            w("sup.L", "tr.L", BROWN, S1_5); w("sup.N", "tr.N", BLUE, S1_5)
            w("tr.S1", "bell.A", WHITE, S0_5); w("tr.S2", "btn.1", WHITE, S0_5); w("btn.2", "bell.B", WHITE, S0_5)
        } else {
            w("sup.L", "btn.1", BROWN, S1_5); w("btn.2", "tr.L", BLACK, S1_5); w("sup.N", "tr.N", BLUE, S1_5)
            w("tr.S1", "bell.A", WHITE, S0_5); w("tr.S2", "bell.B", WHITE, S0_5)
        }
    }

    fun flat(neutralBypassesRcd: Boolean = false, feed: CrossSection = S4) = solve("3-1") {
        w("main.2", "rcd.1", BROWN, feed); w("main.N'", "rcd.N", BLUE, feed)
        w("rcd.2", "m1.1", BROWN, feed); w("m1.1", "m2.1", BROWN, feed); w("m2.1", "m3.1", BROWN, feed)
        if (neutralBypassesRcd) w("main.N'", "nbar.1", BLUE, feed) else w("rcd.N'", "nbar.1", BLUE, feed)
        w("m1.2", "c1.L", BROWN, S1_5); w("nbar.2", "c1.N", BLUE, S1_5); w("pebar.2", "c1.PE", GREEN_YELLOW, S1_5)
        w("m2.2", "c2.L", BROWN, S2_5); w("nbar.3", "c2.N", BLUE, S2_5); w("pebar.3", "c2.PE", GREEN_YELLOW, S2_5)
        w("m3.2", "c3.L", BROWN, S2_5); w("nbar.4", "c3.N", BLUE, S2_5); w("pebar.4", "c3.PE", GREEN_YELLOW, S2_5)
    }

    fun power400(swapL2L3: Boolean = false, mcb: String = "C16") = solve("3-3") {
        choose("mcb", mcb)
        w("sup.L1", "rcd.1", BROWN, S4); w("sup.L2", "rcd.3", BLACK, S4); w("sup.L3", "rcd.5", GREY, S4); w("sup.N", "rcd.N", BLUE, S4)
        w("rcd.2", "mcb.1", BROWN, S4); w("rcd.4", "mcb.3", BLACK, S4); w("rcd.6", "mcb.5", GREY, S4)
        w("mcb.2", "gn.L1", BROWN, S2_5)
        if (!swapL2L3) { w("mcb.4", "gn.L2", BLACK, S2_5); w("mcb.6", "gn.L3", GREY, S2_5) } else { w("mcb.4", "gn.L3", BLACK, S2_5); w("mcb.6", "gn.L2", GREY, S2_5) }
        w("rcd.N'", "gn.N", BLUE, S2_5); w("sup.PE", "gn.PE", GREEN_YELLOW, S2_5)
    }

    fun house(spdAfterRcd: Boolean = false, allOnL1: Boolean = false) = solve("3-4") {
        if (!spdAfterRcd) {
            w("main.2", "spd.L1", BROWN, S6); w("main.4", "spd.L2", BLACK, S6); w("main.6", "spd.L3", GREY, S6); w("main.N'", "spd.N", BLUE, S6)
        } else {
            w("rcd.2", "spd.L1", BROWN, S6); w("rcd.4", "spd.L2", BLACK, S6); w("rcd.6", "spd.L3", GREY, S6); w("rcd.N'", "spd.N", BLUE, S6)
        }
        w("spd.PE", "pebar.2", GREEN_YELLOW, S6)
        w("main.2", "rcd.1", BROWN, S6); w("main.4", "rcd.3", BLACK, S6); w("main.6", "rcd.5", GREY, S6); w("main.N'", "rcd.N", BLUE, S6)
        if (!spdAfterRcd) {
            w("rcd.2", "m1.1", BROWN, S6); w("rcd.4", "m2.1", BLACK, S6); w("rcd.6", "m3.1", GREY, S6)
        } else {
            w("m6.2", "m1.1", BROWN, S6); w("m6.4", "m2.1", BLACK, S6); w("m6.6", "m3.1", GREY, S6)
        }
        w("rcd.2", "m6.1", BROWN, S6); w("rcd.4", "m6.3", BLACK, S6); w("rcd.6", "m6.5", GREY, S6)
        if (!allOnL1) { w("m1.1", "m4.1", BROWN, S6); w("m2.1", "m5.1", BLACK, S6) } else { w("m1.1", "m4.1", BROWN, S6); w("m4.1", "m5.1", BROWN, S6) }
        w("rcd.N'", "nbar.1", BLUE, S6)
        val l = listOf("m1" to "c1", "m2" to "c2", "m3" to "c3", "m4" to "c4", "m5" to "c5")
        val cs = listOf(S1_5, S1_5, S2_5, S2_5, S2_5)
        val phaseColor = if (allOnL1) listOf(BROWN, BROWN, GREY, BROWN, BROWN) else listOf(BROWN, BLACK, GREY, BROWN, BLACK)
        l.forEachIndexed { i, (m, c) ->
            w("$m.2", "$c.L", phaseColor[i], cs[i]); w("nbar.${i + 2}", "$c.N", BLUE, cs[i]); w("pebar.${i + 3}", "$c.PE", GREEN_YELLOW, cs[i])
        }
        w("m6.2", "c6.L1", BROWN, S2_5); w("m6.4", "c6.L2", BLACK, S2_5); w("m6.6", "c6.L3", GREY, S2_5)
        w("nbar.7", "c6.N", BLUE, S2_5); w("pebar.8", "c6.PE", GREEN_YELLOW, S2_5)
    }
}
