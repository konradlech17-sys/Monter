package pl.monter.app.ui.board

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.sp
import pl.monter.core.level.Level
import pl.monter.core.model.Kind
import pl.monter.core.model.Part
import pl.monter.core.model.Potential
import pl.monter.core.model.Role
import pl.monter.core.model.TermRef
import pl.monter.core.model.Wire
import pl.monter.core.model.WireColor
import pl.monter.core.sim.LoadState
import pl.monter.core.sim.SimResult
import kotlin.math.PI
import kotlin.math.cos
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
)

private val Brass = Color(0xFFD4AF37)
private val ModuleWhite = Color(0xFFF4F6F8)
private val ModuleEdge = Color(0xFFB0BEC5)
private val Dark = Color(0xFF263238)

/** Rysuje tekst tak, by jego rozmiar był wyrażony w jednostkach planszy (niezależnie od gęstości ekranu). */
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
    // Tło: płyta montażowa z siatką
    drawRoundRect(
        Brush.linearGradient(listOf(inp.theme.background, inp.theme.background2), Offset.Zero, Offset(level.width, level.height)),
        Offset.Zero, Size(level.width, level.height), CornerRadius(18f),
    )
    var gx = 0f
    while (gx <= level.width) { drawLine(inp.theme.grid, Offset(gx, 0f), Offset(gx, level.height), 1f); gx += 40f }
    var gy = 0f
    while (gy <= level.height) { drawLine(inp.theme.grid, Offset(0f, gy), Offset(level.width, gy), 1f); gy += 40f }

    drawRails(level)

    val tripped = inp.sim?.tripped ?: emptySet()
    for (p in level.parts) {
        translate(p.x, p.y) { drawPart(p, inp, tm, p.id in tripped) }
        if (p.id in inp.errorParts) {
            val a = 0.5f + 0.5f * sin(inp.time * 6f)
            drawRoundRect(Color(0xFFEF5350).copy(alpha = a), Offset(p.x - 6, p.y - 6), Size(p.kind.w + 12, p.kind.h + 12), CornerRadius(12f), style = Stroke(4f))
        }
    }

    for (w in inp.wires) drawWire(w, inp)
    for (p in level.parts) drawTerminals(p, inp, tm)
    drawSparks(inp)
}

private fun DrawScope.drawRails(level: Level) {
    val din = level.parts.filter { it.kind.isSwitchgear || it.kind == Kind.SPD_4P || it.kind == Kind.TRANSFORMER && level.parts.any { p -> p.kind.isSwitchgear } }
    din.groupBy { it.y }.forEach { (y, row) ->
        val x0 = row.minOf { it.x } - 20; val x1 = row.maxOf { it.x + it.kind.w } + 20
        val ry = y + 50
        drawRect(Color(0xFF9EA7AD), Offset(x0, ry), Size(x1 - x0, 20f))
        drawRect(Color(0xFFCFD8DC), Offset(x0, ry + 3), Size(x1 - x0, 5f))
    }
}

// ============================================================================ przewody

