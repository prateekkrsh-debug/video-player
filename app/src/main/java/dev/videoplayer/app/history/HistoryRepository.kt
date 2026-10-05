package dev.videoplayer.app.history

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

data class HistoryEntry(
    val id: Long,
    val url: String,
    val title: String,
    val visitedAt: Long
)

class HistoryRepository(context: Context) {
    private val db = HistoryDb(context.applicationContext)
    private val _entries = MutableStateFlow<List<HistoryEntry>>(emptyList())
    val entries: StateFlow<List<HistoryEntry>> = _entries.asStateFlow()

    suspend fun refresh() = withContext(Dispatchers.IO) {
        _entries.value = db.recent()
    }

    suspend fun record(url: String, title: String) = withContext(Dispatchers.IO) {
        if (url.isBlank() || title.isBlank()) return@withContext
        if (!url.contains("youtube.com") && !url.contains("youtu.be")) return@withContext
        db.insert(url, title)
        _entries.value = db.recent()
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        db.clear()
        _entries.value = emptyList()
    }

    suspend fun delete(id: Long) = withContext(Dispatchers.IO) {
        db.delete(id)
        _entries.value = db.recent()
    }
}

private class HistoryDb(context: Context) : SQLiteOpenHelper(context, "history.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE history (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                url TEXT NOT NULL,
                title TEXT NOT NULL,
                visited_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_history_time ON history(visited_at)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS history")
        onCreate(db)
    }

    fun insert(url: String, title: String) {
        val now = System.currentTimeMillis()
        writableDatabase.delete("history", "url = ?", arrayOf(url))
        val values = ContentValues().apply {
            put("url", url)
            put("title", title)
            put("visited_at", now)
        }
        writableDatabase.insert("history", null, values)
        writableDatabase.execSQL(
            "DELETE FROM history WHERE id NOT IN (SELECT id FROM history ORDER BY visited_at DESC LIMIT 200)"
        )
    }

    fun recent(): List<HistoryEntry> {
        val out = ArrayList<HistoryEntry>()
        readableDatabase.rawQuery(
            "SELECT id, url, title, visited_at FROM history ORDER BY visited_at DESC LIMIT 100",
            null
        ).use { c ->
            while (c.moveToNext()) {
                out += HistoryEntry(c.getLong(0), c.getString(1), c.getString(2), c.getLong(3))
            }
        }
        return out
    }

    fun clear() {
        writableDatabase.delete("history", null, null)
    }

    fun delete(id: Long) {
        writableDatabase.delete("history", "id = ?", arrayOf(id.toString()))
    }
}
