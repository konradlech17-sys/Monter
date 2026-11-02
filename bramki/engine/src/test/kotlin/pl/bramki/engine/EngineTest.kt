package pl.bramki.engine

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OpTest {
    @Test
    fun `operacje są monotoniczne – więcej na wejściu to więcej na wyjściu`() {
        val ops = listOf(Op.Add(50), Op.Sub(50), Op.Mul(2.0), Op.Div(3.0), Op.Pct(50), Op.Pct(-30))
        for (op in ops) {
            var prev = op.apply(0.0)
            for (v in 1..2000) {
                val cur = op.apply(v.toDouble())
                assertTrue(cur >= prev, "${op.label} nie jest monotoniczna przy $v")
                prev = cur
            }
        }
    }

    @Test
    fun `odejmowanie nie schodzi poniżej zera`() {
        assertEquals(0.0, Op.Sub(100).apply(30.0))
        assertEquals(0.0, Op.Pct(-150).apply(30.0))
    }

    @Test
    fun `etykiety po polsku`() {
        assertEquals("+320", Op.Add(320).label)
        assertEquals("−50", Op.Sub(50).label)
        assertEquals("×2", Op.Mul(2.0).label)
        assertEquals("×1,5", Op.Mul(1.5).label)
        assertEquals("÷3", Op.Div(3.0).label)
        assertEquals("+50%", Op.Pct(50).label)
        assertEquals("−30%", Op.Pct(-30).label)
        assertEquals("+12,3 tys.", Op.Add(12_300).label)
        assertEquals("+4,5 mln", Op.Add(4_500_000).label)
    }

    @Test
    fun `procenty to mnożenie – a strata procentowa nie wraca sama`() {
        assertEquals(Op.Mul(1.5).apply(200.0), Op.Pct(50).apply(200.0))
        // 100 → −50% → +50% = 75, nie 100 (stąd wskazówka w grze)
        assertEquals(75.0, Op.Pct(50).apply(Op.Pct(-50).apply(100.0)))
    }
}

class FormatTest {
    @Test
    fun `formatowanie liczb`() {
        assertEquals("999", Format.compact(999.0))
        assertEquals("9999", Format.compact(9_999.0))
        assertEquals("10 tys.", Format.compact(10_000.0))
        assertEquals("12,3 tys.", Format.compact(12_345.0))
        assertEquals("120 tys.", Format.compact(120_400.0))
        assertEquals("1,2 mln", Format.compact(1_234_567.0))
        assertEquals("3 mld", Format.compact(3_000_000_000.0))
        assertEquals("1 234 567", Format.full(1_234_567.0))
        assertEquals("42", Format.full(42.0))
        assertEquals("62%", Format.percent(0.624))
    }

    @Test
    fun `ładne zaokrąglanie bramek`() {
        assertEquals(140, TrackGenerator.nice(137.0))
        assertEquals(2900, TrackGenerator.nice(2861.0))
        assertEquals(48_000, TrackGenerator.nice(48_123.0))
        assertEquals(7, TrackGenerator.nice(7.2))
        assertEquals(1, TrackGenerator.nice(0.3))
    }
}

class TrackTest {
    @Test
    fun `każdy tryb generuje trasę o zadanej liczbie bramek, w rosnącej kolejności`() {
        for (mode in Modes.ALL) for (seed in 0L until 200L) {
            val t = TrackGenerator.generate(mode, seed)
            assertEquals(mode.gateCount, t.gates.size, "${mode.name}/$seed")
            assertTrue(t.items.zipWithNext().all { (a, b) -> a.z < b.z }, "elementy muszą być posortowane po z")
            assertTrue(t.length > t.items.last().z, "meta za ostatnim elementem")
            assertTrue(t.gates.all { it.z >= TrackGenerator.FIRST_GATE_Z })
        }
    }

    @Test
    fun `ta sama ziarno daje tę samą trasę`() {
        val a = TrackGenerator.generate(Modes.EPOKI, 42)
        val b = TrackGenerator.generate(Modes.EPOKI, 42)
        assertEquals(a.items, b.items)
    }

    @Test
    fun `tryb Fortuna opisuje mnożenie procentami, pozostałe znakiem razy`() {
        val f = TrackGenerator.generate(Modes.FORTUNA, 1)
        assertTrue(f.gates.flatMap { listOf(it.left, it.right) }.none { it is Op.Mul || it is Op.Div })
        assertTrue(f.walls.all { it.op is Op.Pct })
        val e = TrackGenerator.generate(Modes.EPOKI, 1)
        assertTrue(e.gates.flatMap { listOf(it.left, it.right) }.none { it is Op.Pct })
    }

