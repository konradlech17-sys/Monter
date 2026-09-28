package pl.monter.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.monter.app.ui.board.BoardTheme
import pl.monter.app.ui.board.RenderInput
import pl.monter.app.ui.board.drawBoard
import pl.monter.app.ui.board.hitTerminal
import pl.monter.app.ui.board.terminalPos
import pl.monter.app.ui.theme.Palette
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin

/**
 * Plansza montażowa. Gesty:
 *  - przeciągnięcie od zacisku do zacisku = nowy przewód (z podglądem i „przyciąganiem"),
 *  - dotknięcie zacisku, potem drugiego = to samo dla precyzyjnych,
 *  - przytrzymanie przewodu = usunięcie,
 *  - dwa palce = powiększenie i przesuwanie, jeden palec na pustym miejscu = przesuwanie (po powiększeniu).
 */
@Composable
internal fun Board(
    st: GameState,
    theme: BoardTheme,
    time: Float,
    view3d: Boolean,
    haptics: Boolean,
    tutorial: Boolean,
    onShort: () -> Unit,
    onToggle3d: () -> Unit,
) {
    val tm = rememberTextMeasurer(cacheSize = 256)
    val haptic = LocalHapticFeedback.current
    var zoom by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    val level = st.level

    fun buzz(type: HapticFeedbackType) { if (haptics) haptic.performHapticFeedback(type) }
    fun baseScale(): Float = if (canvasSize.width == 0f) 1f else min(canvasSize.width / level.width, canvasSize.height / level.height) * 0.95f
    fun origin(s: Float) = Offset((canvasSize.width - level.width * s) / 2f, (canvasSize.height - level.height * s) / 2f) + pan
    fun toBoard(p: Offset): Pair<Offset, Float> {
        val s = baseScale() * zoom
        return ((p - origin(s)) / s) to s
    }
    fun zoomAround(centroid: Offset, factor: Float, panChange: Offset = Offset.Zero) {
        val s0 = baseScale() * zoom
        val nz = (zoom * factor).coerceIn(1f, 5f)
        val s1 = baseScale() * nz
        val boardPt = (centroid - origin(s0)) / s0
        zoom = nz
        val centered = Offset((canvasSize.width - level.width * s1) / 2f, (canvasSize.height - level.height * s1) / 2f)
        pan = if (nz == 1f) Offset.Zero else centroid - boardPt * s1 - centered + panChange
    }

    // Wstrząs planszy przy zwarciu
    val shakeAge = time - st.shakeAt
    val shake = if (shakeAge in 0f..0.6f) 14f * exp(-7f * shakeAge) * sin(shakeAge * 70f) else 0f
    val showTutorial = tutorial && st.mode == Mode.BUILD && st.session.playerWires.isEmpty() && st.dragFrom == null
    val tutorialWire = if (showTutorial) level.solution.wires.firstOrNull() else null

    Box(Modifier.fillMaxSize().background(theme.background2)) {
        Canvas(
            Modifier.fillMaxSize()
                .graphicsLayer {
                    // Widok 2.5D: plansza lekko odchylona jak stół warsztatowy
                    rotationX = if (view3d) 14f else 0f
                    cameraDistance = 18f * density
                    translationX = shake * density
                }
                .pointerInput(level.id) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val (bp, s0) = toBoard(down.position)
                        val startTerm = if (st.mode == Mode.BUILD) level.hitTerminal(bp, terminalRadius(s0)) else null
                        var moved = false
                        var multi = false
                        var last = down.position
                        if (startTerm != null) {
                            st.dragFrom = startTerm; st.dragPos = bp; st.dragHover = null
                            buzz(HapticFeedbackType.TextHandleMove)
                        }
                        while (true) {
                            val ev = awaitPointerEvent()
                            val pressed = ev.changes.filter { it.pressed }
                            if (pressed.isEmpty()) break
                            if (pressed.size >= 2) {
                                multi = true
                                st.dragFrom = null; st.dragHover = null; st.dragPos = null
                                zoomAround(ev.calculateCentroid(useCurrent = true), ev.calculateZoom(), ev.calculatePan())
                                ev.changes.forEach { it.consume() }
                                continue
                            }
                            val ch = pressed.first()
                            if ((ch.position - down.position).getDistance() > viewConfiguration.touchSlop) moved = true
                            if (st.dragFrom != null) {
                                val (p, s) = toBoard(ch.position)
                                st.dragPos = p
                                val hover = level.hitTerminal(p, terminalRadius(s) * 1.4f)?.takeIf { it != startTerm }
                                if (hover != st.dragHover) {
                                    st.dragHover = hover
                                    if (hover != null) buzz(HapticFeedbackType.TextHandleMove)
                                }
                            } else if (moved && !multi && zoom > 1f) {
                                pan += ch.position - last
                            }
                            last = ch.position
                            ch.consume()
                        }
                        val duration = (currentEvent.changes.firstOrNull()?.uptimeMillis ?: down.uptimeMillis) - down.uptimeMillis
                        val from = st.dragFrom
                        val to = st.dragHover
                        st.dragFrom = null; st.dragHover = null; st.dragPos = null
                        when {
                            multi -> {}
                            from != null && moved && to != null -> if (st.connect(from, to, time)) buzz(HapticFeedbackType.LongPress)
                            from != null && moved -> st.message = "Upuść koniec przewodu na zacisku – zobaczysz zielony pierścień."
                            moved -> {}
                            st.mode == Mode.BUILD && startTerm == null && duration > 450 -> if (st.longPress(bp, s0)) buzz(HapticFeedbackType.LongPress)
                            st.mode == Mode.BUILD -> if (st.tapBuild(bp, s0, time)) buzz(HapticFeedbackType.LongPress)
                            else -> if (st.tapTest(bp, time)) { onShort(); buzz(HapticFeedbackType.LongPress) }
                        }
                    }
                },
        ) {
            canvasSize = size
            val s = baseScale() * zoom
            val o = origin(s)
            val input = RenderInput(
                level = level,
                wires = st.session.wires,
                choices = st.session.choices,
                controls = st.controls,
                sim = st.sim,
                theme = theme,
                roleHints = st.difficulty.roleHints,
                selected = st.selected,
                selectedWire = st.selectedWire,
                errorParts = st.highlightParts,
                errorWires = st.highlightWires,
                hotWires = st.hotWires,
                meter = st.meter,
                tester = st.tester,
                time = time,
                sparks = st.sparks,
                dragFrom = st.dragFrom,
                dragPos = st.dragPos,
                dragHover = st.dragHover,
                dragColor = st.dragColor(),
                suggested = st.suggested,
                ghost = st.ghost ?: tutorialWire,
                wireBorn = st.wireBorn,
                lastToggle = st.lastToggle,
            )
            withTransform({
                translate(o.x, o.y)
                scale(s, s, Offset.Zero)
            }) {
                drawBoard(input, tm)
                // Samouczek: dłoń pokazuje, jak przeciągnąć pierwszy przewód
                if (tutorialWire != null) {
                    val a = level.terminalPos(tutorialWire.a); val b = level.terminalPos(tutorialWire.b)
                    val k = ((time % 2.4f) / 1.8f).coerceIn(0f, 1f)
                    val eased = k * k * (3 - 2 * k)
                    val hand = a + (b - a) * eased
                    val layout = tm.measure("👆", TextStyle(fontSize = (44f / (density * fontScale)).sp))
                    drawText(layout, topLeft = hand + Offset(-14f, 4f))
                    val tip = tm.measure(
                        "Przeciągnij palcem od zacisku do zacisku",
                        TextStyle(color = Color.White, fontWeight = FontWeight.Bold, fontSize = (16f / (density * fontScale)).sp),
                    )
                    val at = Offset(level.width / 2 - tip.size.width / 2f, 8f)
                    drawRoundRect(Color(0xE0101820), at - Offset(10f, 4f), Size(tip.size.width + 20f, tip.size.height + 8f), androidx.compose.ui.geometry.CornerRadius(10f))
                    drawText(tip, topLeft = at)
                }
            }
        }
        // Przyciski widoku
        Column(Modifier.align(Alignment.TopStart).padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ViewButton("+") { zoomAround(Offset(canvasSize.width / 2, canvasSize.height / 2), 1.35f) }
            ViewButton("−") { zoomAround(Offset(canvasSize.width / 2, canvasSize.height / 2), 1 / 1.35f) }
            ViewButton("⤢") { zoom = 1f; pan = Offset.Zero }
            ViewButton(if (view3d) "3D" else "2D", onToggle3d)
        }
    }
}

@Composable
private fun ViewButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xCC101820)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = Palette.Text, fontWeight = FontWeight.Bold, fontSize = 15.sp) }
}
