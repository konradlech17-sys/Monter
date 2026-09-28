package pl.monter.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.monter.app.ui.theme.Palette
import kotlin.math.sin
import kotlin.random.Random

/** Duży, „sprężysty" przycisk w stylu gry. */
@Composable
fun GameButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Palette.Volt,
    textColor: Color = Color(0xFF1A1300),
    enabled: Boolean = true,
    small: Boolean = false,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.93f else 1f, spring(dampingRatio = 0.4f), label = "btn")
    val bg = if (enabled) color else Palette.SurfaceHi
    Box(
        modifier
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.verticalGradient(listOf(bg, bg.copy(alpha = 0.8f))))
            .clickable(interactionSource = source, indication = null, enabled = enabled, onClick = onClick)
            .padding(horizontal = if (small) 12.dp else 20.dp, vertical = if (small) 8.dp else 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (enabled) textColor else Palette.TextDim, fontWeight = FontWeight.Bold, fontSize = if (small) 13.sp else 16.sp)
    }
}

/** Mały przełącznik-„pigułka". */
@Composable
fun Pill(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, accent: Color = Palette.Volt) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) accent.copy(alpha = 0.25f) else Palette.SurfaceHi)
            .border(1.5.dp, if (selected) accent else Color.Transparent, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(text, color = if (selected) accent else Palette.Text, fontSize = 13.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

/** Licznik iskier (waluta gry) z animacją zmiany wartości. */
@Composable
fun SparkBadge(sparks: Int, modifier: Modifier = Modifier) {
    val anim = remember { Animatable(sparks.toFloat()) }
    LaunchedEffect(sparks) { anim.animateTo(sparks.toFloat(), tween(700)) }
    Row(
        modifier
            .clip(RoundedCornerShape(50))
            .background(Palette.SurfaceHi)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("⚡", fontSize = 16.sp)
        Spacer(Modifier.width(4.dp))
        Text(anim.value.toInt().toString(), color = Palette.Volt, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
fun Stars(count: Int, modifier: Modifier = Modifier, size: Dp = 16.dp, max: Int = 3) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(max) { i ->
            Text("★", color = if (i < count) Palette.Volt else Palette.SurfaceHi, fontSize = (size.value).sp)
        }
    }
}

fun helmetColor(id: String): Color = when (id) {
    "helmet_blue" -> Color(0xFF1E88E5)
    "helmet_red" -> Color(0xFFE53935)
    "helmet_gold" -> Color(0xFFFFD54F)
    else -> Color(0xFFFFC107)
}

/** Kask gracza – awatar. */
@Composable
fun Helmet(color: Color, modifier: Modifier = Modifier.size(64.dp), shine: Boolean = false) {
    val t = rememberInfiniteTransition(label = "helmet")
    val glow by t.animateFloat(0.2f, 0.6f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "glow")
    Canvas(modifier) {
        val w = size.width; val h = size.height
        if (shine) drawCircle(color.copy(alpha = glow * 0.5f), radius = w * 0.55f, center = Offset(w / 2, h * 0.55f))
        val dome = Path().apply {
            moveTo(w * 0.15f, h * 0.7f)
            cubicTo(w * 0.15f, h * 0.2f, w * 0.85f, h * 0.2f, w * 0.85f, h * 0.7f)
            close()
        }
        drawPath(dome, color)
        drawRect(color.copy(red = color.red * 0.85f, green = color.green * 0.85f, blue = color.blue * 0.85f), Offset(w * 0.45f, h * 0.28f), Size(w * 0.1f, h * 0.42f))
        drawRoundRect(color, Offset(w * 0.05f, h * 0.68f), Size(w * 0.9f, h * 0.1f), androidx.compose.ui.geometry.CornerRadius(w * 0.05f))
        // Błyskawica
        val bolt = Path().apply {
            moveTo(w * 0.56f, h * 0.36f); lineTo(w * 0.42f, h * 0.56f); lineTo(w * 0.5f, h * 0.56f)
            lineTo(w * 0.46f, h * 0.72f); lineTo(w * 0.6f, h * 0.5f); lineTo(w * 0.52f, h * 0.5f); close()
        }
        drawPath(bolt, Palette.Cyan)
    }
}

private data class Particle(val x: Float, val speed: Float, val size: Float, val color: Color, val phase: Float, val spin: Float)

/** Konfetti po ukończeniu poziomu. */
@Composable
fun Confetti(modifier: Modifier = Modifier, count: Int = 90) {
    val colors = listOf(Palette.Volt, Palette.Cyan, Palette.Ok, Color(0xFFE91E63), Color(0xFFAB47BC), Color.White)
    val particles = remember {
        List(count) {
            Particle(Random.nextFloat(), 0.15f + Random.nextFloat() * 0.35f, 6f + Random.nextFloat() * 10f, colors.random(), Random.nextFloat() * 6f, Random.nextFloat() * 360f)
        }
    }
    var time by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (true) withFrameNanos { time = it - start }
    }
    Canvas(modifier) {
        val t = time / 1_000_000_000f
        for (p in particles) {
            val y = ((p.phase * 0.2f + t * p.speed) % 1.2f - 0.1f) * size.height
            val x = (p.x + 0.03f * sin(t * 2f + p.phase)) * size.width
            rotate(p.spin + t * 180f, Offset(x, y)) {
                drawRect(p.color, Offset(x - p.size / 2, y - p.size / 4), Size(p.size, p.size / 2))
            }
        }
    }
}

/** Pulsująca błyskawica – logo. */
@Composable
fun BoltLogo(modifier: Modifier = Modifier.size(56.dp)) {
    val t = rememberInfiniteTransition(label = "bolt")
    val a by t.animateFloat(0.55f, 1f, infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse), label = "a")
    Canvas(modifier) {
        val w = size.width; val h = size.height
        drawCircle(Palette.Volt.copy(alpha = 0.18f * a), radius = w * 0.5f)
        val bolt = Path().apply {
            moveTo(w * 0.62f, h * 0.05f); lineTo(w * 0.22f, h * 0.55f); lineTo(w * 0.48f, h * 0.55f)
            lineTo(w * 0.36f, h * 0.95f); lineTo(w * 0.8f, h * 0.4f); lineTo(w * 0.54f, h * 0.4f); close()
        }
        drawPath(bolt, Palette.Volt.copy(alpha = a))
    }
}

@Composable
fun Header(title: String, onBack: (() -> Unit)?, modifier: Modifier = Modifier, trailing: @Composable () -> Unit = {}) {
    Row(modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Palette.SurfaceHi).clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) { Text("←", color = Palette.Text, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.width(12.dp))
        }
        Text(title, color = Palette.Text, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        trailing()
    }
}
