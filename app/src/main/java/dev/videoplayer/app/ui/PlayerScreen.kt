package dev.videoplayer.app.ui

import android.graphics.Bitmap
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import dev.videoplayer.app.editor.ClipExporter
import dev.videoplayer.app.library.VideoFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Teal = Color(0xFF7EAEB8)
private val Panel = Color(0xFF1C1E22)

@Composable
fun PlayerScreen(queue: List<VideoFile>, startIndex: Int, onClose: () -> Unit) {
    val context = LocalContext.current
    val activity = context as android.app.Activity
    val scope = rememberCoroutineScope()
    val landscape = LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    var index by remember { mutableIntStateOf(startIndex.coerceIn(0, queue.lastIndex)) }
    val video = queue[index]
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItems(queue.map { MediaItem.fromUri(it.uri) }, startIndex, 0)
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(Unit) { onDispose { player.release() } }

    var playing by remember { mutableStateOf(true) }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(video.durationMs) }
    var controls by remember { mutableStateOf(true) }
    var locked by remember { mutableStateOf(false) }
    var repeat by remember { mutableIntStateOf(Player.REPEAT_MODE_OFF) }
    var shuffle by remember { mutableStateOf(false) }
    var speed by remember { mutableFloatStateOf(1f) }
    var gestureSide by remember { mutableStateOf<String?>(null) }
    var editor by remember { mutableStateOf(false) }
    BackHandler {
        if (editor) editor = false else onClose()
    }
    var menu by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    val audio = remember { context.getSystemService(AudioManager::class.java) }
    var volume by remember { mutableFloatStateOf(audio.getStreamVolume(AudioManager.STREAM_MUSIC) / audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat()) }
    var brightness by remember { mutableFloatStateOf(activity.window.attributes.screenBrightness.let { if (it < 0f) 0.6f else it }) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) { playing = isPlaying }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) duration = player.duration.coerceAtLeast(0)
            }
            override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
                index = player.currentMediaItemIndex
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }
    LaunchedEffect(playing, controls, locked) {
        while (playing && controls && !locked && !editor) {
            delay(250)
            position = player.currentPosition
            if (player.duration > 0) duration = player.duration
        }
    }
    LaunchedEffect(controls, locked, editor) {
        if (controls && !locked && !editor) {
            delay(2000)
            controls = false
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(locked) {
                detectTapGestures { controls = if (locked) controls else !controls }
            }
    ) {
        AndroidView(
            factory = {
                PlayerView(it).apply {
                    useController = false
                    this.player = player
                    layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        if (!locked) {
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .fillMaxWidth(0.32f)
                    .pointerInput(brightness) {
                        detectVerticalDragGestures(
                            onDragStart = { gestureSide = "brightness"; controls = true },
                            onDragEnd = { gestureSide = null },
                            onDragCancel = { gestureSide = null }
                        ) { _, drag ->
                            val next = (brightness - drag / size.height).coerceIn(0.01f, 1f)
                            brightness = next
                            val attrs = activity.window.attributes
                            attrs.screenBrightness = next
                            activity.window.attributes = attrs
                        }
                    }
            )
            Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .fillMaxWidth(0.32f)
                    .pointerInput(volume) {
                        detectVerticalDragGestures(
                            onDragStart = { gestureSide = "volume"; controls = true },
                            onDragEnd = { gestureSide = null },
                            onDragCancel = { gestureSide = null }
                        ) { _, drag ->
                            val next = (volume - drag / size.height).coerceIn(0f, 1f)
                            volume = next
                            val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                            audio.setStreamVolume(AudioManager.STREAM_MUSIC, (next * max).toInt(), 0)
                        }
                    }
            )
        }
        if (controls || locked) {
            PlayerChrome(
                landscape = landscape,
                title = video.name,
                playing = playing,
                locked = locked,
                position = position,
                duration = duration,
                volume = volume,
                brightness = brightness,
                menu = menu,
                onClose = onClose,
                onEdit = { editor = true; controls = true },
                onShare = { shareVideo(activity, video.uri, video.name) },
                onMenu = { menu = true },
                onDismissMenu = { menu = false },
                onRename = { menu = false; toast(context, "Rename from the folder list") },
                onDelete = { menu = false; toast(context, "Delete from the folder list") },
                onPrevious = { player.seekToPreviousMediaItem() },
                onPlay = { if (player.isPlaying) player.pause() else player.play() },
                onNext = { player.seekToNextMediaItem() },
                onSeek = {
                    player.seekTo(it)
                    position = it
                    controls = true
                },
                onBrightness = {
                    brightness = it
                    val attrs = activity.window.attributes
                    attrs.screenBrightness = it
                    activity.window.attributes = attrs
                    controls = true
                },
                onVolume = {
                    volume = it
                    val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                    audio.setStreamVolume(AudioManager.STREAM_MUSIC, (it * max).toInt(), 0)
                    controls = true
                },
                onQueue = { toast(context, "${queue.size} videos in this folder") },
                onRepeat = {
                    repeat = if (repeat == Player.REPEAT_MODE_OFF) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
                    player.repeatMode = repeat
                    controls = true
                },
                onShuffle = {
                    shuffle = !shuffle
                    player.shuffleModeEnabled = shuffle
                    controls = true
                },
                onSpeed = {
                    speed = when (speed) { 1f -> 1.25f; 1.25f -> 1.5f; 1.5f -> 2f; else -> 1f }
                    player.setPlaybackSpeed(speed)
                    toast(context, "${speed}x")
                    controls = true
                },
                onLock = { locked = !locked; controls = true }
            )
        }
        if (editor) {
            ClipEditorDialog(
                video = video,
                position = player.currentPosition,
                duration = duration.coerceAtLeast(1),
                saving = saving,
                onDismiss = { editor = false },
                onPreview = { start, end ->
                    player.seekTo(start)
                    player.play()
                    scope.launch {
                        delay((end - start).coerceAtLeast(300))
                        player.pause()
                    }
                },
                onSave = { start, end ->
                    saving = true
                    scope.launch {
                        val result = runCatching {
                            withContext(Dispatchers.IO) {
                                ClipExporter.export(
                                    context,
                                    Uri.parse(video.uri),
                                    video.name.substringBeforeLast('.') + "-clip.mp4",
                                    start,
                                    end
                                )
                            }
                        }
                        saving = false
                        editor = false
                        toast(context, result.fold({ "Clip saved to Movies/Clips" }, { "This file could not be clipped" }))
                    }
                }
            )
        }
    }
}

@Composable
private fun PlayerChrome(
    landscape: Boolean,
    title: String,
    playing: Boolean,
    locked: Boolean,
    position: Long,
    duration: Long,
    volume: Float,
    brightness: Float,
    menu: Boolean,
    onClose: () -> Unit,
    onEdit: () -> Unit,
    onShare: () -> Unit,
    onMenu: () -> Unit,
    onDismissMenu: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onPrevious: () -> Unit,
    onPlay: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onBrightness: (Float) -> Unit,
    onVolume: (Float) -> Unit,
    onQueue: () -> Unit,
    onRepeat: () -> Unit,
    onShuffle: () -> Unit,
    onSpeed: () -> Unit,
    onLock: () -> Unit
) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f))) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp).clip(RoundedCornerShape(28.dp)).background(Panel).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, "Close", tint = Color.White) }
            Text(title, color = Color.White, modifier = Modifier.weight(1f), maxLines = 1)
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "Edit clip", tint = Color.White) }
            IconButton(onClick = onShare) { Icon(Icons.Default.Share, "Share", tint = Color.White) }
        }
        if (!locked) {
            Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically) {
                RoundButton(onPrevious, Icons.Default.SkipPrevious, "Previous")
                Spacer(Modifier.width(18.dp))
                RoundButton(onPlay, if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, "Play", 72.dp)
                Spacer(Modifier.width(18.dp))
                RoundButton(onNext, Icons.Default.SkipNext, "Next")
            }
            if (controls || gestureSide != null) {
                ThinMeter(Modifier.align(Alignment.CenterStart).padding(start = 18.dp), brightness, Icons.Default.BrightnessMedium)
                ThinMeter(Modifier.align(Alignment.CenterEnd).padding(end = 18.dp), volume, Icons.AutoMirrored.Filled.VolumeOff)
            }
        }
        Column(Modifier.align(Alignment.BottomCenter).padding(16.dp)) {
            if (!locked) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(formatDuration(position), color = Color.White, fontSize = 12.sp)
                    Slider(
                        value = position.coerceAtMost(duration).toFloat(),
                        onValueChange = { onSeek(it.toLong()) },
                        valueRange = 0f..duration.coerceAtLeast(1).toFloat(),
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        colors = SliderDefaults.colors(thumbColor = Teal, activeTrackColor = Teal)
                    )
                    Text(formatDuration(duration), color = Color.White, fontSize = 12.sp)
                }
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Panel).padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(onClick = onQueue) { Icon(Icons.Default.QueueMusic, "Queue", tint = Color.White) }
                    IconButton(onClick = onRepeat) { Icon(Icons.Default.Repeat, "Repeat", tint = Color.White) }
                    IconButton(onClick = onShuffle) { Icon(Icons.Default.Shuffle, "Shuffle", tint = Color.White) }
                    IconButton(onClick = onSpeed) { Icon(Icons.Default.Speed, "Speed", tint = Color.White) }
                    IconButton(onClick = onLock) { Icon(Icons.Default.Lock, "Lock", tint = Color.White) }
                    Box {
                        IconButton(onClick = onMenu) { Icon(Icons.Default.MoreVert, "More", tint = Color.White) }
                        DropdownMenu(expanded = menu, onDismissRequest = onDismissMenu) {
                            DropdownMenuItem(text = { Text("Rename in folder") }, onClick = onRename)
                            DropdownMenuItem(text = { Text("Delete in folder") }, onClick = onDelete)
                        }
                    }
                }
            } else {
                TextButton(onClick = onLock, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Unlock", color = Color.White) }
            }
        }
    }
}

