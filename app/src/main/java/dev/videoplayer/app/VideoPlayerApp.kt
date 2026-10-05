package dev.videoplayer.app

import android.app.Application
import dev.videoplayer.app.blocker.BlockerEngine
import dev.videoplayer.app.filters.FilterRepository
import dev.videoplayer.app.history.HistoryRepository
import dev.videoplayer.app.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class VideoPlayerApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.start()
    }
}

class AppContainer(val app: Application) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val engine = BlockerEngine()
    val settings = SettingsRepository(app)
    val filters = FilterRepository(app, settings, engine)
    val history = HistoryRepository(app)

    fun start() {
        scope.launch {
            settings.settings
                .distinctUntilChanged { a, b ->
                    a.blockerEnabled == b.blockerEnabled &&
                        a.blockTrackers == b.blockTrackers &&
                        a.blockPopups == b.blockPopups &&
                        a.cosmeticEnabled == b.cosmeticEnabled &&
                        a.whitelist == b.whitelist &&
                        a.enabledLists == b.enabledLists
                }
                .collect { value ->
                    engine.enabled = value.blockerEnabled
                    engine.trackersEnabled = value.blockTrackers
                    engine.popupsEnabled = value.blockPopups
                    engine.cosmeticEnabled = value.cosmeticEnabled
                    engine.whitelist = value.whitelist
                    filters.publishEnabled(value.enabledLists)
                }
        }
        scope.launch(Dispatchers.IO) {
            history.refresh()
            filters.loadCachedOrAssets()
            filters.updateRemote()
        }
    }
}
