package pl.monter.app.ui.board

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.sp
import pl.monter.core.level.Level
import pl.monter.core.level.SolWire
import pl.monter.core.model.Kind
import pl.monter.core.model.Part
import pl.monter.core.model.Potential
import pl.monter.core.model.Role
import pl.monter.core.model.TermRef
import pl.monter.core.model.Wire
import pl.monter.core.model.WireColor
import pl.monter.core.sim.LoadState
import pl.monter.core.sim.Note
import pl.monter.core.sim.SimResult
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin

data class SparkBurst(val at: Offset, val start: Float)

class RenderInput(
    val level: Level,
    val wires: List<Wire>,
    val choices: Map<String, String>,
    val controls: Map<String, Int>,
    val sim: SimResult?,
    val theme: BoardTheme,
    val roleHints: Boolean,
    val selected: TermRef?,
    val selectedWire: Int?,
    val errorParts: Set<String>,
    val errorWires: Set<Int>,
    val hotWires: Set<Int>,
    val meter: Boolean,
    val tester: Boolean,
    val time: Float,
    val sparks: List<SparkBurst>,
    /** Przeciąganie przewodu: skąd, gdzie jest palec, nad którym zaciskiem. */
    val dragFrom: TermRef? = null,
    val dragPos: Offset? = null,
    val dragHover: TermRef? = null,
    val dragColor: WireColor = WireColor.BROWN,
    /** Zaciski podpowiadane w trybie „Uczeń". */
    val suggested: Set<TermRef> = emptySet(),
    /** Podpowiedź: przewód do położenia. */
    val ghost: SolWire? = null,
    /** Kiedy (czas animacji) powstał dany przewód – do animacji wkładania. */
    val wireBorn: Map<Int, Float> = emptyMap(),
    /** Czas ostatniego przełączenia – do animacji rozjaśniania lamp. */
    val lastToggle: Float = -10f,
    /** Element przestawiany palcem – rysowany „uniesiony" nad planszą. */
    val lifted: String? = null,
    val liftedValid: Boolean = true,
)

private val Brass = Color(0xFFD4AF37)
private val ModuleWhite = Color(0xFFF4F6F8)
private val ModuleEdge = Color(0xFFB0BEC5)
private val Dark = Color(0xFF263238)

/** Tekst o rozmiarze wyrażonym w jednostkach planszy. */
private fun DrawScope.text(
    tm: TextMeasurer, s: String, center: Offset, size: Float, color: Color,
    bold: Boolean = false, maxWidth: Float = 400f,
) {
    if (s.isEmpty()) return
    val style = TextStyle(
        color = color, fontSize = (size / (density * fontScale)).sp,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, textAlign = TextAlign.Center,
    )
    val layout = tm.measure(s, style, constraints = Constraints(maxWidth = maxWidth.toInt().coerceAtLeast(1)))
    drawText(layout, topLeft = Offset(center.x - layout.size.width / 2f, center.y - layout.size.height / 2f))
}

fun DrawScope.drawBoard(inp: RenderInput, tm: TextMeasurer) {
    val level = inp.level
    // Płyta montażowa: gradient, siatka i „grubość" krawędzi (efekt bryły)
    drawRoundRect(Color.Black.copy(alpha = 0.35f), Offset(6f, 10f), Size(level.width, level.height), CornerRadius(18f))
    drawRoundRect(
        Brush.linearGradient(listOf(inp.theme.background, inp.theme.background2), Offset.Zero, Offset(level.width, level.height)),
        Offset.Zero, Size(level.width, level.height), CornerRadius(18f),
    )
    var gx = 0f
    while (gx <= level.width) { drawLine(inp.theme.grid, Offset(gx, 0f), Offset(gx, level.height), 1f); gx += 40f }
    var gy = 0f
    while (gy <= level.height) { drawLine(inp.theme.grid, Offset(0f, gy), Offset(level.width, gy), 1f); gy += 40f }
    drawRoundRect(Color.White.copy(alpha = 0.25f), Offset(2f, 2f), Size(level.width - 4, level.height - 4), CornerRadius(16f), style = Stroke(2f))

    drawRails(level)

    val tripped = inp.sim?.tripped ?: emptySet()

    // 1) Przewody (z cieniami) – leżą NA PŁYCIE, POD aparatami, jak w prawdziwej rozdzielnicy
    for (w in inp.wires) {
        val path = level.wirePath(w, sagScale(inp, w))
        translate(5f, 9f) { drawPath(path, Color.Black.copy(alpha = 0.16f), style = Stroke(wireWidth(w) + 2f, cap = StrokeCap.Round, join = StrokeJoin.Round)) }
    }
    for (w in inp.wires) drawWire(w, inp)

    // 2) Aparaty przykrywają przewody
    for (p in level.parts) if (p.id != inp.lifted) drawPartAt(p, inp, tm, p.id in tripped)

    // 3) Zaciski na wierzchu – zawsze widoczne i łatwe do trafienia
    for (p in level.parts) if (p.id != inp.lifted) drawTerminals(p, inp, tm)

    // 4) Podpowiedzi i przeciągany przewód nad wszystkim
    inp.ghost?.let { drawGhost(it, inp, tm) }
    drawRubberBand(inp)

    // 5) Przestawiany element – uniesiony, z dużym cieniem
    inp.lifted?.let { id ->
        val p = level.part(id)
        drawRoundRect(Color.Black.copy(alpha = 0.3f), Offset(p.x + 14, p.y + 22), Size(p.kind.w, p.kind.h), CornerRadius(12f))
        translate(-4f, -8f) {
            drawPartAt(p, inp, tm, p.id in tripped)
            drawTerminals(p, inp, tm)
            drawRoundRect(
                (if (inp.liftedValid) Color(0xFF66BB6A) else Color(0xFFEF5350)).copy(alpha = 0.9f),
                Offset(p.x - 6, p.y - 6), Size(p.kind.w + 12, p.kind.h + 12), CornerRadius(12f), style = Stroke(3f),
            )
        }
    }
    drawSparks(inp)
}