@Composable
private fun ThinMeter(modifier: Modifier, value: Float, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(
        modifier.width(36.dp).clip(RoundedCornerShape(18.dp)).background(Color.Black.copy(alpha = 0.28f)).padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(16.dp))
        Spacer(Modifier.height(8.dp))
        Box(Modifier.width(4.dp).height(120.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.25f))) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height((120 * value.coerceIn(0f, 1f)).dp)
                    .background(Teal)
            )
        }
    }
}

@Composable
private fun RoundButton(onClick: () -> Unit, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, size: androidx.compose.ui.unit.Dp = 56.dp) {
    Box(
        Modifier.size(size).clip(CircleShape).background(Color.Black.copy(alpha = 0.55f)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, label, tint = Color.White, modifier = Modifier.size(size / 2)) }
}

@Composable
fun ClipEditorDialog(
    video: VideoFile,
    position: Long,
    duration: Long,
    saving: Boolean,
    onDismiss: () -> Unit,
    onPreview: (Long, Long) -> Unit,
    onSave: (Long, Long) -> Unit
) {
    val context = LocalContext.current
    var start by remember { mutableFloatStateOf(0f) }
    var end by remember { mutableFloatStateOf(duration.toFloat()) }
    var frames by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    LaunchedEffect(video.uri) {
        frames = withContext(Dispatchers.IO) { thumbnails(context, video.uri, duration) }
    }
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.clip(RoundedCornerShape(28.dp)).background(Color(0xFF2A3140)).padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(Modifier.clip(RoundedCornerShape(22.dp)).background(Color.Black).padding(16.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(formatDuration(((start + end) / 2).toLong()), color = Color.White)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(54.dp).clip(CircleShape).background(Color(0xFF3E5360)).clickable { onPreview(start.toLong(), end.toLong()) },
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Default.PlayArrow, "Preview", tint = Color.White) }
                        Spacer(Modifier.width(8.dp))
                        Row(
                            Modifier.weight(1f).height(54.dp).clip(RoundedCornerShape(16.dp)).background(Teal),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("<", color = Color.White, modifier = Modifier.padding(6.dp))
                            frames.take(4).forEach { frame ->
                                Image(frame.asImageBitmap(), null, Modifier.weight(1f).height(46.dp), contentScale = ContentScale.Crop)
                            }
                            if (frames.isEmpty()) Spacer(Modifier.weight(1f))
                            Text(">", color = Color.White, modifier = Modifier.padding(6.dp))
                        }
                    }
                    Slider(start, { start = it.coerceAtMost(end - 500) }, valueRange = 0f..duration.toFloat())
                    Slider(end, { end = it.coerceAtLeast(start + 500) }, valueRange = 0f..duration.toFloat())
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Edit and create clips from your\nvideo", color = Color.White, fontSize = 22.sp)
            if (saving) {
                CircularProgressIndicator(color = Teal, modifier = Modifier.padding(top = 12.dp))
            } else {
                TextButton(onClick = { onSave(start.toLong(), end.toLong()) }) { Text("Save clip", color = Teal) }
            }
        }
    }
}

private fun thumbnails(context: android.content.Context, uri: String, duration: Long): List<Bitmap> {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, Uri.parse(uri))
        (1..4).mapNotNull { step ->
            retriever.getFrameAtTime(duration * 1000 * step / 5, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        }
    } catch (_: Exception) {
        emptyList()
    } finally {
        retriever.release()
    }
}
