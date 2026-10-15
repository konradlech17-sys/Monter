package pl.monter.core.exam

import kotlin.random.Random

/** Działy przygotowania do egzaminu kwalifikacyjnego SEP (grupa 1, eksploatacja, do 1 kV). */
enum class SepCategory(val title: String, val icon: String) {
    LAW("Przepisy i organizacja pracy", "📜"),
    SHOCK("Ochrona przeciwporażeniowa", "⚡"),
    MEASURE("Pomiary i badania", "📟"),
    INSTALL("Instalacje, przewody, zabezpieczenia", "🔌"),
    RESCUE("Pierwsza pomoc i ppoż.", "🚑"),
}

/**
 * Pytanie testowe. [answers] zawiera odpowiedzi w kolejności „z bazy" – poprawna ma indeks [correct];
 * przed pokazaniem graczowi odpowiedzi są tasowane ([shuffled]).
 */
data class Question(
    val id: String,
    val category: SepCategory,
    val text: String,
    val answers: List<String>,
    val correct: Int,
    /** Wyjaśnienie – najważniejsza część nauki. */
    val explanation: String,
    /** Skąd pochodzi zasada (norma, przepis). */
    val source: String? = null,
    /** Pytanie wygenerowane losowo (zadanie obliczeniowe). */
    val generated: Boolean = false,
) {
    init {
        require(answers.size >= 2 && correct in answers.indices) { "Błędne pytanie $id" }
    }

    /** Kopia z przetasowanymi odpowiedziami (indeks poprawnej odpowiedzi jest przeliczany). */
    fun shuffled(random: Random): Question {
        val order = answers.indices.shuffled(random)
        return copy(answers = order.map { answers[it] }, correct = order.indexOf(correct))
    }
}

internal fun q(
    id: String, cat: SepCategory, text: String, correct: String, wrong: List<String>, explanation: String, source: String? = null,
) = Question(id, cat, text, listOf(correct) + wrong, 0, explanation, source)