private fun DrawScope.drawRails(level: Level) {
    val din = level.parts.filter { it.kind.isSwitchgear || it.kind.isSpd || it.kind == Kind.CONTACTOR || (it.kind == Kind.TRANSFORMER && level.parts.any { p -> p.kind.isSwitchgear }) }
    din.groupBy { it.y }.forEach { (y, row) ->
        val x0 = row.minOf { it.x } - 20; val x1 = row.maxOf { it.x + it.kind.w } + 20
        val ry = y + 50
        drawRect(Color.Black.copy(alpha = 0.2f), Offset(x0 + 3, ry + 5), Size(x1 - x0, 20f))
        drawRect(Brush.verticalGradient(listOf(Color(0xFFCFD8DC), Color(0xFF78909C)), ry, ry + 20), Offset(x0, ry), Size(x1 - x0, 20f))
        drawRect(Color.White.copy(alpha = 0.5f), Offset(x0, ry + 2), Size(x1 - x0, 3f))
    }
}

private fun DrawScope.drawPartAt(p: Part, inp: RenderInput, tm: TextMeasurer, tripped: Boolean) {
    translate(p.x, p.y) { drawPart(p, inp, tm, tripped) }
    if (p.id in inp.errorParts) {
        val a = 0.5f + 0.5f * sin(inp.time * 6f)
        drawRoundRect(Color(0xFFEF5350).copy(alpha = a), Offset(p.x - 6, p.y - 6), Size(p.kind.w + 12, p.kind.h + 12), CornerRadius(12f), style = Stroke(4f))
    }
}

// ============================================================================ przewody

private fun wireWidth(w: Wire) = when (w.cs.mm2) {
    in 0.0..0.6 -> 3f; in 0.6..1.6 -> 5f; in 1.6..2.6 -> 6f; in 2.6..4.1 -> 7f; in 4.1..6.1 -> 8f; else -> 9.5f
}

/** Sprężynowanie świeżo położonego przewodu. */
private fun sagScale(inp: RenderInput, w: Wire): Float {
    val born = inp.wireBorn[w.id] ?: return 1f
    val a = inp.time - born
    if (a < 0f || a > 1.5f) return 1f
    return 1f + 0.45f * exp(-4f * a) * sin(14f * a)
}

