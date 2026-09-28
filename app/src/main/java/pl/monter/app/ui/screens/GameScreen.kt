package pl.monter.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.monter.app.ui.AppViewModel
import pl.monter.app.ui.board.BoardTheme
import pl.monter.app.ui.board.ui
import pl.monter.app.ui.components.GameButton
import pl.monter.app.ui.components.Pill
import pl.monter.app.ui.theme.Palette
import pl.monter.core.game.Attempt
import pl.monter.core.game.Difficulty
import pl.monter.core.game.SaveData
import pl.monter.core.level.Level
import pl.monter.core.level.Levels
import pl.monter.core.model.CrossSection
import pl.monter.core.model.WireColor
import pl.monter.core.rules.Severity

/** Po tylu bezbłędnych procedurach BHP gracz dostaje skróconą, automatyczną wersję. */
private const val QUICK_SAFETY_AFTER = 3

@Composable
fun GameScreen(
    level: Level,
    difficulty: Difficulty,
    save: SaveData,
    vm: AppViewModel,
    onExit: () -> Unit,
    onNext: (Level?) -> Unit,
    onRetry: () -> Unit,
    /** Gotowy stan – do podglądów i testów zrzutów ekranu. */
    initial: GameState? = null,
) {
    val st = remember(level.id, difficulty) {
        initial ?: GameState(level, difficulty).also {
            // Teorię pokazujemy tylko przy pierwszym podejściu – potem jest pod przyciskiem 📘.
            it.phase = when {
                !save.introSeen(level) -> Phase.INTRO
                level.safetyProcedure -> Phase.SAFETY
                else -> Phase.BUILD
            }
        }
    }
    val quickSafety = save.stats.perfectProcedures >= QUICK_SAFETY_AFTER
    when (st.phase) {
        Phase.INTRO -> IntroScreen(level, difficulty, onBack = onExit) {
            vm.markIntroSeen(level.id)
            st.phase = if (level.safetyProcedure) Phase.SAFETY else Phase.BUILD
            st.startMs = System.currentTimeMillis()
        }
        Phase.SAFETY -> if (quickSafety) {
            QuickSafety { st.procedureMistakes = 0; st.phase = Phase.BUILD; st.startMs = System.currentTimeMillis() }
        } else {
            SafetyScreen(difficulty, seed = level.id.hashCode(), onBack = onExit) { mistakes ->
                st.procedureMistakes = mistakes
                st.phase = Phase.BUILD
                st.startMs = System.currentTimeMillis()
            }
        }
        Phase.BUILD -> BuildScreen(st, save, vm, onExit, onNext, onRetry)
    }
}