    @Test
    fun `pierwsze dwie bramki to zawsze prosty wybór dobra kontra zła`() {
        for (mode in Modes.ALL) for (seed in 0L until 100L) {
            val t = TrackGenerator.generate(mode, seed)
            for (g in t.gates.take(2)) assertTrue(g.left.positive != g.right.positive, "${mode.name}/$seed: ${g.left.label} vs ${g.right.label}")
        }
    }
}

class AnalysisTest {
    @Test
    fun `zachłanny wybór jest optymalny – sprawdzone brutalnie na wszystkich 2^n ścieżkach`() {
        val mode = Modes.EPOKI.copy(gateCount = 10)
        for (seed in 0L until 60L) {
            val t = TrackGenerator.generate(mode, seed)
            var best = 0.0
            for (mask in 0 until (1 shl t.gates.size)) {
                var v = mode.start
                for ((i, g) in t.gates.withIndex()) v = (if (mask and (1 shl i) != 0) g.left else g.right).applyRounded(v)
                if (v > best) best = v
            }
            assertEquals(best, Analysis.optimalValue(t), "seed $seed")
        }
    }

    @Test
    fun `najgorsza ścieżka nie jest lepsza od najlepszej`() {
        for (mode in Modes.ALL) for (seed in 0L until 50L) {
            val t = TrackGenerator.generate(mode, seed)
            assertTrue(Analysis.worstValue(t) <= Analysis.optimalValue(t))
        }
    }

    @Test
    fun `kalibracja progów – gra na „zielone bez myślenia” nie daje najwyższego szczebla w większości gier`() {
        val rnd = Random(7)
        for (mode in Modes.ALL) {
            var top = 0
            val n = 500
            for (seed in 0L until n.toLong()) {
                val t = TrackGenerator.generate(mode, seed)
                val v = Analysis.naiveValue(t, rnd)
                if (mode.tierIndex(v, Analysis.optimalValue(t)) == mode.tiers.lastIndex) top++
            }
            assertTrue(top < n * 0.35, "${mode.name}: naiwna gra osiąga szczyt w $top/$n gier")
        }
    }
}

class RunTest {
    /** Przejeżdża całą trasę, w każdej bramce wybierając stronę wg [choose]; omija ściany. */
    private fun play(track: Track, choose: (Item.GatePair) -> Side): RunResult {
        val run = Run(track)
        var guard = 0
        while (!run.finished && guard++ < 100_000) {
            val next = run.visibleItems(1000.0).firstOrNull()
            when (next) {
                is Item.GatePair -> run.setTarget(if (choose(next) == Side.LEFT) -0.8 else 0.8)
                is Item.Wall -> run.setTarget(if (next.side == Side.LEFT) 0.8 else -0.8)
                null -> Unit
            }
            run.step(1.0 / 60)
        }
        assertTrue(run.finished, "przebieg powinien się skończyć")
        return assertNotNull(run.result)
    }

    private fun greedy(g: Item.GatePair, v: Double) = if (g.left.applyRounded(v) >= g.right.applyRounded(v)) Side.LEFT else Side.RIGHT

    @Test
    fun `gracz optymalny kończy z wartością optymalną i na najwyższym szczeblu`() {
        for (mode in Modes.ALL) for (seed in 0L until 20L) {
            val t = TrackGenerator.generate(mode, seed)
            val run = Run(t)
            // Zachłanny wybór zależy od bieżącej wartości – czytamy ją z przebiegu.
            var guard = 0
            while (!run.finished && guard++ < 100_000) {
                when (val next = run.visibleItems(1000.0).firstOrNull()) {
                    is Item.GatePair -> run.setTarget(if (greedy(next, run.value) == Side.LEFT) -0.8 else 0.8)
                    is Item.Wall -> run.setTarget(if (next.side == Side.LEFT) 0.8 else -0.8)
                    null -> Unit
                }
                run.step(1.0 / 60)
            }
            val r = assertNotNull(run.result)
            assertEquals(r.optimal, r.value, "${mode.name}/$seed")
            assertEquals(mode.tiers.lastIndex, r.tierIndex)
            assertEquals(3, r.stars)
            assertTrue(r.mistakes.isEmpty())
            assertEquals(0, r.wallsHit)
            assertEquals(mode.gateCount, r.decisions.size)
            if (mode.enemyFraction != null) assertEquals(true, r.battleWon)
        }
    }

