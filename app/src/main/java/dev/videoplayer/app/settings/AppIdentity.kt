package dev.videoplayer.app.settings

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

object AppIdentity {
    val icons = listOf("play", "film", "shield", "star")
    val names = listOf("Video Player", "Movies", "Player", "Films", "Calculator", "Notes", "Weather", "Gallery", "Files", "Clock")

    fun iconRes(icon: String) = when (icon) {
        "film" -> dev.videoplayer.app.R.drawable.ic_launcher_film
        "shield" -> dev.videoplayer.app.R.drawable.ic_launcher_shield
        "star" -> dev.videoplayer.app.R.drawable.ic_launcher_star
        else -> dev.videoplayer.app.R.drawable.ic_launcher_foreground
    }

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
        val selected = alias(icon, name)
        val all = icons.flatMap { chosenIcon -> names.map { chosenName -> alias(chosenIcon, chosenName) } }
        all.filterNot { it == selected }.forEach { className ->
            pm.setComponentEnabledSetting(
                ComponentName(context.packageName, className),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }
        pm.setComponentEnabledSetting(
            ComponentName(context.packageName, selected),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
        context.getSharedPreferences("identity", Context.MODE_PRIVATE).edit()
            .putString("icon", icon)
            .putString("name", name)
            .apply()
    }
}
