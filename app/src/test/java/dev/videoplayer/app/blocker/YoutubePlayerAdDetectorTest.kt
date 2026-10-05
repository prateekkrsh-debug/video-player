package dev.videoplayer.app.blocker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YoutubePlayerAdDetectorTest {
    private fun feed(detector: YoutubePlayerAdDetector, fixture: String) =
        detector.onSignals(YoutubeAdFixtures.signals(fixture))

    @Test
    fun noAdStaysNormal() {
        val decision = feed(YoutubePlayerAdDetector(), YoutubeAdFixtures.NO_AD)
        assertEquals(YoutubeAdState.NORMAL, decision.state)
        assertFalse(decision.clickSkip)
        assertFalse(decision.watchdog)
    }

    @Test
    fun permanentAdContainerIsNotAnActiveAd() {
        val detector = YoutubePlayerAdDetector()
        val decision = feed(detector, YoutubeAdFixtures.PERMANENT_MODULE)
        assertFalse(detector.isActiveAd(YoutubeAdFixtures.signals(YoutubeAdFixtures.PERMANENT_MODULE)))
        assertEquals(YoutubeAdState.NORMAL, decision.state)
        assertFalse(decision.clickSkip)
    }

    @Test
    fun activeAdIsDetectedWithoutReloading() {
        val decision = feed(YoutubePlayerAdDetector(), YoutubeAdFixtures.ACTIVE_AD)
        assertEquals(YoutubeAdState.AD_DETECTED, decision.state)
        assertTrue(decision.hideAdChrome)
        assertTrue(decision.watchdog)
        assertFalse(decision.clickSkip)
    }

    @Test
    fun skipButtonClicksOnce() {
        val detector = YoutubePlayerAdDetector()
        val first = feed(detector, YoutubeAdFixtures.SKIP_READY)
        val second = feed(detector, YoutubeAdFixtures.SKIP_READY)
        assertEquals(YoutubeAdState.AD_SKIP_AVAILABLE, first.state)
        assertTrue(first.clickSkip)
        assertFalse(second.clickSkip)
    }

    @Test
    fun skipAppearsAfterCountdown() {
        val detector = YoutubePlayerAdDetector()
        val waiting = feed(detector, YoutubeAdFixtures.COUNTDOWN)
        assertEquals(YoutubeAdState.AD_WAITING_FOR_SKIP, waiting.state)
        assertFalse(waiting.clickSkip)
        val ready = feed(detector, YoutubeAdFixtures.SKIP_READY)
        assertEquals(YoutubeAdState.AD_SKIP_AVAILABLE, ready.state)
        assertTrue(ready.clickSkip)
    }

    @Test
    fun adEndReturnsFinishedThenNormal() {
        val detector = YoutubePlayerAdDetector()
        feed(detector, YoutubeAdFixtures.ACTIVE_AD)
        val finished = feed(detector, YoutubeAdFixtures.NO_AD)
        assertEquals(YoutubeAdState.AD_FINISHED, finished.state)
        val normal = feed(detector, YoutubeAdFixtures.NO_AD)
        assertEquals(YoutubeAdState.NORMAL, normal.state)
        assertFalse(normal.watchdog)
    }

    @Test
    fun videoChangeResetsPreviousAd() {
        val detector = YoutubePlayerAdDetector()
        feed(detector, YoutubeAdFixtures.ACTIVE_AD)
        val next = detector.onSignals(
            YoutubeAdFixtures.signals(YoutubeAdFixtures.ACTIVE_AD).copy(videoId = "next")
        )
        assertTrue(next.reset)
        assertEquals(YoutubeAdState.AD_DETECTED, next.state)
        assertEquals("next", detector.videoId)
    }

    @Test
    fun playlistNavigationDoesNotCarrySkipAttempt() {
        val detector = YoutubePlayerAdDetector()
        feed(detector, YoutubeAdFixtures.SKIP_READY)
        detector.onSignals(YoutubeAdFixtures.signals(YoutubeAdFixtures.NO_AD).copy(videoId = "playlist-next"))
        val skip = detector.onSignals(
            YoutubeAdFixtures.signals(YoutubeAdFixtures.SKIP_READY).copy(videoId = "playlist-next")
        )
        assertTrue(skip.clickSkip)
    }

    @Test
    fun openAppAndUpNextAreNotAds() {
        val signals = YoutubeAdFixtures.signals(YoutubeAdFixtures.OPEN_APP_AND_UP_NEXT)
        val decision = YoutubePlayerAdDetector().onSignals(signals)
        assertEquals(YoutubeAdState.NORMAL, decision.state)
        assertFalse(decision.hideAdChrome)
    }

    @Test
    fun normalPlaybackIsNeverClassifiedAsAd() {
        val detector = YoutubePlayerAdDetector()
        repeat(3) {
            val decision = feed(detector, YoutubeAdFixtures.NO_AD)
            assertEquals(YoutubeAdState.NORMAL, decision.state)
        }
    }
}