private fun DrawScope.drawWire(w: Wire, inp: RenderInput) {
    val level = inp.level
    val path = level.wirePath(w)
    val width = when (w.cs.mm2) {
        in 0.0..0.6 -> 3f; in 0.6..1.6 -> 5f; in 1.6..2.6 -> 6f; in 2.6..4.1 -> 7f; in 4.1..6.1 -> 8f; else -> 9.5f
    }
    if (w.id in inp.errorWires || w.id in inp.hotWires) {
        val a = 0.4f + 0.4f * sin(inp.time * 6f)
        val c = if (w.id in inp.hotWires) Color(0xFFFF7043) else Color(0xFFEF5350)
        drawPath(path, c.copy(alpha = a), style = Stroke(width + 12f, cap = StrokeCap.Round))
    }
    if (w.id == inp.selectedWire) drawPath(path, Color(0xFFFFC107).copy(alpha = 0.7f), style = Stroke(width + 10f, cap = StrokeCap.Round))
    if (inp.theme.neon) drawPath(path, w.color.ui().copy(alpha = 0.35f), style = Stroke(width + 8f, cap = StrokeCap.Round))
    // Obrys i izolacja
    drawPath(path, Color.Black.copy(alpha = 0.35f), style = Stroke(width + 2f, cap = StrokeCap.Round))
    drawPath(path, w.color.ui(), style = Stroke(width, cap = StrokeCap.Round))
    if (w.color == WireColor.GREEN_YELLOW) {
        drawPath(path, Color(0xFFFDD835), style = Stroke(width, cap = StrokeCap.Butt, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))))
    }
    // Połysk
    drawPath(path, Color.White.copy(alpha = 0.18f), style = Stroke(width * 0.3f, cap = StrokeCap.Round))
    // Przepływ prądu w trybie TEST
    val sim = inp.sim ?: return
    if (sim.wireLive(w)) {
        val lv = sim.potentials(w.a).all { it.isLowVoltage }
        drawPath(
            path, (if (lv) Color(0xFF80DEEA) else Color(0xFFFFF59D)).copy(alpha = 0.9f),
            style = Stroke(width * 0.45f, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 22f), -inp.time * 60f)),
        )
    }
    if (w.fixed) {
        val (a, _, _, b) = level.wireControls(w)
        drawCircle(Color.Black.copy(alpha = 0.3f), 3f, a); drawCircle(Color.Black.copy(alpha = 0.3f), 3f, b)
    }
}

// ============================================================================ zaciski

