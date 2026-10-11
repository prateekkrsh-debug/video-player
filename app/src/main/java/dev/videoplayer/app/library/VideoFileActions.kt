package dev.videoplayer.app.library

import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore

object VideoFileActions {
    fun rename(context: Context, video: VideoFile, newName: String): IntentSender? {
        val values = ContentValues().apply { put(MediaStore.Video.Media.DISPLAY_NAME, newName) }
        return runWrite(context, video) {
            context.contentResolver.update(Uri.parse(video.uri), values, null, null)
        }
    }

    fun delete(context: Context, video: VideoFile): IntentSender? {
        if (Build.VERSION.SDK_INT >= 30) {
            val pending = MediaStore.createDeleteRequest(context.contentResolver, listOf(Uri.parse(video.uri)))
            return pending.intentSender
        }
        return runWrite(context, video) {
            context.contentResolver.delete(Uri.parse(video.uri), null, null)
        }
    }

    fun copy(context: Context, video: VideoFile, relativePath: String): Boolean {
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, video.name)
            put(MediaStore.Video.Media.MIME_TYPE, mimeFor(video.name))
            put(MediaStore.Video.Media.RELATIVE_PATH, relativePath(relativePath))
        }
        val target = context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: return false
        val copied = runCatching {
            context.contentResolver.openInputStream(Uri.parse(video.uri))?.use { input ->
                context.contentResolver.openOutputStream(target)?.use { output -> input.copyTo(output) }
            } != null
        }.getOrDefault(false)
        if (!copied) {
            runCatching { context.contentResolver.delete(target, null, null) }
            return false
        }
        return true
    }

    fun move(context: Context, video: VideoFile, relativePath: String): IntentSender? {
        if (!copy(context, video, relativePath)) throw IllegalStateException("Could not move the video.")
        return delete(context, video)
    }

    private fun relativePath(folder: String): String {
        val cleaned = folder.trim().trim('/')
        val path = when {
            cleaned.isBlank() || cleaned.equals("Unknown", true) -> "Movies"
            cleaned.contains("/") -> cleaned
            cleaned.equals("Camera", true) -> "DCIM/Camera"
            cleaned.equals("Download", true) || cleaned.equals("Downloads", true) -> "Download"
            cleaned.equals("Movies", true) -> "Movies"
            else -> "Movies/$cleaned"
        }
        return if (path.endsWith("/")) path else "$path/"
    }

    private fun mimeFor(name: String) = when (name.substringAfterLast('.', "").lowercase()) {
        "webm" -> "video/webm"
        "mkv" -> "video/x-matroska"
        "mov" -> "video/quicktime"
        "3gp" -> "video/3gpp"
        else -> "video/mp4"
    }

    private fun runWrite(context: Context, video: VideoFile, block: () -> Unit): IntentSender? {
        return try {
            block()
            null
        } catch (error: SecurityException) {
            val recoverable = error as? RecoverableSecurityException
                ?: (error.cause as? RecoverableSecurityException)
            if (Build.VERSION.SDK_INT >= 29 && recoverable != null) recoverable.userAction.actionIntent.intentSender
            else if (context is Activity) null
            else throw error
        }
    }
}
