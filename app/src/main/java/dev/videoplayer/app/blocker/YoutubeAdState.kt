package dev.videoplayer.app.blocker

enum class YoutubeAdState {
    NORMAL,
    AD_DETECTED,
    AD_WAITING_FOR_SKIP,
    AD_SKIP_AVAILABLE,
    AD_FINISHED
}

/**
 * Signals collected from the active player, not from the whole page.
 * A permanent ad container is not an active advertisement.
 */
data class PlayerAdSignals(
    val videoId: String = "",
    val playerPresent: Boolean = false,
    val adShowing: Boolean = false,
    val adInterrupting: Boolean = false,
    val overlayVisible: Boolean = false,
    val adTextVisible: Boolean = false,
    val countdownVisible: Boolean = false,
    val skipButtonVisible: Boolean = false,
    val adModulePresent: Boolean = false
)

data class AdDecision(
    val state: YoutubeAdState,
    val clickSkip: Boolean = false,
    val hideAdChrome: Boolean = false,
    val watchdog: Boolean = false,
    val reset: Boolean = false
)
