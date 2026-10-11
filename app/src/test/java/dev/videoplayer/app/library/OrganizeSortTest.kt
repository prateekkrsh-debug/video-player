package dev.videoplayer.app.library

import org.junit.Assert.assertEquals
import org.junit.Test

class OrganizeSortTest {
    private val videos = listOf(
        VideoFile(1, "a", "b.mp4", "Camera", 5_000, 2_000, 10, 480),
        VideoFile(2, "b", "a.mp4", "Movies", 20_000, 9_000, 30, 1080),
        VideoFile(3, "c", "c.mp4", "Camera", 10_000, 1_000, 20, 720)
    )

    @Test
    fun sortsByTitleAndDate() {
        assertEquals(listOf("a.mp4", "b.mp4", "c.mp4"), LibraryGrouping.sortVideos(videos, "title", false).map { it.name })
        assertEquals(listOf(30L, 20L, 10L), LibraryGrouping.sortVideos(videos, "date", true).map { it.dateAddedSec })
    }

    @Test
    fun sortsByLengthSizeAndResolution() {
        assertEquals(listOf(20_000L, 10_000L, 5_000L), LibraryGrouping.sortVideos(videos, "length", true).map { it.durationMs })
        assertEquals(listOf(1_000L, 2_000L, 9_000L), LibraryGrouping.sortVideos(videos, "size", false).map { it.sizeBytes })
        assertEquals(listOf(1080, 720, 480), LibraryGrouping.sortVideos(videos, "resolution", true).map { it.resolutionHeight })
    }
}
