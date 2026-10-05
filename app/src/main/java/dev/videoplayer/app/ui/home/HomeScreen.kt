package dev.videoplayer.app.ui.home

import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PictureInPicture
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import dev.videoplayer.app.youtube.PageState
import dev.videoplayer.app.youtube.YoutubeSession
import dev.videoplayer.app.youtube.YoutubeUrls

@Composable
fun HomeScreen(
    session: YoutubeSession,
    page: PageState,
    blocked: Long,
    resumeTitle: String,
    resumeUrl: String,
    onOpen: (String) -> Unit,
    onPip: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Surface(tonalElevation = 2.dp) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = session::goBack, enabled = page.canGoBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                    IconButton(onClick = session::goForward, enabled = page.canGoForward) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = "Forward")
                    }
                    IconButton(onClick = session::home) {
                        Icon(Icons.Outlined.Home, contentDescription = "YouTube home")
                    }
                    IconButton(onClick = session::reload) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Reload")
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            page.title.ifBlank { "YouTube" },
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "$blocked blocked",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onPip) {
                        Icon(Icons.Outlined.PictureInPicture, contentDescription = "Picture in picture")
                    }
                }
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(onClick = { onOpen(YoutubeUrls.HOME) }, label = { Text("Home") })
                    AssistChip(onClick = { onOpen(YoutubeUrls.SUBSCRIPTIONS) }, label = { Text("Subscriptions") })
                    AssistChip(onClick = { onOpen(YoutubeUrls.TRENDING) }, label = { Text("Trending") })
                    AssistChip(onClick = { onOpen(YoutubeUrls.LIBRARY) }, label = { Text("YouTube library") })
                }
                if (resumeTitle.isNotBlank() && resumeUrl.isNotBlank()) {
                    TextButton(onClick = { onOpen(resumeUrl) }) {
                        Text("Resume · $resumeTitle", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        if (page.loading) {
            LinearProgressIndicator(progress = { page.progress / 100f }, modifier = Modifier.fillMaxWidth())
        }
        BoxBrowser(session, page, onOpen)
    }
}

@Composable
private fun BoxBrowser(session: YoutubeSession, page: PageState, onOpen: (String) -> Unit) {
    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                session.webView.also { web ->
                    (web.parent as? ViewGroup)?.removeView(web)
                    if (web.url.isNullOrBlank()) web.loadUrl(YoutubeUrls.HOME)
                }
            },
            update = { }
        )
        if (page.error != null) {
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text("YouTube unavailable", style = MaterialTheme.typography.headlineSmall)
                Text(page.error, modifier = Modifier.padding(top = 8.dp))
                TextButton(onClick = { onOpen(page.url.ifBlank { YoutubeUrls.HOME }) }) {
                    Text("Retry")
                }
            }
        }
    }
}
