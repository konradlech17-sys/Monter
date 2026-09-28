package pl.monter.app.ui.board

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import pl.monter.core.level.Level
import pl.monter.core.model.Part
import pl.monter.core.model.Role
import pl.monter.core.model.TermRef
import pl.monter.core.model.Wire
import pl.monter.core.model.WireColor
import kotlin.math.hypot

/** Motyw tła planszy – kupowany w sklepie. */
data class BoardTheme(val background: Color, val background2: Color, val grid: Color, val text: Color, val neon: Boolean = false) {
    companion object {
        fun of(id: String) = when (id) {
            "theme_wood" -> BoardTheme(Color(0xFF8D6E63), Color(0xFF6D4C41), Color(0x22000000), Color(0xFFFFF8E1))
            "theme_blueprint" -> BoardTheme(Color(0xFF0D47A1), Color(0xFF1565C0), Color(0x33FFFFFF), Color.White)
            "theme_neon" -> BoardTheme(Color(0xFF0A0A12), Color(0xFF151528), Color(0x2229B6F6), Color(0xFFE0F7FA), neon = true)
            else -> BoardTheme(Color(0xFFE8ECF1), Color(0xFFD5DCE5), Color(0x14000000), Color(0xFF263238))
        }
    }
}

fun WireColor.ui(): Color = when (this) {
    WireColor.BROWN -> Color(0xFF8D5524)
    WireColor.BLACK -> Color(0xFF212121)
    WireColor.GREY -> Color(0xFF9E9E9E)
    WireColor.BLUE -> Color(0xFF1E88E5)
    WireColor.GREEN_YELLOW -> Color(0xFF43A047)
    WireColor.WHITE -> Color(0xFFF5F5F5)
    WireColor.RED -> Color(0xFFE53935)
}

fun Role.ui(): Color = suggestedColor().let { if (it == WireColor.GREEN_YELLOW) Color(0xFFC0CA33) else it.ui() }

/** Położenie zacisku w układzie współrzędnych planszy. */
fun Level.terminalPos(t: TermRef): Offset {
    val p = part(t.part)
    val d = p.kind.terminal(t.terminal)
    return Offset(p.x + d.x, p.y + d.y)
}

/** Czy przewód wychodzi z zacisku w dół (zacisk w dolnej części elementu). */
fun Level.terminalDown(t: TermRef): Boolean {
    val p = part(t.part)
    return p.kind.terminal(t.terminal).y > p.kind.h / 2
}

fun Part.rect() = Rect(x, y, x + kind.w, y + kind.h)

/** Punkty kontrolne krzywej Béziera – przewód „zwisa" jak prawdziwy. [sagScale] pozwala animować sprężynowanie. */
fun curveControls(a: Offset, aDown: Boolean, b: Offset, bDown: Boolean, sagScale: Float = 1f): Array<Offset> {
    val d = hypot(b.x - a.x, b.y - a.y)
    val sag = (26f + d * 0.22f) * sagScale
    val c1 = a + Offset(0f, if (aDown) sag else -sag)
    val c2 = b + Offset(0f, if (bDown) sag else -sag)
    return arrayOf(a, c1, c2, b)
}

fun Level.wireControls(w: Wire, sagScale: Float = 1f): Array<Offset> =
    curveControls(terminalPos(w.a), terminalDown(w.a), terminalPos(w.b), terminalDown(w.b), sagScale)

fun curvePath(c: Array<Offset>): Path = Path().apply {
    moveTo(c[0].x, c[0].y); cubicTo(c[1].x, c[1].y, c[2].x, c[2].y, c[3].x, c[3].y)
}

fun Level.wirePath(w: Wire, sagScale: Float = 1f): Path = curvePath(wireControls(w, sagScale))

private fun bezier(p: Array<Offset>, t: Float): Offset {
    val u = 1 - t
    return p[0] * (u * u * u) + p[1] * (3 * u * u * t) + p[2] * (3 * u * t * t) + p[3] * (t * t * t)
}

private fun distToSegment(p: Offset, a: Offset, b: Offset): Float {
    val ab = b - a
    val len2 = ab.x * ab.x + ab.y * ab.y
    val t = if (len2 == 0f) 0f else (((p - a).x * ab.x + (p - a).y * ab.y) / len2).coerceIn(0f, 1f)
    val q = a + ab * t
    return hypot(p.x - q.x, p.y - q.y)
}

/** Najbliższy zacisk w promieniu [radius]. */
fun Level.hitTerminal(p: Offset, radius: Float): TermRef? =
    parts.flatMap { part -> part.kind.terminals.map { TermRef(part.id, it.id) } }
        .map { it to (terminalPos(it) - p).getDistance() }
        .filter { it.second <= radius }
        .minByOrNull { it.second }?.first

fun Level.hitWire(p: Offset, wires: List<Wire>, radius: Float): Wire? {
    var best: Wire? = null
    var bestD = radius
    for (w in wires) {
        val c = wireControls(w)
        var prev = c[0]
        for (i in 1..24) {
            val q = bezier(c, i / 24f)
            val d = distToSegment(p, prev, q)
            if (d < bestD) { bestD = d; best = w }
            prev = q
        }
    }
    return best
}

fun Level.hitPart(p: Offset): Part? = parts.lastOrNull { it.rect().contains(p) }
