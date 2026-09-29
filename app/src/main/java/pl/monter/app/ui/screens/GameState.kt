package pl.monter.app.ui.screens

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import pl.monter.app.ui.board.SparkBurst
import pl.monter.app.ui.board.hitPart
import pl.monter.app.ui.board.hitTerminal
import pl.monter.app.ui.board.hitWire
import pl.monter.core.game.BuildSession
import pl.monter.core.game.Difficulty
import pl.monter.core.game.Reward
import pl.monter.core.level.Level
import pl.monter.core.level.SolWire
import pl.monter.core.model.CrossSection
import pl.monter.core.model.Kind
import pl.monter.core.model.Part
import pl.monter.core.model.Role
import pl.monter.core.model.TermRef
import pl.monter.core.model.Wire
import pl.monter.core.model.WireColor
import pl.monter.core.rules.Issue
import pl.monter.core.rules.Report
import pl.monter.core.sim.LoadState
import pl.monter.core.sim.SimResult

enum class Phase { INTRO, SAFETY, BUILD }
enum class Mode { BUILD, TEST }

/** Promień „łapania" zacisku w jednostkach planszy – duży, żeby łatwo trafić palcem. */
fun terminalRadius(scale: Float) = maxOf(18f, 34f / scale)

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
    var autoColor by mutableStateOf(difficulty != Difficulty.HARD)
    var autoCs by mutableStateOf(difficulty == Difficulty.EASY)
    var scissors by mutableStateOf(false)
    var controls by mutableStateOf(level.parts.filter { it.kind.states > 0 }.associate { it.id to it.kind.defaultState })

    // Przeciąganie przewodu palcem
    var dragFrom by mutableStateOf<TermRef?>(null)
    var dragPos by mutableStateOf<Offset?>(null)
    var dragHover by mutableStateOf<TermRef?>(null)

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
    var ghost by mutableStateOf<SolWire?>(null)
    var reward by mutableStateOf<Reward?>(null)
    var rewardWarnings by mutableStateOf<List<Issue>>(emptyList())
    var showResult by mutableStateOf(false)
    var showTheory by mutableStateOf(false)

    var meter by mutableStateOf(false)
    var tester by mutableStateOf(false)
    var camera by mutableStateOf(false)
    var hotWires by mutableStateOf<Set<Int>>(emptySet())
    var sparks by mutableStateOf<List<SparkBurst>>(emptyList())
    var wireBorn by mutableStateOf<Map<Int, Float>>(emptyMap())
    var lastToggle by mutableFloatStateOf(-10f)
    var shakeAt by mutableFloatStateOf(-10f)

    // Własny układ elementów (przestawianych palcem)
    var layout by mutableStateOf<Map<String, Offset>>(emptyMap())
    var moving by mutableStateOf<String?>(null)
    var moveValid by mutableStateOf(true)
    private var moveGrab = Offset.Zero
    private var moveStart = Offset.Zero

    /** Poziom z uwzględnieniem przestawionych elementów – do rysowania i trafiania palcem. */
    val view: Level by derivedStateOf {
        if (layout.isEmpty()) level
        else level.copy(parts = level.parts.map { p -> layout[p.id]?.let { p.copy(x = it.x, y = it.y) } ?: p })
    }

    fun startMove(partId: String, at: Offset) {
        val p = view.part(partId)
        moving = partId
        moveStart = Offset(p.x, p.y)
        moveGrab = at - moveStart
        moveValid = true
        selected = null; selectedWire = null
    }

    fun moveTo(at: Offset) {
        val id = moving ?: return
        val p = level.part(id)
        val raw = at - moveGrab
        // Przyciąganie do siatki 10 jednostek i trzymanie w granicach płyty
        val x = (Math.round(raw.x / 10f) * 10f).coerceIn(0f, level.width - p.kind.w)
        val y = (Math.round(raw.y / 10f) * 10f).coerceIn(0f, level.height - p.kind.h)
        layout = layout + (id to Offset(x, y))
        moveValid = !overlaps(id)
    }

    /** Kończy przestawianie; zwraca true, jeśli układ się zmienił. */
    fun endMove(): Boolean {
        val id = moving ?: return false
        moving = null
        val now = layout[id]
        if (!moveValid || now == null) {
            layout = layout + (id to moveStart)
            message = "Tu się nie zmieści – element wrócił na miejsce."
            moveValid = true
            return false
        }
        if (now == moveStart) return false
        message = "📐 Przestawiono: ${level.part(id).label}"
        return true
    }

    fun resetLayout() { layout = emptyMap(); message = "Przywrócono układ domyślny." }

    private fun overlaps(id: String): Boolean {
        val a = view.part(id)
        return view.parts.any { b ->
            b.id != id && a.x < b.x + b.kind.w - 4 && b.x < a.x + a.kind.w - 4 && a.y < b.y + b.kind.h - 4 && b.y < a.y + a.kind.h - 4
        }
    }

    val sim: SimResult? by derivedStateOf { if (mode == Mode.TEST) session.simulate(controls) else null }

    val highlightParts: Set<String> get() = focusIssue?.parts ?: if (showReport) report?.errors?.flatMap { it.parts }?.toSet().orEmpty() else emptySet()
    val highlightWires: Set<Int> get() = focusIssue?.wires ?: if (showReport) report?.errors?.flatMap { it.wires }?.toSet().orEmpty() else emptySet()

    val seconds get() = ((System.currentTimeMillis() - startMs) / 1000).toInt()

    /** Tryb „Uczeń": zaciski, do których powinien biec przewód z zaznaczonego zacisku. */
    val suggested: Set<TermRef>
        get() {
            if (difficulty != Difficulty.EASY) return emptySet()
            val from = dragFrom ?: selected ?: return emptySet()
            return level.solution.wires.filter { it.a == from || it.b == from }
                .map { if (it.a == from) it.b else it.a }
                .filter { other -> session.wires.none { w -> w.touches(from) && w.touches(other) } }
                .toSet()
        }

    private fun commit(next: BuildSession) {
        if (next === session) return
        history = (history + session).takeLast(50)
        session = next
        report = null; showReport = false; focusIssue = null
    }

    fun undo() {
        val prev = history.lastOrNull() ?: run { message = "Nie ma czego cofnąć."; return }
        history = history.dropLast(1)
        session = prev
        selected = null; selectedWire = null
    }

    fun clearAll() = commit(session.clear())

    /** Kolor i przekrój dla nowego przewodu: z rozwiązania wzorcowego lub z roli zacisku. */
    fun wireStyle(a: TermRef, b: TermRef): Pair<WireColor, CrossSection> {
        val exact = level.solution.wires.firstOrNull { it.same(a, b) }
        val near = exact ?: level.solution.wires.firstOrNull { it.a == a || it.b == a || it.a == b || it.b == b }
        val c = if (!autoColor) color else exact?.color ?: run {
            val roles = listOf(a, b).map { level.part(it.part).kind.terminal(it.terminal).role }
            val role = roles.firstOrNull { it != Role.ANY && it != Role.L } ?: roles.firstOrNull { it != Role.ANY } ?: Role.L
            if (role == Role.L && level.threePhase) color else role.suggestedColor()
        }
        val s = if (!autoCs) cs else near?.cs ?: cs
        return c to s
    }

    fun dragColor(): WireColor = dragFrom?.let { from -> dragHover?.let { wireStyle(from, it).first } } ?: color

    /** Łączy dwa zaciski (po przeciągnięciu lub dwóch dotknięciach). */
    fun connect(a: TermRef, b: TermRef, now: Float): Boolean {
        val problem = session.connectProblem(a, b)
        if (problem != null) { message = problem; return false }
        val (c, s) = wireStyle(a, b)
        val id = session.nextId
        commit(session.connect(a, b, c, s))
        wireBorn = (wireBorn + (id to now)).filterValues { now - it < 3f }
        message = "✔ ${session.label(a)} ↔ ${session.label(b)} • ${c.pl}, ${s.label}"
        if (ghost != null && ghost!!.same(a, b)) ghost = null
        return true
    }

    /** Dotknięcie planszy w trybie montażu. */
    fun tapBuild(p: Offset, scale: Float, now: Float): Boolean {
        message = null
        val term = view.hitTerminal(p, terminalRadius(scale))
        if (term != null) {
            selectedWire = null
            val sel = selected
            when {
                sel == null -> {
                    selected = term
                    message = "${session.label(term)} → dotknij drugiego zacisku (albo przeciągnij palcem)"
                }
                sel == term -> selected = null
                else -> { selected = null; return connect(sel, term, now) }
            }
            return false
        }
        // Przewody leżą pod aparatami – dotknięcie obudowy dotyczy aparatu.
        view.hitPart(p)?.let { part ->
            selected = null; selectedWire = null
            if (part.options.isNotEmpty()) choosing = part
            else message = "${part.label} – przeciągnij obudowę, aby przestawić."
            return false
        }
        val wire = view.hitWire(p, session.wires, maxOf(10f, 20f / scale))
        if (wire != null) {
            if (scissors) cut(wire) else {
                selectedWire = if (selectedWire == wire.id) null else wire.id
                selected = null
                message = "Przewód: ${wire.color.pl}, ${wire.cs.label}. Przytrzymaj, aby usunąć."
            }
            return false
        }
        val part = view.hitPart(p)
        selected = null; selectedWire = null
        if (part != null && part.options.isNotEmpty()) choosing = part
        return false
    }

    /** Przytrzymanie palca na przewodzie – usunięcie. */
    fun longPress(p: Offset, scale: Float): Boolean {
        val wire = view.hitWire(p, session.wires, maxOf(10f, 20f / scale)) ?: return false
        cut(wire)
        return true
    }

    private fun cut(wire: Wire) {
        if (wire.fixed) { message = "To przewód fabryczny – zostaje."; return }
        commit(session.remove(wire.id))
        if (selectedWire == wire.id) selectedWire = null
        message = "✂️ Usunięto przewód (${wire.color.pl}). Cofnij: ↶"
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
        session.wires.firstOrNull { it.id == id }?.let(::cut)
        selectedWire = null
    }

    /** Pierwszy przewód wzorcowego rozwiązania, którego jeszcze nie ma na planszy. */
    fun nextSolutionWire(): SolWire? =
        level.solution.wires.firstOrNull { sw -> session.wires.none { sw.same(it.a, it.b) } }

    /**
     * Dotknięcie w trybie TEST – przełączanie łączników i aparatów.
     * @return true, jeśli doszło do nowego zwarcia (do statystyk).
     */
    fun tapTest(p: Offset, now: Float): Boolean {
        message = null
        val part = view.hitPart(p) ?: return false
        if (part.options.isNotEmpty() && part.kind.states == 0) { choosing = part; return false }
        if (part.kind.states == 0) return false
        val before = sim
        val cur = controls[part.id] ?: part.kind.defaultState
        val wasTripped = before != null && part.id in before.tripped
        val next = when {
            part.kind == Kind.SWITCH_2 -> if (p.x - part.x < part.kind.w / 2) cur xor 1 else cur xor 2
            wasTripped -> cur
            else -> (cur + 1) % part.kind.states
        }
        if (wasTripped) message = "${part.label} zadziałał – załączam ponownie…"
        controls = controls + (part.id to next)
        lastToggle = now
        val after = sim ?: return false
        val newTrips = after.trips.filter { t -> before?.trips?.none { it.part == t.part } ?: true }
        if (newTrips.isNotEmpty()) {
            burst(newTrips.map { it.part }, now)
            message = newTrips.joinToString("\n") { "💥 ${it.reason}: zadziałał ${level.part(it.part).label}" }
            return true
        }
        val damaged = after.loads.filter { it.value.state == LoadState.DAMAGED }
        if (damaged.isNotEmpty()) {
            shakeAt = now
            message = damaged.values.mapNotNull { it.message }.distinct().joinToString("\n")
        }
        return false
    }

    private fun burst(partIds: List<String>, now: Float) {
        sparks = (sparks + partIds.map { id -> val d = view.part(id); SparkBurst(Offset(d.x + d.kind.w / 2, d.y + d.kind.h / 2), now) }).takeLast(6)
        shakeAt = now
    }

    fun enterTest(now: Float): Boolean {
        mode = Mode.TEST
        selected = null; selectedWire = null
        lastToggle = now
        val s = sim ?: return false
        if (s.trips.isNotEmpty()) {
            burst(s.trips.map { it.part }, now)
            message = s.trips.joinToString("\n") { "💥 ${it.reason}: zadziałał ${level.part(it.part).label}" }
            return true
        }
        return false
    }

    fun wireAt(id: Int): Wire? = session.wires.firstOrNull { it.id == id }
}
