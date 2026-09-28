package pl.monter.core.game

import pl.monter.core.level.Level
import pl.monter.core.model.CrossSection
import pl.monter.core.model.TermRef
import pl.monter.core.model.Wire
import pl.monter.core.model.WireColor
import pl.monter.core.rules.Report
import pl.monter.core.rules.Validator
import pl.monter.core.sim.SimResult
import pl.monter.core.sim.Simulator

/**
 * Stan układania przewodów na planszy. Klasa niezmienna – każda operacja zwraca nową instancję,
 * co ułatwia cofanie ruchów i współpracę z Compose.
 */
data class BuildSession(
    val level: Level,
    val wires: List<Wire> = level.prewired,
    val choices: Map<String, String> = emptyMap(),
    val nextId: Int = 1,
) {
    fun wiresAt(t: TermRef) = wires.count { it.touches(t) }

    fun maxWires(t: TermRef): Int = level.part(t.part).kind.terminal(t.terminal).maxWires

    /** Zwraca komunikat, dlaczego nie da się połączyć, albo null. */
    fun connectProblem(a: TermRef, b: TermRef): String? = when {
        a == b -> "Wybierz inny zacisk."
        wires.any { (it.a == a && it.b == b) || (it.a == b && it.b == a) } -> "Te zaciski są już połączone."
        wiresAt(a) >= maxWires(a) -> "Zacisk ${label(a)} jest pełny (maks. ${maxWires(a)} przew.)."
        wiresAt(b) >= maxWires(b) -> "Zacisk ${label(b)} jest pełny (maks. ${maxWires(b)} przew.)."
        else -> null
    }

    fun label(t: TermRef): String {
        val part = level.part(t.part)
        val term = part.kind.terminal(t.terminal)
        return "${part.label} [${term.label.ifEmpty { term.id }}]"
    }

    fun connect(a: TermRef, b: TermRef, color: WireColor, cs: CrossSection): BuildSession {
        if (connectProblem(a, b) != null) return this
        return copy(wires = wires + Wire(nextId, a, b, color, cs), nextId = nextId + 1)
    }

    fun remove(wireId: Int): BuildSession = copy(wires = wires.filterNot { it.id == wireId && !it.fixed })

    fun recolor(wireId: Int, color: WireColor, cs: CrossSection): BuildSession =
        copy(wires = wires.map { if (it.id == wireId && !it.fixed) it.copy(color = color, cs = cs) else it })

    fun choose(partId: String, optionId: String) = copy(choices = choices + (partId to optionId))

    fun clear() = copy(wires = level.prewired, choices = emptyMap())

    val playerWires get() = wires.filter { !it.fixed }

    fun validate(difficulty: Difficulty): Report = Validator(level, wires, choices, difficulty).validate()

    fun simulate(controls: Map<String, Int>): SimResult = Simulator(level.parts, wires, choices).simulate(controls)
}

/** Stan planszy z ułożonym wzorcowym rozwiązaniem. */
fun Level.solved(): BuildSession = BuildSession(
    level = this,
    wires = prewired.filter { it.fixed } + solution.wires.mapIndexed { i, w -> Wire(i + 1, w.a, w.b, w.color, w.cs) },
    choices = solution.choices,
    nextId = solution.wires.size + 1,
)
