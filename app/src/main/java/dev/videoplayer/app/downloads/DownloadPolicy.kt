package dev.videoplayer.app.downloads

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.URLUtil
import android.widget.Toast

/**
 * Hands ordinary file downloads to the system DownloadManager.
 * YouTube media streams are refused: this app does not download or redistribute videos.
 */
object DownloadPolicy {
    fun handle(
        context: Context,
        url: String,
        userAgent: String,
        contentDisposition: String,
        mimeType: String
    ) {
        val host = Uri.parse(url).host.orEmpty().lowercase()
        val path = url.lowercase()
        val isYoutubeMedia = host.endsWith("googlevideo.com") ||
            path.contains("videoplayback") ||
            (host.endsWith("youtube.com") && (mimeType.startsWith("video/") || path.contains("/videoplayback")))
        if (isYoutubeMedia) {
            Toast.makeText(
                context,
                "Video downloads are not supported.",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        runCatching {
            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setMimeType(mimeType)
                addRequestHeader("User-Agent", userAgent)
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    URLUtil.guessFileName(url, contentDisposition, mimeType)
                )
                setAllowedOverMetered(true)
            }
            val manager = context.getSystemService(DownloadManager::class.java)
            manager.enqueue(request)
        }.onFailure {
            Toast.makeText(context, "Could not start download.", Toast.LENGTH_SHORT).show()
        }
    }
}
