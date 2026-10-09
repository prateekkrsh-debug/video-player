package dev.videoplayer.app.player

import android.content.Context

object GesturePrefs {
    private const val PREFS = "gestures"
    private const val ENABLED = "seek_enabled"
    private const val OVERLAY = "seek_overlay"
    private const val SENSITIVITY = "ms_per_screen"

    fun enabled(context: Context) = prefs(context).getBoolean(ENABLED, true)
    fun overlay(context: Context) = prefs(context).getBoolean(OVERLAY, true)
    fun msPerScreen(context: Context) = prefs(context).getLong(SENSITIVITY, SeekGesture.DEFAULT_MS_PER_SCREEN)

    fun setEnabled(context: Context, value: Boolean) = prefs(context).edit().putBoolean(ENABLED, value).apply()
    fun setOverlay(context: Context, value: Boolean) = prefs(context).edit().putBoolean(OVERLAY, value).apply()
    fun setMsPerScreen(context: Context, value: Long) = prefs(context).edit().putLong(SENSITIVITY, value).apply()

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
