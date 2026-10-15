package pl.monter.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import pl.monter.app.ui.AppViewModel
import pl.monter.app.ui.components.Confetti
import pl.monter.app.ui.components.GameButton
import pl.monter.app.ui.components.Header
import pl.monter.app.ui.components.SparkBadge
import pl.monter.app.ui.theme.Palette
import pl.monter.core.exam.ExamResult
import pl.monter.core.exam.Question
import pl.monter.core.exam.SepCategory
import pl.monter.core.exam.SepTrainer
import pl.monter.core.game.SaveData
import kotlin.random.Random

/** Rodzaj sesji nauki do SEP. */
enum class SepMode(val title: String) { LEARN("Nauka"), CALC("Zadania obliczeniowe"), MISTAKES("Powtórka błędów"), EXAM("Egzamin próbny") }

@Composable
fun SepHomeScreen(save: SaveData, onBack: () -> Unit, onStart: (SepMode, SepCategory?) -> Unit) {
    val sep = save.sep
    Column(Modifier.fillMaxSize()) {
        Header("📚 Przygotowanie do egzaminu SEP", onBack) { SparkBadge(save.sparks) }
        Row(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Grupa 1 (elektryczna) • Eksploatacja „E\" • do 1 kV", color = Palette.TextDim, fontSize = 12.sp)
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Palette.Surface).padding(14.dp)) {
                    Text("Opanowanie materiału: ${sep.overallMastery()}%", color = Palette.Volt, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(progress = { sep.overallMastery() / 100f }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)), color = Palette.Volt, trackColor = Palette.SurfaceHi)
                    Spacer(Modifier.height(6.dp))
                    val acc = if (sep.answered == 0) 0 else sep.correct * 100 / sep.answered
                    Text("Odpowiedzi: ${sep.answered} • trafność $acc% • egzaminy: ${sep.examsPassed}/${sep.examsTaken} zaliczone • najlepszy wynik ${sep.bestExamPercent}%", color = Palette.TextDim, fontSize = 12.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GameButton("📝 Egzamin próbny", { onStart(SepMode.EXAM, null) }, Modifier.weight(1f))
                    GameButton("🎲 Nauka – wszystko", { onStart(SepMode.LEARN, null) }, Modifier.weight(1f), color = Palette.Cyan, textColor = Color(0xFF00202E))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GameButton("📟 Zadania obliczeniowe", { onStart(SepMode.CALC, null) }, Modifier.weight(1f), color = Palette.SurfaceHi, textColor = Palette.Text)
                    val m = sep.mistakes().size
                    GameButton("🔁 Powtórka błędów ($m)", { onStart(SepMode.MISTAKES, null) }, Modifier.weight(1f), enabled = m > 0, color = Palette.SurfaceHi, textColor = Palette.Text)
                }
                Text(
                    "Egzamin próbny: ${SepTrainer.EXAM_SIZE} pytań, ${SepTrainer.EXAM_MINUTES} min, zaliczenie od ${SepTrainer.PASS_PERCENT}%. " +
                        "To trening – prawdziwy egzamin prowadzi komisja kwalifikacyjna (zwykle część pisemna i rozmowa). Pytania z gry nie są oficjalnymi pytaniami komisji; sprawdzaj też aktualne wydania przepisów i norm.",
                    color = Palette.TextDim, fontSize = 11.sp,
                )
            }
            LazyVerticalGrid(GridCells.Fixed(2), Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(SepCategory.entries) { c ->
                    val mastery = sep.mastery(c)
                    Column(
                        Modifier.clip(RoundedCornerShape(16.dp)).background(Palette.Surface).clickable { onStart(SepMode.LEARN, c) }.padding(12.dp),
                    ) {
                        Text("${c.icon} ${c.title}", color = Palette.Text, fontWeight = FontWeight.Bold, fontSize = 14.sp, minLines = 2)
                        Text("${SepTrainer.idsIn(c).size} pytań/zadań", color = Palette.TextDim, fontSize = 11.sp)
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(progress = { mastery / 100f }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)), color = if (mastery >= 80) Palette.Ok else Palette.Cyan, trackColor = Palette.SurfaceHi)
                        Text("Opanowane: $mastery%", color = Palette.TextDim, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

/** Sesja pytań – nauka (z natychmiastowym wyjaśnieniem) albo egzamin próbny (wynik na końcu). */
@Composable
fun SepSessionScreen(mode: SepMode, category: SepCategory?, save: SaveData, vm: AppViewModel, onExit: () -> Unit, onAgain: () -> Unit) {
    val questions = remember {
        val r = Random(System.nanoTime())
        when (mode) {
            SepMode.EXAM -> SepTrainer.exam(r)
            SepMode.MISTAKES -> SepTrainer.mistakesSet(save.sep, r).take(20)
            SepMode.CALC -> SepTrainer.learningSet(save.sep, null, 15, r, onlyCalc = true)
            SepMode.LEARN -> SepTrainer.learningSet(save.sep, category, 15, r)
        }
    }
    if (questions.isEmpty()) {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Brak pytań do powtórki – świetnie! 🎉", color = Palette.Text, fontSize = 20.sp)
            GameButton("Wróć", onExit)
        }
        return
    }
    if (mode == SepMode.EXAM) ExamSession(questions, vm, onExit, onAgain) else LearnSession(mode, category, questions, vm, onExit, onAgain)
}

@Composable
private fun LearnSession(mode: SepMode, category: SepCategory?, questions: List<Question>, vm: AppViewModel, onExit: () -> Unit, onAgain: () -> Unit) {
    var index by remember { mutableIntStateOf(0) }
    var chosen by remember { mutableStateOf<Int?>(null) }
    var good by remember { mutableIntStateOf(0) }
    var done by remember { mutableStateOf(false) }
    val title = category?.let { "${it.icon} ${it.title}" } ?: mode.title

    if (done) {
        SessionSummary("$title – koniec serii", good, questions.size, emptyList(), passed = null, onExit = onExit, onAgain = onAgain)
        return
    }
    val q = questions[index]
    Column(Modifier.fillMaxSize()) {
        Header("$title • ${index + 1}/${questions.size}", onExit) { Text("✔ $good", color = Palette.Ok, fontWeight = FontWeight.Bold) }
        LinearProgressIndicator(progress = { index / questions.size.toFloat() }, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), color = Palette.Volt, trackColor = Palette.SurfaceHi)
        Row(Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1.2f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                QuestionText(q)
                q.answers.forEachIndexed { i, a ->
                    AnswerButton(a, i, chosen = chosen, correct = q.correct, reveal = chosen != null) {
                        if (chosen == null) {
                            chosen = i
                            if (vm.sepAnswer(q, i)) good++
                        }
                    }
                }
            }
            Column(Modifier.weight(0.9f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (chosen != null) {
                    val ok = chosen == q.correct
                    Text(if (ok) "✅ Dobrze! +3 ⚡" else "❌ Niestety", color = if (ok) Palette.Ok else Palette.Bad, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Explanation(q)
                    GameButton(if (index + 1 < questions.size) "Dalej ▶" else "Podsumowanie", {
                        if (index + 1 < questions.size) { index++; chosen = null } else done = true
                    }, Modifier.fillMaxWidth())
                } else {
                    Text("Wybierz odpowiedź. Po wyborze zobaczysz wyjaśnienie i źródło zasady.", color = Palette.TextDim, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun ExamSession(questions: List<Question>, vm: AppViewModel, onExit: () -> Unit, onAgain: () -> Unit) {
    val answers = remember { mutableStateListOf<Int?>().apply { repeat(questions.size) { add(null) } } }
    var index by remember { mutableIntStateOf(0) }
    var secondsLeft by remember { mutableIntStateOf(SepTrainer.EXAM_MINUTES * 60) }
    var result by remember { mutableStateOf<Pair<ExamResult, Int>?>(null) }
    var confirmFinish by remember { mutableStateOf(false) }

    fun finish() { if (result == null) result = vm.sepExam(questions, answers.toList()) }

    LaunchedEffect(Unit) {
        while (secondsLeft > 0 && result == null) { delay(1000); secondsLeft-- }
        if (result == null) finish()
    }

    result?.let { (r, sparks) ->
        SessionSummary(
            if (r.passed) "Egzamin próbny ZALICZONY 🎉" else "Egzamin próbny niezaliczony",
            r.correct, r.total, r.wrong, passed = r.passed, sparks = sparks, onExit = onExit, onAgain = onAgain,
        )
        return
    }

    val q = questions[index]
    Column(Modifier.fillMaxSize()) {
        Header("📝 Egzamin próbny • ${index + 1}/${questions.size}", { confirmFinish = true }) {
            val m = secondsLeft / 60; val s = secondsLeft % 60
            Text("⏱ $m:${s.toString().padStart(2, '0')}", color = if (secondsLeft < 300) Palette.Bad else Palette.Volt, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        Row(Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1.3f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                QuestionText(q)
                q.answers.forEachIndexed { i, a ->
                    AnswerButton(a, i, chosen = answers[index], correct = q.correct, reveal = false) { answers[index] = i }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GameButton("◀", { if (index > 0) index-- }, small = true, enabled = index > 0, color = Palette.SurfaceHi, textColor = Palette.Text)
                    GameButton("▶", { if (index + 1 < questions.size) index++ }, small = true, enabled = index + 1 < questions.size, color = Palette.SurfaceHi, textColor = Palette.Text)
                    Spacer(Modifier.weight(1f))
                    GameButton("Zakończ egzamin", { confirmFinish = true }, small = true, color = Palette.Ok, textColor = Color.White)
                }
            }
            LazyVerticalGrid(GridCells.Fixed(6), Modifier.weight(0.7f), verticalArrangement = Arrangement.spacedBy(6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(questions.indices.toList()) { i ->
                    Box(
                        Modifier.size(38.dp).clip(RoundedCornerShape(10.dp))
                            .background(if (answers[i] != null) Palette.Cyan.copy(alpha = 0.35f) else Palette.SurfaceHi)
                            .border(2.dp, if (i == index) Palette.Volt else Color.Transparent, RoundedCornerShape(10.dp))
                            .clickable { index = i },
                        contentAlignment = Alignment.Center,
                    ) { Text("${i + 1}", color = Palette.Text, fontSize = 12.sp) }
                }
            }
        }
    }
    if (confirmFinish) {
        val missing = answers.count { it == null }
        AlertDialog(
            onDismissRequest = { confirmFinish = false },
            text = { Text(if (missing > 0) "Bez odpowiedzi: $missing pytań. Zakończyć egzamin?" else "Zakończyć egzamin i zobaczyć wynik?") },
            confirmButton = { TextButton(onClick = { confirmFinish = false; finish() }) { Text("Zakończ") } },
            dismissButton = { TextButton(onClick = { confirmFinish = false }) { Text("Wracam") } },
        )
    }
}

@Composable
private fun QuestionText(q: Question) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Palette.Surface).padding(14.dp)) {
        Text("${q.category.icon} ${q.category.title}${if (q.generated) " • zadanie" else ""}", color = Palette.TextDim, fontSize = 11.sp)
        Spacer(Modifier.height(4.dp))
        Text(q.text, color = Palette.Text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AnswerButton(text: String, i: Int, chosen: Int?, correct: Int, reveal: Boolean, onClick: () -> Unit) {
    val isChosen = chosen == i
    val bg = when {
        reveal && i == correct -> Palette.Ok.copy(alpha = 0.3f)
        reveal && isChosen -> Palette.Bad.copy(alpha = 0.3f)
        isChosen -> Palette.Cyan.copy(alpha = 0.3f)
        else -> Palette.SurfaceHi
    }
    val border = when {
        reveal && i == correct -> Palette.Ok
        reveal && isChosen -> Palette.Bad
        isChosen -> Palette.Cyan
        else -> Color.Transparent
    }
    val scale by animateFloatAsState(if (reveal && (isChosen || i == correct)) 1.02f else 1f, spring(dampingRatio = 0.4f), label = "ans")
    Row(
        Modifier.fillMaxWidth().scale(scale).clip(RoundedCornerShape(14.dp)).background(bg).border(2.dp, border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("${'A' + i}", color = Palette.Volt, fontWeight = FontWeight.Black, fontSize = 18.sp, modifier = Modifier.width(26.dp))
        Text(text, color = Palette.Text, fontSize = 15.sp)
    }
}

@Composable
private fun Explanation(q: Question) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Palette.Surface).padding(14.dp)) {
        Text("Poprawna odpowiedź: ${q.answers[q.correct]}", color = Palette.Ok, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(Modifier.height(6.dp))
        Text("💡 ${q.explanation}", color = Palette.Text, fontSize = 14.sp)
        q.source?.let { Spacer(Modifier.height(6.dp)); Text("📖 Źródło: $it", color = Palette.TextDim, fontSize = 12.sp) }
    }
}

@Composable
private fun SessionSummary(
    title: String, correct: Int, total: Int, wrong: List<Pair<Question, Int>>, passed: Boolean?,
    sparks: Int? = null, onExit: () -> Unit, onAgain: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        if (passed != false && correct * 100 >= total * SepTrainer.PASS_PERCENT) Confetti(Modifier.fillMaxSize(), count = 60)
        Row(Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(0.8f).fillMaxHeight(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, color = if (passed == false) Palette.Bad else Palette.Volt, fontSize = 24.sp, fontWeight = FontWeight.Black)
                Text("$correct / $total (${if (total == 0) 0 else correct * 100 / total}%)", color = Palette.Text, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                sparks?.let { Text("+ ⚡ $it iskier", color = Palette.Volt, fontSize = 18.sp) }
                if (passed != null) Text("Próg zaliczenia: ${SepTrainer.PASS_PERCENT}%", color = Palette.TextDim, fontSize = 12.sp)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GameButton("Menu SEP", onExit, color = Palette.SurfaceHi, textColor = Palette.Text)
                    GameButton("Jeszcze raz", onAgain)
                }
            }
            if (wrong.isNotEmpty()) {
                LazyColumn(Modifier.weight(1.2f).fillMaxHeight(), contentPadding = PaddingValues(4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { Text("Do powtórki (${wrong.size}) – trafią do „Powtórki błędów\":", color = Palette.Warn, fontWeight = FontWeight.Bold) }
                    items(wrong) { (q, chosen) ->
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Palette.Surface).padding(12.dp)) {
                            Text(q.text, color = Palette.Text, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            if (chosen >= 0) Text("Twoja: ${q.answers[chosen]}", color = Palette.Bad, fontSize = 12.sp)
                            Text("Poprawna: ${q.answers[q.correct]}", color = Palette.Ok, fontSize = 12.sp)
                            Text(q.explanation, color = Palette.TextDim, fontSize = 12.sp)
                        }
                    }
                }
            } else if (passed == null) {
                Column(Modifier.weight(1.2f).fillMaxHeight(), verticalArrangement = Arrangement.Center) {
                    Text("Każda dobra odpowiedź przesuwa pytanie do wyższego „pudełka\" – wróci rzadziej. Błędne wracają szybko, aż je opanujesz.", color = Palette.TextDim, fontSize = 13.sp)
                }
            }
        }
    }
}
