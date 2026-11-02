package pl.bramki.app.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import pl.bramki.engine.Item
import pl.bramki.engine.Run
import pl.bramki.engine.Side
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin

/** Napis unoszący się nad graczem („+320", „÷2"). */
class Floater(val text: String, val color: Color, val bornMs: Long, val xBias: Float)

private val GateGood = Color(0xFF2ECC71)
private val GateBad = Color(0xFFE74C3C)
private val WallColor = Color(0xFF8D5A3B)
private val RoadColor = Color(0xFF2E3358)
private val GroundColor = Color(0xFF1A1D36)

/**
 * Pseudo-3D: punkt w odległości dz przed graczem rzutujemy skalą s = f / (f + dz).
 * s = 1 na płaszczyźnie gracza, s → 0 na horyzoncie. To dokładnie ten sam wzór,
 * którego używały gry wyścigowe z lat 80. (OutRun) – perspektywa bez żadnej macierzy 3D.
 */
class Camera(val width: Float, val height: Float) {
    val horizonY = height * 0.40f
    val playerY = height * 0.84f
    val focal = 6.5f
    val viewDistance = 48.0
    val cx = width / 2f

    fun scale(dz: Double): Float = (focal / (focal + dz.toFloat()))
    fun y(s: Float): Float = horizonY + (playerY - horizonY) * s
    fun roadHalf(s: Float): Float = width * 0.46f * s + width * 0.004f
}

fun DrawScope.drawWorld(
    run: Run,
    cam: Camera,
    textMeasurer: TextMeasurer,
    skyColor: Color,
    floaters: List<Floater>,
    nowMs: Long,
) {
    drawSky(cam, skyColor)
    drawRoad(run, cam)
    drawFinish(run, cam)
    // Od najdalszych do najbliższych, żeby bliskie zasłaniały dalekie.
    for (item in run.visibleItems(cam.viewDistance).asReversed()) {
        when (item) {
            is Item.GatePair -> drawGatePair(item, run, cam, textMeasurer)
            is Item.Wall -> drawWall(item, run, cam, textMeasurer)
        }
    }
    drawPlayer(run, cam, textMeasurer, nowMs)
    drawFloaters(floaters, run, cam, textMeasurer, nowMs)
}

private fun DrawScope.drawSky(cam: Camera, sky: Color) {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(sky.lighten(0.25f), sky, sky.darken(0.35f)),
            startY = 0f,
            endY = cam.horizonY,
        ),
        size = Size(cam.width, cam.horizonY),
    )
    // Gwiazdy / pył – kilka stałych punktów, żeby niebo nie było płaskie.
    for (i in 0 until 40) {
        val x = ((i * 977) % 1000) / 1000f * cam.width
        val y = ((i * 613) % 1000) / 1000f * cam.horizonY * 0.9f
        drawCircle(Color.White.copy(alpha = 0.25f + (i % 3) * 0.15f), radius = 1.2f + (i % 2), center = Offset(x, y))
    }
}

private fun DrawScope.drawRoad(run: Run, cam: Camera) {
    drawRect(GroundColor, topLeft = Offset(0f, cam.horizonY), size = Size(cam.width, cam.height - cam.horizonY))
    val sBottom = (cam.height - cam.horizonY) / (cam.playerY - cam.horizonY)
    val halfBottom = cam.roadHalf(sBottom)
    val road = Path().apply {
        moveTo(cam.cx - halfBottom, cam.height)
        lineTo(cam.cx + halfBottom, cam.height)
        lineTo(cam.cx + cam.roadHalf(0f), cam.horizonY)
        lineTo(cam.cx - cam.roadHalf(0f), cam.horizonY)
        close()
    }
    drawPath(road, RoadColor)
    // Poprzeczne pasy – dają wrażenie prędkości.
    val step = 4.0
    var zLine = kotlin.math.floor(run.z / step) * step
    while (zLine < run.z + cam.viewDistance) {
        val dz = zLine - run.z
        if (dz > -1.5) {
            val s = cam.scale(dz)
            val y = cam.y(s)
            if (y <= cam.height) {
                val half = cam.roadHalf(s)
                drawLine(Color.White.copy(alpha = 0.10f * s + 0.02f), Offset(cam.cx - half, y), Offset(cam.cx + half, y), strokeWidth = max(1f, 3f * s))
            }
        }
        zLine += step
    }
    // Linia środkowa: granica między „lewo" i „prawo".
    drawLine(Color.White.copy(alpha = 0.35f), Offset(cam.cx, cam.height), Offset(cam.cx, cam.horizonY), strokeWidth = 2f)
}

private fun DrawScope.drawFinish(run: Run, cam: Camera) {
    val dz = run.track.length - run.z
    if (dz < -1 || dz > cam.viewDistance) return
    val s = cam.scale(dz)
    val y = cam.y(s)
    val half = cam.roadHalf(s)
    val bandH = max(3f, half * 0.12f)
    val cells = 10
    val cellW = half * 2 / cells
    for (i in 0 until cells) {
        val c = if (i % 2 == 0) Color.White else Color(0xFF222222)
        drawRect(c, topLeft = Offset(cam.cx - half + i * cellW, y - bandH), size = Size(cellW, bandH))
    }
}

