package dev.videoplayer.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import dev.videoplayer.app.library.VideoFile
import dev.videoplayer.app.ui.LibraryRoot
import dev.videoplayer.app.ui.PlayerScreen
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.SideEffect
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import dev.videoplayer.app.ui.theme.VideoPlayerTheme

class MainActivity : ComponentActivity() {
    private var granted by mutableStateOf(false)
    private var queue by mutableStateOf<List<VideoFile>?>(null)
    private var startIndex by mutableStateOf(0)

    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        granted = hasVideoPermission()
        if (!granted) permission.launch(videoPermission())
        setContent {
            VideoPlayerTheme {
                val playing = queue
                SideEffect {
                    val controller = WindowCompat.getInsetsController(window, window.decorView)
                    if (playing != null) {
                        controller.hide(WindowInsetsCompat.Type.systemBars())
                        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    } else {
                        controller.show(WindowInsetsCompat.Type.systemBars())
                    }
                }
                Box(Modifier.fillMaxSize()) {
                    if (granted) {
                        LibraryRoot { videos, index ->
                            queue = videos
                            startIndex = index
                        }
                    } else {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Button(onClick = { permission.launch(videoPermission()) }) {
                                Text("Allow video access")
                            }
                        }
                    }
                    if (playing != null) {
                        PlayerScreen(playing, startIndex) { queue = null }
                    }
                }
            }
        }
        handleViewIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleViewIntent(intent)
    }

    private fun handleViewIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        if (intent.action != Intent.ACTION_VIEW) return
        queue = listOf(
            VideoFile(
                id = -1,
                uri = uri.toString(),
                name = uri.lastPathSegment ?: "Video",
                folder = "Opened",
                durationMs = 0,
                sizeBytes = 0,
                dateAddedSec = 0
            )
        )
        startIndex = 0
    }

    private fun hasVideoPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, videoPermission()) == PackageManager.PERMISSION_GRANTED

    private fun videoPermission(): String =
        if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_VIDEO
        else Manifest.permission.READ_EXTERNAL_STORAGE
}
