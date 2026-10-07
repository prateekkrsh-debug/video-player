package dev.videoplayer.app.library

import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryGroupingTest {
    @Test
    fun groupsByFolderAndCountsRecent() {
        val videos = listOf(
            sample(1, "Camera", "a.mp4", 1_000),
            sample(2, "Camera", "b.mp4", 10),
            sample(3, "Download", "c.mkv", 1_000)
        )
        val folders = LibraryGrouping.folders(videos, nowSec = 1_100, recentWindowSec = 200)
        assertEquals(2, folders.size)
        assertEquals("Camera", folders[0].name)
        assertEquals(1, folders[0].recentCount)
        assertEquals(2, folders[0].videos.size)
    }

    @Test
    fun searchKeepsMatchingFolder() {
        val folders = LibraryGrouping.folders(
            listOf(sample(1, "Telegram", "note.mp4", 1)),
            nowSec = 10
        )
        val found = LibraryGrouping.filter(folders, "tele")
        assertEquals(1, found.size)
        assertEquals("Telegram", found[0].name)
    }

    private fun sample(id: Long, folder: String, name: String, added: Long) = VideoFile(
        id = id,
        uri = "content://media/external/video/media/$id",
        name = name,
        folder = folder,
        durationMs = 1_000,
        sizeBytes = 10,
        dateAddedSec = added
    )
}
