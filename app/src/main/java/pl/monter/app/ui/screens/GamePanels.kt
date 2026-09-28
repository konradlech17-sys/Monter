package pl.monter.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.monter.app.ui.AppViewModel
import pl.monter.app.ui.board.BoardTheme
import pl.monter.app.ui.board.RenderInput
import pl.monter.app.ui.board.drawBoard
import pl.monter.app.ui.board.ui
import pl.monter.app.ui.components.Confetti
import pl.monter.app.ui.components.GameButton
import pl.monter.app.ui.components.Pill
import pl.monter.app.ui.components.Stars
import pl.monter.app.ui.theme.Palette
import pl.monter.core.game.Attempt
import pl.monter.core.game.Difficulty
import pl.monter.core.game.SaveData
import pl.monter.core.level.Goal
import pl.monter.core.level.Level
import pl.monter.core.level.Levels
import pl.monter.core.model.WireColor
import pl.monter.core.rules.Issue
import pl.monter.core.rules.Severity
import pl.monter.core.sim.LoadState
import kotlin.math.min

@Composable
internal fun IntroScreen(level: Level, difficulty: Difficulty, onBack: () -> Unit, onStart: () -> Unit) {
    Row(Modifier.fillMaxSize().padding(20.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        Column(Modifier.weight(1.3f).fillMaxHeight().verticalScroll(rememberScrollState())) {
            Text("Zadanie ${level.id}", color = Palette.TextDim, fontSize = 13.sp)
            Text(level.title, color = Palette.Volt, fontSize = 28.sp, fontWeight = FontWeight.Black)
            Text(level.subtitle, color = Palette.Text, fontSize = 15.sp)
            Spacer(Modifier.height(12.dp))
            level.story?.let { story ->
                Column(Modifier.clip(RoundedCornerShape(14.dp)).background(Color(0xFF3E2723)).padding(12.dp)) {
                    Text("🛠️ Zgłoszenie serwisowe", color = Palette.Warn, fontWeight = FontWeight.Bold)
                    Text(story, color = Palette.Text, fontSize = 15.sp)
                }
                Spacer(Modifier.height(10.dp))
            }
            Text("📘 Teoria", color = Palette.Cyan, fontWeight = FontWeight.Bold)
            level.theory.forEach { Text("• $it", color = Palette.Text, fontSize = 14.sp, modifier = Modifier.padding(vertical = 3.dp)) }
            Spacer(Modifier.height(10.dp))
            Column(Modifier.clip(RoundedCornerShape(14.dp)).background(Palette.SurfaceHi).padding(12.dp)) {
                Text("⚡ Ciekawostka", color = Palette.Volt, fontWeight = FontWeight.Bold)
                Text(level.funFact, color = Palette.Text, fontSize = 13.sp)
            }
        }
        Column(
            Modifier.weight(0.9f).fillMaxHeight().clip(RoundedCornerShape(20.dp)).background(Palette.Surface).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("🎯 Cel", color = Palette.Cyan, fontWeight = FontWeight.Bold)
            goalsText(level).forEach { Text("• $it", color = Palette.Text, fontSize = 13.sp) }
            Spacer(Modifier.height(6.dp))
            Text("Trudność: ${difficulty.title} (×${difficulty.multiplier})", color = Palette.Volt, fontWeight = FontWeight.Bold)
            Text(difficulty.description, color = Palette.TextDim, fontSize = 12.sp)
            Text("Sterowanie: dotknij zacisku, potem drugiego – powstanie przewód w wybranym kolorze i przekroju. Szczypanie = powiększenie.", color = Palette.TextDim, fontSize = 12.sp)
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GameButton("← Wróć", onBack, color = Palette.SurfaceHi, textColor = Palette.Text)
                GameButton(if (level.safetyProcedure) "🦺 Przygotuj stanowisko" else "Do pracy!", onStart, Modifier.weight(1f))
            }
        }
    }
}