    @Test
    fun `gracz najgorszy kończy na dole i dostaje listę błędów`() {
        val t = TrackGenerator.generate(Modes.ARMIA, 3)
        var v = Modes.ARMIA.start
        val r = play(t) { g ->
            val side = if (g.left.applyRounded(v) <= g.right.applyRounded(v)) Side.LEFT else Side.RIGHT
            v = g.op(side).applyRounded(v)
            side
        }
        assertEquals(Analysis.worstValue(t), r.value)
        assertTrue(r.tierIndex <= 1)
        assertTrue(r.mistakes.isNotEmpty())
        assertEquals(false, r.battleWon)
        assertTrue(Analysis.describe(r.mistakes.first(), Modes.ARMIA).startsWith("Bramka "))
    }

    @Test
    fun `wjazd w ścianę kosztuje i jest liczony`() {
        val mode = Modes.EPOKI
        val t = (0L until 500L).map { TrackGenerator.generate(mode, it) }.first { it.walls.isNotEmpty() }
        val run = Run(t)
        val wall = t.walls.first()
        var guard = 0
        while (!run.finished && guard++ < 100_000) {
            run.setTarget(if (wall.side == Side.LEFT) -0.8 else 0.8) // celowo w ścianę, a bramki po tej samej stronie
            run.step(1.0 / 60)
        }
        val r = assertNotNull(run.result)
        assertTrue(r.wallsHit >= 1)
        val events = run.drainEvents()
        assertTrue(events.count { it is GameEvent.WallHit } == r.wallsHit)
        assertTrue(events.last() is GameEvent.Finished)
        assertTrue(run.drainEvents().isEmpty(), "kolejka zdarzeń opróżnia się po odczycie")
    }

    @Test
    fun `zdarzenia – bramka, zmiana szczebla, meta`() {
        val t = TrackGenerator.generate(Modes.KOSMOS, 11)
        val run = Run(t)
        val events = ArrayList<GameEvent>()
        var guard = 0
        while (!run.finished && guard++ < 100_000) {
            when (val next = run.visibleItems(1000.0).firstOrNull()) {
                is Item.GatePair -> run.setTarget(if (greedy(next, run.value) == Side.LEFT) -0.8 else 0.8)
                is Item.Wall -> run.setTarget(if (next.side == Side.LEFT) 0.8 else -0.8)
                null -> Unit
            }
            run.step(1.0 / 60)
            events += run.drainEvents()
        }
        assertEquals(Modes.KOSMOS.gateCount, events.count { it is GameEvent.GatePassed })
        assertTrue(events.any { it is GameEvent.TierChanged })
        assertTrue(events.last() is GameEvent.Finished)
        assertEquals(1.0, run.progress)
    }

    @Test
    fun `ruch boczny jest ograniczony i trzyma się toru`() {
        val run = Run(TrackGenerator.generate(Modes.EPOKI, 1))
        run.setTarget(5.0)
        run.step(0.1)
        assertTrue(run.x in 0.0..1.0)
        assertTrue(run.x < 1.0, "nie teleportuje się na krawędź w jednym kroku")
        repeat(100) { run.step(0.1) }
        assertEquals(1.0, run.x)
        run.nudge(-10.0)
        repeat(100) { run.step(0.1) }
        assertEquals(-1.0, run.x)
    }
}

class ModeTest {
    @Test
    fun `szczeble – progi, postęp i następny próg`() {
        val m = Modes.EPOKI
        assertEquals(0, m.tierIndex(0.0, 1000.0))
        assertEquals(m.tiers.lastIndex, m.tierIndex(1000.0, 1000.0))
        assertEquals(m.tiers.lastIndex, m.tierIndex(5000.0, 1000.0))
        assertEquals(1.0, m.tierProgress(1000.0, 1000.0))
        assertEquals(null, m.nextTierValue(1000.0, 1000.0))
        val next = assertNotNull(m.nextTierValue(0.0, 1000.0))
        assertEquals(m.tiers[1].fraction * 1000, next)
        assertEquals(0, m.tierIndex(10.0, 0.0), "brak optimum nie wywraca gry")
    }

    @Test
    fun `każdy tryb ma 8 szczebli z ciekawostką i unikalne id`() {
        assertEquals(Modes.ALL.size, Modes.ALL.map { it.id }.toSet().size)
        for (m in Modes.ALL) {
            assertEquals(8, m.tiers.size, m.name)
            assertTrue(m.tiers.all { it.fact.length > 40 }, "${m.name}: ciekawostki mają coś mówić")
            assertTrue(m.tips.size >= 5)
            assertEquals(m, Modes.byId(m.id))
        }
    }
}
