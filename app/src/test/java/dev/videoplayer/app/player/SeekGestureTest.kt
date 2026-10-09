package dev.videoplayer.app.player

import org.junit.Assert.assertEquals
import org.junit.Test

class SeekGestureTest {
    @Test
    fun swipeRightAdvancesAndStopsAtEnd() {
        assertEquals(15_000L, SeekGesture.target(0, 100f, 600f, 60_000, 90_000))
        assertEquals(60_000L, SeekGesture.target(50_000, 600f, 600f, 60_000, 90_000))
    }

    @Test
    fun swipeLeftRewindsAndStopsAtStart() {
        assertEquals(30_000L, SeekGesture.target(45_000, -100f, 600f, 90_000, 90_000))
        assertEquals(0L, SeekGesture.target(5_000, -600f, 600f, 90_000, 90_000))
    }

    @Test
    fun labelAndDirection() {
        assertEquals("+00:15", SeekGesture.label(15_000))
        assertEquals("−00:15", SeekGesture.label(-15_000))
        assertEquals("seek", SeekGesture.direction(40f, 8f))
        assertEquals("vertical", SeekGesture.direction(8f, 40f))
        assertEquals(null, SeekGesture.direction(4f, 4f))
    }
}