private fun DrawScope.drawTerminals(p: Part, inp: RenderInput, tm: TextMeasurer) {
    for (t in p.kind.terminals) {
        val ref = TermRef(p.id, t.id)
        val c = Offset(p.x + t.x, p.y + t.y)
        if (inp.selected == ref) {
            val r = 13f + 3f * sin(inp.time * 8f)
            drawCircle(Color(0xFFFFC107).copy(alpha = 0.45f), r + 4f, c)
            drawCircle(Color(0xFFFFC107), r, c, style = Stroke(3f))
        }
        if (inp.roleHints && t.role != Role.ANY) drawCircle(t.role.ui(), 10f, c)
        drawCircle(Color(0xFF5D4037), 7.5f, c)
        drawCircle(Brass, 6.5f, c)
        drawLine(Color(0xFF6D4C41), c + Offset(-4f, 0f), c + Offset(4f, 0f), 1.8f)
        if (t.label.isNotEmpty()) {
            val above = t.y > p.kind.h / 2
            text(tm, t.label, c + Offset(0f, if (above) -15f else 15f), 10f, Dark, bold = true)
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
            val at = c + Offset(0f, if (t.y > p.kind.h / 2) 24f else -24f)
            drawRoundRect(bg, at - Offset(20f, 8f), Size(40f, 16f), CornerRadius(8f))
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
        drawCircle(Color(0xFFFFF59D).copy(alpha = (1f - k) * 0.8f), 30f * (1f - k) + 6f, s.at)
        for (i in 0 until 14) {
            val ang = (i * 2 * PI / 14 + s.start).toFloat()
            val r0 = 10f + 60f * k; val r1 = r0 + 18f * (1f - k)
            drawLine(
                Color(0xFFFFC107).copy(alpha = 1f - k),
                s.at + Offset(cos(ang) * r0, sin(ang) * r0), s.at + Offset(cos(ang) * r1, sin(ang) * r1), 3f, StrokeCap.Round,
            )
        }
    }
}

// ============================================================================ elementy

private fun DrawScope.body(x: Float, y: Float, w: Float, h: Float, color: Color = ModuleWhite, r: Float = 8f, edge: Color = ModuleEdge) {
    drawRoundRect(Color.Black.copy(alpha = 0.18f), Offset(x + 3, y + 4), Size(w, h), CornerRadius(r))
    drawRoundRect(color, Offset(x, y), Size(w, h), CornerRadius(r))
    drawRoundRect(edge, Offset(x, y), Size(w, h), CornerRadius(r), style = Stroke(1.5f))
}

private fun DrawScope.terminalStrip(x: Float, y: Float, w: Float, h: Float) {
    drawRoundRect(Color(0xFFCFD8DC), Offset(x, y), Size(w, h), CornerRadius(5f))
}

private fun DrawScope.drawPart(p: Part, inp: RenderInput, tm: TextMeasurer, tripped: Boolean) {
    val k = p.kind
    val w = k.w; val h = k.h
    val st = inp.controls[p.id] ?: k.defaultState
    val load = inp.sim?.loads?.get(p.id)
    val on = load?.state == LoadState.ON
    val damaged = load?.state == LoadState.DAMAGED
    val t = inp.time
    val labelColor = inp.theme.text

    when (k) {
        Kind.SUPPLY_1P, Kind.SUPPLY_3P -> {
            drawRect(Color(0xFF546E7A), Offset(w / 2 - 12, -30f), Size(24f, 34f))
            body(0f, 0f, w, h - 4, Color(0xFF37474F), 10f, Color(0xFF263238))
            text(tm, "⚡ " + p.label, Offset(w / 2, 18f), 11f, Color.White, bold = true, maxWidth = w - 8)
            for (term in k.terminals) {
                drawLine(term.role.suggestedColor().ui(), Offset(term.x, 34f), Offset(term.x, term.y), 6f, StrokeCap.Round)
            }
        }
        Kind.LAMP -> {
            drawRoundRect(Color(0xFF90A4AE), Offset(w / 2 - 30, 0f), Size(60f, 10f), CornerRadius(4f))
            drawLine(Color(0xFF455A64), Offset(w / 2, 10f), Offset(w / 2, 30f), 3f)
            val bulbC = Offset(w / 2, 52f)
            val needsChoice = p.options.isNotEmpty() && inp.choices[p.id] == null
            if (on && !needsChoice) {
                val pulse = 0.75f + 0.25f * sin(t * 3f)
                drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF176).copy(alpha = 0.8f * pulse), Color.Transparent), bulbC, 90f), 90f, bulbC)
                drawCircle(Color(0xFFFFEB3B), 20f, bulbC)
                drawCircle(Color.White.copy(alpha = 0.8f), 8f, bulbC)
            } else {
                drawCircle(if (damaged) Color(0xFF424242) else Color(0xFFECEFF1), 20f, bulbC)
                drawCircle(Color(0xFF90A4AE), 20f, bulbC, style = Stroke(1.5f))
                if (!needsChoice) drawLine(Color(0xFF8D6E63), bulbC + Offset(-6f, 4f), bulbC + Offset(6f, 4f), 1.5f)
            }
            if (needsChoice) text(tm, "?", bulbC, 22f, Color(0xFFE65100), bold = true)
            drawRect(Color(0xFF9E9E9E), Offset(w / 2 - 9, 70f), Size(18f, 10f))
            terminalStrip(4f, 84f, w - 8, 24f)
            if (damaged) smoke(Offset(w / 2, 30f), t)
        }
        Kind.SOCKET -> {
            body(4f, 0f, w - 8, 80f, Color.White, 10f)
            val c = Offset(w / 2, 40f)
            drawCircle(if (on) Color(0xFFE8F5E9) else Color(0xFFECEFF1), 30f, c)
            drawCircle(Color(0xFFB0BEC5), 30f, c, style = Stroke(1.5f))
            drawCircle(Dark, 5f, c + Offset(-12f, 2f)); drawCircle(Dark, 5f, c + Offset(12f, 2f))
            drawRoundRect(Color(0xFF9E9E9E), c + Offset(-3f, -24f), Size(6f, 12f), CornerRadius(2f))
            if (on) drawCircle(Color(0xFF66BB6A), 4f, Offset(w - 16, 10f))
            terminalStrip(4f, 84f, w - 8, 24f)
            if (damaged) smoke(c, t)
        }
        Kind.SOCKET_400 -> {
            body(4f, 0f, w - 8, 92f, Color(0xFFF5F5F5), 10f)
            val c = Offset(w / 2, 46f)
            drawCircle(Color(0xFFD32F2F), 40f, c)
            drawCircle(Color(0xFFB71C1C), 30f, c)
            for (i in 0 until 5) {
                val a = (-PI / 2 + i * 2 * PI / 5).toFloat()
                drawCircle(Color.Black, 5f, c + Offset(cos(a) * 18f, sin(a) * 18f))
            }
            if (on) drawCircle(Color(0xFF66BB6A), 4f, Offset(w - 16, 10f))
            terminalStrip(4f, 96f, w - 8, 22f)
        }
        Kind.SWITCH_1, Kind.SWITCH_STAIR, Kind.SWITCH_CROSS -> {
            body(4f, 0f, w - 8, 74f, Color.White, 10f)
            rocker(Offset(w / 2 - 20, 10f), 40f, 54f, if (k == Kind.SWITCH_1) st == 1 else st == 0)
            val sym = when (k) { Kind.SWITCH_1 -> if (st == 1) "I" else "O"; Kind.SWITCH_STAIR -> "↕${st + 1}"; else -> if (st == 0) "‖" else "✕" }
            text(tm, sym, Offset(w / 2, 37f), 13f, Dark, bold = true)
            terminalStrip(4f, 78f, w - 8, 20f)
        }
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
            drawCircle(if (pressed) Color(0xFFFFA000) else Color(0xFFFFCA28), if (pressed) 15f else 18f, Offset(w / 2, 32f))
            text(tm, "🔔", Offset(w / 2, 32f), 14f, Dark)
            terminalStrip(4f, 68f, w - 8, 20f)
        }
        Kind.BELL -> {
            val c = Offset(w / 2, 46f)
            val shake = if (on) 10f * sin(t * 40f) else 0f
            rotate(shake, c) {
                drawArc(Brush.linearGradient(listOf(Color(0xFFFFE082), Color(0xFFFFA000)), c - Offset(38f, 38f), c + Offset(38f, 38f)), 180f, 180f, true, c - Offset(38f, 38f), Size(76f, 76f))
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
            drawCircle(Color(0xFFE0E0E0), 26f, c)
            for (i in 0 until 4) drawCircle(Color(0xFFBDBDBD), 26f - i * 6f, c, style = Stroke(1f))
            if (st == 1) text(tm, "🚶", c, 22f, Dark)
            drawCircle(if (on) Color(0xFF66BB6A) else Color(0xFF9E9E9E), 4f, Offset(16f, 10f))
            terminalStrip(4f, 78f, w - 8, 20f)
        }
        Kind.WAGO3, Kind.WAGO5 -> {
            drawRoundRect(Color(0xCCB0BEC5), Offset(0f, 8f), Size(w, 28f), CornerRadius(6f))
            for (term in k.terminals) drawRoundRect(Color(0xFFFF9800), Offset(term.x - 7f, 0f), Size(14f, 12f), CornerRadius(3f))
        }
        Kind.MAIN_SWITCH_2P, Kind.MAIN_SWITCH_4P, Kind.RCD_2P, Kind.RCD_4P, Kind.MCB_1P, Kind.MCB_3P, Kind.SPD_4P -> drawModule(p, inp, tm, st, tripped, on)
        Kind.N_BAR, Kind.PE_BAR -> {
            if (k == Kind.N_BAR) drawRoundRect(Color(0xFF1E88E5), Offset(0f, 4f), Size(w, h - 8), CornerRadius(6f))
            else {
                drawRoundRect(Color(0xFF43A047), Offset(0f, 4f), Size(w, h - 8), CornerRadius(6f))
                var x = 0f
                while (x < w) { drawRect(Color(0xFFFDD835), Offset(x, 4f), Size(10f, h - 8)); x += 20f }
            }
            drawRect(Color(0xFFBCAAA4), Offset(6f, h / 2 - 5), Size(w - 12, 10f))
            text(tm, if (k == Kind.N_BAR) "N" else "PE", Offset(w + 14f, h / 2), 13f, labelColor, bold = true)
        }
        Kind.CIRCUIT_1P, Kind.CIRCUIT_3P -> {
            body(0f, 0f, w, h, if (on) Color(0xFFE8F5E9) else Color(0xFFECEFF1), 10f, if (on) Color(0xFF43A047) else ModuleEdge)
            text(tm, "🏠 " + p.label, Offset(w / 2, 40f), 12f, Dark, bold = true, maxWidth = w - 8)
            p.circuit?.let { text(tm, "maks. ${it.maxA} A" + if (it.needsRcd) " • RCD ${it.rcdMaxmA} mA" else "", Offset(w / 2, 57f), 9f, Color(0xFF546E7A), maxWidth = w - 8) }
            if (on) text(tm, "✔", Offset(w - 14f, 14f), 14f, Color(0xFF2E7D32), bold = true)
        }
    }

    // Etykieta nad elementem (dla elementów bez tekstu w środku)
    val inside = k.isSupply || k == Kind.CIRCUIT_1P || k == Kind.CIRCUIT_3P
    if (!inside) {
        val chosen = p.options.firstOrNull { it.id == inp.choices[p.id] }?.label
        val lbl = if (chosen != null) chosen else p.label
        text(tm, lbl, Offset(w / 2, -12f), 11f, labelColor, bold = true, maxWidth = maxOf(w + 60f, 120f))
    }
}