private fun goalsText(level: Level): List<String> = level.goals.flatMap { g ->
    when (g) {
        is Goal.Powered -> listOf("Zasil poprawnie: ${level.part(g.part).label}")
        is Goal.Follows -> listOf("${level.part(g.part).label}: ${g.describe}")
        is Goal.Toggles -> listOf("${level.part(g.part).label} sterowana z: " + g.controls.joinToString(", ") { level.part(it).label })
        is Goal.OneOf -> listOf("Każdy klawisz łącznika steruje osobną grupą")
    }
} + (if (level.choiceRules.isNotEmpty() || level.parts.any { it.options.isNotEmpty() }) listOf("Dobierz właściwe elementy (dotknij ich)") else emptyList()) +
    listOf("Zachowaj kolory przewodów i przekroje zgodne z normami")

// =============================================================================== procedura BHP

@Composable
internal fun SafetyScreen(difficulty: Difficulty, seed: Int, onBack: () -> Unit, onDone: (Int) -> Unit) {
    val deck = remember { pl.monter.core.game.Safety.deck(seed) }
    var done by remember { mutableStateOf(listOf<String>()) }
    var wrong by remember { mutableStateOf<String?>(null) }
    var mistakes by remember { mutableStateOf(0) }
    var explanation by remember { mutableStateOf(if (difficulty == Difficulty.EASY) "Wskazówka: zaczynamy od wyłączenia zasilania. Uważaj na fałszywe karty!" else "Ułóż kroki w odpowiedniej kolejności. Uważaj na fałszywe karty!") }
    val total = pl.monter.core.game.Safety.steps.size

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🦺 5 zasad bezpiecznej pracy (PN-EN 50110-1)", color = Palette.Volt, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("Błędy: $mistakes", color = if (mistakes > 0) Palette.Bad else Palette.TextDim)
            Spacer(Modifier.width(12.dp))
            GameButton("Wyjdź", onBack, small = true, color = Palette.SurfaceHi, textColor = Palette.Text)
        }
        Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(total) { i ->
                Box(
                    Modifier.size(34.dp).clip(CircleShape).background(if (i < done.size) Palette.Ok else Palette.SurfaceHi),
                    contentAlignment = Alignment.Center,
                ) { Text("${i + 1}", color = Color.White, fontWeight = FontWeight.Bold) }
            }
        }
        Text(explanation, color = Palette.Text, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
        val rows = deck.chunked(4)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                    row.forEach { step ->
                        val isDone = step.id in done
                        val isWrong = step.id == wrong
                        val shake = remember { Animatable(0f) }
                        LaunchedEffect(isWrong, mistakes) {
                            if (isWrong) { shake.snapTo(1.08f); shake.animateTo(1f, spring(dampingRatio = 0.3f)) }
                        }
                        Box(
                            Modifier.weight(1f).fillMaxHeight()
                                .scale(if (shake.value == 0f) 1f else shake.value)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    when {
                                        isDone -> Palette.Ok.copy(alpha = 0.3f)
                                        isWrong -> Palette.Bad.copy(alpha = 0.35f)
                                        else -> Palette.Surface
                                    },
                                )
                                .border(2.dp, if (isDone) Palette.Ok else Color.Transparent, RoundedCornerShape(16.dp))
                                .clickable(enabled = !isDone && done.size < total) {
                                    if (pl.monter.core.game.Safety.isNext(done.size, step)) {
                                        done = done + step.id
                                        wrong = null
                                        explanation = "✔ ${step.text}: ${step.why}"
                                    } else {
                                        wrong = step.id
                                        mistakes++
                                        explanation = if (!step.correct) "✘ ${step.why}" else "✘ Dobry krok, ale jeszcze nie teraz. Pomyśl, co trzeba zrobić wcześniej."
                                    }
                                }
                                .padding(12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                (if (isDone) "${done.indexOf(step.id) + 1}. " else "") + step.text,
                                color = Palette.Text, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                            )
                        }
                    }
                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        if (done.size == total) {
            Spacer(Modifier.height(8.dp))
            GameButton(if (mistakes == 0) "Bezbłędnie! Do pracy ▶" else "Do pracy ▶", { onDone(mistakes) }, Modifier.fillMaxWidth())
        }
    }
}

// =============================================================================== plansza

@Composable
internal fun SectionTitle(t: String) = Text(t, color = Palette.TextDim, fontSize = 12.sp, fontWeight = FontWeight.Bold)

