package dev.videoplayer.app.player

object SeekGesture {
    const val TOUCH_SLOP_PX = 24f
    const val DEFAULT_MS_PER_SCREEN = 90_000L

    fun target(originMs: Long, dxPx: Float, widthPx: Float, durationMs: Long, msPerScreen: Long): Long {
        if (durationMs <= 0L) return 0L
        val width = widthPx.coerceAtLeast(1f)
        val delta = (dxPx / width) * msPerScreen.coerceIn(15_000L, 240_000L)
        return (originMs + delta.toLong()).coerceIn(0L, durationMs)
    }

    fun label(deltaMs: Long): String {
        val sign = if (deltaMs >= 0) "+" else "−"
        val totalSeconds = kotlin.math.abs(deltaMs) / 1000
        return "%s%02d:%02d".format(sign, totalSeconds / 60, totalSeconds % 60)
    }

    fun direction(dx: Float, dy: Float): String? {
        if (kotlin.math.abs(dx) < TOUCH_SLOP_PX && kotlin.math.abs(dy) < TOUCH_SLOP_PX) return null
        return if (kotlin.math.abs(dx) > kotlin.math.abs(dy)) "seek" else "vertical"
    }
}
