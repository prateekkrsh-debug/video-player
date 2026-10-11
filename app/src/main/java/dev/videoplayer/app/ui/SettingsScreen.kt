package dev.videoplayer.app.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import dev.videoplayer.app.player.GesturePrefs
import dev.videoplayer.app.settings.AppIdentity
import dev.videoplayer.app.settings.ThemePrefs

@Composable
fun SettingsScreen(onBack: () -> Unit, onTheme: (String) -> Unit, onOpenVault: () -> Unit) {
    val context = LocalContext.current
    var seek by remember { mutableStateOf(GesturePrefs.enabled(context)) }
    var overlay by remember { mutableStateOf(GesturePrefs.overlay(context)) }
    var sensitivity by remember { mutableStateOf(GesturePrefs.msPerScreen(context)) }
    var theme by remember { mutableStateOf(ThemePrefs.current(context)) }
    val identity = remember { AppIdentity.current(context) }
    var icon by remember { mutableStateOf(identity.first) }
    var name by remember { mutableStateOf(identity.second) }
    var customName by remember { mutableStateOf(AppIdentity.customName(context)) }
    Column(Modifier.fillMaxSize().background(Color.Black).statusBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }
            Text("Settings", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        Section("Theme")
        listOf("Dark", "AMOLED", "Midnight", "Ocean", "Forest", "Sunset", "Light", "Paper").forEach { choice ->
            Choice(choice, theme == choice) {
                theme = choice
                ThemePrefs.set(context, choice)
                onTheme(choice)
            }
        }
        Section("Gesture controls")
        Toggle("Swipe to seek", seek) { seek = it; GesturePrefs.setEnabled(context, it) }
        Toggle("Seek overlay", overlay) { overlay = it; GesturePrefs.setOverlay(context, it) }
        Choice("Seek ${sensitivity / 1000}s per swipe", false) {
            sensitivity = when (sensitivity) { 45_000L -> 90_000L; 90_000L -> 150_000L; else -> 45_000L }
            GesturePrefs.setMsPerScreen(context, sensitivity)
        }
        Section("App icon and name")
        Text("Android can only switch among names built into the app. The home screen may take a few seconds to update.", color = Color(0xFFB7BDC7), fontSize = 13.sp)
        AppIdentity.icons.forEach { choice -> Choice("Icon: $choice", icon == choice) { icon = choice; AppIdentity.apply(context, icon, name) } }
        AppIdentity.names.forEach { choice -> Choice(choice, name == choice) { name = choice; AppIdentity.apply(context, icon, name) } }
        OutlinedTextField(customName, { customName = it }, label = { Text("Custom app name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        TextButton(onClick = {
            AppIdentity.setCustomName(context, customName)
            toast(context, "Name saved in the app and recent apps. The home screen icon can only use the four built-in names.")
        }) { Text("Save typed name") }
        Text("Android does not allow an app to set a free-typed name on the home screen icon. The typed name is used inside the app and in recent apps.", color = Color(0xFFB7BDC7), fontSize = 13.sp)
        Section("Private vault")
        Row(Modifier.fillMaxWidth().clickable(onClick = onOpenVault).padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Lock, null, tint = Color.White)
            Text("Private vault", color = Color.White, modifier = Modifier.padding(start = 12.dp))
        }
    }
}

@Composable
private fun Section(title: String) {
    Spacer(Modifier.height(18.dp))
    Text(title, color = Color(0xFF1E88E5), fontWeight = FontWeight.Bold)
}

@Composable
private fun Choice(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(if (selected) "● $label" else label, color = Color.White, modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp))
}

@Composable
private fun Toggle(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White, modifier = Modifier.weight(1f))
        Switch(checked = value, onCheckedChange = onChange)
    }
}
