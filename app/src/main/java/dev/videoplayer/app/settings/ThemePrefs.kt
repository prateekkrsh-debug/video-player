package dev.videoplayer.app.settings

import android.content.Context

object ThemePrefs {
    fun current(context: Context) = context.getSharedPreferences("theme", Context.MODE_PRIVATE).getString("name", "Dark").orEmpty()
    fun set(context: Context, name: String) = context.getSharedPreferences("theme", Context.MODE_PRIVATE).edit().putString("name", name).apply()
}