private fun DrawScope.rocker(topLeft: Offset, w: Float, h: Float, on: Boolean) {
    drawRoundRect(Color(0xFFF5F5F5), topLeft, Size(w, h), CornerRadius(6f))
    drawRoundRect(Color(0xFFBDBDBD), topLeft, Size(w, h), CornerRadius(6f), style = Stroke(1.5f))
    // Wciśnięta połowa klawisza jest zacieniona
    val shadeTop = if (on) topLeft else topLeft + Offset(0f, h / 2)
    drawRoundRect(Color(0x22000000), shadeTop, Size(w, h / 2), CornerRadius(6f))
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
    val closed = st == 1 && !tripped
    val leverColor = when {
        k.isMainSwitch -> Color(0xFFD32F2F)
        k.isRcd -> Color(0xFF1565C0)
        else -> Color(0xFF37474F)
    }
    if (k != Kind.SPD_4P) {
        val lw = if (k == Kind.MCB_1P) 18f else w * 0.5f
        val lx = w / 2 - lw / 2
        drawRoundRect(Color(0xFF90A4AE), Offset(lx - 3, h * 0.36f), Size(lw + 6, h * 0.3f), CornerRadius(4f))
        val ly = if (closed) h * 0.36f else h * 0.52f
        drawRoundRect(leverColor, Offset(lx, ly), Size(lw, h * 0.14f), CornerRadius(3f))
        text(tm, if (closed) "I" else "0", Offset(w / 2, if (closed) h * 0.6f else h * 0.42f), 9f, Dark, bold = true)
        // Okienko stanu: czerwone = załączony, zielone = wyłączony
        drawRect(if (closed) Color(0xFFE53935) else Color(0xFF43A047), Offset(w / 2 - 5, h * 0.26f), Size(10f, 5f))
    }
    val spec = p.effectiveSpec(inp.choices)
    val top = when {
        k.isMcb -> spec?.label ?: "?"
        k.isRcd -> spec?.rcdmA?.let { "${it}mA" } ?: "?"
        k.isMainSwitch -> "0-I"
        else -> "SPD"
    }
    text(tm, top, Offset(w / 2, 31f), if (k == Kind.MCB_1P) 9f else 11f, if (spec == null && p.options.isNotEmpty()) Color(0xFFE65100) else Dark, bold = true, maxWidth = w)
    if (k.isRcd) {
        drawCircle(Color(0xFFFDD835), 6f, Offset(w - 14f, h * 0.72f))
        text(tm, "T", Offset(w - 14f, h * 0.72f), 8f, Dark, bold = true)
    }
    if (k == Kind.SPD_4P) {
        val poles = 4
        for (i in 0 until poles) {
            val x = 20f + i * 40f
            drawRoundRect(if (on) Color(0xFF43A047) else Color(0xFF9E9E9E), Offset(x - 10, h * 0.42f), Size(20f, 14f), CornerRadius(3f))
        }
        text(tm, "⚡ T1+T2", Offset(w / 2, h * 0.68f), 10f, Dark, bold = true)
    }
    if (tripped) {
        val a = 0.4f + 0.4f * sin(inp.time * 10f)
        drawRoundRect(Color(0xFFFF1744).copy(alpha = a), Offset(-3f, -3f), Size(w + 6, h + 6), CornerRadius(8f), style = Stroke(4f))
    }
}
