package dev.videoplayer.app.ui

import androidx.activity.compose.BackHandler
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dev.videoplayer.app.library.LibraryGrouping
import dev.videoplayer.app.library.VideoFile
import dev.videoplayer.app.library.VideoFolder
import dev.videoplayer.app.library.VideoLibrary
import dev.videoplayer.app.vault.VaultItem
import dev.videoplayer.app.vault.VaultStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun VaultScreen(onBack: () -> Unit, onPlay: (List<VideoFile>, Int) -> Unit) {
    val context = LocalContext.current
    var hasPassword by remember { mutableStateOf(VaultStore.hasPassword(context)) }
    var unlocked by remember { mutableStateOf(!VaultStore.hasPassword(context)) }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var fingerprint by remember { mutableStateOf(VaultStore.fingerprint(context)) }
    var items by remember { mutableStateOf(VaultStore.items(context)) }
    var picking by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(setOf<String>()) }
    val scope = rememberCoroutineScope()
    BackHandler(enabled = picking) { picking = false }

    LaunchedEffect(hasPassword) {
        val activity = context as? FragmentActivity ?: return@LaunchedEffect
        if (hasPassword && !unlocked && VaultStore.fingerprint(context) &&
            BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS
        ) {
            BiometricPrompt(activity, ContextCompat.getMainExecutor(context), object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    unlocked = true
                }
            }).authenticate(BiometricPrompt.PromptInfo.Builder().setTitle("Open vault").setNegativeButtonText("Use password").build())
        }
    }

    Column(Modifier.fillMaxSize().background(Color.Black).statusBarsPadding()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }
            Text("Vault", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        if (!hasPassword) {
            Column(Modifier.padding(16.dp)) {
                Text("Set a vault password", color = Color.White, fontSize = 18.sp)
                OutlinedTextField(password, { password = it }, label = { Text("Password") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
                OutlinedTextField(confirm, { confirm = it }, label = { Text("Confirm password") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                TextButton(onClick = {
                    when {
                        password.length < 4 -> toast(context, "Use at least 4 characters")
                        password != confirm -> toast(context, "Passwords do not match")
                        else -> {
                            VaultStore.setPassword(context, password)
                            hasPassword = true
                            unlocked = true
                            toast(context, "Vault unlocked")
                        }
                    }
                }) { Text("Save and open") }
            }
        } else if (!unlocked) {
            Column(Modifier.padding(16.dp)) {
                OutlinedTextField(password, { password = it }, label = { Text("Vault password") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                TextButton(onClick = {
                    if (VaultStore.checkPassword(context, password)) unlocked = true else toast(context, "Wrong password")
                }) { Text("Unlock") }
            }
        } else if (picking) {
            VaultPicker(
                onBack = { picking = false },
                onAdd = { video ->
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            VaultStore.add(context, Uri.parse(video.uri), video.name, video.durationMs, video.folder)
                        }
                        items = VaultStore.items(context)
                        toast(context, "Moved to vault")
                    }
                }
            )
        } else {
            Scaffold(
                containerColor = Color.Black,
                floatingActionButton = {
                    FloatingActionButton(onClick = { picking = true }, containerColor = Color(0xFF1E88E5), shape = CircleShape) {
                        Icon(Icons.Default.Add, "Add", tint = Color.White)
                    }
                }
            ) { padding ->
                Column(Modifier.fillMaxSize().padding(padding)) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Enable fingerprint", color = Color.White, modifier = Modifier.weight(1f))
                        Switch(checked = fingerprint, onCheckedChange = {
                            fingerprint = it
                            VaultStore.setFingerprint(context, it)
                        })
                    }
                    if (selected.isNotEmpty()) {
                        TextButton(onClick = {
                            scope.launch {
                                val chosen = items.filter { it.id in selected }
                                withContext(Dispatchers.IO) { chosen.forEach { VaultStore.restore(context, it) } }
                                items = VaultStore.items(context)
                                selected = emptySet()
                                toast(context, "Restored ${chosen.size} video${if (chosen.size == 1) "" else "s"}")
                            }
                        }) { Text("Restore ${selected.size}") }
                    }
                    if (items.isEmpty()) {
                        Text("No videos in the vault", color = Color(0xFFB7BDC7), modifier = Modifier.padding(16.dp))
                    }
                    LazyColumn {
                        items(items, key = { it.id }) { item ->
                            val checked = item.id in selected
                            Row(Modifier.fillMaxWidth().clickable {
                                if (selected.isEmpty()) onPlay(items.map { it.toVideo() }, items.indexOf(item))
                                else selected = if (checked) selected - item.id else selected + item.id
                            }.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                VideoThumb(item.toVideo(), false)
                                Spacer(Modifier.width(12.dp))
                                Text(item.name, color = Color.White, modifier = Modifier.weight(1f))
                                Checkbox(checked = checked, onCheckedChange = {
                                    selected = if (it) selected + item.id else selected - item.id
                                })
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun VaultItem.toVideo() = VideoFile(
    id = path.hashCode().toLong(),
    uri = Uri.fromFile(java.io.File(path)).toString(),
    name = name,
    folder = "Vault",
    durationMs = durationMs,
    sizeBytes = sizeBytes,
    dateAddedSec = 0
)

@Composable
private fun VaultPicker(onBack: () -> Unit, onAdd: (VideoFile) -> Unit) {
    val context = LocalContext.current
    var videos by remember { mutableStateOf<List<VideoFile>>(emptyList()) }
    var folder by remember { mutableStateOf<VideoFolder?>(null) }
    LaunchedEffect(Unit) {
        videos = withContext(Dispatchers.IO) { VideoLibrary.scan(context) }
    }
    val folders = LibraryGrouping.folders(videos, System.currentTimeMillis() / 1000)
    BackHandler {
        if (folder != null) folder = null else onBack()
    }
    Column(Modifier.fillMaxSize().background(Color.Black)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (folder != null) folder = null else onBack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
            }
            Text(folder?.name ?: "Add to vault", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        if (folder == null) {
            LazyColumn {
                items(folders, key = { it.name }) { item ->
                    FolderRow(item) { folder = item }
                }
            }
        } else {
            LazyColumn {
                items(folder!!.videos, key = { it.uri }) { video ->
                    Row(Modifier.fillMaxWidth().clickable { onAdd(video); folder = folder!!.copy(videos = folder!!.videos.filterNot { it.uri == video.uri }) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        VideoThumb(video, false)
                        Spacer(Modifier.width(12.dp))
                        Text(video.name, color = Color.White, modifier = Modifier.weight(1f))
                        Icon(Icons.Default.Add, "Add", tint = Color.White)
                    }
                }
            }
        }
    }
}
