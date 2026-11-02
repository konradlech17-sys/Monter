package pl.bramki.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.bramki.engine.Analysis
import pl.bramki.engine.Format
import pl.bramki.engine.RunResult

@Composable
fun ResultScreen(result: RunResult, isRecord: Boolean, onRetry: () -> Unit, onHome: () -> Unit) {
    val mode = result.mode
    val tier = result.tier
    val tip = remember(result) { mode.tips.random() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Night)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(tier.emoji, fontSize = 84.sp)
        Text(tier.name, fontSize = 30.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
        Text(
            "${Format.full(result.value)} ${mode.unit}",
            fontSize = 22.sp,
            color = Accent,
            fontWeight = FontWeight.Bold,
        )
        Text(stars(result.stars), fontSize = 30.sp, color = Accent)
        if (isRecord) {
            Text("🏆 Nowy rekord!", color = Good, fontWeight = FontWeight.Bold)
        }
        Text(
            "To ${Format.percent(result.ratio)} najlepszej możliwej trasy (${Format.compact(result.optimal)} ${mode.unit}).",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        result.enemy?.let { enemy ->
            val won = result.battleWon == true
            InfoCard(color = if (won) Color(0xFF1F4A2E) else Color(0xFF4A1F1F)) {
                Text(if (won) "⚔️ Zwycięstwo!" else "💀 Porażka", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Twoja armia: ${Format.full(result.value)} · wróg: ${Format.full(enemy)}" +
                        if (won) ". Masz przewagę ${Format.compact(result.value - enemy)} wojów." else ". Brakło ${Format.compact(enemy - result.value)} wojów.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        InfoCard {
            Text("Ciekawostka: ${tier.name}", fontWeight = FontWeight.Bold, color = Accent)
            Text(tier.fact, style = MaterialTheme.typography.bodyMedium)
        }

        val mistakes = result.mistakes
        InfoCard {
            Text("Gdzie uciekły punkty", fontWeight = FontWeight.Bold, color = Accent)
            if (mistakes.isEmpty() && result.wallsHit == 0) {
                Text("Nigdzie – każda bramka wybrana optymalnie. Tak się to robi! 🎯", style = MaterialTheme.typography.bodyMedium)
            } else {
                mistakes.take(4).forEach { d ->
                    Text("• " + Analysis.describe(d, mode), style = MaterialTheme.typography.bodyMedium)
                }
                if (mistakes.size > 4) {
                    Text("…i jeszcze ${mistakes.size - 4} mniej kosztownych wyborów.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (result.wallsHit > 0) {
                    Text("• Ściany: ${result.wallsHit}× – każda zabrała część wartości.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        InfoCard {
            Text("Wskazówka", fontWeight = FontWeight.Bold, color = Accent)
            Text(tip, style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onHome, modifier = Modifier.weight(1f)) { Text("Menu") }
            Button(
                onClick = onRetry,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Night),
            ) { Text("Jeszcze raz", fontWeight = FontWeight.Bold) }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun InfoCard(color: Color = NightCard, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { content() }
    }
}

private fun stars(n: Int): String = "★".repeat(n) + "☆".repeat(3 - n)
