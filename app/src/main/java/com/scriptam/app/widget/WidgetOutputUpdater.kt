package com.scriptam.app.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context

/**
 * Utility to push captured output or widgetUI JSON into all widget instances
 * that are bound to a given script.
 */
object WidgetOutputUpdater {

    /**
     * Finds every home-screen widget bound to [scriptId] and updates it
     * with the given [output] text and current timestamp.
     */
    fun pushOutput(
        context: Context,
        scriptId: Long,
        output: String,
        isWidgetUI: Boolean = false
    ) {
        val indexedIds = WidgetPrefs.getWidgetIdsForScript(context, scriptId)
        val targetIds: Collection<Int> = if (indexedIds.isNotEmpty()) {
            indexedIds
        } else {
            // Fallback for widgets bound before the inverted index was populated
            val manager = AppWidgetManager.getInstance(context)
            val allWidgetIds = manager.getAppWidgetIds(
                ComponentName(context, ScriptWidget::class.java)
            )
            allWidgetIds.filter { WidgetPrefs.getScriptId(context, it) == scriptId }
        }

        for (widgetId in targetIds) {
            ScriptWidget.updateWidgetOutput(
                context = context,
                widgetId = widgetId,
                output = output,
                timestamp = System.currentTimeMillis(),
                isWidgetUI = isWidgetUI
            )
        }
    }
}
