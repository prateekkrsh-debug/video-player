package dev.videoplayer.app.settings

import android.content.Context

object OrganizePrefs {
    private const val PREFS = "organize"

    fun viewMode(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("view", "folders") ?: "folders"
    fun layout(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("layout", "list") ?: "list"
    fun sortKey(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("sort", "date") ?: "date"
    fun newest(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("newest", true)

    fun save(context: Context, view: String, layout: String, sort: String, newest: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("view", view)
            .putString("layout", layout)
            .putString("sort", sort)
            .putBoolean("newest", newest)
            .apply()
    }
}
