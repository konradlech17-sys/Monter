package pl.monter.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object Palette {
    val Bg = Color(0xFF0F1724)
    val Surface = Color(0xFF1B2536)
    val SurfaceHi = Color(0xFF26324A)
    val Volt = Color(0xFFFFC107)
    val VoltDark = Color(0xFFE0A800)
    val Cyan = Color(0xFF29B6F6)
    val Ok = Color(0xFF66BB6A)
    val Bad = Color(0xFFEF5350)
    val Warn = Color(0xFFFFA726)
    val Text = Color(0xFFECEFF4)
    val TextDim = Color(0xFF9AA5B8)
}

private val scheme = darkColorScheme(
    primary = Palette.Volt,
    onPrimary = Color(0xFF1A1300),
    secondary = Palette.Cyan,
    onSecondary = Color(0xFF00202E),
    background = Palette.Bg,
    onBackground = Palette.Text,
    surface = Palette.Surface,
    onSurface = Palette.Text,
    surfaceVariant = Palette.SurfaceHi,
    onSurfaceVariant = Palette.TextDim,
    error = Palette.Bad,
)

@Composable
fun MonterTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, content = content)
}
