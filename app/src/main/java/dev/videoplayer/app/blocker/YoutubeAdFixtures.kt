package dev.videoplayer.app.blocker

/**
 * Small HTML fixtures used to prove the detector does not treat normal YouTube UI as an ad.
 */
object YoutubeAdFixtures {
    const val NO_AD = """
        <div id="movie_player" class="html5-video-player" data-video-id="abc">
          <video></video>
        </div>
    """

    const val PERMANENT_MODULE = """
        <div id="movie_player" class="html5-video-player" data-video-id="abc">
          <div class="ytp-ad-module"></div>
          <div id="player-ads"></div>
          <video></video>
        </div>
    """

    const val ACTIVE_AD = """
        <div id="movie_player" class="html5-video-player ad-showing" data-video-id="abc">
          <div class="ytp-ad-player-overlay"></div>
          <div class="ytp-ad-text">Ad</div>
        </div>
    """

    const val SKIP_READY = """
        <div id="movie_player" class="html5-video-player ad-showing" data-video-id="abc">
          <button class="ytp-ad-skip-button">Skip</button>
        </div>
    """

    const val COUNTDOWN = """
        <div id="movie_player" class="html5-video-player ad-interrupting" data-video-id="abc">
          <div class="ytp-ad-preview-container">Skip in 5</div>
        </div>
    """

    const val OPEN_APP_AND_UP_NEXT = """
        <div id="movie_player" class="html5-video-player" data-video-id="abc">
          <video></video>
        </div>
        <button class="ytm-app-promo">Open App</button>
        <div class="ytp-upnext">Up Next</div>
    """

    fun signals(fixture: String): PlayerAdSignals {
        val player = fixture.contains("id=\"movie_player\"") || fixture.contains("html5-video-player")
        val videoId = Regex("data-video-id=\"([^\"]+)\"").find(fixture)?.groupValues?.get(1).orEmpty()
        val adShowing = fixture.contains("ad-showing")
        val interrupting = fixture.contains("ad-interrupting")
        val overlay = fixture.contains("ytp-ad-player-overlay") && !fixture.contains("overlay-hidden")
        val adText = fixture.contains("ytp-ad-text")
        val countdown = fixture.contains("ytp-ad-preview-container") || fixture.contains("ytp-ad-duration-remaining")
        val skip = fixture.contains("ytp-ad-skip-button") || fixture.contains("ytp-skip-ad-button")
        val module = fixture.contains("ytp-ad-module") || fixture.contains("id=\"player-ads\"")
        return PlayerAdSignals(
            videoId = videoId,
            playerPresent = player,
            adShowing = adShowing,
            adInterrupting = interrupting,
            overlayVisible = overlay,
            adTextVisible = adText,
            countdownVisible = countdown,
            skipButtonVisible = skip,
            adModulePresent = module
        )
    }
}
