package pl.monter.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.monter.app.ui.components.GameButton
import pl.monter.app.ui.components.Header
import pl.monter.app.ui.theme.Palette
import pl.monter.core.game.Achievements
import pl.monter.core.game.SaveData

@Composable
fun AchievementsScreen(save: SaveData, signedIn: Boolean, onPlayGames: () -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Header("Osiągnięcia ${save.achievements.size}/${Achievements.all.size}", onBack) {
            if (signedIn) GameButton("Google Play Gry", onPlayGames, small = true, color = Palette.Ok, textColor = Color.White)
        }
        Text(
            "Statystyki: sprawdzeń ${save.stats.checks} • ułożonych przewodów ${save.stats.wiresLaid} • zwarć w teście ${save.stats.shortsCaused} • bezbłędnych procedur BHP ${save.stats.perfectProcedures}",
            color = Palette.TextDim, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 16.dp),
        )
        LazyVerticalGrid(
            GridCells.Adaptive(240.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(Achievements.all) { a ->
                val got = a.id in save.achievements
                Row(
                    Modifier.clip(RoundedCornerShape(16.dp)).background(if (got) Palette.SurfaceHi else Palette.Surface).padding(12.dp).alpha(if (got) 1f else 0.5f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(if (got) a.icon else "🔒", fontSize = 30.sp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(a.title, color = if (got) Palette.Volt else Palette.Text, fontWeight = FontWeight.Bold)
                        Text(a.description, color = Palette.TextDim, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
