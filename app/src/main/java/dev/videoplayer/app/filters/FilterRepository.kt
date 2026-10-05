package dev.videoplayer.app.filters

import android.content.Context
import dev.videoplayer.app.blocker.BlockerEngine
import dev.videoplayer.app.blocker.CompiledFilters
import dev.videoplayer.app.blocker.CosmeticRule
import dev.videoplayer.app.blocker.FilterListParser
import dev.videoplayer.app.blocker.NetworkRule
import dev.videoplayer.app.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class FilterUpdateState(
    val running: Boolean = false,
    val message: String = "Filter lists not updated yet",
    val lastSuccessMs: Long = 0L
)

class FilterRepository(
    private val context: Context,
    private val settings: SettingsRepository,
    private val engine: BlockerEngine
) {
    private val dir = File(context.filesDir, "filters").apply { mkdirs() }
    private val _state = MutableStateFlow(FilterUpdateState())
    val state: StateFlow<FilterUpdateState> = _state.asStateFlow()

    suspend fun loadCachedOrAssets() = withContext(Dispatchers.IO) {
        publishEnabled(settings.enabledLists())
    }

    suspend fun updateRemote(force: Boolean = true): Result<Int> = withContext(Dispatchers.IO) {
        _state.value = _state.value.copy(running = true, message = "Updating filter lists…")
        var updated = 0
        var failed = 0
        val enabled = settings.enabledLists()
        FilterCatalog.sources.filter { it.url != null && it.id in enabled }.forEach { source ->
            val result = download(source.url!!)
            if (result.isSuccess) {
                File(dir, "${source.id}.txt").writeText(result.getOrThrow())
                updated++
            } else {
                failed++
            }
        }
        publishEnabled(enabled)
        val message = when {
            updated == 0 && failed > 0 -> "Update failed. Using the last cached lists."
            failed > 0 -> "Updated $updated lists. $failed could not be downloaded."
            else -> "Updated $updated lists."
        }
        _state.value = FilterUpdateState(
            running = false,
            message = message,
            lastSuccessMs = if (updated > 0) System.currentTimeMillis() else _state.value.lastSuccessMs
        )
        if (updated == 0 && failed > 0) Result.failure(IllegalStateException(message)) else Result.success(updated)
    }

    suspend fun publishEnabled(enabled: Set<String>) = withContext(Dispatchers.IO) {
        val compiled = ArrayList<CompiledFilters>()
        FilterCatalog.sources.filter { it.id in enabled }.forEach { source ->
            val text = readSource(source) ?: return@forEach
            compiled += FilterListParser.parse(text, source.id)
        }
        engine.install(merge(compiled))
    }

    private fun readSource(source: FilterSource): String? {
        val cached = File(dir, "${source.id}.txt")
        if (cached.exists() && cached.length() > 0) {
            return runCatching { cached.readText() }.getOrNull()
        }
        val asset = source.asset ?: return null
        return runCatching {
            context.assets.open(asset).bufferedReader().use { it.readText() }
        }.getOrNull()
    }

    private fun download(url: String): Result<String> = runCatching {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = 20_000
            readTimeout = 30_000
            setRequestProperty("User-Agent", "VideoPlayer/1.0 (Android filter updater)")
        }
        try {
            val code = connection.responseCode
            if (code !in 200..299) error("HTTP $code")
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun merge(parts: List<CompiledFilters>): CompiledFilters {
        if (parts.isEmpty()) return CompiledFilters.EMPTY
        val network = ArrayList<NetworkRule>()
        val domainBlocks = HashMap<String, NetworkRule>()
        val domainExceptions = HashMap<String, NetworkRule>()
        val cosmetic = ArrayList<CosmeticRule>()
        val ids = HashSet<String>()
        parts.forEach { part ->
            network += part.network
            domainBlocks.putAll(part.domainBlocks)
            domainExceptions.putAll(part.domainExceptions)
            cosmetic += part.cosmetic
            ids += part.sourceIds
        }
        return CompiledFilters(network, domainBlocks, domainExceptions, cosmetic, ids)
    }
}
