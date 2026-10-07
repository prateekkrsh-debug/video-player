package dev.videoplayer.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Scheme = darkColorScheme(
    background = Color(0xFF000000),
    surface = Color(0xFF111111),
    primary = Color(0xFF1E88E5)
)

@Composable
fun VideoPlayerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}
