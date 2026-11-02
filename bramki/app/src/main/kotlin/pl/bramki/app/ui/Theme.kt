package pl.bramki.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Night = Color(0xFF101329)
val NightCard = Color(0xFF1B1F3B)
val Accent = Color(0xFFFFD166)
val Good = Color(0xFF2ECC71)
val Bad = Color(0xFFE74C3C)

private val scheme = darkColorScheme(
    primary = Accent,
    onPrimary = Night,
    secondary = Good,
    background = Night,
    surface = NightCard,
    onBackground = Color(0xFFF4F4F8),
    onSurface = Color(0xFFF4F4F8),
    surfaceVariant = Color(0xFF262B4F),
    onSurfaceVariant = Color(0xFFC9CCE0),
)

@Composable
fun BramkiTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, content = content)
}
