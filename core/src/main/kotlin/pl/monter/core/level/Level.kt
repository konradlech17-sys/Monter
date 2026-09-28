package pl.monter.core.level

import pl.monter.core.model.CrossSection
import pl.monter.core.model.Part
import pl.monter.core.model.TermRef
import pl.monter.core.model.Wire
import pl.monter.core.model.WireColor

/** Stan elementów sterujących (łączników) – id elementu → numer stanu. */
class Controls(val states: Map<String, Int>) {
    fun on(id: String) = (states[id] ?: 0) != 0
    fun bit(id: String, bit: Int) = ((states[id] ?: 0) shr bit) and 1 == 1
}

/** Cel funkcjonalny sprawdzany symulacją w każdym stanie łączników. */
sealed interface Goal {
    /** Odbiornik musi być poprawnie zasilony zawsze. */
    data class Powered(val part: String) : Goal

    /** Odbiornik ma działać dokładnie wtedy, gdy [expected] zwraca true. */
    data class Follows(val part: String, val describe: String, val expected: (Controls) -> Boolean) : Goal

    /** Przełączenie DOWOLNEGO z łączników zmienia stan odbiornika (łączniki schodowe / krzyżowe). */
    data class Toggles(val part: String, val controls: List<String>) : Goal

    /** Wystarczy spełnić jeden z zestawów celów (np. dowolne przypisanie klawiszy łącznika świecznikowego). */
    data class OneOf(val options: List<List<Goal>>) : Goal
}

/** Wymóg dotyczący wyboru wariantu elementu (np. właściwa żarówka). */
data class ChoiceRule(
    val part: String,
    val valid: Set<String>,
    /** Wyjaśnienia dla błędnych wariantów: id wariantu → komunikat. */
    val explain: Map<String, String>,
)

/** Przewód wzorcowego rozwiązania. */
data class SolWire(val a: TermRef, val b: TermRef, val color: WireColor, val cs: CrossSection) {
    fun same(x: TermRef, y: TermRef) = (a == x && b == y) || (a == y && b == x)
}

/** Wzorcowe rozwiązanie poziomu – służy do testów, podpowiedzi i trybu „Uczeń". */
data class Solution(val wires: List<SolWire> = emptyList(), val choices: Map<String, String> = emptyMap())

data class Level(
    val id: String,
    val chapter: Int,
    val number: Int,
    val title: String,
    val subtitle: String,
    /** Wprowadzenie teoretyczne pokazywane przed poziomem. */
    val theory: List<String>,
    val funFact: String,
    val parts: List<Part>,
    val prewired: List<Wire> = emptyList(),
    val goals: List<Goal>,
    val choiceRules: List<ChoiceRule> = emptyList(),
    /** Czy przed pracą gracz wykonuje procedurę bezpiecznego przygotowania miejsca pracy. */
    val safetyProcedure: Boolean = true,
    val minCs: CrossSection = CrossSection.S1_5,
    val threePhase: Boolean = false,
    /** Sprawdzaj równomierne obciążenie faz. */
    val balancePhases: Boolean = false,
    val basePoints: Int = 100,
    /** Poziom bonusowy – odblokowywany za punkty w sklepie. */
    val unlockCost: Int? = null,
    val hints: List<String> = emptyList(),
    val width: Float = 1000f,
    val height: Float = 600f,
    val solution: Solution = Solution(),
    /** Poziom serwisowy: opis zgłoszenia klienta. */
    val story: String? = null,
    /** Id poziomu, który trzeba ukończyć, żeby odblokować ten (null = reguła rozdziału). */
    val requires: String? = null,
) {
    fun part(id: String): Part = parts.first { it.id == id }
    fun partOrNull(id: String): Part? = parts.firstOrNull { it.id == id }
    val key get() = id
}