@Composable
private fun BuildScreen(
    st: GameState,
    save: SaveData,
    vm: AppViewModel,
    onExit: () -> Unit,
    onNext: (Level?) -> Unit,
    onRetry: () -> Unit,
) {
    val level = st.level
    val scope = rememberCoroutineScope()
    val time by produceState(0f) {
        val start = withFrameNanos { it }
        while (true) withFrameNanos { value = (it - start) / 1_000_000_000f }
    }
    val theme = BoardTheme.of(save.activeTheme)
    var confirmExit by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }

    fun check() {
        if (st.checking) return
        st.checking = true
        scope.launch {
            val session = st.session
            val r = withContext(Dispatchers.Default) { session.validate(st.difficulty) }
            vm.recordCheck(session.playerWires.size)
            st.checking = false
            if (r.passed) {
                val attempt = Attempt(st.failedChecks, r.warnings.size, st.procedureMistakes, st.hintsUsed, st.seconds)
                st.rewardWarnings = r.warnings
                st.reward = vm.completeLevel(level, st.difficulty, attempt)
                st.showResult = true
                st.report = null
                st.ghost = null
                st.enterTest(time)
            } else {
                st.failedChecks++
                st.report = r
                st.showReport = true
                st.focusIssue = null
            }
        }
    }

    fun useHint() {
        when {
            st.freeHints > 0 -> st.freeHints--
            vm.consumeHint() -> {}
            else -> { st.message = "Brak podpowiedzi. Kup je w sklepie za iskry ⚡"; return }
        }
        st.hintsUsed++
        st.mode = Mode.BUILD
        scope.launch {
            val session = st.session
            val r = withContext(Dispatchers.Default) { session.validate(st.difficulty) }
            val issue = r.errors.firstOrNull() ?: r.warnings.firstOrNull()
            // Najpierw pokaż błąd, który gracz już popełnił; jeśli brak – następny przewód z rozwiązania.
            val tip = level.hints.getOrNull((st.hintsUsed - 1) % maxOf(1, level.hints.size)) ?: ""
            st.ghost = if (issue == null || issue.code.startsWith("goal")) st.nextSolutionWire() else null
            st.hint = tip to issue
            st.focusIssue = issue
        }
    }

    fun toggleCamera() {
        st.camera = !st.camera
        if (!st.camera) { st.hotWires = emptySet(); return }
        scope.launch {
            val session = st.session
            val r = withContext(Dispatchers.Default) { session.validate(st.difficulty) }
            st.hotWires = r.issues.filter { it.code.startsWith("cs.") }.flatMap { it.wires }.toSet()
            if (st.hotWires.isEmpty()) st.message = "📷 Termowizja: żaden przewód się nie przegrzewa."
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            // -------------------------------------------------------------- pasek górny
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                GameButton("←", { confirmExit = true }, small = true, color = Palette.SurfaceHi, textColor = Palette.Text)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("${level.id} • ${level.title}", color = Palette.Text, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
                    Text("${st.difficulty.title} • sprawdzenia z błędami: ${st.failedChecks}", color = Palette.TextDim, fontSize = 11.sp)
                }
                GameButton("📘", { st.showTheory = true }, small = true, color = Palette.SurfaceHi, textColor = Palette.Text)
                Spacer(Modifier.width(6.dp))
                Pill("🔧 Montaż", st.mode == Mode.BUILD, { st.mode = Mode.BUILD; st.sparks = emptyList() })
                Spacer(Modifier.width(6.dp))
                Pill("⚡ Test", st.mode == Mode.TEST, { if (st.enterTest(time)) vm.recordShort() }, accent = Palette.Cyan)
                Spacer(Modifier.width(10.dp))
                GameButton("💡 ${st.freeHints + save.hints}", ::useHint, small = true, color = Palette.SurfaceHi, textColor = Palette.Volt)
                Spacer(Modifier.width(6.dp))
                GameButton(if (st.checking) "…" else "✅ Sprawdź", ::check, small = true, color = Palette.Ok, textColor = Color.White)
            }

            Row(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxHeight().padding(start = 8.dp, bottom = 8.dp).clip(RoundedCornerShape(16.dp))) {
                    Board(st, theme, time, view3d = save.view3d, haptics = save.haptics, tutorial = save.stats.wiresLaid == 0, onShort = { vm.recordShort() }, onToggle3d = { vm.setView3d(!save.view3d) })
                    st.message?.let {
                        Text(
                            it, color = Color.White, fontSize = 12.sp,
                            modifier = Modifier.align(Alignment.BottomStart).padding(8.dp).clip(RoundedCornerShape(10.dp))
                                .background(Color(0xCC101820)).padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                    if (st.showReport) st.report?.let { ReportPanel(st, it, Modifier.align(Alignment.TopEnd)) }
                }
                Column(
                    Modifier.width(214.dp).fillMaxHeight().padding(8.dp).clip(RoundedCornerShape(16.dp)).background(Palette.Surface)
                        .verticalScroll(rememberScrollState()).padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (st.mode == Mode.BUILD) BuildTools(st, onClear = { confirmClear = true }) else TestPanel(st, save)
                    ToolToggles(st, save, ::toggleCamera)
                }
            }
        }

        st.choosing?.let { part ->
            AlertDialog(
                onDismissRequest = { st.choosing = null },
                title = { Text(part.label) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        part.options.forEach { opt -> Pill(opt.label, st.session.choices[part.id] == opt.id, { st.choose(part, opt.id) }) }
                    }
                },
                confirmButton = { TextButton(onClick = { st.choosing = null }) { Text("Zamknij") } },
            )
        }
        st.hint?.let { (tip, issue) ->
            AlertDialog(
                onDismissRequest = { st.hint = null },
                title = { Text("💡 Podpowiedź") },
                text = {
                    Column {
                        if (tip.isNotEmpty()) Text(tip)
                        Spacer(Modifier.height(8.dp))
                        if (issue != null) {
                            Text(issue.title, fontWeight = FontWeight.Bold, color = if (issue.severity == Severity.ERROR) Palette.Bad else Palette.Warn)
                            Text(issue.detail, fontSize = 13.sp)
                        }
                        st.ghost?.let {
                            Spacer(Modifier.height(8.dp))
                            Text("✨ Na planszy migocze następny przewód: ${st.session.label(it.a)} ↔ ${st.session.label(it.b)} (${it.color.pl}, ${it.cs.label})", color = Palette.Cyan, fontSize = 13.sp)
                        }
                        if (issue == null && st.ghost == null) Text("Nie widzę błędów – kliknij „Sprawdź\"!", color = Palette.Ok)
                    }
                },
                confirmButton = { TextButton(onClick = { st.hint = null }) { Text("Dzięki!") } },
            )
        }
        if (st.showTheory) {
            AlertDialog(
                onDismissRequest = { st.showTheory = false },
                title = { Text("📘 ${level.title}") },
                text = {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        level.story?.let { Text("🛠️ $it", fontWeight = FontWeight.Bold); Spacer(Modifier.height(6.dp)) }
                        level.theory.forEach { Text("• $it", fontSize = 13.sp, modifier = Modifier.padding(vertical = 2.dp)) }
                        Spacer(Modifier.height(8.dp))
                        Text("⚡ ${level.funFact}", fontSize = 13.sp, color = Palette.Volt)
                    }
                },
                confirmButton = { TextButton(onClick = { st.showTheory = false }) { Text("Wracam do pracy") } },
            )
        }
        if (confirmExit) {
            AlertDialog(
                onDismissRequest = { confirmExit = false },
                text = { Text("Przerwać zadanie? Ułożone przewody zostaną utracone.") },
                confirmButton = { TextButton(onClick = onExit) { Text("Przerwij") } },
                dismissButton = { TextButton(onClick = { confirmExit = false }) { Text("Zostań") } },
            )
        }
        if (confirmClear) {
            AlertDialog(
                onDismissRequest = { confirmClear = false },
                text = { Text("Usunąć wszystkie ułożone przewody?") },
                confirmButton = { TextButton(onClick = { st.clearAll(); confirmClear = false }) { Text("Usuń") } },
                dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Anuluj") } },
            )
        }
        AnimatedVisibility(st.showResult && st.reward != null, enter = fadeIn(), exit = fadeOut()) {
            ResultOverlay(st, onMap = onExit, onRetry = onRetry, onNext = {
                val chapter = Levels.inChapter(level.chapter).filter { it.unlockCost == null }
                val idx = Levels.campaign.indexOf(level)
                val next = chapter.getOrNull(chapter.indexOf(level) + 1) ?: Levels.campaign.getOrNull(idx + 1)
                onNext(next?.takeIf { save.isUnlocked(it) || it.chapter == level.chapter })
            })
        }
    }
}

