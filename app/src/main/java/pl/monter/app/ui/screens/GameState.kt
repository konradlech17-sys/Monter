package pl.monter.app.ui.screens

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import pl.monter.app.ui.board.SparkBurst
import pl.monter.app.ui.board.hitPart
import pl.monter.app.ui.board.hitTerminal
import pl.monter.app.ui.board.hitWire
import pl.monter.app.ui.board.terminalPos
import pl.monter.core.game.BuildSession
import pl.monter.core.game.Difficulty
import pl.monter.core.game.Reward
import pl.monter.core.level.Level
import pl.monter.core.model.CrossSection
import pl.monter.core.model.Kind
import pl.monter.core.model.Part
import pl.monter.core.model.Role
import pl.monter.core.model.TermRef
import pl.monter.core.model.WireColor
import pl.monter.core.rules.Issue
import pl.monter.core.rules.Report
import pl.monter.core.sim.SimResult

enum class Phase { INTRO, SAFETY, BUILD }
enum class Mode { BUILD, TEST }

/** Stan rozgrywki jednego poziomu (trzymany w `remember`). */
class GameState(val level: Level, val difficulty: Difficulty) {
    var phase by mutableStateOf(Phase.INTRO)
    var mode by mutableStateOf(Mode.BUILD)
    var session by mutableStateOf(BuildSession(level))
    private var history = listOf<BuildSession>()

    var selected by mutableStateOf<TermRef?>(null)
    var selectedWire by mutableStateOf<Int?>(null)
    var color by mutableStateOf(WireColor.BROWN)
    var cs by mutableStateOf(level.minCs)
    var autoColor by mutableStateOf(difficulty.roleHints)
    var scissors by mutableStateOf(false)
    var controls by mutableStateOf(level.parts.filter { it.kind.states > 0 }.associate { it.id to it.kind.defaultState })

    var report by mutableStateOf<Report?>(null)
    var showReport by mutableStateOf(false)
    var focusIssue by mutableStateOf<Issue?>(null)
    var checking by mutableStateOf(false)

    var failedChecks by mutableIntStateOf(0)
    var hintsUsed by mutableIntStateOf(0)
    var freeHints by mutableIntStateOf(difficulty.freeHints)
    var procedureMistakes by mutableIntStateOf(0)
    var startMs = System.currentTimeMillis()

    var message by mutableStateOf<String?>(null)
    var choosing by mutableStateOf<Part?>(null)
    var hint by mutableStateOf<Pair<String, Issue?>?>(null)
    var reward by mutableStateOf<Reward?>(null)
    var rewardWarnings by mutableStateOf<List<Issue>>(emptyList())
    var showResult by mutableStateOf(false)

    var meter by mutableStateOf(false)
    var tester by mutableStateOf(false)
    var camera by mutableStateOf(false)
    var hotWires by mutableStateOf<Set<Int>>(emptySet())
    var sparks by mutableStateOf<List<SparkBurst>>(emptyList())

    val sim: SimResult? by derivedStateOf { if (mode == Mode.TEST) session.simulate(controls) else null }

    val highlightParts: Set<String> get() = focusIssue?.parts ?: if (showReport) report?.errors?.flatMap { it.parts }?.toSet().orEmpty() else emptySet()
    val highlightWires: Set<Int> get() = focusIssue?.wires ?: if (showReport) report?.errors?.flatMap { it.wires }?.toSet().orEmpty() else emptySet()

    val seconds get() = ((System.currentTimeMillis() - startMs) / 1000).toInt()

    private fun commit(next: BuildSession) {
        if (next === session) return
        history = (history + session).takeLast(50)
        session = next
        report = null; showReport = false; focusIssue = null
    }

    fun undo() {
        val prev = history.lastOrNull() ?: return
        history = history.dropLast(1)
        session = prev
        selected = null; selectedWire = null
    }

    fun clearAll() = commit(session.clear())

    private fun autoColorFor(a: TermRef, b: TermRef): WireColor {
        val roles = listOf(a, b).map { level.part(it.part).kind.terminal(it.terminal).role }
        val role = roles.firstOrNull { it != Role.ANY && it != Role.L } ?: roles.firstOrNull { it != Role.ANY } ?: Role.L
        // Na planszy trójfazowej faza wynika z zacisku zasilania, więc zostajemy przy kolorze wybranym ręcznie.
        return if (role == Role.L && level.threePhase) color else role.suggestedColor()
    }

