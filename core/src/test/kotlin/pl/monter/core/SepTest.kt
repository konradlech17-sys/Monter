package pl.monter.core

import pl.monter.core.exam.Generators
import pl.monter.core.exam.QuestionBank
import pl.monter.core.exam.SepCategory
import pl.monter.core.exam.SepProgress
import pl.monter.core.exam.SepTrainer
import pl.monter.core.game.SaveData
import pl.monter.core.game.SepRewards
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SepTest {
    @Test
    fun `static bank is well formed`() {
        val ids = QuestionBank.static.map { it.id }
        assertEquals(ids.size, ids.toSet().size, "Zdublowane id pytań")
        for (q in QuestionBank.static) {
            assertEquals(4, q.answers.size, q.id)
            assertEquals(4, q.answers.toSet().size, "Powtórzone odpowiedzi: ${q.id}")
            assertTrue(q.explanation.length > 30, "Za krótkie wyjaśnienie: ${q.id}")
        }
        for (c in SepCategory.entries) assertTrue(QuestionBank.byCategory(c).size >= 8, "Za mało pytań w dziale ${c.title}")
        println("Pytań w banku: ${QuestionBank.static.size}")
    }

    @Test
    fun `generators produce valid questions for many seeds`() {
        assertEquals(Generators.all.size, Generators.ids.size)
        Generators.all.zip(Generators.ids).forEach { (gen, id) ->
            repeat(400) { seed ->
                val q = gen(Random(seed))
                assertEquals(id, q.id)
                assertTrue(q.generated)
                assertTrue(q.answers.size >= 3, "$id: za mało odpowiedzi")
                assertEquals(q.answers.size, q.answers.toSet().size, "$id/$seed: powtórzone odpowiedzi ${q.answers}")
                val s = q.shuffled(Random(seed))
                assertEquals(q.answers[q.correct], s.answers[s.correct])
            }
        }
    }

    @Test
    fun `loop impedance numbers are right`() {
        // B16: Ia = 80 A, Zs ≤ 230/80 = 2,875 Ω
        val q = (0 until 2000).map { Generators.zsMax(Random(it)) }.first { "B16 " in it.text }
        assertEquals("2,88 Ω", q.answers[q.correct])
        val c10 = (0 until 2000).map { Generators.iaValue(Random(it)) }.first { "C10?" in it.text }
        assertEquals("100 A", c10.answers[c10.correct])
    }

    @Test
    fun `learning set prefers weak questions`() {
        val known = SepTrainer.allIds.associateWith { 5 } + ("shock.t04" to 0)
        val set = SepTrainer.learningSet(SepProgress(boxes = known), null, 5, Random(1))
        assertEquals("shock.t04", set.first().id)
        assertEquals(5, set.size)
    }

    @Test
    fun `exam has 30 questions and pass threshold`() {
        val exam = SepTrainer.exam(Random(7))
        assertEquals(SepTrainer.EXAM_SIZE, exam.size)
        val allRight = SepTrainer.grade(exam, exam.map { it.correct })
        assertTrue(allRight.passed); assertEquals(100, allRight.percent)
        val half = SepTrainer.grade(exam, exam.mapIndexed { i, q -> if (i % 2 == 0) q.correct else -1 })
        assertFalse(half.passed)
        assertEquals(15, half.wrong.size)
    }

    @Test
    fun `rewards and progress`() {
        val exam = SepTrainer.exam(Random(3))
        val (out, result) = SepRewards.exam(SaveData(), exam, exam.map { it.correct }, 1)
        assertTrue(result.passed)
        assertEquals(exam.size * SepRewards.PER_CORRECT + SepRewards.EXAM_PASS_FIRST, out.sparks)
        assertTrue("sep_ready" in out.save.achievements)
        assertTrue(out.save.sep.overallMastery() > 0)
        val again = SepRewards.exam(out.save, exam, exam.map { it.correct }, 2).first
        assertEquals(exam.size * SepRewards.PER_CORRECT + SepRewards.EXAM_PASS_AGAIN, again.sparks)
        // Zła odpowiedź spycha pytanie do pudełka 0
        val q = SepTrainer.make("shock.t04", Random(1))!!
        val wrong = SepRewards.answer(again.save, q, (q.correct + 1) % q.answers.size, 3).save
        assertEquals(0, wrong.sep.box("shock.t04"))
        assertTrue("shock.t04" in wrong.sep.mistakes())
        assertEquals(wrong, SaveData.fromJson(wrong.toJson()))
    }
}
