package dev.videoplayer.app.editor

object ClipSelection {
    fun error(startMs: Long, endMs: Long, durationMs: Long): String? {
        if (durationMs <= 0) return "This video has no duration."
        if (startMs < 0 || endMs > durationMs + 250) return "Selection is outside the video."
        if (endMs - startMs < 300) return "Select at least a third of a second."
        return null
    }

    fun fileName(sourceName: String, startMs: Long): String {
        val stem = sourceName.substringBeforeLast('.').ifBlank { "clip" }
        val seconds = startMs / 1000
        return "$stem-clip-${seconds}s.mp4"
    }
}
