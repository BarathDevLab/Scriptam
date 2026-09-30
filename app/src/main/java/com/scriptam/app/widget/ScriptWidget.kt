package com.scriptam.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import com.scriptam.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Home-screen AppWidget that displays script output and lets users trigger
 * script execution with a single tap on the run button.
 *
 * Each widget instance is bound to a specific script ID stored in
 * [WidgetPrefs]. The widget is updated:
 *  1. After the configuration activity picks a script.
 *  2. When the run button is tapped (triggers [ScriptWidgetRunService]).
 *  3. After a script finishes and calls [updateWidgetOutput].
 */
class ScriptWidget : AppWidgetProvider() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (widgetId in appWidgetIds) {
            refreshWidget(context, appWidgetManager, widgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == ACTION_RUN_SCRIPT) {
            val widgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
            val scriptId = intent.getLongExtra(EXTRA_SCRIPT_ID, -1L)
            if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID && scriptId != -1L) {
                handleRunScript(context, widgetId, scriptId)
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        for (widgetId in appWidgetIds) {
            WidgetPrefs.clearWidget(context, widgetId)
        }
    }

    // -------------------------------------------------------------------------

    private fun handleRunScript(context: Context, widgetId: Int, scriptId: Long) {
        val manager = AppWidgetManager.getInstance(context)
        // Show "running…" state immediately
        val views = buildViews(context, widgetId, output = "Running…", timestamp = null)
        manager.updateAppWidget(widgetId, views)

        // Kick off execution via WorkManager (reuses the existing ScriptWorker)
        scope.launch {
            com.scriptam.app.worker.ScriptWorker.enqueueOnce(context, scriptId)
        }
    }

    companion object {
        const val ACTION_RUN_SCRIPT = "com.scriptam.app.widget.ACTION_RUN_SCRIPT"
        const val EXTRA_SCRIPT_ID = "extra_script_id"

        /**
         * Rebuild and push a widget update.
         * Called from [WidgetConfigActivity] after setup, and from
         * [WidgetOutputUpdater] after a script run completes.
         */
        fun refreshWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            widgetId: Int,
            output: String? = null,
            timestamp: Long? = null
        ) {
            val resolvedOutput = output ?: WidgetPrefs.getOutput(context, widgetId)
            val resolvedTime = timestamp ?: WidgetPrefs.getLastRun(context, widgetId)
            val views = buildViews(context, widgetId, resolvedOutput, resolvedTime)
            appWidgetManager.updateAppWidget(widgetId, views)
        }

        /** Update stored output and refresh the widget UI. */
        fun updateWidgetOutput(
            context: Context,
            widgetId: Int,
            output: String,
            timestamp: Long = System.currentTimeMillis()
        ) {
            WidgetPrefs.saveOutput(context, widgetId, output, timestamp)
            val manager = AppWidgetManager.getInstance(context)
            refreshWidget(context, manager, widgetId, output, timestamp)
        }

        // -------------------------------------------------------------------------

        private fun buildViews(
            context: Context,
            widgetId: Int,
            output: String?,
            timestamp: Long?
        ): RemoteViews {
            val scriptId = WidgetPrefs.getScriptId(context, widgetId)
            val scriptTitle = WidgetPrefs.getScriptTitle(context, widgetId)
            val accentColorInt = WidgetPrefs.getAccentColor(context, widgetId)

            return RemoteViews(context.packageName, R.layout.widget_script).apply {
                // Title
                setTextViewText(R.id.widget_script_title, scriptTitle.ifBlank { "Script" })

                // Output text
                setTextViewText(
                    R.id.widget_output,
                    output.takeIf { !it.isNullOrBlank() } ?: "Tap ▶ to run"
                )

                // Timestamp footer
                setTextViewText(
                    R.id.widget_timestamp,
                    if (timestamp != null && timestamp > 0)
                        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
                    else "–"
                )

                // Accent dot tint
                if (accentColorInt != 0) {
                    setInt(R.id.widget_accent_dot, "setColorFilter", accentColorInt)
                }

                // Run button PendingIntent
                val runIntent = Intent(context, ScriptWidget::class.java).apply {
                    action = ACTION_RUN_SCRIPT
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                    putExtra(EXTRA_SCRIPT_ID, scriptId)
                }
                val runPi = PendingIntent.getBroadcast(
                    context,
                    widgetId,
                    runIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                setOnClickPendingIntent(R.id.widget_run_button, runPi)

                // Tap on widget body → open the app
                val openIntent = context.packageManager
                    .getLaunchIntentForPackage(context.packageName)
                if (openIntent != null) {
                    val openPi = PendingIntent.getActivity(
                        context, widgetId + 1000, openIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    setOnClickPendingIntent(R.id.widget_root, openPi)
                }
            }
        }
    }
}
