package dev.videoplayer.app.ui

import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.util.Rational
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dev.videoplayer.app.MainActivity
import kotlinx.coroutines.launch
import dev.videoplayer.app.ui.home.HomeScreen
import dev.videoplayer.app.ui.library.LibraryScreen
import dev.videoplayer.app.ui.search.SearchScreen
import dev.videoplayer.app.ui.settings.SettingsScreen
import dev.videoplayer.app.youtube.BrowserHost
import dev.videoplayer.app.youtube.PageState
import dev.videoplayer.app.youtube.YoutubeSession

private data class Tab(val route: String, val label: String)

@Composable
fun VideoPlayerRoot(activity: MainActivity) {
    val container = activity.container
    val settings by container.settings.settings.collectAsStateWithLifecycle(
        initialValue = dev.videoplayer.app.settings.AppSettings()
    )
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route ?: "home"
    var page by remember { mutableStateOf(PageState()) }
    var customView by remember { mutableStateOf<View?>(null) }
    var customCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    val session = remember {
        YoutubeSession(
            context = activity,
            engine = container.engine,
            host = object : BrowserHost {
                override fun onPageState(state: PageState) {
                    page = state
                    if (state.positionMs > 5_000 && state.url.contains("watch")) {
                        activity.container.scope.launchResume(container, state)
                    }
                }

                override fun onShowCustomView(view: View, callback: WebChromeClient.CustomViewCallback) {
                    customCallback?.onCustomViewHidden()
                    customView = view
                    customCallback = callback
                    if (settings.fullscreenLandscape) activity.enterFullscreen(true)
                }

                override fun onHideCustomView() {
                    customView = null
                    customCallback = null
                    activity.exitFullscreen()
                }

                override fun openExternal(url: String) {
                    activity.openExternal(url)
                }
            }
        )
    }

    DisposableEffect(settings.playbackSpeed) {
        session.setPlaybackSpeed(settings.playbackSpeed)
        onDispose { }
    }

    val incoming by MainActivity.incoming.collectAsStateWithLifecycle()
    LaunchedEffect(incoming) {
        val url = incoming ?: return@LaunchedEffect
        MainActivity.incoming.value = null
        session.load(url)
    }

    BackHandler(enabled = customView != null || (route == "home" && page.canGoBack)) {
        if (customView != null) {
            customCallback?.onCustomViewHidden()
            customView = null
            activity.exitFullscreen()
        } else {
            session.goBack()
        }
    }

    val tabs = listOf(
        Tab("home", "Home"),
        Tab("search", "Search"),
        Tab("library", "Library"),
        Tab("settings", "Settings")
    )

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                if (customView == null) {
                    NavigationBar {
                        tabs.forEach { tab ->
                            NavigationBarItem(
                                selected = route == tab.route,
                                onClick = {
                                    nav.navigate(tab.route) {
                                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        when (tab.route) {
                                            "search" -> Icons.Outlined.Search
                                            "library" -> Icons.Outlined.History
                                            "settings" -> Icons.Outlined.Settings
                                            else -> Icons.Outlined.Home
                                        },
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) }
                            )
                        }
                    }
                }
            }
        ) { padding ->
            NavHost(
                navController = nav,
                startDestination = "home",
                modifier = Modifier.padding(padding)
            ) {
                composable("home") {
                    HomeScreen(
                        session = session,
                        page = page,
                        blocked = container.engine.blocked.collectAsStateWithLifecycle().value,
                        resumeTitle = settings.resumeTitle,
                        resumeUrl = settings.resumeUrl,
                        onOpen = { url ->
                            nav.navigate("home") { launchSingleTop = true }
                            session.load(url)
                        },
                        onPip = {
                            if (settings.pipEnabled) activity.enterPip()
                        }
                    )
                }
                composable("search") {
                    SearchScreen(onSubmit = { url ->
                        nav.navigate("home") { launchSingleTop = true }
                        session.load(url)
                    })
                }
                composable("library") {
                    LibraryScreen(onOpen = { url ->
                        nav.navigate("home") { launchSingleTop = true }
                        session.load(url)
                    })
                }
                composable("settings") { SettingsScreen(session) }
            }
        }

        if (customView != null) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    FrameLayout(ctx).apply {
                        addView(
                            customView,
                            FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        )
                    }
                }
            )
        }
    }
}

private fun kotlinx.coroutines.CoroutineScope.launchResume(
    container: dev.videoplayer.app.AppContainer,
    state: PageState
) {
    launch {
        container.settings.update {
            it.copy(resumeUrl = state.url, resumeTitle = state.title, resumePositionMs = state.positionMs)
        }
        container.history.record(state.url, state.title)
    }
}

fun MainActivity.enterPip() {
    val params = PictureInPictureParams.Builder()
        .setAspectRatio(Rational(16, 9))
        .build()
    enterPictureInPictureMode(params)
}

fun MainActivity.isInPip(): Boolean =
    resources.configuration?.let { false } ?: false

fun Configuration.isPipMode(): Boolean =
    (uiMode and Configuration.UI_MODE_TYPE_MASK) == Configuration.UI_MODE_TYPE_NORMAL && false
