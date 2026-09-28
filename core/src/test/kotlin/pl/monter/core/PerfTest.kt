package pl.monter.core

import pl.monter.core.game.Difficulty
import kotlin.test.Test
import kotlin.test.assertTrue

class PerfTest {
    @Test
    fun `validation is fast enough for a phone`() {
        repeat(3) { Solutions.all.values.forEach { it.validate(Difficulty.HARD) } } // rozgrzewka JIT
        for ((id, s) in Solutions.all) {
            val t0 = System.nanoTime()
            s.validate(Difficulty.HARD)
            val ms = (System.nanoTime() - t0) / 1_000_000
            println("walidacja $id: $ms ms")
            assertTrue(ms < 500, "Poziom $id: $ms ms")
        }
    }
}
