package dev.videoplayer.app.library

import android.content.Context

object PlaybackMemory {
    private const val PREFS = "playback"
    private const val URI = "uri"
    private const val NAME = "name"
    private const val FOLDER = "folder"
    private const val POSITION = "position"

    fun save(context: Context, video: VideoFile, positionMs: Long) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(URI, video.uri)
            .putString(NAME, video.name)
            .putString(FOLDER, video.folder)
            .putLong(POSITION, positionMs)
            .apply()
    }

    fun load(context: Context): Pair<VideoFile, Long>? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val uri = prefs.getString(URI, null) ?: return null
        val video = VideoFile(
            id = -1,
            uri = uri,
            name = prefs.getString(NAME, "Video") ?: "Video",
            folder = prefs.getString(FOLDER, "Recent") ?: "Recent",
            durationMs = 0,
            sizeBytes = 0,
            dateAddedSec = 0
        )
        return video to prefs.getLong(POSITION, 0L)
    }
}
