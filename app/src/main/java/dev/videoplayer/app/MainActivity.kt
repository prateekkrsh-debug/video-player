package dev.videoplayer.app

import android.Manifest
import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.videoplayer.app.player.PlaybackController
import dev.videoplayer.app.player.PlaybackService
import dev.videoplayer.app.ui.VideoPlayerRoot
import dev.videoplayer.app.ui.theme.VideoPlayerTheme
import dev.videoplayer.app.youtube.YoutubeUrls
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {
    val container by lazy { (application as VideoPlayerApp).container }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            val settings = container.settings.settings.collectAsStateWithLifecycle(
                initialValue = dev.videoplayer.app.settings.AppSettings()
            )
            VideoPlayerTheme(settings.value.theme) {
                VideoPlayerRoot(this)
            }
        }
        handleIntent(intent)
        requestNotificationPermission()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (shouldEnterPip()) enterPip()
    }

    override fun onPause() {
        super.onPause()
        if (isInPictureInPictureMode) return
        if (shouldBackground()) {
            PlaybackController.holdInBackground(true)
            PlaybackService.start(this)
        }
    }

    override fun onResume() {
        super.onResume()
        PlaybackController.holdInBackground(false)
        PlaybackService.stop(this)
        updateAutoPip()
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        pipMode.value = isInPictureInPictureMode
        if (!isInPictureInPictureMode && !hasWindowFocus()) {
            if (shouldBackground()) {
                PlaybackController.holdInBackground(true)
                PlaybackService.start(this)
            }
        }
    }

    private fun handleIntent(intent: Intent?) {
        val data = intent?.dataString ?: return
        val target = YoutubeUrls.fromExternal(data) ?: return
        incoming.value = target
    }

    fun enterPip() {
        val params = pipParams(autoEnter = false)
        enterPictureInPictureMode(params)
    }

    fun updateAutoPip() {
        if (Build.VERSION.SDK_INT >= 31) {
            setPictureInPictureParams(pipParams(autoEnter = shouldEnterPip()))
        }
    }

    private fun pipParams(autoEnter: Boolean): PictureInPictureParams {
        val builder = PictureInPictureParams.Builder().setAspectRatio(Rational(16, 9))
        if (Build.VERSION.SDK_INT >= 31) builder.setAutoEnterEnabled(autoEnter)
        return builder.build()
    }

    private fun shouldEnterPip(): Boolean {
        val settings = runBlocking { container.settings.current() }
        return settings.pipEnabled && PlaybackController.playing.value && !isInPictureInPictureMode
    }

    private fun shouldBackground(): Boolean {
        val settings = runBlocking { container.settings.current() }
        return settings.backgroundPlayback && PlaybackController.playing.value
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < 33) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            return
        }
        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    fun openExternal(url: String) {
        runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    fun enterFullscreen(landscape: Boolean) {
        if (landscape) requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    fun exitFullscreen() {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        WindowCompat.getInsetsController(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
    }

    companion object {
        val incoming = MutableStateFlow<String?>(null)
        val pipMode = MutableStateFlow(false)
    }
}
