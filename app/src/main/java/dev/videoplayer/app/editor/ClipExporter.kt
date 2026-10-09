package dev.videoplayer.app.editor

import android.content.ContentValues
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.media3.common.MediaItem
import androidx.media3.effect.Presentation
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class ClipExport(
    val uri: Uri,
    val name: String,
    val bytes: Long,
    val durationMs: Long,
    val optimized: Boolean,
    val tookMs: Long
)

object ClipExporter {
    suspend fun export(
        context: Context,
        source: Uri,
        displayName: String,
        startMs: Long,
        endMs: Long,
        height: Int? = null,
        relativePath: String = "Movies/Clips",
        onProgress: (Int) -> Unit = {}
    ): ClipExport {
        val error = ClipSelection.error(startMs, endMs, Long.MAX_VALUE)
        if (error != null && startMs >= endMs) throw IllegalArgumentException(error)
        val started = System.currentTimeMillis()
        val clipped = MediaItem.Builder()
            .setUri(source)
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(startMs)
                    .setEndPositionMs(endMs)
                    .build()
            )
            .build()
        val builder = EditedMediaItem.Builder(clipped)
        if (height != null) {
            builder.setEffects(Effects(emptyList(), listOf(Presentation.createForHeight(height))))
        }
        val edited = builder.build()
        val temp = File(context.cacheDir, "clip-${System.currentTimeMillis()}.mp4")
        var optimized = false
        try {
            suspendCancellableCoroutine { cont ->
                val transformer = Transformer.Builder(context)
                    .experimentalSetTrimOptimizationEnabled(height == null)
                    .addListener(object : Transformer.Listener {
                        override fun onCompleted(composition: androidx.media3.transformer.Composition, exportResult: ExportResult) {
                            optimized = exportResult.optimizationResult == ExportResult.OPTIMIZATION_SUCCEEDED
                            if (cont.isActive) cont.resume(Unit)
                        }

                        override fun onError(
                            composition: androidx.media3.transformer.Composition,
                            exportResult: ExportResult,
                            exportException: androidx.media3.transformer.ExportException
                        ) {
                            if (cont.isActive) cont.resumeWithException(exportException)
                        }
                    })
                    .build()
                cont.invokeOnCancellation { transformer.cancel() }
                transformer.start(edited, temp.absolutePath)
            }
            onProgress(100)
        } catch (error: Exception) {
            temp.delete()
            throw error
        }
        if (!temp.exists() || temp.length() < 1024) {
            temp.delete()
            throw IllegalStateException("Export did not produce a playable clip.")
        }
        val bytes = temp.length()
        val duration = readDuration(temp)
        if (duration in 1 until (endMs - startMs) / 4) {
            temp.delete()
            throw IllegalStateException("Exported clip duration does not match the selection.")
        }
        val uri = publish(context, temp, displayName, relativePath)
        return ClipExport(uri, displayName, bytes, duration, optimized, System.currentTimeMillis() - started)
    }

    private fun readDuration(file: File): Long {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            val value = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            retriever.release()
            value
        } catch (_: Exception) {
            0L
        }
    }

    private fun querySize(context: Context, uri: Uri): Long {
        return context.contentResolver.query(uri, arrayOf(MediaStore.Video.Media.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getLong(0) else 0L
        } ?: 0L
    }

    private fun publish(context: Context, file: File, displayName: String, relativePath: String): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, relativePath.ifBlank { "Movies/Clips" })
            if (Build.VERSION.SDK_INT >= 29) put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("Could not create the clip.")
        try {
            resolver.openOutputStream(uri)?.use { out -> file.inputStream().use { it.copyTo(out) } }
                ?: error("Could not write the clip.")
            if (Build.VERSION.SDK_INT >= 29) {
                values.clear()
                values.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
        } catch (error: Exception) {
            resolver.delete(uri, null, null)
            throw error
        } finally {
            file.delete()
        }
        return uri
    }
}
