package dev.videoplayer.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Ink = Color(0xFF14120F)
private val InkRaised = Color(0xFF221E19)
private val Paper = Color(0xFFF6F1E8)
private val PaperRaised = Color(0xFFFFFBF5)
private val Amber = Color(0xFFE8A15A)
private val AmberDark = Color(0xFFB86A2E)
private val Mist = Color(0xFF8A8175)

private val DarkColors = darkColorScheme(
    primary = Amber,
    onPrimary = Ink,
    secondary = Color(0xFFD7CBB8),
    background = Ink,
    onBackground = Color(0xFFF3EDE3),
    surface = InkRaised,
    onSurface = Color(0xFFF3EDE3),
    surfaceVariant = Color(0xFF2C271F),
    onSurfaceVariant = Mist,
    outline = Color(0xFF3C342C)
)

private val LightColors = lightColorScheme(
    primary = AmberDark,
    onPrimary = Color.White,
    secondary = Color(0xFF5C5146),
    background = Paper,
    onBackground = Ink,
    surface = PaperRaised,
    onSurface = Ink,
    surfaceVariant = Color(0xFFE7DFD2),
    onSurfaceVariant = Color(0xFF5E564C),
    outline = Color(0xFFD5CBBC)
)

@Composable
fun VideoPlayerTheme(theme: String, content: @Composable () -> Unit) {
    val dark = when (theme) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        content = content
    )
}