@Composable
internal fun TestPanel(st: GameState, save: SaveData) {
    SectionTitle("TRYB TESTU")
    Text("Dotykaj łączników i aparatów, aby je przełączać. Obserwuj przepływ prądu.", color = Palette.Text, fontSize = 12.sp)
    val sim = st.sim ?: return
    SectionTitle("ODBIORNIKI")
    sim.loads.forEach { (id, s) ->
        val icon = when (s.state) { LoadState.ON -> "🟢"; LoadState.OFF -> "⚪"; LoadState.DAMAGED -> "💥" }
        Text("$icon ${st.level.part(id).label}", color = Palette.Text, fontSize = 12.sp)
    }
    if (sim.trips.isNotEmpty()) {
        SectionTitle("ZADZIAŁAŁY ZABEZPIECZENIA")
        sim.trips.forEach { Text("⚠ ${st.level.part(it.part).label}: ${it.reason}", color = Palette.Bad, fontSize = 12.sp) }
    }
}

@Composable
internal fun ToolToggles(st: GameState, save: SaveData, toggleCamera: () -> Unit) {
    val owned = save.owned
    if (owned.none { it.startsWith("tool_") }) {
        Text("Kup narzędzia pomiarowe w sklepie, aby widzieć napięcia na zaciskach.", color = Palette.TextDim, fontSize = 11.sp)
        return
    }
    SectionTitle("PRZYRZĄDY")
    if ("tool_tester" in owned) Pill("🪛 Wskaźnik napięcia", st.tester, { st.tester = !st.tester })
    if ("tool_meter" in owned) Pill("📟 Miernik", st.meter, { st.meter = !st.meter })
    if ("tool_camera" in owned) Pill("📷 Termowizja", st.camera, toggleCamera)
    if ((st.tester || st.meter) && st.mode == Mode.BUILD) Text("Przyrządy działają w trybie ⚡ Test.", color = Palette.TextDim, fontSize = 11.sp)
}

@Composable
internal fun ReportPanel(st: GameState, report: pl.monter.core.rules.Report, modifier: Modifier) {
    Column(
        modifier.padding(8.dp).widthIn(max = 380.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xF01B2536)).padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("❌ Błędy: ${report.errors.size}   ⚠ Uwagi: ${report.warnings.size}", color = Palette.Text, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("✕", color = Palette.TextDim, fontSize = 18.sp, modifier = Modifier.clickable { st.showReport = false; st.focusIssue = null }.padding(4.dp))
        }
        Text("Dotknij pozycji, aby wskazać miejsce na planszy.", color = Palette.TextDim, fontSize = 11.sp)
        Spacer(Modifier.height(6.dp))
        Column(Modifier.heightIn(max = 260.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            report.issues.sortedBy { it.severity }.forEach { issue -> IssueRow(issue, st.focusIssue == issue) { st.focusIssue = if (st.focusIssue == issue) null else issue } }
        }
    }
}

@Composable
private fun IssueRow(issue: Issue, focused: Boolean, onClick: () -> Unit) {
    val c = if (issue.severity == Severity.ERROR) Palette.Bad else Palette.Warn
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(if (focused) c.copy(alpha = 0.2f) else Palette.SurfaceHi)
            .clickable(onClick = onClick).padding(8.dp),
    ) {
        Text((if (issue.severity == Severity.ERROR) "❌ " else "⚠ ") + issue.title, color = c, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text(issue.detail, color = Palette.Text, fontSize = 12.sp)
    }
}

// =============================================================================== wynik

