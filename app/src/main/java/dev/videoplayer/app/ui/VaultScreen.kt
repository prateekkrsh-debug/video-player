package dev.videoplayer.app.ui

import android.net.Uri
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import dev.videoplayer.app.library.VideoFile
import dev.videoplayer.app.vault.VaultItem
import dev.videoplayer.app.vault.VaultStore

@Composable
fun VaultScreen(onBack: () -> Unit, onPlay: (List<VideoFile>, Int) -> Unit) {
    val context = LocalContext.current
    var hasPassword by remember { mutableStateOf(VaultStore.hasPassword(context)) }
    var unlocked by remember { mutableStateOf(!VaultStore.hasPassword(context)) }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var fingerprint by remember { mutableStateOf(VaultStore.fingerprint(context)) }
    var items by remember { mutableStateOf(VaultStore.items(context)) }

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
        } else {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Enable fingerprint", color = Color.White, modifier = Modifier.weight(1f))
                Switch(checked = fingerprint, onCheckedChange = {
                    fingerprint = it
                    VaultStore.setFingerprint(context, it)
                })
            }
            if (items.isEmpty()) {
                Text("No videos in the vault", color = Color(0xFFB7BDC7), modifier = Modifier.padding(16.dp))
            }
            LazyColumn {
                items(items, key = { it.id }) { item ->
                    Row(Modifier.fillMaxWidth().clickable {
                        onPlay(items.map { it.toVideo() }, items.indexOf(item))
                    }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(item.name, color = Color.White)
                            Text(formatSize(item.sizeBytes), color = Color(0xFFB7BDC7), fontSize = 13.sp)
                        }
                        TextButton(onClick = {
                            VaultStore.restore(context, item)
                            items = VaultStore.items(context)
                            toast(context, "Restored to ${item.folder.ifBlank { "Movies" }}")
                        }) { Text("Restore") }
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
