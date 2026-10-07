package dev.videoplayer.app.ui

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.videoplayer.app.library.LibraryGrouping
import dev.videoplayer.app.library.VideoFile
import dev.videoplayer.app.library.VideoFileActions
import dev.videoplayer.app.library.VideoFolder
import dev.videoplayer.app.library.VideoLibrary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Ink = Color(0xFF000000)
private val Card = Color(0xFF2C313A)
private val Muted = Color(0xFFB7BDC7)
private val Badge = Color(0xFFE53935)
private val PlayBlue = Color(0xFF1E88E5)

@Composable
fun LibraryRoot(onPlay: (List<VideoFile>, Int) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var videos by remember { mutableStateOf<List<VideoFile>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("") }
    var searching by remember { mutableStateOf(false) }
    var sortByCount by remember { mutableStateOf(false) }
    var openFolder by remember { mutableStateOf<VideoFolder?>(null) }
    var pendingConsent by remember { mutableStateOf<(() -> Unit)?>(null) }

    val consent = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        pendingConsent?.invoke()
        pendingConsent = null
    }

    fun reload() {
        scope.launch {
            loading = true
            videos = withContext(Dispatchers.IO) { VideoLibrary.scan(context) }
            loading = false
        }
    }
    LaunchedEffect(Unit) { reload() }

    val folders = LibraryGrouping.folders(videos, System.currentTimeMillis() / 1000)
        .let { LibraryGrouping.filter(it, query) }
        .let { if (sortByCount) it.sortedByDescending { folder -> folder.videos.size } else it }

    if (openFolder != null) {
        FolderVideosScreen(
            folder = folders.firstOrNull { it.name == openFolder?.name } ?: openFolder!!,
            onBack = { openFolder = null },
            onPlay = { index -> onPlay(openFolder!!.videos, index) },
            onRename = { video, name ->
                scope.launch {
                    val sender = withContext(Dispatchers.IO) { VideoFileActions.rename(context, video, name) }
                    if (sender != null) {
                        pendingConsent = { reload() }
                        consent.launch(IntentSenderRequest.Builder(sender).build())
                    } else reload()
                }
            },
            onDelete = { video ->
                scope.launch {
                    val sender = withContext(Dispatchers.IO) { VideoFileActions.delete(context, video) }
                    if (sender != null) {
                        pendingConsent = { reload() }
                        consent.launch(IntentSenderRequest.Builder(sender).build())
                    } else reload()
                }
            }
        )
        return
    }

    Scaffold(
        containerColor = Ink,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val latest = videos.maxByOrNull { it.dateAddedSec } ?: return@FloatingActionButton
                    onPlay(listOf(latest), 0)
                },
                containerColor = PlayBlue,
                shape = CircleShape
            ) { Icon(Icons.Default.PlayArrow, "Play latest", tint = Color.White) }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).background(Ink)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Folders", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = { searching = !searching }) { Icon(Icons.Default.Search, "Search", tint = Color.White) }
                IconButton(onClick = { sortByCount = !sortByCount }) { Icon(Icons.Default.SortByAlpha, "Sort", tint = Color.White) }
                Icon(Icons.Default.Person, "Library", tint = Color.White, modifier = Modifier.padding(8.dp).size(28.dp).clip(CircleShape).background(PlayBlue).padding(4.dp))
            }
            if (searching) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    placeholder = { Text("Search folders and videos") },
                    singleLine = true
                )
            }
            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = PlayBlue) }
            } else if (folders.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No videos found", color = Muted)
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                    items(folders, key = { it.name }) { folder ->
                        FolderRow(folder) { openFolder = folder }
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderRow(folder: VideoFolder, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            Box(
                Modifier.size(72.dp).clip(RoundedCornerShape(8.dp)).background(Card),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (folder.name.contains("Movie", true)) Icons.Default.Movie else Icons.Default.Folder,
                    null,
                    tint = Color(0xFF8E96A3),
                    modifier = Modifier.size(34.dp)
                )
            }
            if (folder.recentCount > 0) {
                Box(
                    Modifier.align(Alignment.TopEnd).clip(CircleShape).background(Badge).padding(horizontal = 6.dp, vertical = 2.dp)
                ) { Text(folder.recentCount.toString(), color = Color.White, fontSize = 12.sp) }
            }
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(folder.name, color = Color.White, fontSize = 18.sp)
            Text("${folder.videos.size} videos", color = Muted, fontSize = 14.sp)
        }
    }
}

@Composable
private fun FolderVideosScreen(
    folder: VideoFolder,
    onBack: () -> Unit,
    onPlay: (Int) -> Unit,
    onRename: (VideoFile, String) -> Unit,
    onDelete: (VideoFile) -> Unit
) {
    var menuFor by remember { mutableStateOf<VideoFile?>(null) }
    var renameTarget by remember { mutableStateOf<VideoFile?>(null) }
    var renameText by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().background(Ink)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
            TextButton(onClick = onBack) { Text("Folders", color = Color.White) }
            Text(folder.name, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        }
        LazyColumn {
            items(folder.videos.size) { index ->
                val video = folder.videos[index]
                Row(
                    Modifier.fillMaxWidth().clickable { onPlay(index) }.padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(video.name, color = Color.White)
                        Text(formatDuration(video.durationMs), color = Muted, fontSize = 13.sp)
                    }
                    Box {
                        IconButton(onClick = { menuFor = video }) { Icon(Icons.Default.MoreVert, "More", tint = Color.White) }
                        DropdownMenu(expanded = menuFor == video, onDismissRequest = { menuFor = null }) {
                            DropdownMenuItem(
                                text = { Text("Rename") },
                                leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, null) },
                                onClick = {
                                    menuFor = null
                                    renameTarget = video
                                    renameText = video.name
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete") },
                                leadingIcon = { Icon(Icons.Default.Delete, null) },
                                onClick = {
                                    menuFor = null
                                    onDelete(video)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    if (renameTarget != null) {
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Rename video") },
            text = { OutlinedTextField(renameText, { renameText = it }, singleLine = true) },
            confirmButton = {
                TextButton(onClick = {
                    val target = renameTarget ?: return@TextButton
                    if (renameText.isNotBlank()) onRename(target, renameText.trim())
                    renameTarget = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { renameTarget = null }) { Text("Cancel") } }
        )
    }
}

fun formatDuration(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    return "%02d:%02d".format(total / 60, total % 60)
}

fun shareVideo(activity: Activity, uri: String, name: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "video/*"
        putExtra(Intent.EXTRA_STREAM, Uri.parse(uri))
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    activity.startActivity(Intent.createChooser(send, name))
}

fun toast(context: android.content.Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}
