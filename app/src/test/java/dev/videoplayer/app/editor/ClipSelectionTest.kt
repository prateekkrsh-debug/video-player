package dev.videoplayer.app.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ClipSelectionTest {
    @Test
    fun rejectsEmptyAndInvertedRanges() {
        assertEquals("Select at least a third of a second.", ClipSelection.error(1_000, 1_100, 10_000))
        assertEquals("Selection is outside the video.", ClipSelection.error(-1, 2_000, 10_000))
        assertNull(ClipSelection.error(1_000, 4_000, 10_000))
    }

    @Test
    fun namesClipFromSourceAndStart() {
        assertEquals("holiday-clip-12s.mp4", ClipSelection.fileName("holiday.mp4", 12_400))
    }
}
