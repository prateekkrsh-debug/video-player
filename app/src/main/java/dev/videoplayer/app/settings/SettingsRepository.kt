package dev.videoplayer.app.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dev.videoplayer.app.filters.FilterCatalog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "video_player_settings")

data class AppSettings(
    val blockerEnabled: Boolean = true,
    val blockTrackers: Boolean = true,
    val blockPopups: Boolean = true,
    val cosmeticEnabled: Boolean = true,
    val pipEnabled: Boolean = true,
    val autoplay: Boolean = false,
    val theme: String = "system",
    val playbackSpeed: Float = 1f,
    val qualityLabel: String = "Auto",
    val fullscreenLandscape: Boolean = true,
    val enabledLists: Set<String> = FilterCatalog.sources.filter { it.enabledByDefault }.map { it.id }.toSet(),
    val whitelist: Set<String> = emptySet(),
    val resumeUrl: String = "",
    val resumeTitle: String = "",
    val resumePositionMs: Long = 0L
)

class SettingsRepository(private val context: Context) {
    private object Keys {
        val blocker = booleanPreferencesKey("blocker_enabled")
        val trackers = booleanPreferencesKey("block_trackers")
        val popups = booleanPreferencesKey("block_popups")
        val cosmetic = booleanPreferencesKey("cosmetic")
        val pip = booleanPreferencesKey("pip")
        val autoplay = booleanPreferencesKey("autoplay")
        val theme = stringPreferencesKey("theme")
        val speed = floatPreferencesKey("speed")
        val quality = stringPreferencesKey("quality")
        val landscape = booleanPreferencesKey("fullscreen_landscape")
        val lists = stringSetPreferencesKey("enabled_lists")
        val whitelist = stringSetPreferencesKey("whitelist")
        val resumeUrl = stringPreferencesKey("resume_url")
        val resumeTitle = stringPreferencesKey("resume_title")
        val resumePos = longPreferencesKey("resume_pos")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs -> prefs.toSettings() }

    suspend fun current(): AppSettings = settings.first()

    suspend fun enabledLists(): Set<String> = current().enabledLists

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.dataStore.edit { prefs ->
            val next = transform(prefs.toSettings())
            prefs[Keys.blocker] = next.blockerEnabled
            prefs[Keys.trackers] = next.blockTrackers
            prefs[Keys.popups] = next.blockPopups
            prefs[Keys.cosmetic] = next.cosmeticEnabled
            prefs[Keys.pip] = next.pipEnabled
            prefs[Keys.autoplay] = next.autoplay
            prefs[Keys.theme] = next.theme
            prefs[Keys.speed] = next.playbackSpeed
            prefs[Keys.quality] = next.qualityLabel
            prefs[Keys.landscape] = next.fullscreenLandscape
            prefs[Keys.lists] = next.enabledLists
            prefs[Keys.whitelist] = next.whitelist
            prefs[Keys.resumeUrl] = next.resumeUrl
            prefs[Keys.resumeTitle] = next.resumeTitle
            prefs[Keys.resumePos] = next.resumePositionMs
        }
    }

    private fun Preferences.toSettings(): AppSettings {
        val defaultLists = FilterCatalog.sources.filter { it.enabledByDefault }.map { it.id }.toSet()
        return AppSettings(
            blockerEnabled = this[Keys.blocker] ?: true,
            blockTrackers = this[Keys.trackers] ?: true,
            blockPopups = this[Keys.popups] ?: true,
            cosmeticEnabled = this[Keys.cosmetic] ?: true,
            pipEnabled = this[Keys.pip] ?: true,
            autoplay = this[Keys.autoplay] ?: false,
            theme = this[Keys.theme] ?: "system",
            playbackSpeed = this[Keys.speed] ?: 1f,
            qualityLabel = this[Keys.quality] ?: "Auto",
            fullscreenLandscape = this[Keys.landscape] ?: true,
            enabledLists = this[Keys.lists] ?: defaultLists,
            whitelist = this[Keys.whitelist] ?: emptySet(),
            resumeUrl = this[Keys.resumeUrl] ?: "",
            resumeTitle = this[Keys.resumeTitle] ?: "",
            resumePositionMs = this[Keys.resumePos] ?: 0L
        )
    }
}
