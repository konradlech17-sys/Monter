package pl.bramki.app.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import pl.bramki.engine.Item
import pl.bramki.engine.Run
import pl.bramki.engine.Side
import kotlin.math.PI
import kotlin.math.abs
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
    art: ModeArt,
    fx: Fx,
) {
    drawBackground(run, cam, art, skyColor)
    drawRoad(run, cam)
    drawFinish(run, cam)
    // Od najdalszych do najbliższych, żeby bliskie zasłaniały dalekie.
    for (item in run.visibleItems(cam.viewDistance).asReversed()) {
        when (item) {
            is Item.GatePair -> drawGatePair(item, run, cam, textMeasurer)
            is Item.Wall -> drawWall(item, run, cam, textMeasurer)
        }
    }
    drawPlayer(run, cam, art, fx, nowMs)
    drawFloaters(floaters, run, cam, textMeasurer, nowMs)
}

/** Tło trybu (obraz z Gemini) z lekką paralaksą przy ruchu w bok, przyciemnione kolorem bieżącego szczebla. */
private fun DrawScope.drawBackground(run: Run, cam: Camera, art: ModeArt, tint: Color) {
    val img = art.background
    val scale = max(cam.width * 1.12f / img.width, cam.horizonY / img.height)
    val w = (img.width * scale).toInt()
    val h = (img.height * scale).toInt()
    val parallax = -run.x.toFloat() * cam.width * 0.05f
    val x = ((cam.width - w) / 2f + parallax).toInt()
    drawImage(img, dstOffset = IntOffset(x, (cam.horizonY - h).toInt()), dstSize = IntSize(w, h))
    drawRect(tint.copy(alpha = 0.28f), size = Size(cam.width, cam.horizonY))
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

private fun DrawScope.drawPlayer(run: Run, cam: Camera, art: ModeArt, fx: Fx, nowMs: Long) {
    val half = cam.roadHalf(1f)
    val px = cam.cx + run.x.toFloat() * half * 0.82f
    val size = cam.width * 0.26f

    // Bieg: podskok i kołysanie w rytmie prędkości.
    val phase = nowMs / 1000.0 * run.speed * 0.9
    val bob = -(abs(sin(phase * PI)) * size * 0.05).toFloat()
    val wobble = (sin(phase * 2 * PI) * 2.5).toFloat()
    // Pochylenie w stronę ruchu (jak motocyklista w zakręcie).
    val lean = (run.lateralVelocity * 7.0).coerceIn(-16.0, 16.0).toFloat()

    // Squash & stretch: dobra bramka rozciąga w górę, zła spłaszcza.
    var sx = 1f
    var sy = 1f
    val tg = (nowMs - fx.gateMs) / Fx.GATE_DURATION.toFloat()
    if (tg in 0f..1f) {
        val k = sin(tg * PI).toFloat()
        if (fx.gatePositive) { sy += 0.22f * k; sx -= 0.14f * k } else { sy -= 0.22f * k; sx += 0.18f * k }
    }
    // Ściana: wstrząs na boki.
    var shake = 0f
    val tw = (nowMs - fx.wallMs) / Fx.WALL_DURATION.toFloat()
    if (tw in 0f..1f) shake = (sin(tw * 28.0) * (1 - tw) * size * 0.08).toFloat()

    // Przemiana: nowa postać „wyskakuje", stara znika, wokół rozchodzą się iskry.
    val tt = (nowMs - fx.tierMs) / Fx.TIER_DURATION.toFloat()
    val morphing = tt in 0f..1f
    var pop = 1f
    if (morphing) pop += (sin(tt * PI) * 0.35 * (1 - tt * 0.5)).toFloat()

    // Cień – spłaszcza się razem z postacią.
    val shadowW = size * 0.55f * sx * pop
    drawOval(Color.Black.copy(alpha = 0.35f), topLeft = Offset(px + shake - shadowW / 2, cam.playerY - size * 0.05f), size = Size(shadowW, size * 0.11f))

    fun drawSprite(img: ImageBitmap, scale: Float, alpha: Float) {
        val w = size * scale
        val h = w * img.height / img.width
        withTransform({
            translate(px + shake, cam.playerY + bob)
            rotate(lean + wobble, pivot = Offset.Zero)
            scale(sx, sy, pivot = Offset.Zero)
        }) {
            drawImage(
                img,
                dstOffset = IntOffset((-w / 2).toInt(), (-h).toInt()),
                dstSize = IntSize(w.toInt(), h.toInt()),
                alpha = alpha,
            )
        }
    }

    if (morphing && fx.tierUp) {
        // Poświata i iskry.
        val glow = (1 - tt)
        drawCircle(Color(0xFFFFE08A).copy(alpha = 0.45f * glow), radius = size * (0.45f + 0.5f * tt), center = Offset(px, cam.playerY - size * 0.5f))
        for (i in 0 until 14) {
            val ang = i * 2 * PI / 14 + tt * 1.2
            val r = size * (0.3f + 0.9f * tt)
            val cx = px + (kotlin.math.cos(ang) * r).toFloat()
            val cy = cam.playerY - size * 0.5f + (sin(ang) * r * 0.75).toFloat()
            drawCircle(if (i % 2 == 0) Color(0xFFFFD166) else Color.White, radius = max(1.5f, size * 0.028f * glow), center = Offset(cx, cy), alpha = glow)
        }
        drawSprite(art.characters[fx.previousTier.coerceIn(0, art.characters.lastIndex)], (1 - tt) * 0.9f, 1 - tt)
    } else if (morphing && !fx.tierUp) {
        drawCircle(Color(0xFFE74C3C).copy(alpha = 0.35f * (1 - tt)), radius = size * 0.6f, center = Offset(px, cam.playerY - size * 0.5f))
    }
    drawSprite(art.characters[run.tierIndex], pop, 1f)
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
