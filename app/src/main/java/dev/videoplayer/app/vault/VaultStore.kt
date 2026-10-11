package dev.videoplayer.app.vault

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

data class VaultItem(
    val id: String,
    val name: String,
    val path: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val folder: String
)

object VaultStore {
    private const val PREFS = "vault"

    fun items(context: Context): List<VaultItem> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("items", "[]").orEmpty()
        val array = JSONArray(raw)
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    VaultItem(
                        id = item.getString("id"),
                        name = item.getString("name"),
                        path = item.getString("path"),
                        durationMs = item.optLong("duration"),
                        sizeBytes = item.optLong("size"),
                        folder = item.optString("folder")
                    )
                )
            }
        }.filter { File(it.path).exists() }
    }

    fun add(context: Context, source: Uri, name: String, durationMs: Long, folder: String): VaultItem {
        val dir = File(context.getExternalFilesDir(null), "vault").apply { mkdirs() }
        val file = File(dir, "${System.currentTimeMillis()}-$name")
        val sourceFile = resolveFile(context, source)
        val moved = sourceFile != null && sourceFile.exists() && sourceFile.renameTo(file)
        if (!moved) {
            context.contentResolver.openInputStream(source)?.use { input ->
                file.outputStream().use { input.copyTo(it) }
            } ?: error("Could not read the video.")
            context.contentResolver.delete(source, null, null)
        } else {
            context.contentResolver.delete(source, null, null)
        }
        val item = VaultItem(file.name, name, file.absolutePath, durationMs, file.length(), folder)
        save(context, items(context) + item)
        return item
    }

    fun remove(context: Context, item: VaultItem) {
        save(context, items(context).filterNot { it.id == item.id })
        File(item.path).delete()
    }

    fun restore(context: Context, item: VaultItem): Uri {
        val source = File(item.path)
        val folder = item.folder.ifBlank { "Movies" }
        val destDir = File("/storage/emulated/0/$folder").apply { mkdirs() }
        val dest = File(destDir, item.name)
        if (source.exists() && source.renameTo(dest)) {
            android.media.MediaScannerConnection.scanFile(context, arrayOf(dest.absolutePath), arrayOf("video/*"), null)
            forget(context, item)
            return Uri.fromFile(dest)
        }
        val values = android.content.ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, item.name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, folder)
        }
        val uri = context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("Could not restore the video.")
        context.contentResolver.openOutputStream(uri)?.use { out -> source.inputStream().use { it.copyTo(out) } }
        source.delete()
        forget(context, item)
        return uri
    }

    private fun forget(context: Context, item: VaultItem) {
        save(context, items(context).filterNot { it.id == item.id })
    }

    private fun resolveFile(context: Context, uri: Uri): File? {
        return context.contentResolver.query(
            uri,
            arrayOf(MediaStore.Video.Media.DATA, MediaStore.Video.Media.RELATIVE_PATH, MediaStore.Video.Media.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return null
            val data = cursor.getColumnIndex(MediaStore.Video.Media.DATA).takeIf { it >= 0 }?.let { cursor.getString(it) }
            if (!data.isNullOrBlank()) return File(data)
            val relative = cursor.getColumnIndex(MediaStore.Video.Media.RELATIVE_PATH).takeIf { it >= 0 }?.let { cursor.getString(it) }
            val name = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME).takeIf { it >= 0 }?.let { cursor.getString(it) }
            if (!relative.isNullOrBlank() && !name.isNullOrBlank()) File("/storage/emulated/0/$relative$name") else null
        }
    }

    fun hasPassword(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).contains("password")
    fun setPassword(context: Context, password: String) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("password", hash(password)).apply()
    fun checkPassword(context: Context, password: String) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("password", "") == hash(password)
    fun fingerprint(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("fingerprint", true)
    fun setFingerprint(context: Context, enabled: Boolean) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("fingerprint", enabled).apply()

    private fun save(context: Context, items: List<VaultItem>) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(
                JSONObject()
                    .put("id", item.id)
                    .put("name", item.name)
                    .put("path", item.path)
                    .put("duration", item.durationMs)
                    .put("size", item.sizeBytes)
                    .put("folder", item.folder)
            )
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("items", array.toString()).apply()
    }

    private fun hash(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
