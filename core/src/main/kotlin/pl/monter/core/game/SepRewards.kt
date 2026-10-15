package pl.monter.core.game

import pl.monter.core.exam.ExamResult
import pl.monter.core.exam.Question
import pl.monter.core.exam.SepTrainer

/** Nagrody za naukę do SEP – iskry wydasz w sklepie gry. */
object SepRewards {
    const val PER_CORRECT = 3
    const val EXAM_PASS_FIRST = 200
    const val EXAM_PASS_AGAIN = 60

    data class Outcome(val save: SaveData, val sparks: Int, val newAchievements: List<Achievement>)

    private fun finish(save: SaveData, sparks: Int, now: Long): Outcome {
        val next = save.copy(sparks = save.sparks + sparks, totalEarned = save.totalEarned + sparks, updatedAt = now)
        val unlocked = Achievements.newlyUnlocked(next, null)
        return Outcome(next.copy(achievements = next.achievements + unlocked.map { it.id }), sparks, unlocked)
    }

    /** Odpowiedź w trybie nauki. */
    fun answer(save: SaveData, q: Question, chosen: Int, now: Long): Outcome {
        val ok = chosen == q.correct
        return finish(save.copy(sep = save.sep.record(q.id, ok, q.generated)), if (ok) PER_CORRECT else 0, now)
    }

    /** Zakończony egzamin próbny – zapisujemy wynik każdego pytania i przyznajemy nagrodę. */
    fun exam(save: SaveData, questions: List<Question>, answers: List<Int?>, now: Long): Pair<Outcome, ExamResult> {
        val result = SepTrainer.grade(questions, answers)
        var sep = save.sep
        questions.forEachIndexed { i, q -> sep = sep.record(q.id, answers.getOrNull(i) == q.correct, q.generated) }
        sep = sep.copy(
            examsTaken = sep.examsTaken + 1,
            examsPassed = sep.examsPassed + if (result.passed) 1 else 0,
            bestExamPercent = maxOf(sep.bestExamPercent, result.percent),
        )
        val sparks = result.correct * PER_CORRECT + when {
            !result.passed -> 0
            save.sep.examsPassed == 0 -> EXAM_PASS_FIRST
            else -> EXAM_PASS_AGAIN
        }
        return finish(save.copy(sep = sep), sparks, now) to result
    }
}
