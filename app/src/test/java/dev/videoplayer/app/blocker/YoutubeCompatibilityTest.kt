package dev.videoplayer.app.blocker

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YoutubeCompatibilityTest {
    @Test
    fun mediaAndPlayerEndpointsStayOpen() {
        assertTrue(
            YoutubeCompatibility.isPlaybackCritical(
                RequestContext(
                    url = "https://rr1.googlevideo.com/videoplayback?expire=1",
                    host = "rr1.googlevideo.com",
                    documentHost = "m.youtube.com",
                    kind = ResourceKind.OTHER,
                    isMainFrame = false,
                    isThirdParty = true
                )
            )
        )
        assertTrue(
            YoutubeCompatibility.isPlaybackCritical(
                RequestContext(
                    url = "https://m.youtube.com/youtubei/v1/player/heartbeat",
                    host = "m.youtube.com",
                    documentHost = "m.youtube.com",
                    kind = ResourceKind.XHR,
                    isMainFrame = false,
                    isThirdParty = false
                )
            )
        )
        assertFalse(
            YoutubeCompatibility.isPlaybackCritical(
                RequestContext(
                    url = "https://www.youtube.com/pagead/adview",
                    host = "www.youtube.com",
                    documentHost = "m.youtube.com",
                    kind = ResourceKind.SCRIPT,
                    isMainFrame = false,
                    isThirdParty = false
                )
            )
        )
    }

    @Test
    fun knownAdEndpointsAreMarked() {
        assertTrue(
            YoutubeCompatibility.isYoutubeAdEndpoint(
                RequestContext(
                    url = "https://ads.youtube.com/api/stats/ads",
                    host = "ads.youtube.com",
                    documentHost = "m.youtube.com",
                    kind = ResourceKind.XHR,
                    isMainFrame = false,
                    isThirdParty = true
                )
            )
        )
        assertFalse(
            YoutubeCompatibility.isYoutubeAdEndpoint(
                RequestContext(
                    url = "https://i.ytimg.com/vi/abc/hqdefault.jpg",
                    host = "i.ytimg.com",
                    documentHost = "m.youtube.com",
                    kind = ResourceKind.IMAGE,
                    isMainFrame = false,
                    isThirdParty = true
                )
            )
        )
    }
}