private fun DrawScope.drawWire(w: Wire, inp: RenderInput) {
    val level = inp.level
    var path = level.wirePath(w, sagScale(inp, w))
    val width = wireWidth(w)
    // Animacja „wkładania" – przewód wysuwa się od pierwszego zacisku
    val age = inp.wireBorn[w.id]?.let { inp.time - it } ?: 10f
    if (age in 0f..0.3f) {
        val pm = PathMeasure(); pm.setPath(path, false)
        val seg = Path(); pm.getSegment(0f, pm.length * (age / 0.3f), seg, true)
        path = seg
    }
    if (w.id in inp.errorWires || w.id in inp.hotWires) {
        val a = 0.4f + 0.4f * sin(inp.time * 6f)
        val c = if (w.id in inp.hotWires) Color(0xFFFF7043) else Color(0xFFEF5350)
        drawPath(path, c.copy(alpha = a), style = Stroke(width + 12f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
    if (w.id == inp.selectedWire) drawPath(path, Color(0xFFFFC107).copy(alpha = 0.7f), style = Stroke(width + 10f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    if (inp.theme.neon) drawPath(path, w.color.ui().copy(alpha = 0.35f), style = Stroke(width + 8f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(path, Color.Black.copy(alpha = 0.4f), style = Stroke(width + 2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(path, w.color.ui(), style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
    if (w.color == WireColor.GREEN_YELLOW) {
        drawPath(path, Color(0xFFFDD835), style = Stroke(width, cap = StrokeCap.Butt, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))))
    }
    // Połysk izolacji (walcowy kształt przewodu)
    translate(-width * 0.18f, -width * 0.22f) {
        drawPath(path, Color.White.copy(alpha = 0.28f), style = Stroke(width * 0.28f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
    if (!w.fixed && age > 0.3f) {
        drawCircle(Color(0xFFB0BEC5), width * 0.55f, level.terminalPos(w.a)); drawCircle(Color(0xFFB0BEC5), width * 0.55f, level.terminalPos(w.b))
    }
    val sim = inp.sim ?: return
    if (sim.wireLive(w)) {
        val lv = sim.potentials(w.a).all { it.isLowVoltage }
        val glow = if (lv) Color(0xFF80DEEA) else Color(0xFFFFF59D)
        drawPath(path, glow.copy(alpha = 0.25f), style = Stroke(width + 6f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        // „Elektrony" płynące wzdłuż przewodu
        val pm = PathMeasure(); pm.setPath(path, false)
        val len = pm.length
        if (len > 1f) {
            val n = (len / 55f).toInt().coerceAtLeast(1)
            for (i in 0 until n) {
                val d = ((inp.time * 70f + i * len / n) % len)
                val pos = pm.getPosition(d)
                drawCircle(glow.copy(alpha = 0.45f), width * 0.9f, pos)
                drawCircle(Color.White, width * 0.35f, pos)
            }
        }
    }
}

private fun DrawScope.drawRubberBand(inp: RenderInput) {
    val from = inp.dragFrom ?: return
    val pos = inp.dragPos ?: return
    val level = inp.level
    val a = level.terminalPos(from)
    val target = inp.dragHover?.let { level.terminalPos(it) } ?: pos
    val downB = inp.dragHover?.let { level.terminalDown(it) } ?: (target.y > a.y)
    val path = polyPath(orthoRoute(a, level.terminalDown(from), target, downB))
    translate(5f, 9f) { drawPath(path, Color.Black.copy(alpha = 0.15f), style = Stroke(8f, cap = StrokeCap.Round, join = StrokeJoin.Round)) }
    drawPath(path, Color.Black.copy(alpha = 0.4f), style = Stroke(8f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(path, inp.dragColor.ui(), style = Stroke(6f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    if (inp.dragColor == WireColor.GREEN_YELLOW) {
        drawPath(path, Color(0xFFFDD835), style = Stroke(6f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))))
    }
    val snap = inp.dragHover != null
    drawCircle((if (snap) Color(0xFF66BB6A) else Color(0xFFFFC107)).copy(alpha = 0.35f), if (snap) 22f else 16f, target)
    drawCircle(if (snap) Color(0xFF66BB6A) else Color(0xFFFFC107), if (snap) 22f else 16f, target, style = Stroke(3f))
}

private fun DrawScope.drawGhost(g: SolWire, inp: RenderInput, tm: TextMeasurer) {
    val level = inp.level
    val path = polyPath(orthoRoute(level.terminalPos(g.a), level.terminalDown(g.a), level.terminalPos(g.b), level.terminalDown(g.b)))
    val a = 0.55f + 0.35f * sin(inp.time * 5f)
    drawPath(path, Color.White.copy(alpha = a * 0.6f), style = Stroke(12f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(path, g.color.ui().copy(alpha = a), style = Stroke(6f, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), -inp.time * 40f)))
    for (t in listOf(g.a, g.b)) {
        val c = level.terminalPos(t)
        drawCircle(Color(0xFFFFC107).copy(alpha = a), 18f + 4f * sin(inp.time * 6f), c, style = Stroke(3f))
    }
    val mid = level.terminalPos(g.a) + (level.terminalPos(g.b) - level.terminalPos(g.a)) * 0.5f
    drawRoundRect(Color(0xE0101820), mid - Offset(70f, 12f), Size(140f, 24f), CornerRadius(12f))
    text(tm, "${g.color.pl}, ${g.cs.label}", mid, 11f, Color.White, bold = true, maxWidth = 140f)
}

// ============================================================================ zaciski

private fun DrawScope.drawTerminals(p: Part, inp: RenderInput, tm: TextMeasurer) {
    for (t in p.kind.terminals) {
        val ref = TermRef(p.id, t.id)
        val c = Offset(p.x + t.x, p.y + t.y)
        if (inp.selected == ref || inp.dragFrom == ref) {
            val r = 13f + 3f * sin(inp.time * 8f)
            drawCircle(Color(0xFFFFC107).copy(alpha = 0.45f), r + 4f, c)
            drawCircle(Color(0xFFFFC107), r, c, style = Stroke(3f))
        }
        if (ref in inp.suggested) {
            val r = 16f + 4f * sin(inp.time * 7f)
            drawCircle(Color(0xFF66BB6A).copy(alpha = 0.35f), r, c)
            drawCircle(Color(0xFF66BB6A), r, c, style = Stroke(2.5f))
        }
        if (inp.dragHover == ref) drawCircle(Color(0xFF66BB6A), 14f, c)
        if (inp.roleHints && t.role != Role.ANY) drawCircle(t.role.ui(), 10.5f, c)
        // Śruba zacisku z cieniem i połyskiem
        drawCircle(Color.Black.copy(alpha = 0.3f), 8f, c + Offset(1.5f, 2.5f))
        drawCircle(Color(0xFF5D4037), 8f, c)
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF3C4), Brass, Color(0xFF8D6E2A)), c - Offset(2.5f, 2.5f), 9f), 7f, c)
        drawLine(Color(0xFF6D4C41), c + Offset(-4.5f, 0f), c + Offset(4.5f, 0f), 2f)
        if (t.label.isNotEmpty()) {
            val above = t.y > p.kind.h / 2
            text(tm, t.label, c + Offset(0f, if (above) -16f else 16f), 10.5f, Dark, bold = true)
        }
        val sim = inp.sim ?: continue
        val pots = sim.potentials(ref)
        if (inp.meter && pots.isNotEmpty()) {
            val label = pots.sortedBy { it.ordinal }.joinToString("+") { it.label }
            val bg = when {
                pots.any { it.isPhase } -> Color(0xFFD84315)
                Potential.N in pots -> Color(0xFF1565C0)
                Potential.PE in pots -> Color(0xFF2E7D32)
                else -> Color(0xFF00838F)
            }
            val at = c + Offset(0f, if (t.y > p.kind.h / 2) 26f else -26f)
            drawRoundRect(bg, at - Offset(21f, 8f), Size(42f, 16f), CornerRadius(8f))
            text(tm, label, at, 9f, Color.White, bold = true)
        } else if (inp.tester && pots.any { it.isPhase }) {
            val a = 0.5f + 0.5f * sin(inp.time * 10f)
            drawCircle(Color(0xFFFF1744).copy(alpha = a), 11f, c, style = Stroke(3f))
        }
    }
}

// ============================================================================ iskry przy zwarciu

private fun DrawScope.drawSparks(inp: RenderInput) {
    for (s in inp.sparks) {
        val age = inp.time - s.start
        if (age < 0f || age > 1.2f) continue
        val k = age / 1.2f
        drawCircle(Color(0xFFFFF59D).copy(alpha = (1f - k) * 0.8f), 34f * (1f - k) + 6f, s.at)
        for (i in 0 until 16) {
            val ang = (i * 2 * PI / 16 + s.start).toFloat()
            val r0 = 10f + 70f * k; val r1 = r0 + 20f * (1f - k)
            drawLine(
                Color(0xFFFFC107).copy(alpha = 1f - k),
                s.at + Offset(cos(ang) * r0, sin(ang) * r0 + 30f * k * k), s.at + Offset(cos(ang) * r1, sin(ang) * r1 + 30f * k * k), 3f, StrokeCap.Round,
            )
        }
    }
}

// ============================================================================ elementy

/** Bryła obudowy: cień, gradient „światło z góry", połysk krawędzi. */
private fun DrawScope.body(x: Float, y: Float, w: Float, h: Float, color: Color = ModuleWhite, r: Float = 8f, edge: Color = ModuleEdge) {
    drawRoundRect(Color.Black.copy(alpha = 0.22f), Offset(x + 5, y + 8), Size(w, h), CornerRadius(r))
    drawRoundRect(darker(color, 0.75f), Offset(x, y + 4), Size(w, h), CornerRadius(r))
    drawRoundRect(Brush.verticalGradient(listOf(lighter(color), color, darker(color, 0.92f)), y, y + h), Offset(x, y), Size(w, h), CornerRadius(r))
    drawRoundRect(edge, Offset(x, y), Size(w, h), CornerRadius(r), style = Stroke(1.5f))
    drawRoundRect(Color.White.copy(alpha = 0.55f), Offset(x + 2, y + 1.5f), Size(w - 4, 3f), CornerRadius(2f))
}

private fun lighter(c: Color) = Color(min(1f, c.red + 0.08f), min(1f, c.green + 0.08f), min(1f, c.blue + 0.08f), c.alpha)
private fun darker(c: Color, k: Float) = Color(c.red * k, c.green * k, c.blue * k, c.alpha)

private fun DrawScope.terminalStrip(x: Float, y: Float, w: Float, h: Float) {
    drawRoundRect(Color.Black.copy(alpha = 0.15f), Offset(x + 2, y + 3), Size(w, h), CornerRadius(5f))
    drawRoundRect(Brush.verticalGradient(listOf(Color(0xFFE0E6EA), Color(0xFFB0BEC5)), y, y + h), Offset(x, y), Size(w, h), CornerRadius(5f))
}

private fun DrawScope.drawPart(p: Part, inp: RenderInput, tm: TextMeasurer, tripped: Boolean) {
    val k = p.kind
    val w = k.w; val h = k.h
    val st = inp.controls[p.id] ?: k.defaultState
    val load = inp.sim?.loads?.get(p.id)
    val on = load?.state == LoadState.ON
    val damaged = load?.state == LoadState.DAMAGED
    val t = inp.time
    val fade = ((t - inp.lastToggle) / 0.45f).coerceIn(0f, 1f)
    val labelColor = inp.theme.text

    when (k) {
        Kind.SUPPLY_1P, Kind.SUPPLY_3P -> {
            drawRect(Brush.horizontalGradient(listOf(Color(0xFF37474F), Color(0xFF78909C), Color(0xFF37474F)), w / 2 - 14, w / 2 + 14), Offset(w / 2 - 14, -30f), Size(28f, 34f))
            body(0f, 0f, w, h - 4, Color(0xFF37474F), 10f, Color(0xFF263238))
            text(tm, "⚡ " + p.label, Offset(w / 2, 18f), 11f, Color.White, bold = true, maxWidth = w - 8)
            for (term in k.terminals) {
                drawLine(term.role.suggestedColor().ui(), Offset(term.x, 34f), Offset(term.x, term.y), 7f, StrokeCap.Round)
                drawLine(Color.White.copy(alpha = 0.3f), Offset(term.x - 1.5f, 34f), Offset(term.x - 1.5f, term.y - 4), 2f, StrokeCap.Round)
            }
        }
        Kind.LAMP -> {
            drawRoundRect(Brush.verticalGradient(listOf(Color(0xFFCFD8DC), Color(0xFF78909C)), 0f, 12f), Offset(w / 2 - 30, 0f), Size(60f, 12f), CornerRadius(4f))
            drawLine(Color(0xFF455A64), Offset(w / 2, 12f), Offset(w / 2, 30f), 3f)
            val bulbC = Offset(w / 2, 52f)
            val needsChoice = p.options.isNotEmpty() && inp.choices[p.id] == null
            if (on && !needsChoice) {
                val pulse = (0.8f + 0.2f * sin(t * 3f)) * fade
                drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF176).copy(alpha = 0.85f * pulse), Color.Transparent), bulbC, 110f), 110f, bulbC)
                drawCircle(Brush.radialGradient(listOf(Color.White, Color(0xFFFFEB3B)), bulbC - Offset(5f, 5f), 22f), 20f, bulbC)
            } else {
                drawCircle(Brush.radialGradient(listOf(Color.White, if (damaged) Color(0xFF424242) else Color(0xFFCFD8DC)), bulbC - Offset(6f, 6f), 24f), 20f, bulbC)
                drawCircle(Color(0xFF90A4AE), 20f, bulbC, style = Stroke(1.5f))
                if (!needsChoice) drawLine(Color(0xFF8D6E63), bulbC + Offset(-6f, 4f), bulbC + Offset(6f, 4f), 1.5f)
            }
            if (needsChoice) text(tm, "?", bulbC, 22f, Color(0xFFE65100), bold = true)
            drawRect(Brush.horizontalGradient(listOf(Color(0xFF757575), Color(0xFFBDBDBD), Color(0xFF757575)), w / 2 - 9, w / 2 + 9), Offset(w / 2 - 9, 70f), Size(18f, 10f))
            terminalStrip(4f, 84f, w - 8, 24f)
            if (damaged) smoke(Offset(w / 2, 30f), t)
        }
        Kind.FAN -> {
            body(4f, 0f, w - 8, 84f, Color.White, 12f)
            val c = Offset(w / 2, 42f)
            drawCircle(Color(0xFFECEFF1), 32f, c)
            val angle = if (on) t * 720f * fade else 0f
            rotate(angle, c) {
                for (i in 0 until 4) {
                    rotate(i * 90f, c) { drawOval(Color(0xFF90A4AE), c + Offset(-7f, -28f), Size(14f, 26f)) }
                }
            }
            drawCircle(Color(0xFF607D8B), 6f, c)
            for (i in 0 until 5) drawLine(Color(0x33000000), c + Offset(-30f, -20f + i * 10f), c + Offset(30f, -20f + i * 10f), 1f)
            text(tm, "⧈", Offset(w - 16f, 12f), 12f, Dark, bold = true)
            terminalStrip(4f, 86f, w - 8, 22f)
        }
        Kind.MOTOR -> {
            val reverse = load?.notes?.contains(Note.PHASE_ORDER) == true
            // Korpus żebrowany
            body(8f, 6f, w - 16, 84f, Color(0xFF546E7A), 14f, Color(0xFF37474F))
            for (i in 0 until 7) drawLine(Color(0x55000000), Offset(20f + i * 17f, 12f), Offset(20f + i * 17f, 84f), 2f)
            val c = Offset(w - 28f, 48f)
            drawCircle(Color(0xFF263238), 22f, c)
            val angle = if (on) (if (reverse) -1f else 1f) * t * 900f * fade else 0f
            rotate(angle, c) {
                for (i in 0 until 3) rotate(i * 120f, c) { drawRect(Color(0xFFB0BEC5), c + Offset(-3f, -20f), Size(6f, 18f)) }
            }
            drawCircle(Color(0xFFCFD8DC), 4f, c)
            if (on) text(tm, if (reverse) "↺ źle!" else "↻", Offset(46f, 48f), 18f, if (reverse) Color(0xFFFF5252) else Color(0xFFB9F6CA), bold = true)
            terminalStrip(4f, 96f, w - 8, 22f)
            if (damaged) smoke(c, t)
        }
        Kind.SOCKET -> {
            body(4f, 0f, w - 8, 80f, Color.White, 10f)
            val c = Offset(w / 2, 40f)
            drawCircle(Brush.radialGradient(listOf(Color(0xFFCFD8DC), if (on) Color(0xFFE8F5E9) else Color(0xFFF5F5F5)), c + Offset(4f, 4f), 32f), 30f, c)
            drawCircle(Color(0xFFB0BEC5), 30f, c, style = Stroke(1.5f))
            drawCircle(Dark, 5f, c + Offset(-12f, 2f)); drawCircle(Dark, 5f, c + Offset(12f, 2f))
            drawRoundRect(Brush.horizontalGradient(listOf(Color(0xFF9E9E9E), Color(0xFFE0E0E0), Color(0xFF9E9E9E)), c.x - 3, c.x + 3), c + Offset(-3f, -24f), Size(6f, 12f), CornerRadius(2f))
            if (on) drawCircle(Color(0xFF66BB6A).copy(alpha = fade), 4f, Offset(w - 16, 10f))
            terminalStrip(4f, 84f, w - 8, 24f)
            if (damaged) smoke(c, t)
        }
        Kind.SOCKET_400 -> {
            body(4f, 0f, w - 8, 92f, Color(0xFFF5F5F5), 10f)
            val c = Offset(w / 2, 46f)
            drawCircle(Brush.radialGradient(listOf(Color(0xFFEF5350), Color(0xFFB71C1C)), c - Offset(10f, 10f), 50f), 40f, c)
            drawCircle(Color(0xFF8E0000), 30f, c)
            for (i in 0 until 5) {
                val a = (-PI / 2 + i * 2 * PI / 5).toFloat()
                drawCircle(Color.Black, 5f, c + Offset(cos(a) * 18f, sin(a) * 18f))
            }
            if (on) drawCircle(Color(0xFF66BB6A).copy(alpha = fade), 4f, Offset(w - 16, 10f))
            terminalStrip(4f, 96f, w - 8, 22f)
        }
        Kind.SWITCH_1, Kind.SWITCH_STAIR, Kind.SWITCH_CROSS -> {
            body(4f, 0f, w - 8, 74f, Color.White, 10f)
            rocker(Offset(w / 2 - 20, 10f), 40f, 54f, if (k == Kind.SWITCH_1) st == 1 else st == 0)
            val sym = when (k) { Kind.SWITCH_1 -> if (st == 1) "I" else "O"; Kind.SWITCH_STAIR -> "↕${st + 1}"; else -> if (st == 0) "‖" else "✕" }
            text(tm, sym, Offset(w / 2, 37f), 13f, Dark, bold = true)
            terminalStrip(4f, 78f, w - 8, 20f)
        }
        Kind.DIMMER -> {
            body(4f, 0f, w - 8, 74f, Color.White, 10f)
            val c = Offset(w / 2, 37f)
            drawCircle(Color.Black.copy(alpha = 0.2f), 24f, c + Offset(2f, 3f))
            drawCircle(Brush.radialGradient(listOf(Color.White, Color(0xFFB0BEC5)), c - Offset(6f, 6f), 30f), 24f, c)
            val ang = if (st == 1) 120f else -120f
            rotate(ang, c) { drawLine(Color(0xFFFFA000), c, c + Offset(0f, -18f), 4f, StrokeCap.Round) }
            if (st == 1) drawArc(Color(0xFFFFC107).copy(alpha = 0.8f), 150f, 240f * fade, false, c - Offset(30f, 30f), Size(60f, 60f), style = Stroke(3f))
            terminalStrip(4f, 78f, w - 8, 20f)
        }
        Kind.STRIKE -> {
            body(8f, 0f, w - 16, 74f, Color(0xFF90A4AE), 8f, Color(0xFF546E7A))
            drawRoundRect(Color(0xFF37474F), Offset(w / 2 - 18, 20f), Size(36f, 30f), CornerRadius(4f))
            val open = on
            drawRoundRect(Color(0xFFFFC107), Offset(w / 2 - (if (open) 30f else 10f), 28f), Size(20f, 14f), CornerRadius(3f))
            text(tm, if (open) "🔓" else "🔒", Offset(w / 2, 62f), 14f, Dark)
            if (damaged) smoke(Offset(w / 2, 30f), t)
            terminalStrip(4f, 78f, w - 8, 20f)
        }
        Kind.BOILER -> {
            body(14f, 0f, w - 28, 104f, Color.White, 30f)
            val heat = if (on) fade else 0f
            drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF90CAF9), Color(0xFFEF5350).copy(alpha = 0.3f + 0.6f * heat)), 12f, 96f), Offset(26f, 14f), Size(w - 52, 80f), CornerRadius(20f))
            if (on) for (i in 0 until 3) {
                val a = ((t * 0.8f + i / 3f) % 1f)
                drawCircle(Color.White.copy(alpha = (1f - a) * 0.7f), 3f + a * 3f, Offset(w / 2 - 12f + i * 12f, 90f - a * 70f))
            }
            text(tm, if (on) "🔥" else "💧", Offset(w / 2, 54f), 18f, Dark)
            terminalStrip(4f, 106f, w - 8, 22f)
            if (damaged) smoke(Offset(w / 2, 30f), t)
        }
        Kind.WALLBOX -> {
            body(8f, 0f, w - 16, 104f, Color(0xFF263238), 14f, Color(0xFF102027))
            drawRoundRect(Color(0xFF37474F), Offset(24f, 14f), Size(w - 48, 30f), CornerRadius(6f))
            val charge = if (on) ((t * 0.25f) % 1f) else 0f
            drawRoundRect(Color(0xFF66BB6A), Offset(28f, 20f), Size((w - 56) * charge, 18f), CornerRadius(4f))
            text(tm, if (on) "⚡ ładowanie" else "EV", Offset(w / 2, 29f), 10f, Color.White, bold = true)
            text(tm, "🚗", Offset(w / 2, 72f), 26f, Color.White)
            terminalStrip(4f, 106f, w - 8, 22f)
        }
        Kind.CONTACTOR -> drawModule(p, inp, tm, st, tripped, on)
        Kind.SWITCH_2 -> {
            body(4f, 0f, w - 8, 74f, Color.White, 10f)
            rocker(Offset(14f, 10f), 38f, 54f, st and 1 != 0)
            rocker(Offset(w - 52f, 10f), 38f, 54f, st and 2 != 0)
            text(tm, "1", Offset(33f, 37f), 12f, Dark, bold = true); text(tm, "2", Offset(w - 33f, 37f), 12f, Dark, bold = true)
            terminalStrip(4f, 78f, w - 8, 20f)
        }
        Kind.BUTTON -> {
            body(4f, 0f, w - 8, 64f, Color.White, 10f)
            val pressed = st == 1
            val c = Offset(w / 2, if (pressed) 34f else 31f)
            if (!pressed) drawCircle(Color(0xFFC79100), 18f, c + Offset(0f, 3f))
            drawCircle(Brush.radialGradient(listOf(Color(0xFFFFE082), if (pressed) Color(0xFFFFA000) else Color(0xFFFFCA28)), c - Offset(5f, 5f), 22f), if (pressed) 16f else 18f, c)
            text(tm, "🔔", c, 14f, Dark)
            terminalStrip(4f, 68f, w - 8, 20f)
        }
        Kind.BELL -> {
            val c = Offset(w / 2, 46f)
            val shake = if (on) 10f * sin(t * 40f) else 0f
            rotate(shake, c) {
                drawArc(Brush.linearGradient(listOf(Color(0xFFFFE082), Color(0xFFFFA000)), c - Offset(38f, 38f), c + Offset(38f, 38f)), 180f, 180f, true, c - Offset(38f, 38f), Size(76f, 76f))
                drawArc(Color.White.copy(alpha = 0.4f), 200f, 50f, false, c - Offset(30f, 30f), Size(60f, 60f), style = Stroke(4f))
                drawCircle(Color(0xFF6D4C41), 5f, c + Offset(0f, 4f))
            }
            if (on) for (i in 1..3) {
                val a = ((t * 2f + i / 3f) % 1f)
                drawArc(Color(0xFFFFC107).copy(alpha = 1f - a), -60f, 120f, false, c - Offset(42f + a * 30f, 42f + a * 30f), Size(84f + a * 60f, 84f + a * 60f), style = Stroke(3f))
            }
            if (damaged) smoke(c, t)
            terminalStrip(4f, 78f, w - 8, 20f)
        }
        Kind.TRANSFORMER -> {
            body(0f, 0f, w, h, ModuleWhite, 6f)
            drawRect(Color(0xFFCFD8DC), Offset(0f, 0f), Size(w, 24f)); drawRect(Color(0xFFCFD8DC), Offset(0f, h - 24f), Size(w, 24f))
            val c = Offset(w / 2, h / 2)
            drawCircle(Color(0xFF455A64), 16f, c + Offset(-10f, 0f), style = Stroke(3f))
            drawCircle(Color(0xFF455A64), 16f, c + Offset(10f, 0f), style = Stroke(3f))
            text(tm, "230V/8V", c + Offset(0f, 26f), 10f, Dark, bold = true)
            if (on) drawCircle(Color(0xFF66BB6A), 4f, Offset(w - 10f, 34f))
        }
        Kind.MOTION_SENSOR -> {
            body(4f, 0f, w - 8, 74f, Color.White, 10f)
            val c = Offset(w / 2, 36f)
            val dusk = p.label.contains("zmierzch", ignoreCase = true)
            val timer = p.label.contains("Zegar")
            drawCircle(Brush.radialGradient(listOf(Color.White, Color(0xFFBDBDBD)), c - Offset(8f, 8f), 34f), 26f, c)
            for (i in 0 until 4) drawCircle(Color(0x22000000), 26f - i * 6f, c, style = Stroke(1f))
            when {
                timer -> {
                    text(tm, if (st == 1) "🌙" else "☀️", c + Offset(0f, 2f), 18f, Dark)
                    val ang = t * 30f
                    rotate(ang, c) { drawLine(Dark, c, c + Offset(0f, -20f), 2f) }
                }
                st == 1 -> text(tm, if (dusk) "🌙" else "🚶", c, 22f, Dark)
                dusk -> text(tm, "☀️", c, 20f, Dark)
            }
            drawCircle(if (on) Color(0xFF66BB6A) else Color(0xFF9E9E9E), 4f, Offset(16f, 10f))
            terminalStrip(4f, 78f, w - 8, 20f)
        }
        Kind.WAGO3, Kind.WAGO5 -> {
            drawRoundRect(Color.Black.copy(alpha = 0.2f), Offset(3f, 13f), Size(w, 28f), CornerRadius(6f))
            drawRoundRect(Brush.verticalGradient(listOf(Color(0xDDE3E8EB), Color(0xCC90A4AE)), 8f, 36f), Offset(0f, 8f), Size(w, 28f), CornerRadius(6f))
            for (term in k.terminals) {
                drawRoundRect(Color(0xFFBF6C00), Offset(term.x - 7f, 1.5f), Size(14f, 12f), CornerRadius(3f))
                drawRoundRect(Color(0xFFFF9800), Offset(term.x - 7f, 0f), Size(14f, 11f), CornerRadius(3f))
            }
        }
        Kind.MAIN_SWITCH_2P, Kind.MAIN_SWITCH_4P, Kind.RCD_2P, Kind.RCD_4P, Kind.MCB_1P, Kind.MCB_3P, Kind.SPD_4P, Kind.SPD_2P ->
            drawModule(p, inp, tm, st, tripped, on)
        Kind.N_BAR, Kind.PE_BAR -> {
            drawRoundRect(Color.Black.copy(alpha = 0.2f), Offset(3f, 9f), Size(w, h - 8), CornerRadius(6f))
            if (k == Kind.N_BAR) drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF42A5F5), Color(0xFF1565C0)), 4f, h - 4), Offset(0f, 4f), Size(w, h - 8), CornerRadius(6f))
            else {
                drawRoundRect(Color(0xFF43A047), Offset(0f, 4f), Size(w, h - 8), CornerRadius(6f))
                var x = 0f
                while (x < w) { drawRect(Color(0xFFFDD835), Offset(x, 4f), Size(10f, h - 8)); x += 20f }
            }
            drawRect(Brush.verticalGradient(listOf(Color(0xFFFFE0B2), Color(0xFFBCAAA4), Color(0xFF8D6E63)), h / 2 - 5, h / 2 + 5), Offset(6f, h / 2 - 5), Size(w - 12, 10f))
            text(tm, if (k == Kind.N_BAR) "N" else "PE", Offset(w + 16f, h / 2), 13f, labelColor, bold = true)
        }
        Kind.CIRCUIT_1P, Kind.CIRCUIT_3P -> {
            body(0f, 0f, w, h, if (on) Color(0xFFE8F5E9) else Color(0xFFECEFF1), 10f, if (on) Color(0xFF43A047) else ModuleEdge)
            text(tm, "🏠 " + p.label, Offset(w / 2, 40f), 12f, Dark, bold = true, maxWidth = w - 8)
            p.circuit?.let { text(tm, "maks. ${it.maxA} A" + if (it.needsRcd) " • RCD ${it.rcdMaxmA} mA" else "", Offset(w / 2, 57f), 9f, Color(0xFF546E7A), maxWidth = w - 8) }
            if (on) text(tm, "✔", Offset(w - 14f, 14f), 14f, Color(0xFF2E7D32), bold = true)
        }
    }

    val inside = k.isSupply || k == Kind.CIRCUIT_1P || k == Kind.CIRCUIT_3P
    if (!inside) {
        val chosen = p.options.firstOrNull { it.id == inp.choices[p.id] }?.label
        text(tm, chosen ?: p.label, Offset(w / 2, -13f), 11f, labelColor, bold = true, maxWidth = maxOf(w + 60f, 120f))
    }
}

