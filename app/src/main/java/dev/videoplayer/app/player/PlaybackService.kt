package dev.videoplayer.app.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import dev.videoplayer.app.MainActivity
import dev.videoplayer.app.R

/**
 * Foreground media session so YouTube's own page player can keep playing
 * after the activity leaves the screen, the same model Brave uses.
 */
class PlaybackService : Service() {
    private var mediaSession: MediaSessionCompat? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        val session = MediaSessionCompat(this, "video-player").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    PlaybackController.play()
                }
                override fun onPause() {
                    PlaybackController.pause()
                }
                override fun onSkipToNext() {
                    PlaybackController.seekForward()
                }
                override fun onSkipToPrevious() {
                    PlaybackController.seekBack()
                }
                override fun onStop() {
                    stopSelf()
                }
            })
            isActive = true
        }
        mediaSession = session
        PlaybackController.onStateChanged = { refresh() }
        startInForeground(buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> PlaybackController.play()
            ACTION_PAUSE -> PlaybackController.pause()
            ACTION_TOGGLE -> if (PlaybackController.playing.value) PlaybackController.pause() else PlaybackController.play()
            ACTION_FORWARD -> PlaybackController.seekForward()
            ACTION_BACK -> PlaybackController.seekBack()
            ACTION_STOP -> {
                PlaybackController.pause()
                PlaybackController.holdInBackground(false)
                stopSelf()
                return START_NOT_STICKY
            }
        }
        refresh()
        return START_STICKY
    }

    override fun onDestroy() {
        PlaybackController.onStateChanged = null
        PlaybackController.holdInBackground(false)
        mediaSession?.isActive = false
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }

    private fun refresh() {
        val playing = PlaybackController.playing.value
        val state = if (playing) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        mediaSession?.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_PLAY_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackStateCompat.ACTION_STOP
                )
                .setState(state, PlaybackController.positionMs.value, if (playing) 1f else 0f)
                .build()
        )
        mediaSession?.setMetadata(
            MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, PlaybackController.title.value)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "YouTube")
                .build()
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val playing = PlaybackController.playing.value
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_play)
            .setContentTitle(PlaybackController.title.value)
            .setContentText(if (playing) "Playing in background" else "Paused")
            .setContentIntent(open)
            .setOngoing(playing)
            .setOnlyAlertOnce(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(android.R.drawable.ic_media_rew, "Back 10s", serviceIntent(ACTION_BACK))
            .addAction(
                if (playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (playing) "Pause" else "Play",
                serviceIntent(ACTION_TOGGLE)
            )
            .addAction(android.R.drawable.ic_media_ff, "Forward 10s", serviceIntent(ACTION_FORWARD))
            .setStyle(
                MediaStyle()
                    .setMediaSession(mediaSession?.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .build()
    }

    private fun serviceIntent(action: String): PendingIntent {
        val intent = Intent(this, PlaybackService::class.java).setAction(action)
        return PendingIntent.getService(
            this,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun startInForeground(notification: Notification) {
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(CHANNEL_ID, "Playback", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Background YouTube playback"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "playback"
        const val NOTIFICATION_ID = 41
        const val ACTION_PLAY = "dev.videoplayer.app.PLAY"
        const val ACTION_PAUSE = "dev.videoplayer.app.PAUSE"
        const val ACTION_TOGGLE = "dev.videoplayer.app.TOGGLE"
        const val ACTION_FORWARD = "dev.videoplayer.app.FORWARD"
        const val ACTION_BACK = "dev.videoplayer.app.BACK"
        const val ACTION_STOP = "dev.videoplayer.app.STOP"

        fun start(context: Context) {
            val intent = Intent(context, PlaybackService::class.java)
            androidx.core.content.ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, PlaybackService::class.java))
        }
    }
}