@Composable
internal fun ResultOverlay(st: GameState, onMap: () -> Unit, onRetry: () -> Unit, onNext: () -> Unit) {
    val reward = st.reward ?: return
    val starAnims = remember { List(3) { Animatable(0f) } }
    val sparks = remember { Animatable(0f) }
    LaunchedEffect(reward) {
        for (i in 0 until reward.stars) {
            starAnims[i].animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 300f))
        }
        sparks.animateTo(reward.sparks.toFloat(), tween(900))
    }
    Box(Modifier.fillMaxSize().background(Color(0xAA000000)).clickable(enabled = false) {}, contentAlignment = Alignment.Center) {
        Confetti(Modifier.fillMaxSize())
        Column(
            Modifier.widthIn(max = 520.dp).clip(RoundedCornerShape(24.dp)).background(Palette.Surface).padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Zadanie wykonane!", color = Palette.Volt, fontSize = 26.sp, fontWeight = FontWeight.Black)
            Text("Instalacja zgodna z zasadami • ${st.difficulty.title}", color = Palette.TextDim, fontSize = 12.sp)
            Row(Modifier.padding(vertical = 8.dp)) {
                starAnims.forEachIndexed { i, a ->
                    Text("★", fontSize = 56.sp, color = if (i < reward.stars) Palette.Volt else Palette.SurfaceHi, modifier = Modifier.scale(if (i < reward.stars) a.value.coerceAtLeast(0.01f) else 1f))
                }
            }
            Text("+ ⚡ ${sparks.value.toInt()} iskier", color = Palette.Volt, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            if (reward.firstClear) Text("w tym premia za pierwsze ukończenie: 50", color = Palette.TextDim, fontSize = 12.sp)
            if (reward.improved) Text("Nowy rekord gwiazdek! 🎉", color = Palette.Ok, fontSize = 13.sp)
            Text("Czas: ${st.seconds / 60}:${(st.seconds % 60).toString().padStart(2, '0')} • podpowiedzi: ${st.hintsUsed} • błędne sprawdzenia: ${st.failedChecks}", color = Palette.TextDim, fontSize = 12.sp)
            reward.newAchievements.forEach {
                Text("${it.icon} Nowe osiągnięcie: ${it.title}", color = Palette.Cyan, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
            }
            if (st.rewardWarnings.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("Na przyszłość (dobre praktyki):", color = Palette.Warn, fontWeight = FontWeight.Bold)
                st.rewardWarnings.take(4).forEach { Text("⚠ ${it.title} – ${it.detail}", color = Palette.Text, fontSize = 12.sp) }
            }
            if (reward.stars < 3) {
                Text(
                    "Jak zdobyć 3★: sprawdzaj dopiero, gdy jesteś pewien, unikaj uwag (w trybach Czeladnik/Mistrz) i bezbłędnie wykonaj procedurę BHP.",
                    color = Palette.TextDim, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp),
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GameButton("Mapa", onMap, small = true, color = Palette.SurfaceHi, textColor = Palette.Text)
                GameButton("Powtórz", onRetry, small = true, color = Palette.SurfaceHi, textColor = Palette.Text)
                GameButton("⚡ Pobaw się układem", { st.showResult = false }, small = true, color = Palette.Cyan, textColor = Color(0xFF00202E))
                GameButton("Dalej ▶", onNext, small = true)
            }
        }
    }
}


/** Skrócona procedura BHP dla doświadczonych graczy – animowana lista, którą można pominąć dotknięciem. */
@Composable
internal fun QuickSafety(onDone: () -> Unit) {
    val steps = listOf("Wyłączam zasilanie obwodu", "Zabezpieczam przed załączeniem", "Sprawdzam brak napięcia")
    var done by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        repeat(steps.size) { kotlinx.coroutines.delay(520); done++ }
        kotlinx.coroutines.delay(380)
        onDone()
    }
    Box(Modifier.fillMaxSize().clickable { onDone() }, contentAlignment = Alignment.Center) {
        Column(Modifier.clip(RoundedCornerShape(24.dp)).background(Palette.Surface).padding(28.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("🦺 Przygotowanie stanowiska", color = Palette.Volt, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            steps.forEachIndexed { i, s ->
                val ok = i < done
                val scale by androidx.compose.animation.core.animateFloatAsState(if (ok) 1f else 0.9f, spring(dampingRatio = 0.4f), label = "s")
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.scale(scale)) {
                    Text(if (ok) "✅" else "⬜", fontSize = 22.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(s, color = if (ok) Palette.Text else Palette.TextDim, fontSize = 17.sp)
                }
            }
            Text("Znasz już zasady – dotknij, aby pominąć", color = Palette.TextDim, fontSize = 12.sp)
        }
    }
}