private fun DrawScope.rocker(topLeft: Offset, w: Float, h: Float, on: Boolean) {
    drawRoundRect(Color.Black.copy(alpha = 0.15f), topLeft + Offset(2f, 3f), Size(w, h), CornerRadius(6f))
    // Klawisz przechylony: jasna połowa wystaje, ciemna jest wciśnięta
    val top = if (on) listOf(Color(0xFFBDBDBD), Color(0xFFF5F5F5)) else listOf(Color.White, Color(0xFFE0E0E0))
    val bottom = if (on) listOf(Color.White, Color(0xFFEEEEEE)) else listOf(Color(0xFFE0E0E0), Color(0xFFBDBDBD))
    drawRoundRect(Brush.verticalGradient(top, topLeft.y, topLeft.y + h / 2), topLeft, Size(w, h / 2 + 2), CornerRadius(6f))
    drawRoundRect(Brush.verticalGradient(bottom, topLeft.y + h / 2, topLeft.y + h), topLeft + Offset(0f, h / 2), Size(w, h / 2), CornerRadius(6f))
    drawRoundRect(Color(0xFFBDBDBD), topLeft, Size(w, h), CornerRadius(6f), style = Stroke(1.5f))
}

private fun DrawScope.smoke(c: Offset, t: Float) {
    for (i in 0 until 4) {
        val a = ((t * 0.7f + i / 4f) % 1f)
        drawCircle(Color(0xFF616161).copy(alpha = (1f - a) * 0.5f), 8f + a * 16f, c + Offset(sin(i + t) * 8f, -a * 50f))
    }
}

