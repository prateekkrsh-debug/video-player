package dev.videoplayer.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private fun dark(bg: Color, surface: Color, primary: Color) = darkColorScheme(
    background = bg,
    surface = surface,
    primary = primary,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun VideoPlayerTheme(theme: String = "Dark", content: @Composable () -> Unit) {
    val scheme = when (theme) {
        "AMOLED" -> dark(Color.Black, Color.Black, Color(0xFF1E88E5))
        "Midnight" -> dark(Color(0xFF070B16), Color(0xFF12182A), Color(0xFF7C4DFF))
        "Ocean" -> dark(Color(0xFF07141C), Color(0xFF102433), Color(0xFF26C6DA))
        "Forest" -> dark(Color(0xFF0D1710), Color(0xFF17301C), Color(0xFF66BB6A))
        "Sunset" -> dark(Color(0xFF1A1014), Color(0xFF2A1820), Color(0xFFFF7043))
        "Cinema" -> dark(Color(0xFF14080C), Color(0xFF241016), Color(0xFFE53935))
        "Light" -> lightColorScheme(background = Color(0xFFF7F8FA), surface = Color.White, primary = Color(0xFF1565C0), onBackground = Color.Black, onSurface = Color.Black)
        "Paper" -> lightColorScheme(background = Color(0xFFF3EFE6), surface = Color(0xFFE7E0D4), primary = Color(0xFF8D6E63), onBackground = Color.Black, onSurface = Color.Black)
        else -> dark(Color(0xFF0E1116), Color(0xFF171B22), Color(0xFF1E88E5))
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
