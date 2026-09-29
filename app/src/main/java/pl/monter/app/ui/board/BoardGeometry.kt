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

/**
 * Trasa przewodu „po elektrykowemu": wyłącznie odcinki pionowe i poziome (łamanie pod kątem 90°).
 * Z zacisku przewód wychodzi pionowo (w dół z zacisków dolnych, w górę z górnych), biegnie
 * poziomym „korytkiem" i wchodzi pionowo do drugiego zacisku.
 *
 * @param lane numer toru – równoległe przewody dostają przesunięte korytka, żeby się nie nakładały
 * @param sagScale mnożnik długości wyjścia z zacisku (animacja sprężynowania)
 */
fun orthoRoute(a: Offset, aDown: Boolean, b: Offset, bDown: Boolean, lane: Int = 0, sagScale: Float = 1f): List<Offset> {
    val stub = (22f + lane * 7f) * sagScale
    val a1 = a.y + if (aDown) stub else -stub
    val b1 = b.y + if (bDown) stub else -stub
    // Ten sam pion – prosty odcinek
    if (kotlin.math.abs(a.x - b.x) < 0.5f) return listOf(a, b)
    val channel: Float? = when {
        aDown && bDown -> maxOf(a1, b1)
        !aDown && !bDown -> minOf(a1, b1)
        aDown && !bDown -> if (a1 <= b1) (a1 + b1) / 2f else null
        else -> if (a1 >= b1) (a1 + b1) / 2f else null
    }
    if (channel != null) return listOf(a, Offset(a.x, channel), Offset(b.x, channel), b)
    // Zaciski „odwrócone" (np. dolny zacisk nad górnym) – obejście pionowym korytkiem między nimi
    val midX = (a.x + b.x) / 2f + lane * 7f
    return listOf(a, Offset(a.x, a1), Offset(midX, a1), Offset(midX, b1), Offset(b.x, b1), b)
}

fun polyPath(points: List<Offset>): Path = Path().apply {
    moveTo(points[0].x, points[0].y)
    for (i in 1 until points.size) lineTo(points[i].x, points[i].y)
}

/** Tor przewodu: równoległe przewody z tego samego zacisku rozsuwamy na kolejne tory. */
private fun laneOf(w: Wire) = ((w.id % 5) + 5) % 5

fun Level.wirePoints(w: Wire, sagScale: Float = 1f): List<Offset> =
    orthoRoute(terminalPos(w.a), terminalDown(w.a), terminalPos(w.b), terminalDown(w.b), laneOf(w), sagScale)

fun Level.wirePath(w: Wire, sagScale: Float = 1f): Path = polyPath(wirePoints(w, sagScale))

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
        val pts = wirePoints(w)
        for (i in 1 until pts.size) {
            val d = distToSegment(p, pts[i - 1], pts[i])
            if (d < bestD) { bestD = d; best = w }
        }
    }
    return best
}

fun Level.hitPart(p: Offset): Part? = parts.lastOrNull { it.rect().contains(p) }
