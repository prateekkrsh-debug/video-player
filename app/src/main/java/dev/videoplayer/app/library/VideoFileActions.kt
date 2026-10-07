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