/** Aparat modułowy na szynie DIN: dźwignia, oznaczenia, okienka stanu. */
private fun DrawScope.drawModule(p: Part, inp: RenderInput, tm: TextMeasurer, st: Int, tripped: Boolean, on: Boolean) {
    val k = p.kind
    val w = k.w; val h = k.h
    body(0f, 0f, w, h, ModuleWhite, 5f)
    drawRect(Color(0xFFCFD8DC), Offset(2f, 2f), Size(w - 4, 20f))
    drawRect(Color(0xFFCFD8DC), Offset(2f, h - 22f), Size(w - 4, 20f))
    // Frontowa „wypukłość" aparatu
    drawRoundRect(Color.Black.copy(alpha = 0.08f), Offset(4f, h * 0.24f + 3), Size(w - 8, h * 0.52f), CornerRadius(4f))
    drawRoundRect(Brush.verticalGradient(listOf(Color.White, Color(0xFFE6EBEF)), h * 0.24f, h * 0.76f), Offset(4f, h * 0.24f), Size(w - 8, h * 0.52f), CornerRadius(4f))
    val closed = st == 1 && !tripped
    val leverColor = when {
        k.isMainSwitch -> Color(0xFFD32F2F)
        k.isRcd -> Color(0xFF1565C0)
        else -> Color(0xFF37474F)
    }
    if (k == Kind.CONTACTOR) {
        // Stycznik: okienko zwory – przesuwa się, gdy cewka jest pod napięciem
        drawRoundRect(Color(0xFF455A64), Offset(w / 2 - 22, h * 0.36f), Size(44f, h * 0.28f), CornerRadius(4f))
        val y = if (on) h * 0.44f else h * 0.38f
        drawRoundRect(if (on) Color(0xFF66BB6A) else Color(0xFFB0BEC5), Offset(w / 2 - 18, y), Size(36f, h * 0.12f), CornerRadius(3f))
        text(tm, if (on) "I" else "0", Offset(w / 2, h * 0.7f), 10f, Dark, bold = true)
    } else if (!k.isSpd) {
        val lw = if (k == Kind.MCB_1P) 18f else w * 0.5f
        val lx = w / 2 - lw / 2
        drawRoundRect(Color(0xFF78909C), Offset(lx - 3, h * 0.36f), Size(lw + 6, h * 0.3f), CornerRadius(4f))
        val ly = if (closed) h * 0.36f else h * 0.52f
        drawRoundRect(Color.Black.copy(alpha = 0.3f), Offset(lx + 1, ly + 3), Size(lw, h * 0.14f), CornerRadius(3f))
        drawRoundRect(Brush.verticalGradient(listOf(lighter(leverColor), leverColor), ly, ly + h * 0.14f), Offset(lx, ly), Size(lw, h * 0.14f), CornerRadius(3f))
        text(tm, if (closed) "I" else "0", Offset(w / 2, if (closed) h * 0.6f else h * 0.42f), 9f, Dark, bold = true)
        drawRect(if (closed) Color(0xFFE53935) else Color(0xFF43A047), Offset(w / 2 - 5, h * 0.26f), Size(10f, 5f))
    }
    val spec = p.effectiveSpec(inp.choices)
    val top = when {
        k.isMcb -> spec?.label ?: "?"
        k.isRcd -> spec?.rcdmA?.let { "${it}mA" } ?: "?"
        k.isMainSwitch -> "0-I"
        k == Kind.CONTACTOR -> "K1 • 230V~"
        else -> "SPD"
    }
    text(tm, top, Offset(w / 2, 31f), if (k == Kind.MCB_1P) 9f else 11f, if (spec == null && p.options.isNotEmpty()) Color(0xFFE65100) else Dark, bold = true, maxWidth = w)
    if (k.isRcd) {
        drawCircle(Color(0xFFFDD835), 6f, Offset(w - 14f, h * 0.72f))
        text(tm, "T", Offset(w - 14f, h * 0.72f), 8f, Dark, bold = true)
    }
    if (k.isSpd) {
        val poles = if (k == Kind.SPD_4P) 4 else 2
        for (i in 0 until poles) {
            val x = 20f + i * 40f
            drawRoundRect(if (on) Color(0xFF43A047) else Color(0xFF9E9E9E), Offset(x - 10, h * 0.42f), Size(20f, 14f), CornerRadius(3f))
        }
        text(tm, "⚡ T1+T2", Offset(w / 2, h * 0.68f), 10f, Dark, bold = true, maxWidth = w)
    }
    if (tripped) {
        val a = 0.4f + 0.4f * sin(inp.time * 10f)
        drawRoundRect(Color(0xFFFF1744).copy(alpha = a), Offset(-3f, -3f), Size(w + 6, h + 6), CornerRadius(8f), style = Stroke(4f))
    }
}
