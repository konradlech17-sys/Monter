package pl.monter.core.exam

import kotlinx.serialization.Serializable
import kotlin.random.Random

/**
 * Postępy nauki do SEP. Każde pytanie (lub typ zadania obliczeniowego) siedzi w „pudełku" 0–5
 * (metoda Leitnera): dobra odpowiedź przesuwa je wyżej, zła – na sam dół. Pytania z niższych pudełek
 * wracają częściej, więc najwięcej czasu spędzasz na tym, czego jeszcze nie umiesz.
 */
@Serializable
data class SepProgress(
    val boxes: Map<String, Int> = emptyMap(),
    val answered: Int = 0,
    val correct: Int = 0,
    val examsTaken: Int = 0,
    val examsPassed: Int = 0,
    val bestExamPercent: Int = 0,
    /** Liczba poprawnie rozwiązanych zadań obliczeniowych. */
    val calcCorrect: Int = 0,
) {
    fun box(id: String) = boxes[id] ?: 0

    /** Opanowanie działu w % (średni poziom pudełek względem maksimum). */
    fun mastery(category: SepCategory): Int {
        val ids = SepTrainer.idsIn(category)
        if (ids.isEmpty()) return 0
        return (ids.sumOf { box(it).coerceAtMost(SepTrainer.MAX_BOX) } * 100) / (ids.size * SepTrainer.MAX_BOX)
    }

    fun overallMastery(): Int {
        val ids = SepTrainer.allIds
        return (ids.sumOf { box(it).coerceAtMost(SepTrainer.MAX_BOX) } * 100) / (ids.size * SepTrainer.MAX_BOX)
    }

    /** Pytania, na które ostatnio odpowiedziano źle (pudełko 0, ale już widziane). */
    fun mistakes(): List<String> = boxes.filterValues { it == 0 }.keys.toList()

    fun record(id: String, ok: Boolean, generated: Boolean): SepProgress = copy(
        boxes = boxes + (id to if (ok) (box(id) + 1).coerceAtMost(SepTrainer.MAX_BOX) else 0),
        answered = answered + 1,
        correct = correct + if (ok) 1 else 0,
        calcCorrect = calcCorrect + if (ok && generated) 1 else 0,
    )

    fun merge(other: SepProgress): SepProgress = SepProgress(
        boxes = (boxes.keys + other.boxes.keys).associateWith { maxOf(box(it), other.box(it)) },
        answered = maxOf(answered, other.answered),
        correct = maxOf(correct, other.correct),
        examsTaken = maxOf(examsTaken, other.examsTaken),
        examsPassed = maxOf(examsPassed, other.examsPassed),
        bestExamPercent = maxOf(bestExamPercent, other.bestExamPercent),
        calcCorrect = maxOf(calcCorrect, other.calcCorrect),
    )
}

data class ExamResult(val total: Int, val correct: Int, val passed: Boolean, val wrong: List<Pair<Question, Int>>) {
    val percent get() = if (total == 0) 0 else correct * 100 / total
}

object SepTrainer {
    const val MAX_BOX = 5
    const val EXAM_SIZE = 30
    const val EXAM_MINUTES = 30
    /** Próg zaliczenia egzaminu próbnego w grze (prawdziwe komisje mają własne zasady). */
    const val PASS_PERCENT = 80

    val allIds: List<String> = QuestionBank.static.map { it.id } + Generators.ids

    fun idsIn(category: SepCategory): List<String> =
        QuestionBank.byCategory(category).map { it.id } + generatorsIn(category).map { it.second }

    private fun generatorsIn(category: SepCategory): List<Pair<(Random) -> Question, String>> =
        Generators.all.zip(Generators.ids).filter { (gen, _) -> gen(Random(0)).category == category }

    /** Tworzy pytanie o danym identyfikatorze (dla zadań obliczeniowych – z nowymi liczbami). */
    fun make(id: String, random: Random): Question? {
        QuestionBank.static.firstOrNull { it.id == id }?.let { return it.shuffled(random) }
        val idx = Generators.ids.indexOf(id)
        return if (idx >= 0) Generators.all[idx](random).shuffled(random) else null
    }

    /**
     * Seria do nauki: najpierw pytania z najniższych pudełek, w obrębie pudełka losowo.
     * @param category null = wszystkie działy; [onlyCalc] – tylko zadania obliczeniowe.
     */
    fun learningSet(progress: SepProgress, category: SepCategory?, size: Int, random: Random, onlyCalc: Boolean = false): List<Question> {
        val ids = when {
            onlyCalc -> Generators.ids
            category == null -> allIds
            else -> idsIn(category)
        }
        return ids.shuffled(random).sortedBy { progress.box(it) }.take(size).mapNotNull { make(it, random) }
    }

    fun mistakesSet(progress: SepProgress, random: Random): List<Question> =
        progress.mistakes().shuffled(random).mapNotNull { make(it, random) }

    /** Egzamin próbny: pytania ze wszystkich działów w proporcji do ich liczby + zadania obliczeniowe. */
    fun exam(random: Random): List<Question> {
        val calc = Generators.ids.shuffled(random).take(6)
        val static = QuestionBank.static.shuffled(random).take(EXAM_SIZE - calc.size).map { it.id }
        return (static + calc).shuffled(random).mapNotNull { make(it, random) }
    }

    fun grade(questions: List<Question>, answers: List<Int?>): ExamResult {
        val wrong = questions.indices.filter { answers.getOrNull(it) != questions[it].correct }.map { questions[it] to (answers.getOrNull(it) ?: -1) }
        val correct = questions.size - wrong.size
        val percent = if (questions.isEmpty()) 0 else correct * 100 / questions.size
        return ExamResult(questions.size, correct, percent >= PASS_PERCENT, wrong)
    }
}
