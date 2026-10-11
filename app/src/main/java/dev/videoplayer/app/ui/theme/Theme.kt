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
fun VideoPlayerTheme(theme: String = "Dark", content: @Composable () -> Unit) {
    val scheme = when (theme) {
        "Light" -> androidx.compose.material3.lightColorScheme(background = Color.White, surface = Color(0xFFF4F4F4), primary = Color(0xFF1E88E5))
        "AMOLED" -> darkColorScheme(background = Color.Black, surface = Color.Black, primary = Color(0xFF1E88E5))
        "Ocean" -> darkColorScheme(background = Color(0xFF07141C), surface = Color(0xFF102433), primary = Color(0xFF26C6DA))
        "Forest" -> darkColorScheme(background = Color(0xFF0D1710), surface = Color(0xFF17301C), primary = Color(0xFF66BB6A))
        "Sunset" -> darkColorScheme(background = Color(0xFF1A1014), surface = Color(0xFF2A1820), primary = Color(0xFFFF7043))
        "Paper" -> androidx.compose.material3.lightColorScheme(background = Color(0xFFF3EFE6), surface = Color(0xFFE7E0D4), primary = Color(0xFF8D6E63))
        "Midnight" -> darkColorScheme(background = Color(0xFF070B16), surface = Color(0xFF12182A), primary = Color(0xFF7C4DFF))
        else -> Scheme
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