@Composable
private fun BuildTools(st: GameState, onClear: () -> Unit) {
    SectionTitle("PRZEWÓD")
    if (st.difficulty != Difficulty.HARD) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Pill(if (st.autoColor) "🪄 kolor" else "kolor ręcznie", st.autoColor, { st.autoColor = !st.autoColor })
            Pill(if (st.autoCs) "🪄 mm²" else "mm² ręcznie", st.autoCs, { st.autoCs = !st.autoCs })
        }
        Text("🪄 = dobierane automatycznie. Wyłącz, by wybrać samemu.", color = Palette.TextDim, fontSize = 10.sp)
    }
    WireColor.entries.chunked(4).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { c ->
                val sel = st.color == c && !st.autoColor
                Box(
                    Modifier.size(36.dp).clip(CircleShape).background(c.ui())
                        .border(if (sel) 3.dp else 1.dp, if (sel) Palette.Volt else Color(0x55FFFFFF), CircleShape)
                        .clickable { st.color = c; st.autoColor = false },
                    contentAlignment = Alignment.Center,
                ) {
                    if (c == WireColor.GREEN_YELLOW) Box(Modifier.size(12.dp, 36.dp).background(Color(0xFFFDD835)))
                }
            }
        }
    }
    Text(if (st.autoColor) "Kolor: automatyczny" else "Kolor: ${st.color.pl}", color = Palette.Text, fontSize = 12.sp)
    CrossSection.entries.chunked(4).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            row.forEach { c -> Pill(c.label.replace(" mm²", ""), st.cs == c && !st.autoCs, { st.cs = c; st.autoCs = false }) }
        }
    }
    Text(if (st.autoCs) "Przekrój: automatyczny" else "Przekrój: ${st.cs.label}", color = Palette.Text, fontSize = 12.sp)
    SectionTitle("NARZĘDZIA")
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Pill("✂️", st.scissors, { st.scissors = !st.scissors }, accent = Palette.Bad)
        Pill("↶ Cofnij", false, { st.undo() })
        Pill("🗑", false, onClear)
    }
    Text(
        if (st.scissors) "Nożyce: dotknij przewodu, aby go przeciąć." else "Przeciągnij palcem od zacisku do zacisku. Przytrzymaj przewód, aby go usunąć.",
        color = if (st.scissors) Palette.Bad else Palette.TextDim, fontSize = 11.sp,
    )
    st.selectedWire?.let { id ->
        val w = st.wireAt(id)
        if (w != null) {
            SectionTitle("WYBRANY PRZEWÓD")
            Text("${w.color.pl}, ${w.cs.label}${if (w.fixed) " (fabryczny)" else ""}", color = Palette.Text, fontSize = 12.sp)
            if (!w.fixed) {
                GameButton("Zmień na: ${st.color.pl}, ${st.cs.label}", { st.applyToSelectedWire() }, small = true, color = Palette.Cyan, textColor = Color(0xFF00202E))
                GameButton("✂️ Usuń", { st.deleteSelectedWire() }, small = true, color = Palette.Bad, textColor = Color.White)
            }
        }
    }
}
