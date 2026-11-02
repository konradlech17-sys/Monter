package pl.bramki.app.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import pl.bramki.app.ui.Accent
import pl.bramki.app.ui.Bad
import pl.bramki.app.ui.Good
import pl.bramki.app.ui.Night
import pl.bramki.engine.Format
import pl.bramki.engine.GameEvent
import pl.bramki.engine.Mode
import pl.bramki.engine.Run
import pl.bramki.engine.RunResult
import pl.bramki.engine.Side
import pl.bramki.engine.Tier
import pl.bramki.engine.TrackGenerator
import kotlin.math.abs

@Composable
fun GameScreen(mode: Mode, seed: Long, onFinished: (RunResult) -> Unit, onQuit: () -> Unit) {
    val run = remember(mode, seed) { Run(TrackGenerator.generate(mode, seed)) }
    var frame by remember { mutableLongStateOf(0L) }
    var nowMs by remember { mutableLongStateOf(0L) }
    var countdown by remember { mutableIntStateOf(3) }
    var paused by remember { mutableStateOf(false) }
    var banner by remember { mutableStateOf<Tier?>(null) }
    var lastBanner by remember { mutableStateOf<Tier?>(null) }
    val floaters = remember { mutableStateListOf<Floater>() }
    val haptic = LocalHapticFeedback.current
    val textMeasurer = rememberTextMeasurer()
    val context = LocalContext.current
    val art = remember(mode) { ModeArt(context, mode) }
    val fx = remember(run) { Fx() }

    // Pętla gry: jeden krok symulacji na klatkę ekranu.
    LaunchedEffect(run) {
        for (i in 3 downTo 1) {
            countdown = i
            delay(650)
        }
        countdown = 0
        var last = 0L
        while (isActive && !run.finished) {
            withFrameNanos { now ->
                if (last != 0L && !paused) {
                    val dt = ((now - last) / 1_000_000_000.0).coerceAtMost(0.05)
                    run.step(dt)
                }
                last = now
                nowMs = now / 1_000_000
                frame++
            }
            for (e in run.drainEvents()) {
                when (e) {
                    is GameEvent.GatePassed -> {
                        fx.gateMs = nowMs
                        fx.gatePositive = e.op.positive
                        floaters += Floater(e.op.label, if (e.op.positive) Good else Bad, nowMs, if (e.side == Side.LEFT) -1f else 1f)
                        haptic.performHapticFeedback(if (e.op.positive) HapticFeedbackType.TextHandleMove else HapticFeedbackType.LongPress)
                    }
                    is GameEvent.WallHit -> {
                        fx.wallMs = nowMs
                        floaters += Floater("🧱 " + e.op.label, Bad, nowMs, 0f)
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    is GameEvent.TierChanged -> {
                        fx.tierMs = nowMs
                        fx.tierUp = e.to > e.from
                        fx.previousTier = e.from
                        if (e.to > e.from) {
                            banner = mode.tiers[e.to]
                            lastBanner = banner
                        }
                    }
                    is GameEvent.Finished -> Unit
                }
            }
            floaters.removeAll { nowMs - it.bornMs > 1300 }
        }
        delay(700)
        run.result?.let(onFinished)
    }

    // Baner „nowa epoka" znika sam po chwili.
    LaunchedEffect(banner) {
        if (banner != null) {
            delay(4200)
            banner = null
        }
    }

    val skyColor by animateColorAsState(
        targetValue = Color(run.tier.color),
        animationSpec = tween(900),
        label = "sky",
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(Night)
            .pointerInput(run) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    var moved = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (change.changedToUp()) {
                            // Stuknięcie bez przesunięcia: skok na wybraną stronę.
                            if (!moved) run.setTarget(if (down.position.x < size.width / 2f) -0.65 else 0.65)
                            break
                        }
                        if (abs(change.position.x - down.position.x) > viewConfiguration.touchSlop) moved = true
                        if (moved) run.nudge(change.positionChange().x / (size.width * 0.28))
                        change.consume()
                    }
                }
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            frame // odczyt stanu = przerysowanie co klatkę
            val cam = Camera(size.width, size.height)
            drawWorld(run, cam, textMeasurer, skyColor, floaters, nowMs, art, fx)
        }

        Hud(run, frame, onPause = { paused = true })

        AnimatedVisibility(
            visible = banner != null,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 20.dp),
            enter = fadeIn() + slideInVertically { -it / 2 },
            exit = fadeOut() + slideOutVertically { -it / 2 },
        ) {
            (banner ?: lastBanner)?.let { TierBanner(mode, it) }
        }

        if (countdown > 0) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$countdown", fontSize = 96.sp, fontWeight = FontWeight.Black, color = Accent)
                    Text(
                        "Przesuń palcem w lewo/prawo albo stuknij połowę ekranu",
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp),
                    )
                }
            }
        }

        if (paused) {
            AlertDialog(
                onDismissRequest = { paused = false },
                title = { Text("Pauza") },
                text = { Text("${mode.emoji} ${mode.name} · ${Format.compact(run.value)} ${mode.unit}") },
                confirmButton = {
                    Button(
                        onClick = { paused = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Night),
                    ) { Text("Graj dalej") }
                },
                dismissButton = { TextButton(onClick = onQuit) { Text("Wyjdź") } },
            )
        }
    }
}

@Composable
private fun Hud(run: Run, frame: Long, onPause: () -> Unit) {
    frame
    val mode = run.mode
    val tier = run.tier
    val next = mode.nextTierValue(run.value, run.optimal)
    Column(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "${Format.full(run.value)} ${mode.unit}",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(Sprites.character(mode.id, run.tierIndex)), contentDescription = null, modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(6.dp))
                Text(tier.name, fontSize = 16.sp, color = Accent, fontWeight = FontWeight.Bold)
            }
            }
            TextButton(onClick = onPause) { Text("⏸", fontSize = 24.sp, color = Color.White) }
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { mode.tierProgress(run.value, run.optimal).toFloat() },
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = Accent,
            trackColor = Color.White.copy(alpha = 0.2f),
        )
        Text(
            if (next != null) {
                val n = mode.tiers[mode.tierIndex(run.value, run.optimal) + 1]
                "do: ${n.name} – ${Format.compact(next)} ${mode.unit}"
            } else "najwyższy szczebel!",
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.padding(top = 2.dp),
        )
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { run.progress.toFloat() },
            modifier = Modifier.fillMaxWidth().height(3.dp),
            color = Color.White.copy(alpha = 0.7f),
            trackColor = Color.White.copy(alpha = 0.15f),
        )
    }
}

@Composable
private fun TierBanner(mode: Mode, tier: Tier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(tier.color).copy(alpha = 0.92f)),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(Sprites.character(mode.id, mode.tiers.indexOf(tier))), contentDescription = null, modifier = Modifier.size(56.dp))
                Spacer(Modifier.width(10.dp))
                Text(tier.name, fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White)
            }
            Spacer(Modifier.height(6.dp))
            Text(tier.fact, color = Color.White, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
