package dev.videoplayer.app.player

import dev.videoplayer.app.youtube.YoutubeSession
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Bridge between the YouTube page player and the media notification.
 * Playback stays inside the WebView. Nothing is downloaded or restreamed.
 */
object PlaybackController {
    @Volatile
    var session: YoutubeSession? = null

    val playing = MutableStateFlow(false)
    val title = MutableStateFlow("Video Player")
    val positionMs = MutableStateFlow(0L)

    var onStateChanged: (() -> Unit)? = null

    fun attach(value: YoutubeSession) {
        session = value
    }

    fun detach(value: YoutubeSession) {
        if (session === value) session = null
    }

    fun update(isPlaying: Boolean, position: Long, pageTitle: String) {
        playing.value = isPlaying
        positionMs.value = position
        if (pageTitle.isNotBlank()) title.value = pageTitle
        onStateChanged?.invoke()
    }

    fun play() = session?.control("play")
    fun pause() = session?.control("pause")
    fun seekForward() = session?.control("forward")
    fun seekBack() = session?.control("back")
    fun holdInBackground(hold: Boolean) = session?.holdInBackground(hold)
}
