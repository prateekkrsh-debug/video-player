package dev.videoplayer.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.videoplayer.app.BuildConfig
import dev.videoplayer.app.VideoPlayerApp
import dev.videoplayer.app.filters.FilterCatalog
import dev.videoplayer.app.youtube.YoutubeSession
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(session: YoutubeSession) {
    val app = LocalContext.current.applicationContext as VideoPlayerApp
    val settings by app.container.settings.settings.collectAsStateWithLifecycle(
        initialValue = dev.videoplayer.app.settings.AppSettings()
    )
    val blocked by app.container.engine.blocked.collectAsStateWithLifecycle()
    val recent by app.container.engine.recent.collectAsStateWithLifecycle()
    val update by app.container.filters.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var domain by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Ad blocking", style = MaterialTheme.typography.titleLarge)
        Toggle("Enable ad blocker", settings.blockerEnabled) {
            scope.launch { app.container.settings.update { s -> s.copy(blockerEnabled = it) } }
        }
        Toggle("Block trackers", settings.blockTrackers) {
            scope.launch { app.container.settings.update { s -> s.copy(blockTrackers = it) } }
        }
        Toggle("Block popups and unwanted redirects", settings.blockPopups) {
            scope.launch { app.container.settings.update { s -> s.copy(blockPopups = it) } }
        }
        Toggle("Cosmetic filtering", settings.cosmeticEnabled) {
            scope.launch { app.container.settings.update { s -> s.copy(cosmeticEnabled = it) } }
        }
        Text("$blocked requests blocked this session", modifier = Modifier.padding(vertical = 8.dp))
        Text(update.message, style = MaterialTheme.typography.bodySmall)
        Button(
            onClick = { scope.launch { app.container.filters.updateRemote() } },
            enabled = !update.running,
            modifier = Modifier.padding(top = 8.dp)
        ) { Text(if (update.running) "Updating…" else "Update filter lists") }
        TextButton(onClick = { app.container.engine.resetCounter() }) { Text("Reset block counter") }

        Text("Filter lists", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
        FilterCatalog.sources.forEach { source ->
            Toggle(source.title, source.id in settings.enabledLists) { enabled ->
                scope.launch {
                    app.container.settings.update { current ->
                        val next = current.enabledLists.toMutableSet()
                        if (enabled) next += source.id else next -= source.id
                        current.copy(enabledLists = next)
                    }
                }
            }
            Text(source.attribution, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 6.dp))
        }

        Text("Whitelist", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
        OutlinedTextField(
            value = domain,
            onValueChange = { domain = it.lowercase().trim() },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Domain exception") },
            supportingText = { Text("Requests to this domain are allowed") },
            singleLine = true
        )
        Button(
            onClick = {
                val value = domain.removePrefix("https://").removePrefix("http://").substringBefore("/")
                if (value.isNotBlank()) {
                    scope.launch {
                        app.container.settings.update { it.copy(whitelist = it.whitelist + value) }
                    }
                    domain = ""
                }
            },
            modifier = Modifier.padding(top = 8.dp)
        ) { Text("Add exception") }
        settings.whitelist.forEach { host ->
            ListItem(
                headlineContent = { Text(host) },
                trailingContent = {
                    TextButton(onClick = {
                        scope.launch { app.container.settings.update { it.copy(whitelist = it.whitelist - host) } }
                    }) { Text("Remove") }
                }
            )
        }

        HorizontalDivider(Modifier.padding(vertical = 12.dp))
        Text("Playback", style = MaterialTheme.typography.titleLarge)
        Text("Speed ${settings.playbackSpeed}x", modifier = Modifier.padding(top = 8.dp))
        RowSpeeds(settings.playbackSpeed) { speed ->
            scope.launch { app.container.settings.update { it.copy(playbackSpeed = speed) } }
            session.setPlaybackSpeed(speed)
        }
        Text("Quality preference: ${settings.qualityLabel}", modifier = Modifier.padding(top = 8.dp))
        Text(
            "The YouTube player still owns quality selection. This preference is remembered; it is not forced through a private API.",
            style = MaterialTheme.typography.bodySmall
        )
        RowSpeedsLabels(listOf("Auto", "360p", "720p", "1080p"), settings.qualityLabel) { label ->
            scope.launch { app.container.settings.update { it.copy(qualityLabel = label) } }
        }
        Toggle("Picture-in-picture", settings.pipEnabled) {
            scope.launch { app.container.settings.update { s -> s.copy(pipEnabled = it) } }
        }
        Text(
            "Leaves the video in a floating window when you switch apps, like Brave.",
            style = MaterialTheme.typography.bodySmall
        )
        Toggle("Background playback", settings.backgroundPlayback) {
            scope.launch { app.container.settings.update { s -> s.copy(backgroundPlayback = it) } }
        }
        Text(
            "Keeps YouTube's own player running with a media notification after you leave. Audio is not extracted or downloaded.",
            style = MaterialTheme.typography.bodySmall
        )
        Toggle("Landscape on fullscreen", settings.fullscreenLandscape) {
            scope.launch { app.container.settings.update { s -> s.copy(fullscreenLandscape = it) } }
        }
        Toggle("Prefer autoplay when the page allows it", settings.autoplay) {
            scope.launch { app.container.settings.update { s -> s.copy(autoplay = it) } }
        }

        HorizontalDivider(Modifier.padding(vertical = 12.dp))
        Text("Appearance", style = MaterialTheme.typography.titleLarge)
        RowSpeedsLabels(listOf("system", "light", "dark"), settings.theme) { theme ->
            scope.launch { app.container.settings.update { it.copy(theme = theme) } }
        }

        HorizontalDivider(Modifier.padding(vertical = 12.dp))
        Text("Privacy", style = MaterialTheme.typography.titleLarge)
        TextButton(onClick = { scope.launch { app.container.history.clear() } }) { Text("Clear browsing history") }
        TextButton(onClick = { session.clearBrowsingData() }) { Text("Clear cookies, site data, and cache") }

        if (recent.isNotEmpty()) {
            Text("Recent blocked requests", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
            recent.take(8).forEach { event ->
                Text("${event.host} · ${event.reason}", style = MaterialTheme.typography.bodySmall)
            }
        }

        HorizontalDivider(Modifier.padding(vertical = 12.dp))
        Text("About", style = MaterialTheme.typography.titleLarge)
        Text("Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        Text(
            "Filter lists remain the property of their authors. EasyList, EasyPrivacy, Peter Lowe, and AdGuard are fetched only when those lists are enabled.",
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            "This client does not download YouTube videos, bypass DRM, or work around sign-in and paid-content restrictions.",
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun Toggle(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onChange) }
    )
}

@Composable
private fun RowSpeeds(current: Float, onPick: (Float) -> Unit) {
    androidx.compose.foundation.layout.Row(Modifier.padding(vertical = 4.dp)) {
        listOf(0.75f, 1f, 1.25f, 1.5f, 2f).forEach { speed ->
            TextButton(onClick = { onPick(speed) }) {
                Text(if (speed == current) "${speed}x" else speed.toString())
            }
        }
    }
}

@Composable
private fun RowSpeedsLabels(values: List<String>, current: String, onPick: (String) -> Unit) {
    androidx.compose.foundation.layout.Row(Modifier.padding(vertical = 4.dp)) {
        values.forEach { value ->
            TextButton(onClick = { onPick(value) }) {
                Text(if (value == current) value.replaceFirstChar { it.uppercase() } else value)
            }
        }
    }
}
