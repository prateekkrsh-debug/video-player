package dev.videoplayer.app.settings

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

object AppIdentity {
    val icons = listOf("play", "film", "shield", "star")
    val names = listOf("Video Player", "Movies", "Player", "Films")

    fun alias(icon: String, name: String) = "dev.videoplayer.app.Launcher_${icon}_${name.replace(" ", "")}"

    fun current(context: Context): Pair<String, String> {
        val prefs = context.getSharedPreferences("identity", Context.MODE_PRIVATE)
        return prefs.getString("icon", "play").orEmpty() to prefs.getString("name", "Video Player").orEmpty()
    }

    fun customName(context: Context) = context.getSharedPreferences("identity", Context.MODE_PRIVATE).getString("custom", "").orEmpty()

    fun setCustomName(context: Context, value: String) {
        context.getSharedPreferences("identity", Context.MODE_PRIVATE).edit().putString("custom", value.trim()).apply()
        val activity = context as? android.app.Activity ?: return
        activity.setTaskDescription(android.app.ActivityManager.TaskDescription(value.trim().ifBlank { "Video Player" }))
    }

    fun apply(context: Context, icon: String, name: String) {
        val pm = context.packageManager
        icons.flatMap { chosenIcon -> names.map { chosenName -> alias(chosenIcon, chosenName) } }.forEach { className ->
            val state = if (className == alias(icon, name)) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            pm.setComponentEnabledSetting(
                ComponentName(context, className),
                state,
                PackageManager.DONT_KILL_APP
            )
        }
        context.getSharedPreferences("identity", Context.MODE_PRIVATE).edit()
            .putString("icon", icon)
            .putString("name", name)
            .apply()
    }
}
