package dev.videoplayer.app.blocker

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FilterListParserTest {
    @Test
    fun domainRuleBlocksAdHostAndPlaybackStaysOpen() {
        val compiled = FilterListParser.parse(
            """
            ||doubleclick.net^
            ||google-analytics.com^${'$'}third-party
            ||youtube.com/api/stats/ads
            @@||googlevideo.com^
            youtube.com##.ytp-ad-module
            """.trimIndent(),
            "test"
        )
        val engine = BlockerEngine()
        engine.install(compiled)

        val ad = engine.decide(
            RequestContext(
                url = "https://ad.doubleclick.net/gampad/ads",
                host = "ad.doubleclick.net",
                documentHost = "m.youtube.com",
                kind = ResourceKind.SCRIPT,
                isMainFrame = false,
                isThirdParty = true
            )
        )
        assertTrue(ad.block)

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

        val css = engine.cosmeticCss("www.youtube.com")
        assertTrue(css.contains(".ytp-ad-module"))
    }
}