    /** Dotknięcie planszy w trybie montażu. */
    fun tapBuild(p: Offset, scale: Float) {
        message = null
        val term = level.hitTerminal(p, maxOf(14f, 26f / scale))
        if (term != null) {
            selectedWire = null
            val sel = selected
            when {
                sel == null -> {
                    selected = term
                    message = "${session.label(term)} → wybierz drugi zacisk"
                }
                sel == term -> selected = null
                else -> {
                    val problem = session.connectProblem(sel, term)
                    if (problem != null) {
                        message = problem
                    } else {
                        val c = if (autoColor) autoColorFor(sel, term) else color
                        commit(session.connect(sel, term, c, cs))
                        message = "Połączono: ${session.label(sel)} ↔ ${session.label(term)} (${c.pl}, ${cs.label})"
                    }
                    selected = null
                }
            }
            return
        }
        val wire = level.hitWire(p, session.wires, maxOf(8f, 18f / scale))
        if (wire != null) {
            if (scissors) {
                if (wire.fixed) message = "To przewód fabryczny – zostaje." else { commit(session.remove(wire.id)); message = "Usunięto przewód." }
            } else {
                selectedWire = if (selectedWire == wire.id) null else wire.id
                selected = null
            }
            return
        }
        val part = level.hitPart(p)
        selected = null; selectedWire = null
        if (part != null && part.options.isNotEmpty()) choosing = part
    }

    fun choose(part: Part, optionId: String) {
        commit(session.choose(part.id, optionId))
        choosing = null
    }

    fun applyToSelectedWire() {
        val id = selectedWire ?: return
        commit(session.recolor(id, color, cs))
    }

    fun deleteSelectedWire() {
        val id = selectedWire ?: return
        val w = session.wires.firstOrNull { it.id == id } ?: return
        if (w.fixed) { message = "To przewód fabryczny – zostaje."; return }
        commit(session.remove(id))
        selectedWire = null
    }

    /**
     * Dotknięcie w trybie TEST – przełączanie łączników i aparatów.
     * @return true, jeśli doszło do nowego zwarcia (do statystyk).
     */
    fun tapTest(p: Offset, now: Float): Boolean {
        message = null
        val part = level.hitPart(p) ?: return false
        if (part.options.isNotEmpty() && part.kind.states == 0) { choosing = part; return false }
        if (part.kind.states == 0) return false
        val before = sim
        val cur = controls[part.id] ?: part.kind.defaultState
        if (before != null && part.id in before.tripped) {
            message = "${part.label} zadziałał. Załączam ponownie…"
        }
        val next = if (part.kind == Kind.SWITCH_2) {
            if (p.x - part.x < part.kind.w / 2) cur xor 1 else cur xor 2
        } else if (before != null && part.id in before.tripped) cur else (cur + 1) % part.kind.states
        controls = controls + (part.id to next)
        val after = sim ?: return false
        if (after.trips.isNotEmpty()) {
            val newTrips = after.trips.filter { t -> before?.trips?.none { it.part == t.part } ?: true }
            if (newTrips.isNotEmpty()) {
                sparks = (sparks + newTrips.map { t ->
                    val dev = level.part(t.part)
                    SparkBurst(dev.let { androidx.compose.ui.geometry.Offset(it.x + it.kind.w / 2, it.y + it.kind.h / 2) }, now)
                }).takeLast(6)
                message = newTrips.joinToString("\n") { "💥 ${it.reason}: zadziałał ${level.part(it.part).label}" }
                return true
            }
        }
        val damaged = after.loads.filter { it.value.state == pl.monter.core.sim.LoadState.DAMAGED }
        if (damaged.isNotEmpty()) message = damaged.values.mapNotNull { it.message }.distinct().joinToString("\n")
        return false
    }

    fun enterTest(now: Float): Boolean {
        mode = Mode.TEST
        selected = null; selectedWire = null
        val s = sim ?: return false
        if (s.trips.isNotEmpty()) {
            sparks = s.trips.map { t -> val d = level.part(t.part); SparkBurst(Offset(d.x + d.kind.w / 2, d.y + d.kind.h / 2), now) }
            message = s.trips.joinToString("\n") { "💥 ${it.reason}: zadziałał ${level.part(it.part).label}" }
            return true
        }
        return false
    }

    fun terminalCenter(t: TermRef) = level.terminalPos(t)

    val wireAt: (Int) -> pl.monter.core.model.Wire? = { id -> session.wires.firstOrNull { it.id == id } }

    val csOptions: List<CrossSection> get() = CrossSection.entries
}
