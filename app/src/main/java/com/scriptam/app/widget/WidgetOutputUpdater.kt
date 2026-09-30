package com.scriptam.app.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context

/**
 * Utility to push captured console output into all widget instances
 * that are bound to a given script.
 *
 * Called by [ScriptWorker] after a background execution completes.
 */
object WidgetOutputUpdater {

    /**
     * Finds every home-screen widget bound to [scriptId] and updates it
     * with the given [output] text and current timestamp.
     */
    fun pushOutput(context: Context, scriptId: Long, output: String) {
        val manager = AppWidgetManager.getInstance(context)
        val widgetIds = manager.getAppWidgetIds(
            ComponentName(context, ScriptWidget::class.java)
        )

        for (widgetId in widgetIds) {
            if (WidgetPrefs.getScriptId(context, widgetId) == scriptId) {
                ScriptWidget.updateWidgetOutput(
                    context = context,
                    widgetId = widgetId,
                    output = output,
                    timestamp = System.currentTimeMillis()
                )
            }
        }
    }
}
