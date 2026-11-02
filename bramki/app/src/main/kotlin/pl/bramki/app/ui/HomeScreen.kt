package pl.bramki.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import pl.bramki.app.game.Sprites
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.bramki.app.data.Records
import pl.bramki.engine.Format
import pl.bramki.engine.Mode
import pl.bramki.engine.Modes

@Composable
fun HomeScreen(records: Records, onPlay: (Mode) -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Night)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column {
                Text("Bramki!", fontSize = 44.sp, fontWeight = FontWeight.Black, color = Accent)
                Text(
                    "Lewo czy prawo? Wybieraj bramki, rozwijaj postać.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Bez reklam, bez mikropłatności, bez internetu.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Good,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Spacer(Modifier.height(8.dp))
            }
        }
        items(Modes.ALL, key = { it.id }) { mode ->
            ModeCard(mode, records.get(mode.id)) { onPlay(mode) }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF151A36)),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Jak grać", fontWeight = FontWeight.Bold, color = Accent)
                    Text("• Przesuń palcem w lewo lub w prawo (albo stuknij połowę ekranu), żeby wybrać stronę.", style = MaterialTheme.typography.bodyMedium)
                    Text("• Zielone bramki dodają lub mnożą, czerwone odejmują lub dzielą. Ściany zabierają część wartości – omijaj je.", style = MaterialTheme.typography.bodyMedium)
                    Text("• Im więcej masz na mecie, tym wyższy szczebel. Po trasie zobaczysz, gdzie dało się wybrać lepiej.", style = MaterialTheme.typography.bodyMedium)
                    Text("• Rada: ×2 opłaca się bardziej niż +N tylko wtedy, gdy masz więcej niż N.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun ModeCard(mode: Mode, best: Records.Best, onClick: () -> Unit) {
    // Postać na karcie: najwyższy zdobyty szczebel (albo startowy, jeśli jeszcze nie grano).
    val shownTier = if (best.played) best.tier.coerceIn(0, mode.tiers.lastIndex) else 0
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = NightCard),
        shape = RoundedCornerShape(18.dp),
    ) {
        Box {
            Image(
                painterResource(Sprites.background(mode.id)),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.45f,
            )
            Box(
                Modifier
                    .matchParentSize()
                    .background(Brush.horizontalGradient(listOf(Color.Transparent, NightCard.copy(alpha = 0.85f)))),
            )
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(Sprites.character(mode.id, shownTier)), contentDescription = null, modifier = Modifier.size(72.dp))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(mode.name, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(mode.tagline, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    if (best.played) {
                        val t = mode.tiers[best.tier.coerceIn(0, mode.tiers.lastIndex)]
                        val wins = if (mode.enemyFraction != null) " · bitwy: ${best.wins}/${best.plays}" else ""
                        Text(
                            "Rekord: ${t.emoji} ${t.name} · ${Format.percent(best.ratio.toDouble())} optimum · gier: ${best.plays}$wins",
                            style = MaterialTheme.typography.labelMedium,
                            color = Accent,
                        )
                    } else {
                        Text("Jeszcze nie grano", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text("▶", fontSize = 22.sp, color = Accent)
            }
        }
    }
}
