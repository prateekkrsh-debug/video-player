package dev.videoplayer.app.blocker

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockerEngineTest {
    private fun engine(): BlockerEngine {
        val compiled = FilterListParser.parse(
            """
            ||doubleclick.net^
            ||youtube.com/pagead/
            @@||googlevideo.com^
            @@||youtube.com/youtubei/v1/player
            youtube.com##.ytp-ad-module
            youtube.com#@#.video-ads
            youtube.com##.video-ads
            """.trimIndent(),
            "test"
        )
        return BlockerEngine().also { it.install(compiled) }
    }

    @Test
    fun playbackHostsAndMediaFailOpen() {
        val engine = engine()
        val player = engine.decide(
            RequestContext(
                url = "https://www.youtube.com/youtubei/v1/player?prettyPrint=false",
                host = "www.youtube.com",
                documentHost = "m.youtube.com",
                kind = ResourceKind.XHR,
                isMainFrame = false,
                isThirdParty = false
            )
        )
        assertFalse(player.block)

        val media = engine.decide(
            RequestContext(
                url = "https://rr3.googlevideo.com/videoplayback?id=1",
                host = "rr3.googlevideo.com",
                documentHost = "m.youtube.com",
                kind = ResourceKind.MEDIA,
                isMainFrame = false,
                isThirdParty = true
            )
        )
        assertFalse(media.block)
    }

    @Test
    fun adEndpointIsBlockedAndWhitelistWins() {
        val engine = engine()
        val ad = engine.decide(
            RequestContext(
                url = "https://www.youtube.com/pagead/interaction/",
                host = "www.youtube.com",
                documentHost = "m.youtube.com",
                kind = ResourceKind.SCRIPT,
                isMainFrame = false,
                isThirdParty = false
            )
        )
        assertTrue(ad.block)

        engine.whitelist = setOf("doubleclick.net")
        val listed = engine.decide(
            RequestContext(
                url = "https://ad.doubleclick.net/gampad/ads",
                host = "ad.doubleclick.net",
                documentHost = "m.youtube.com",
                kind = ResourceKind.SCRIPT,
                isMainFrame = false,
                isThirdParty = true
            )
        )
        assertFalse(listed.block)
    }

    @Test
    fun cosmeticExceptionRemovesSelector() {
        val css = engine().cosmeticCss("m.youtube.com")
        assertTrue(css.contains(".ytp-ad-module"))
        assertFalse(css.contains(".video-ads"))
    }

    @Test
    fun cosmeticSelectorsDoNotTargetThePlayer() {
        val joined = YoutubeCosmetic.selectors.joinToString(",")
        assertFalse(joined.contains("video"))
        assertFalse(joined.contains("ytd-player"))
        assertTrue(joined.contains("ytm-companion-slot"))
        assertTrue(YoutubeCosmetic.styleBlock().contains("display:none"))
    }
}
