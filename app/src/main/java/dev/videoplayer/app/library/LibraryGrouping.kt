package dev.videoplayer.app.library

data class VideoFile(
    val id: Long,
    val uri: String,
    val name: String,
    val folder: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val dateAddedSec: Long
)

data class VideoFolder(
    val name: String,
    val videos: List<VideoFile>,
    val recentCount: Int
)

object LibraryGrouping {
    fun folders(videos: List<VideoFile>, nowSec: Long, recentWindowSec: Long = 7L * 24 * 60 * 60): List<VideoFolder> {
        return videos
            .groupBy { it.folder.ifBlank { "Unknown" } }
            .map { (name, items) ->
                val sorted = items.sortedByDescending { it.dateAddedSec }
                VideoFolder(
                    name = name,
                    videos = sorted,
                    recentCount = sorted.count { nowSec - it.dateAddedSec in 0..recentWindowSec }
                )
            }
            .sortedBy { it.name.lowercase() }
    }

    fun filter(folders: List<VideoFolder>, query: String): List<VideoFolder> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return folders
        return folders.mapNotNull { folder ->
            val videos = folder.videos.filter {
                it.name.lowercase().contains(q) || folder.name.lowercase().contains(q)
            }
            if (videos.isEmpty() && !folder.name.lowercase().contains(q)) null
            else folder.copy(videos = if (folder.name.lowercase().contains(q)) folder.videos else videos)
        }
    }
}
