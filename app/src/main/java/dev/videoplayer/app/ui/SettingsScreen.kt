package dev.videoplayer.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize().background(colors.background).statusBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = colors.onBackground) }
            Text("Settings", color = colors.onBackground, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        Section("Theme")
        listOf("Dark", "AMOLED", "Midnight", "Ocean", "Forest", "Sunset", "Cinema", "Light", "Paper").forEach { choice ->
            Choice(choice, theme == choice, swatch(choice)) {
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
        Section("App icon")
        Text("The home screen may take a few seconds to update.", color = colors.onBackground.copy(alpha = 0.7f), fontSize = 13.sp)
        AppIdentity.icons.forEach { choice ->
            IconChoice(choice, icon == choice) { icon = choice; AppIdentity.apply(context, icon, name) }
        }
        Section("App name")
        AppIdentity.names.forEach { choice -> Choice(choice, name == choice) { name = choice; AppIdentity.apply(context, icon, name) } }
        Section("Private vault")
        Row(Modifier.fillMaxWidth().clickable(onClick = onOpenVault).padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Lock, null, tint = MaterialTheme.colorScheme.onBackground)
            Text("Private vault", color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(start = 12.dp))
        }
    }
}

@Composable
private fun Section(title: String) {
    Spacer(Modifier.height(18.dp))
    Text(title, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
}

@Composable
private fun Choice(label: String, selected: Boolean, swatch: Color? = null, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        if (swatch != null) {
            Box(Modifier.size(22.dp).clip(CircleShape).background(swatch))
            Spacer(Modifier.width(12.dp))
        }
        Text(if (selected) "● $label" else label, color = colors.onBackground)
    }
}

@Composable
private fun IconChoice(icon: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(AppIdentity.iconRes(icon)), icon, Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)))
        Spacer(Modifier.width(14.dp))
        Text(if (selected) "● ${icon.replaceFirstChar { it.uppercase() }}" else icon.replaceFirstChar { it.uppercase() }, color = colors.onBackground)
    }
}

private fun swatch(theme: String) = when (theme) {
    "AMOLED" -> Color.Black
    "Midnight" -> Color(0xFF070B16)
    "Ocean" -> Color(0xFF102433)
    "Forest" -> Color(0xFF17301C)
    "Sunset" -> Color(0xFF2A1820)
    "Cinema" -> Color(0xFF241016)
    "Light" -> Color(0xFFF7F8FA)
    "Paper" -> Color(0xFFF3EFE6)
    else -> Color(0xFF0E1116)
}

@Composable
private fun Toggle(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
        Switch(checked = value, onCheckedChange = onChange)
    }
}