private fun DrawScope.drawGatePair(gate: Item.GatePair, run: Run, cam: Camera, tm: TextMeasurer) {
    val dz = gate.z - run.z
    val s = cam.scale(dz)
    val y = cam.y(s)
    val half = cam.roadHalf(s)
    val h = half * 0.62f
    drawGate(Offset(cam.cx - half, y - h), Size(half, h), gate.left, run, cam, tm, s, Side.LEFT)
    drawGate(Offset(cam.cx, y - h), Size(half, h), gate.right, run, cam, tm, s, Side.RIGHT)
    // Słupki bramek.
    val post = max(1.5f, half * 0.03f)
    for (x in listOf(cam.cx - half, cam.cx, cam.cx + half)) {
        drawRect(Color(0xFFE8E8F0).copy(alpha = 0.9f), topLeft = Offset(x - post / 2, y - h - post * 2), size = Size(post, h + post * 2))
    }
}

private fun DrawScope.drawGate(
    topLeft: Offset, size: Size, op: pl.bramki.engine.Op, run: Run, cam: Camera, tm: TextMeasurer, s: Float, side: Side,
) {
    val base = if (op.positive) GateGood else GateBad
    // Bramka po stronie gracza jest jaśniejsza – od razu widać, w co wjedziesz.
    val highlighted = run.side == side
    val alpha = (0.55f + 0.35f * s).coerceAtMost(0.9f) * (if (highlighted) 1f else 0.8f)
    drawRect(base.copy(alpha = alpha), topLeft, size)
    drawRect(Color.White.copy(alpha = if (highlighted) 0.9f else 0.35f), topLeft, size, style = Stroke(width = max(1f, 2.5f * s)))
    if (s > 0.16f) {
        val fontSize = (46f * s).coerceIn(9f, 46f).sp
        drawCentered(tm, op.label, Offset(topLeft.x + size.width / 2, topLeft.y + size.height / 2), fontSize, Color.White, bold = true)
    }
}

private fun DrawScope.drawWall(wall: Item.Wall, run: Run, cam: Camera, tm: TextMeasurer) {
    val dz = wall.z - run.z
    val s = cam.scale(dz)
    val y = cam.y(s)
    val half = cam.roadHalf(s)
    val h = half * 0.40f
    val left = if (wall.side == Side.LEFT) cam.cx - half else cam.cx
    val topLeft = Offset(left, y - h)
    val size = Size(half, h)
    drawRect(WallColor.copy(alpha = 0.95f), topLeft, size)
    // „Cegły"
    val rows = 3
    for (r in 1 until rows) {
        val ry = topLeft.y + size.height * r / rows
        drawLine(Color.Black.copy(alpha = 0.35f), Offset(topLeft.x, ry), Offset(topLeft.x + size.width, ry), strokeWidth = max(1f, 2f * s))
    }
    drawRect(Color.Black.copy(alpha = 0.5f), topLeft, size, style = Stroke(width = max(1f, 2f * s)))
    if (s > 0.2f) {
        val fontSize = (30f * s).coerceIn(8f, 30f).sp
        drawCentered(tm, "🧱 " + wall.op.label, Offset(topLeft.x + size.width / 2, topLeft.y + size.height / 2), fontSize, Color.White, bold = true)
    }
}

private fun DrawScope.drawPlayer(run: Run, cam: Camera, tm: TextMeasurer, nowMs: Long) {
    val half = cam.roadHalf(1f)
    val px = cam.cx + run.x.toFloat() * half * 0.82f
    val bob = (sin(nowMs / 140.0 * 2 * PI) * 3).toFloat()
    val size = cam.width * 0.14f
    // Cień
    drawOval(Color.Black.copy(alpha = 0.35f), topLeft = Offset(px - size * 0.4f, cam.playerY - size * 0.12f), size = Size(size * 0.8f, size * 0.24f))
    drawCentered(tm, run.tier.emoji, Offset(px, cam.playerY - size * 0.55f + bob), pxToSp(size * 0.62f), Color.White)
}

private fun DrawScope.drawFloaters(floaters: List<Floater>, run: Run, cam: Camera, tm: TextMeasurer, nowMs: Long) {
    val half = cam.roadHalf(1f)
    for (f in floaters) {
        val age = (nowMs - f.bornMs) / 1000f
        if (age < 0f || age > 1.2f) continue
        val alpha = (1f - age / 1.2f).coerceIn(0f, 1f)
        val px = cam.cx + f.xBias * half * 0.6f
        val py = cam.playerY - cam.width * 0.22f - age * cam.height * 0.12f
        drawCentered(tm, f.text, Offset(px, py), 34.sp, f.color.copy(alpha = alpha), bold = true)
    }
}

private fun DrawScope.drawCentered(
    tm: TextMeasurer, text: String, center: Offset, fontSize: TextUnit, color: Color, bold: Boolean = false,
) {
    val layout = tm.measure(
        text = AnnotatedString(text),
        style = TextStyle(fontSize = fontSize, color = color, fontWeight = if (bold) FontWeight.Black else FontWeight.Normal),
    )
    drawText(layout, topLeft = Offset(center.x - layout.size.width / 2f, center.y - layout.size.height / 2f))
}

/** Piksele → sp (DrawScope jest też Density, więc zna gęstość ekranu i skalę czcionki). */
private fun DrawScope.pxToSp(px: Float): TextUnit = (px / density / fontScale).sp

private fun Color.lighten(f: Float) = Color(
    red + (1 - red) * f, green + (1 - green) * f, blue + (1 - blue) * f, alpha,
)

private fun Color.darken(f: Float) = Color(red * (1 - f), green * (1 - f), blue * (1 - f), alpha)
