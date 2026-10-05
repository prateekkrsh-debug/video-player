package dev.videoplayer.app.blocker

/**
 * Playback-critical hosts and paths are always allowed. Filtering that would
 * break navigation or media fails open instead of blanking the player.
 */
object YoutubeCompatibility {
    private val alwaysAllowHosts = listOf(
        "googlevideo.com",
        "ytimg.com",
        "ggpht.com",
        "googleusercontent.com",
        "gstatic.com",
        "widevine.com",
        "accounts.google.com",
        "consent.youtube.com",
        "consent.google.com"
    )

    private val allowPathSnippets = listOf(
        "videoplayback",
        "/youtubei/v1/player",
        "/youtubei/v1/browse",
        "/youtubei/v1/next",
        "/youtubei/v1/search",
        "/youtubei/v1/guide",
        "/youtubei/v1/account",
        "/youtubei/v1/reel",
        "/youtubei/v1/player/heartbeat",
        "/api/stats/playback",
        "/api/stats/watchtime",
        "/api/stats/qoe",
        "/generate_204",
        "/youtubei/v1/log_event"
    )

    private val adPathSnippets = listOf(
        "/api/stats/ads",
        "/pagead/",
        "/ptracking",
        "/get_midroll",
        "get_video_ad",
        "/ad_break",
        "/instream/ad",
        "doubleclick",
        "googlesyndication",
        "googleadservices"
    )

    fun isPlaybackCritical(request: RequestContext): Boolean {
        val host = request.host
        if (alwaysAllowHosts.any { host == it || host.endsWith(".$it") }) return true
        if (request.kind == ResourceKind.MEDIA) return true
        val url = request.url.lowercase()
        if (allowPathSnippets.any { url.contains(it) }) return true
        return false
    }

    fun isYoutubeAdEndpoint(request: RequestContext): Boolean {
        val url = request.url.lowercase()
        val host = request.host
        if (host == "ads.youtube.com" || host.endsWith(".ads.youtube.com")) return true
        if (host.endsWith("doubleclick.net") || host.endsWith("googlesyndication.com") ||
            host.endsWith("googleadservices.com") || host.endsWith("googletagservices.com")
        ) return true
        return adPathSnippets.any { url.contains(it) }
    }
}
