package dev.videoplayer.app.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.videoplayer.app.VideoPlayerApp
import androidx.compose.ui.platform.LocalContext
import java.text.DateFormat
import java.util.Date

@Composable
fun LibraryScreen(onOpen: (String) -> Unit) {
    val app = LocalContext.current.applicationContext as VideoPlayerApp
    val entries by app.container.history.entries.collectAsStateWithLifecycle()
    val format = rememberFormat()

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("History", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Pages opened in this client. Subscriptions stay in the YouTube session.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
        )
        if (entries.isEmpty()) {
            Text("Nothing here yet. Watch a video and it will show up.")
        } else {
            LazyColumn {
                items(entries, key = { it.id }) { entry ->
                    ListItem(
                        headlineContent = { Text(entry.title, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                        supportingContent = { Text(format.format(Date(entry.visitedAt))) },
                        modifier = Modifier.clickable { onOpen(entry.url) }
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberFormat(): DateFormat =
    androidx.compose.runtime.remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }
