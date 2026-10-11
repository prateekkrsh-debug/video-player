package dev.videoplayer.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun OrganizeDialog(
    view: String,
    layout: String,
    sort: String,
    newest: Boolean,
    onDismiss: () -> Unit,
    onDone: (String, String, String, Boolean) -> Unit
) {
    var viewMode by remember { mutableStateOf(view) }
    var layoutMode by remember { mutableStateOf(layout) }
    var sortKey by remember { mutableStateOf(sort) }
    var newestFirst by remember { mutableStateOf(newest) }
    val accent = Color(0xFF1E88E5)
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFF2A2D33)) {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    Text("View Mode", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("Layout", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Mode("All folders", Icons.Default.Folder, viewMode == "folders", accent) { viewMode = "folders" }
                    Mode("Files", Icons.Default.InsertDriveFile, viewMode == "files", accent) { viewMode = "files" }
                    Mode("List", Icons.Default.ViewList, layoutMode == "list", accent) { layoutMode = "list" }
                    Mode("Grid", Icons.Default.GridView, layoutMode == "grid", accent) { layoutMode = "grid" }
                }
                Text("Sort", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 18.dp, bottom = 10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Mode("Title", Icons.Default.SortByAlpha, sortKey == "title", accent) { sortKey = "title" }
                    Mode("Date", Icons.Default.CalendarMonth, sortKey == "date", accent) { sortKey = "date" }
                    Mode("Length", Icons.Default.Timer, sortKey == "length", accent) { sortKey = "length" }
                    Mode("Size", Icons.Default.SdStorage, sortKey == "size", accent) { sortKey = "size" }
                    Mode("Resolution", Icons.Default.HighQuality, sortKey == "resolution", accent) { sortKey = "resolution" }
                }
                Row(Modifier.fillMaxWidth().padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OrderButton("↑  Oldest", !newestFirst, accent, Modifier.weight(1f)) { newestFirst = false }
                    OrderButton("↓  Newest", newestFirst, accent, Modifier.weight(1f)) { newestFirst = true }
                }
                Row(Modifier.fillMaxWidth().padding(top = 18.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = Color.White) }
                    TextButton(onClick = { onDone(viewMode, layoutMode, sortKey, newestFirst) }) { Text("Done", color = accent, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
private fun Mode(label: String, icon: ImageVector, selected: Boolean, accent: Color, onClick: () -> Unit) {
    Column(Modifier.clickable(onClick = onClick).padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, label, tint = if (selected) accent else Color.White, modifier = Modifier.size(26.dp))
        Text(label, color = if (selected) accent else Color.White, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun OrderButton(label: String, selected: Boolean, accent: Color, modifier: Modifier, onClick: () -> Unit) {
    Text(
        label,
        color = if (selected) Color.White else Color(0xFFB7BDC7),
        modifier = modifier
            .background(if (selected) accent else Color.Transparent, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        fontWeight = FontWeight.Medium
    )
}
