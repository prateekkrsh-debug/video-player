package dev.videoplayer.app.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.videoplayer.app.networking.SuggestClient
import dev.videoplayer.app.youtube.YoutubeUrls
import kotlinx.coroutines.delay

@Composable
fun SearchScreen(onSubmit: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    val client = remember { SuggestClient() }

    LaunchedEffect(query) {
        if (query.length < 2) {
            suggestions = emptyList()
            return@LaunchedEffect
        }
        delay(250)
        suggestions = client.suggest(query)
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Search YouTube", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            singleLine = true,
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            label = { Text("Videos, channels, playlists") },
            supportingText = { Text("Opens results in the YouTube home surface") }
        )
        LazyColumn(Modifier.padding(top = 8.dp)) {
            if (query.isNotBlank()) {
                item {
                    ListItem(
                        headlineContent = { Text("Search for “$query”") },
                        modifier = Modifier.clickable { onSubmit(YoutubeUrls.search(query)) }
                    )
                }
            }
            items(suggestions) { suggestion ->
                ListItem(
                    headlineContent = { Text(suggestion) },
                    modifier = Modifier.clickable { onSubmit(YoutubeUrls.search(suggestion)) }
                )
            }
        }
    }
}
