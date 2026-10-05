package dev.videoplayer.app.networking

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class SuggestClient {
    suspend fun suggest(query: String): List<String> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        runCatching {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = URL("https://suggestqueries.google.com/complete/search?client=firefox&ds=yt&q=$encoded")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8_000
                readTimeout = 8_000
                setRequestProperty("User-Agent", "VideoPlayer/1.0")
            }
            try {
                if (connection.responseCode !in 200..299) return@runCatching emptyList()
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                val array = JSONArray(body)
                if (array.length() < 2) return@runCatching emptyList()
                val suggestions = array.getJSONArray(1)
                buildList {
                    for (i in 0 until suggestions.length().coerceAtMost(8)) {
                        add(suggestions.getString(i))
                    }
                }
            } finally {
                connection.disconnect()
            }
        }.getOrDefault(emptyList())
    }
}
