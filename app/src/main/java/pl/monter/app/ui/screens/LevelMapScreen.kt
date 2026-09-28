package pl.monter.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.monter.app.ui.components.Header
import pl.monter.app.ui.components.Pill
import pl.monter.app.ui.components.SparkBadge
import pl.monter.app.ui.components.Stars
import pl.monter.app.ui.theme.Palette
import pl.monter.core.game.Difficulty
import pl.monter.core.game.PurchaseResult
import pl.monter.core.game.SaveData
import pl.monter.core.level.Level
import pl.monter.core.level.Levels

private val chapterColors = listOf(Color(0xFF26A69A), Color(0xFF42A5F5), Color(0xFFAB47BC))

@Composable
fun LevelMapScreen(
    save: SaveData,
    difficulty: Difficulty,
    onDifficulty: (Difficulty) -> Unit,
    onBack: () -> Unit,
    onLevel: (Level) -> Unit,
    onBuyLevel: (Level) -> PurchaseResult,
) {
    var buying by remember { mutableStateOf<Level?>(null) }
    var info by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize()) {
        Header("Wybierz zadanie", onBack) { SparkBadge(save.sparks) }
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Poziom trudności:", color = Palette.TextDim, fontSize = 13.sp)
            Difficulty.entries.forEach { d ->
                Pill("${d.title} ×${d.multiplier}", d == difficulty, { onDifficulty(d) })
            }
        }
        Text(difficulty.description, color = Palette.TextDim, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            items(Levels.chapters) { ch ->
                val color = chapterColors[(ch.number - 1) % chapterColors.size]
                Column {
                    Text("Rozdział ${ch.number}: ${ch.title}", color = color, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(ch.description, color = Palette.TextDim, fontSize = 12.sp)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(Levels.all.filter { it.chapter == ch.number }) { level ->
                            LevelCard(level, save, difficulty, color) {
                                when {
                                    save.isUnlocked(level) -> onLevel(level)
                                    level.unlockCost != null -> buying = level
                                    else -> info = "Najpierw ukończ poprzednie zadanie."
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    buying?.let { level ->
        AlertDialog(
            onDismissRequest = { buying = null },
            title = { Text("Poziom bonusowy: ${level.title}") },
            text = { Text("Odblokuj za ⚡ ${level.unlockCost} iskier? Masz ${save.sparks}.") },
            confirmButton = {
                TextButton(onClick = {
                    info = when (onBuyLevel(level)) {
                        is PurchaseResult.Ok -> "Odblokowano! Powodzenia."
                        PurchaseResult.NotEnough -> "Za mało iskier – zdobądź je, ukańczając zadania."
                        PurchaseResult.AlreadyOwned -> null
                    }
                    buying = null
                }) { Text("Kup") }
            },
            dismissButton = { TextButton(onClick = { buying = null }) { Text("Anuluj") } },
        )
    }
    info?.let {
        AlertDialog(onDismissRequest = { info = null }, confirmButton = { TextButton(onClick = { info = null }) { Text("OK") } }, text = { Text(it) })
    }
}

@Composable
private fun LevelCard(level: Level, save: SaveData, difficulty: Difficulty, color: Color, onClick: () -> Unit) {
    val unlocked = save.isUnlocked(level)
    val stars = save.result(level, difficulty)?.stars ?: 0
    Box(
        Modifier.width(190.dp).height(130.dp).clip(RoundedCornerShape(18.dp))
            .background(
                if (unlocked) Brush.linearGradient(listOf(color.copy(alpha = 0.55f), Palette.Surface))
                else Brush.linearGradient(listOf(Palette.SurfaceHi, Palette.Surface)),
            )
            .border(2.dp, if (stars == 3) Palette.Volt else Color.Transparent, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(30.dp).clip(RoundedCornerShape(50)).background(Palette.Bg.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
                    Text(level.id, color = Palette.Text, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(8.dp))
                Text(level.title, color = Palette.Text, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(4.dp))
            Text(level.subtitle, color = Palette.TextDim, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Box(Modifier.align(Alignment.BottomStart)) {
            when {
                unlocked -> Stars(stars, size = 18.dp)
                level.unlockCost != null -> Text("🔒 ⚡${level.unlockCost}", color = Palette.Volt, fontWeight = FontWeight.Bold)
                else -> Text("🔒", fontSize = 18.sp)
            }
        }
        Text("+${level.basePoints}", color = Palette.TextDim, fontSize = 11.sp, modifier = Modifier.align(Alignment.BottomEnd))
    }
}
