package com.scriptam.app.widget

import android.content.Context

/**
 * Lightweight SharedPreferences store for per-widget persistent state.
 * Each widget instance is keyed by its [appWidgetId].
 *
 * Stored fields per widget:
 *  - scriptId     : Long  — ID of the bound script
 *  - scriptTitle  : String
 *  - accentColor  : Int   — ARGB int
 *  - output       : String — last console output or widgetUI JSON captured from the script
 *  - isWidgetUI   : Boolean — true if output contains structured widgetUI JSON
 *  - lastRun      : Long  — epoch millis of last execution
 *  - interval     : Long  — periodic update interval in minutes (default 0 = manual only)
 */
object WidgetPrefs {

    private const val PREFS_NAME = "scriptam_widget_prefs"
    private const val KEY_SCRIPT_ID     = "widget_%d_script_id"
    private const val KEY_SCRIPT_TITLE  = "widget_%d_script_title"
    private const val KEY_ACCENT_COLOR  = "widget_%d_accent_color"
    private const val KEY_OUTPUT        = "widget_%d_output"
    private const val KEY_IS_WIDGET_UI  = "widget_%d_is_widget_ui"
    private const val KEY_LAST_RUN      = "widget_%d_last_run"
    private const val KEY_INTERVAL      = "widget_%d_interval"

    // ---- Getters ----

    fun getScriptId(ctx: Context, id: Int): Long =
        prefs(ctx).getLong(key(KEY_SCRIPT_ID, id), -1L)

    fun getScriptTitle(ctx: Context, id: Int): String =
        prefs(ctx).getString(key(KEY_SCRIPT_TITLE, id), "") ?: ""

    fun getAccentColor(ctx: Context, id: Int): Int =
        prefs(ctx).getInt(key(KEY_ACCENT_COLOR, id), 0)

    fun getOutput(ctx: Context, id: Int): String =
        prefs(ctx).getString(key(KEY_OUTPUT, id), "") ?: ""

    fun isWidgetUI(ctx: Context, id: Int): Boolean =
        prefs(ctx).getBoolean(key(KEY_IS_WIDGET_UI, id), false)

    fun getLastRun(ctx: Context, id: Int): Long =
        prefs(ctx).getLong(key(KEY_LAST_RUN, id), 0L)

    fun getIntervalMinutes(ctx: Context, id: Int): Long =
        prefs(ctx).getLong(key(KEY_INTERVAL, id), 0L)

    // ---- Setters ----

    fun saveScriptBinding(
        ctx: Context,
        widgetId: Int,
        scriptId: Long,
        title: String,
        accentColor: Int
    ) {
        prefs(ctx).edit()
            .putLong(key(KEY_SCRIPT_ID, widgetId), scriptId)
            .putString(key(KEY_SCRIPT_TITLE, widgetId), title)
            .putInt(key(KEY_ACCENT_COLOR, widgetId), accentColor)
            .apply()
    }

    fun saveOutput(
        ctx: Context,
        widgetId: Int,
        output: String,
        timestamp: Long,
        isWidgetUI: Boolean = false
    ) {
        prefs(ctx).edit()
            .putString(key(KEY_OUTPUT, widgetId), output)
            .putBoolean(key(KEY_IS_WIDGET_UI, widgetId), isWidgetUI)
            .putLong(key(KEY_LAST_RUN, widgetId), timestamp)
            .apply()
    }

    fun saveIntervalMinutes(ctx: Context, widgetId: Int, minutes: Long) {
        prefs(ctx).edit()
            .putLong(key(KEY_INTERVAL, widgetId), minutes)
            .apply()
    }

    fun clearWidget(ctx: Context, widgetId: Int) {
        prefs(ctx).edit()
            .remove(key(KEY_SCRIPT_ID, widgetId))
            .remove(key(KEY_SCRIPT_TITLE, widgetId))
            .remove(key(KEY_ACCENT_COLOR, widgetId))
            .remove(key(KEY_OUTPUT, widgetId))
            .remove(key(KEY_IS_WIDGET_UI, widgetId))
            .remove(key(KEY_LAST_RUN, widgetId))
            .remove(key(KEY_INTERVAL, widgetId))
            .apply()
    }

    // ---- Helpers ----

    private fun key(template: String, id: Int) = template.format(id)
    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
