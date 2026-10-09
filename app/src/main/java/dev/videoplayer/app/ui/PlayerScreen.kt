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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
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
import androidx.compose.material.icons.filled.ScreenRotation
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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.ui.input.pointer.pointerInput
import dev.videoplayer.app.player.GesturePrefs
import dev.videoplayer.app.player.SeekGesture
import kotlin.math.abs
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import dev.videoplayer.app.editor.ClipExporter
import androidx.activity.result.IntentSenderRequest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import dev.videoplayer.app.library.VideoFile
import dev.videoplayer.app.library.VideoFileActions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Teal = Color(0xFF7EAEB8)
private val Panel = Color(0xFF1C1E22)

@Composable
fun PlayerScreen(queue: List<VideoFile>, startIndex: Int, startPosition: Long, onProgress: (VideoFile, Long) -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    val activity = context as android.app.Activity
    val scope = rememberCoroutineScope()
    val landscape = LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    var index by remember { mutableIntStateOf(startIndex.coerceIn(0, queue.lastIndex)) }
    val video = queue[index]
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItems(queue.map { MediaItem.fromUri(it.uri) }, startIndex, startPosition)
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(Unit) {
        activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            player.release()
        }
    }

    var playing by remember { mutableStateOf(true) }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(video.durationMs) }
    var controls by remember { mutableStateOf(true) }
    var locked by remember { mutableStateOf(false) }
    var repeat by remember { mutableIntStateOf(Player.REPEAT_MODE_OFF) }
    var shuffle by remember { mutableStateOf(false) }
    var speed by remember { mutableFloatStateOf(1f) }
    var touchTick by remember { mutableIntStateOf(0) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var showQueue by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf("") }
    var renaming by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var gestureSide by remember { mutableStateOf<String?>(null) }
    val consent = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { }
    var editor by remember { mutableStateOf(false) }
    BackHandler {
        when {
            editor -> editor = false
            showQueue -> showQueue = false
            renaming -> renaming = false
            deleting -> deleting = false
            else -> {
                activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                onClose()
            }
        }
    }
    var menu by remember { mutableStateOf(false) }
    var speedMenu by remember { mutableStateOf(false) }
    var holdingBoost by remember { mutableStateOf(false) }
    var seeking by remember { mutableStateOf(false) }
    var seekDelta by remember { mutableLongStateOf(0L) }
    var seekPreview by remember { mutableLongStateOf(0L) }
    var seekEnabled by remember { mutableStateOf(GesturePrefs.enabled(context)) }
    var seekOverlay by remember { mutableStateOf(GesturePrefs.overlay(context)) }
    var msPerScreen by remember { mutableLongStateOf(GesturePrefs.msPerScreen(context)) }
    var lastTapMs by remember { mutableLongStateOf(0L) }
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
                errorText = null
            }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                errorText = "This video cannot be played on this device."
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }
    LaunchedEffect(controls, playing) {
        while (controls) {
            position = player.currentPosition
            if (player.duration > 0) duration = player.duration
            onProgress(queue.getOrElse(index) { video }, position)
            delay(250)
        }
    }
    LaunchedEffect(touchTick, controls, locked, editor) {
        if (controls && !locked && !editor) {
            delay(2000)
            controls = false
        }
    }
    fun wake() {
        controls = true
        touchTick += 1
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(locked, editor, seekEnabled, msPerScreen) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    if (locked || editor) return@awaitEachGesture
                    val start = down.position
                    val origin = player.currentPosition
                    val wasPlaying = player.isPlaying
                    var mode = "none"
                    val holdJob = scope.launch {
                        delay(280)
                        if (mode == "none") {
                            mode = "hold"
                            holdingBoost = true
                            player.setPlaybackSpeed(2f)
                        }
                    }
                    try {
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) break
                            val dx = change.position.x - start.x
                            val dy = change.position.y - start.y
                            if (mode == "none" || mode == "hold") {
                                val next = SeekGesture.direction(dx, dy)
                                if (next != null) {
                                    holdJob.cancel()
                                    if (holdingBoost) {
                                        holdingBoost = false
                                        player.setPlaybackSpeed(speed)
                                    }
                                    mode = when {
                                        next == "seek" && seekEnabled -> "seek"
                                        start.x < size.width * 0.4f -> "brightness"
                                        start.x > size.width * 0.6f -> "volume"
                                        else -> "ignore"
                                    }
                                    if (mode == "seek") {
                                        seeking = true
                                        seekPreview = origin
                                        seekDelta = 0
                                    }
                                    if (mode == "brightness") gestureSide = "brightness"
                                    if (mode == "volume") gestureSide = "volume"
                                }
                            }
                            when (mode) {
                                "seek" -> {
                                    val target = SeekGesture.target(origin, dx, size.width.toFloat(), player.duration.coerceAtLeast(duration), msPerScreen)
                                    seekPreview = target
                                    seekDelta = target - origin
                                    position = target
                                    change.consume()
                                }
                                "brightness" -> {
                                    val next = (1f - change.position.y / size.height).coerceIn(0.01f, 1f)
                                    brightness = next
                                    val attrs = activity.window.attributes
                                    attrs.screenBrightness = next
                                    activity.window.attributes = attrs
                                    change.consume()
                                }
                                "volume" -> {
                                    val next = (1f - change.position.y / size.height).coerceIn(0f, 1f)
                                    volume = next
                                    val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                    audio.setStreamVolume(AudioManager.STREAM_MUSIC, (next * max).toInt(), 0)
                                    change.consume()
                                }
                            }
                        }
                    } finally {
                        holdJob.cancel()
                        if (mode == "seek") {
                            player.seekTo(seekPreview)
                            position = seekPreview
                            if (wasPlaying) player.play() else player.pause()
                            seeking = false
                        }
                        if (mode == "none") {
                            val now = android.os.SystemClock.uptimeMillis()
                            if (now - lastTapMs < 280) {
                                val delta = if (start.x < size.width / 2) -10_000 else 10_000
                                player.seekTo((player.currentPosition + delta).coerceIn(0, player.duration.coerceAtLeast(0)))
                                position = player.currentPosition
                                lastTapMs = 0
                            } else {
                                lastTapMs = now
                                if (!holdingBoost) controls = !controls
                            }
                        }
                        if (holdingBoost) {
                            holdingBoost = false
                            speed = 1f
                            player.setPlaybackSpeed(1f)
                        }
                        gestureSide = null
                    }
                }
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
        if (gestureSide == "brightness") {
            ThinMeter(Modifier.align(Alignment.CenterStart).padding(start = 18.dp), brightness, Icons.Default.BrightnessMedium)
        }
        if (holdingBoost) {
            Row(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 28.dp)
                    .zIndex(6f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.Black.copy(alpha = 0.62f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("2.0x", color = Color.White, fontSize = 14.sp)
                Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(16.dp))
                Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
        if (gestureSide == "volume") {
            ThinMeter(Modifier.align(Alignment.CenterEnd).padding(end = 18.dp), volume, Icons.AutoMirrored.Filled.VolumeUp)
        }
        if (seeking && seekOverlay) {
            Column(
                Modifier.align(Alignment.Center).zIndex(6f).clip(RoundedCornerShape(18.dp)).background(Color.Black.copy(alpha = 0.62f)).padding(horizontal = 22.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(if (seekDelta >= 0) Icons.Default.FastForward else Icons.Default.FastRewind, null, tint = Color.White, modifier = Modifier.size(28.dp))
                Spacer(Modifier.height(6.dp))
                Text(SeekGesture.label(seekDelta), color = Color.White, fontSize = 22.sp)
                Text("${formatDuration(seekPreview)} / ${formatDuration(duration)}", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
            }
        }
        if ((controls || locked) && !editor && gestureSide == null && !holdingBoost && !seeking) {
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
                speedMenu = speedMenu,
                onClose = {
                    activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    onClose()
                },
                onEdit = {
                    editor = true
                    activity.requestedOrientation = activity.resources.configuration.orientation.let {
                        if (it == android.content.res.Configuration.ORIENTATION_LANDSCAPE) android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                        else android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    }
                    wake()
                },
                onShare = { shareVideo(activity, video.uri, video.name) },
                onMenu = { menu = true },
                onDismissMenu = { menu = false },
                onRename = { menu = false; renameText = video.name; renaming = true },
                onDelete = { menu = false; deleting = true },
                onPrevious = { player.seekToPreviousMediaItem(); wake() },
                onPlay = { if (player.isPlaying) player.pause() else player.play(); wake() },
                onNext = { player.seekToNextMediaItem(); wake() },
                onSeek = {
                    player.seekTo(it)
                    position = it
                    wake()
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
                onQueue = { showQueue = true; wake() },
                onRepeat = {
                    repeat = when (repeat) {
                        Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                        Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                        else -> Player.REPEAT_MODE_OFF
                    }
                    player.repeatMode = repeat
                    toast(context, when (repeat) {
                        Player.REPEAT_MODE_ONE -> "Repeat one"
                        Player.REPEAT_MODE_ALL -> "Repeat all"
                        else -> "Repeat off"
                    })
                    wake()
                },
                onShuffle = {
                    shuffle = !shuffle
                    player.shuffleModeEnabled = shuffle
                    toast(context, if (shuffle) "Shuffle on" else "Shuffle off")
                    wake()
                },
                onSpeed = { speedMenu = true; wake() },
                onSpeedChoice = {
                    speed = it
                    player.setPlaybackSpeed(it)
                    toast(context, "${it}x")
                    speedMenu = false
                    wake()
                },
                onDismissSpeed = { speedMenu = false },
                onRotate = {
                    activity.requestedOrientation = if (landscape) {
                        android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    } else {
                        android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    }
                    wake()
                },
                onLock = { locked = !locked; wake() },
                seekEnabled = seekEnabled,
                seekOverlay = seekOverlay,
                msPerScreen = msPerScreen,
                onToggleSeek = {
                    seekEnabled = !seekEnabled
                    GesturePrefs.setEnabled(context, seekEnabled)
                    menu = false
                },
                onToggleOverlay = {
                    seekOverlay = !seekOverlay
                    GesturePrefs.setOverlay(context, seekOverlay)
                    menu = false
                },
                onCycleSensitivity = {
                    msPerScreen = when (msPerScreen) { 45_000L -> 90_000L; 90_000L -> 150_000L; else -> 45_000L }
                    GesturePrefs.setMsPerScreen(context, msPerScreen)
                    menu = false
                }
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
                onSeek = { player.seekTo(it); position = it },
                onMute = { player.volume = if (player.volume == 0f) 1f else 0f },
                onSave = { start, end, height ->
                    saving = true
                    scope.launch {
                        val result = runCatching {
                            withContext(Dispatchers.Main) {
                                ClipExporter.export(
                                    context,
                                    Uri.parse(video.uri),
                                    video.name.substringBeforeLast('.') + "-clip.mp4",
                                    start,
                                    end,
                                    height,
                                    video.folder.ifBlank { "Movies/Clips" }
                                )
                            }
                        }
                        saving = false
                        editor = false
                        toast(context, result.fold({ "Clip saved in ${video.folder}" }, { "This file could not be clipped" }))
                    }
                }
            )
        }
        if (errorText != null) {
            Text(errorText.orEmpty(), color = Color.White, modifier = Modifier.align(Alignment.Center).padding(24.dp))
        }
        if (showQueue) {
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)).background(Panel).padding(12.dp)
            ) {
                Text("Queue", color = Color.White, modifier = Modifier.padding(8.dp))
                queue.forEachIndexed { itemIndex, item ->
                    Text(
                        item.name,
                        color = if (itemIndex == index) Teal else Color.White,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth().clickable {
                            player.seekTo(itemIndex, 0)
                            showQueue = false
                        }.padding(vertical = 10.dp)
                    )
                }
                TextButton(onClick = { showQueue = false }) { Text("Close", color = Teal) }
            }
        }
        if (renaming) {
            AlertDialog(
                onDismissRequest = { renaming = false },
                title = { Text("Rename video") },
                text = { OutlinedTextField(renameText, { renameText = it }, singleLine = true) },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch {
                            val sender = withContext(Dispatchers.IO) { VideoFileActions.rename(context, video, renameText.trim()) }
                            if (sender != null) consent.launch(IntentSenderRequest.Builder(sender).build())
                            renaming = false
                            toast(context, "Rename requested")
                        }
                    }) { Text("Save") }
                },
                dismissButton = { TextButton(onClick = { renaming = false }) { Text("Cancel") } }
            )
        }
        if (deleting) {
            AlertDialog(
                onDismissRequest = { deleting = false },
                title = { Text("Delete video") },
                text = { Text(video.name) },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch {
                            val sender = withContext(Dispatchers.IO) { VideoFileActions.delete(context, video) }
                            if (sender != null) consent.launch(IntentSenderRequest.Builder(sender).build())
                            deleting = false
                            onClose()
                        }
                    }) { Text("Delete") }
                },
                dismissButton = { TextButton(onClick = { deleting = false }) { Text("Cancel") } }
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
    speedMenu: Boolean,
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
    onSpeedChoice: (Float) -> Unit,
    onDismissSpeed: () -> Unit,
    onRotate: () -> Unit,
    onLock: () -> Unit,
    seekEnabled: Boolean,
    seekOverlay: Boolean,
    msPerScreen: Long,
    onToggleSeek: () -> Unit,
    onToggleOverlay: () -> Unit,
    onCycleSensitivity: () -> Unit
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
                ThinMeter(Modifier.align(Alignment.CenterStart).padding(start = 18.dp), brightness, Icons.Default.BrightnessMedium)
                ThinMeter(Modifier.align(Alignment.CenterEnd).padding(end = 18.dp), volume, Icons.AutoMirrored.Filled.VolumeUp)
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
                    DropdownMenu(expanded = speedMenu, onDismissRequest = onDismissSpeed) {
                        listOf(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f).forEach { choice ->
                            DropdownMenuItem(text = { Text("${choice}x") }, onClick = { onSpeedChoice(choice); onDismissSpeed() })
                        }
                    }
                    IconButton(onClick = onRotate) { Icon(Icons.Default.ScreenRotation, "Orientation", tint = Color.White) }
                    IconButton(onClick = onLock) { Icon(Icons.Default.Lock, "Lock", tint = Color.White) }
                    Box {
                        IconButton(onClick = onMenu) { Icon(Icons.Default.MoreVert, "More", tint = Color.White) }
                        DropdownMenu(expanded = menu, onDismissRequest = onDismissMenu) {
                            DropdownMenuItem(text = { Text("Rename in folder") }, onClick = onRename)
                            DropdownMenuItem(text = { Text("Delete in folder") }, onClick = onDelete)
                            DropdownMenuItem(text = { Text(if (seekEnabled) "Swipe seek on" else "Swipe seek off") }, onClick = onToggleSeek)
                            DropdownMenuItem(text = { Text("Seek sensitivity ${msPerScreen / 1000}s") }, onClick = onCycleSensitivity)
                            DropdownMenuItem(text = { Text(if (seekOverlay) "Seek overlay on" else "Seek overlay off") }, onClick = onToggleOverlay)
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
    onSeek: (Long) -> Unit,
    onMute: () -> Unit,
    onSave: (Long, Long, Int?) -> Unit
) {
    val context = LocalContext.current
    val safeDuration = duration.coerceAtLeast(1)
    var startFrac by remember { mutableFloatStateOf((position.toFloat() / safeDuration).coerceIn(0f, 0.8f)) }
    var endFrac by remember { mutableFloatStateOf((startFrac + 0.18f).coerceAtMost(1f)) }
    var frames by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var height by remember { mutableStateOf<Int?>(null) }
    var qualityMenu by remember { mutableStateOf(false) }
    var muted by remember { mutableStateOf(false) }
    var dragMode by remember { mutableStateOf("move") }
    var timeLabel by remember { mutableLongStateOf(position) }
    LaunchedEffect(video.uri) {
        frames = withContext(Dispatchers.IO) { thumbnails(context, video.uri, safeDuration) }
    }
    val startMs = (startFrac * safeDuration).toLong()
    val endMs = (endFrac * safeDuration).toLong().coerceAtLeast(startMs + 400)
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f))) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            RoundButton(onDismiss, Icons.Default.Close, "Close", 42.dp)
            Spacer(Modifier.width(8.dp))
            RoundButton({ muted = !muted; onMute() }, if (muted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp, "Mute", 42.dp)
            Spacer(Modifier.weight(1f))
            Box {
                TextButton(onClick = { qualityMenu = true }) {
                    Text(if (height == null) "Original" else "${height}p", color = Color.White, modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(Color(0xFF3A3A3A)).padding(horizontal = 12.dp, vertical = 8.dp))
                }
                DropdownMenu(expanded = qualityMenu, onDismissRequest = { qualityMenu = false }) {
                    DropdownMenuItem(text = { Text("Original") }, onClick = { height = null; qualityMenu = false })
                    listOf(1080, 720, 480).forEach { choice ->
                        DropdownMenuItem(text = { Text("${choice}p") }, onClick = { height = choice; qualityMenu = false })
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier.clip(RoundedCornerShape(22.dp)).background(Color(0xFFF5C518)).clickable(enabled = !saving) { onSave(startMs, endMs, height) }.padding(horizontal = 16.dp, vertical = 10.dp)
            ) { if (saving) CircularProgressIndicator(Modifier.size(18.dp), color = Color.Black) else Icon(Icons.Default.Check, "Save", tint = Color.Black) }
        }
        Box(
            Modifier.align(Alignment.Center).size(64.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.45f)).clickable { onPreview(startMs, endMs) },
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Default.PlayArrow, "Preview", tint = Color.White) }
        Column(Modifier.align(Alignment.BottomCenter).padding(bottom = 28.dp)) {
            Text(
                formatClock(timeLabel),
                color = Color.Black,
                fontSize = 13.sp,
                modifier = Modifier.padding(start = 24.dp, bottom = 8.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFFF5C518)).padding(horizontal = 10.dp, vertical = 4.dp)
            )
            BoxWithConstraints(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(72.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF2A2A2A)).pointerInput(safeDuration) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val startX = startFrac * size.width
                            val endX = endFrac * size.width
                            dragMode = when {
                                kotlin.math.abs(offset.x - startX) < 36.dp.toPx() -> "start"
                                kotlin.math.abs(offset.x - endX) < 36.dp.toPx() -> "end"
                                offset.x in startX..endX -> "move"
                                else -> "move"
                            }
                        },
                        onDrag = { _, amount ->
                            val dx = amount.x / size.width
                            val span = (endFrac - startFrac).coerceAtLeast(0.04f)
                            when (dragMode) {
                                "start" -> startFrac = (startFrac + dx).coerceIn(0f, endFrac - 0.04f)
                                "end" -> endFrac = (endFrac + dx).coerceIn(startFrac + 0.04f, 1f)
                                else -> {
                                    startFrac = (startFrac + dx).coerceIn(0f, 1f - span)
                                    endFrac = startFrac + span
                                }
                            }
                            timeLabel = ((if (dragMode == "end") endFrac else startFrac) * safeDuration).toLong()
                            onSeek(timeLabel)
                        }
                    )
                }
            ) {
                Row(Modifier.fillMaxSize()) {
                    val shown = if (frames.isEmpty()) List(6) { null } else frames
                    shown.forEach { frame ->
                        if (frame == null) Spacer(Modifier.weight(1f).fillMaxHeight().background(Color.DarkGray))
                        else Image(frame.asImageBitmap(), null, Modifier.weight(1f).fillMaxHeight(), contentScale = ContentScale.Crop)
                    }
                }
                Box(
                    Modifier
                        .fillMaxHeight()
                        .width(maxWidth * (endFrac - startFrac).coerceAtLeast(0.04f))
                        .offset(x = maxWidth * startFrac)
                        .border(3.dp, Color(0xFFF5C518), RoundedCornerShape(6.dp))
                )
            }
            Text("${formatClock(startMs)}  –  ${formatClock(endMs)}", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(start = 20.dp, top = 8.dp))
        }
    }
}

private fun formatClock(ms: Long): String {
    val total = ms.coerceAtLeast(0)
    val hours = total / 3_600_000
    val minutes = (total / 60_000) % 60
    val seconds = (total / 1000) % 60
    val centis = (total % 1000) / 10
    return "%02d:%02d:%02d.%02d".format(hours, minutes, seconds, centis)
}

private fun thumbnails(context: android.content.Context, uri: String, duration: Long): List<Bitmap> {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, Uri.parse(uri))
        (0 until 8).mapNotNull { step ->
            retriever.getFrameAtTime(duration * 1000 * step / 8, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        }
    } catch (_: Exception) {
        emptyList()
    } finally {
        retriever.release()
    }
}
