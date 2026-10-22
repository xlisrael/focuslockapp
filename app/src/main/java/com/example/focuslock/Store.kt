package com.example.focuslock

import android.content.Context

object Store {
    private const val PREFS_NAME = "focuslock_prefs"
    private const val KEY_END_TIME = "end_time"
    private const val KEY_ALLOWED = "allowed_apps"
    private const val KEY_DARK_MODE = "dark_mode"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun endTime(context: Context): Long = prefs(context).getLong(KEY_END_TIME, 0L)

    fun isActive(context: Context): Boolean = System.currentTimeMillis() < endTime(context)

    fun allowed(context: Context): Set<String> {
        val set = prefs(context).getStringSet(KEY_ALLOWED, emptySet())
        return set ?: emptySet()
    }

    fun start(context: Context, minutes: Int, allowed: Set<String>) {
        if (isActive(context)) return
        val end = System.currentTimeMillis() + minutes * 60_000L
        prefs(context).edit()
            .putLong(KEY_END_TIME, end)
            .putStringSet(KEY_ALLOWED, allowed)
            .apply()
    }

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }

    fun isDarkMode(context: Context): Boolean = prefs(context).getBoolean(KEY_DARK_MODE, false)

    fun setDarkMode(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_DARK_MODE, enabled).apply()
    }
}
