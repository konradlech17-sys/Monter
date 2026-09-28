package pl.monter.app.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.monter.app.ui.CloudState
import pl.monter.app.ui.components.BoltLogo
import pl.monter.app.ui.components.GameButton
import pl.monter.app.ui.components.Helmet
import pl.monter.app.ui.components.SparkBadge
import pl.monter.app.ui.components.Stars
import pl.monter.app.ui.components.helmetColor
import pl.monter.app.ui.theme.Palette
import pl.monter.core.game.SaveData
import pl.monter.core.level.Levels
import kotlin.math.sin

@Composable
fun HomeScreen(
    save: SaveData,
    cloud: CloudState,
    onPlay: () -> Unit,
    onShop: () -> Unit,
    onAchievements: () -> Unit,
    onSignIn: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        AnimatedWires(Modifier.fillMaxSize())
        Row(Modifier.fillMaxSize().padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1.2f), verticalArrangement = Arrangement.Center) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BoltLogo(Modifier.size(64.dp))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("MONTER", color = Palette.Volt, fontSize = 44.sp, fontWeight = FontWeight.Black, letterSpacing = 4.sp)
                        Text("szkoła elektryka – od gniazdka do rozdzielnicy 400 V", color = Palette.TextDim, fontSize = 14.sp)
                    }
                }
                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GameButton("▶  Graj", onPlay, Modifier.width(180.dp))
                    GameButton("🛒 Sklep", onShop, color = Palette.Cyan, textColor = Color(0xFF00202E))
                    GameButton("🏆 Osiągnięcia", onAchievements, color = Palette.SurfaceHi, textColor = Palette.Text)
                }
                Spacer(Modifier.height(20.dp))
                val done = Levels.all.count { save.completed(it) }
                val stars = Levels.all.sumOf { save.bestStars(it) }
                Text("Ukończone poziomy: $done / ${Levels.all.size}   •   ★ $stars / ${Levels.all.size * 3}", color = Palette.TextDim, fontSize = 13.sp)
            }
            Column(
                Modifier.weight(0.8f).fillMaxHeight().clip(RoundedCornerShape(24.dp)).background(Palette.Surface.copy(alpha = 0.85f)).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Helmet(helmetColor(save.activeHelmet), Modifier.size(96.dp), shine = true)
                Text(cloud.playerName ?: "Monter-praktykant", color = Palette.Text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                SparkBadge(save.sparks)
                Spacer(Modifier.height(6.dp))
                Stars(rank(save), size = 22.dp)
                Text(rankName(save), color = Palette.TextDim, fontSize = 12.sp)
                Spacer(Modifier.height(12.dp))
                if (!cloud.signedIn) {
                    GameButton(if (cloud.busy) "Łączenie…" else "Zaloguj przez Google Play", onSignIn, enabled = !cloud.busy, small = true, color = Palette.Ok, textColor = Color.White)
                    Spacer(Modifier.height(6.dp))
                }
                Text(cloud.status, color = Palette.TextDim, fontSize = 11.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

private fun rank(s: SaveData): Int = when {
    Levels.campaign.filter { it.chapter == 5 }.all { s.completed(it) } -> 3
    Levels.campaign.filter { it.chapter == 3 }.all { s.completed(it) } -> 2
    Levels.all.any { s.completed(it) } -> 1
    else -> 0
}

private fun rankName(s: SaveData) = when (rank(s)) {
    3 -> "Mistrz rozdzielnic"
    2 -> "Instalator-automatyk"
    1 -> "Uczeń elektryka"
    else -> "Nowicjusz"
}

/** Tło menu: przewody L/N/PE, przez które „płynie" prąd. */
@Composable
private fun AnimatedWires(modifier: Modifier) {
    val t = rememberInfiniteTransition(label = "wires")
    val phase by t.animateFloat(0f, 1f, infiniteRepeatable(tween(4000), RepeatMode.Restart), label = "p")
    Canvas(modifier) {
        val colors = listOf(Color(0xFF8D5524), Color(0xFF1E88E5), Color(0xFF43A047))
        colors.forEachIndexed { i, c ->
            val y0 = size.height * (0.78f + i * 0.06f)
            var prev = Offset(0f, y0)
            val steps = 60
            for (k in 1..steps) {
                val x = size.width * k / steps
                val y = y0 + 10f * sin((k / 6f) + i)
                drawLine(c.copy(alpha = 0.35f), prev, Offset(x, y), strokeWidth = 8f, cap = StrokeCap.Round)
                prev = Offset(x, y)
            }
            val px = ((phase + i * 0.33f) % 1f) * size.width
            drawCircle(Palette.Volt.copy(alpha = 0.7f), 7f, Offset(px, y0 + 10f * sin((px / size.width * steps / 6f) + i)))
        }
    }
}
