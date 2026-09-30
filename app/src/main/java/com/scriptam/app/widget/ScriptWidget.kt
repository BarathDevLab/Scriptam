package com.scriptam.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import com.scriptam.app.R
import com.scriptam.app.widget.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Home-screen AppWidget that displays script output or a rich custom UI (via `widgetUI()`)
 * and lets users trigger script execution with a single tap on the run button.
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
            val action = intent.getStringExtra(EXTRA_ACTION)
            if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID && scriptId != -1L) {
                handleRunScript(context, widgetId, scriptId, action)
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

    private fun handleRunScript(context: Context, widgetId: Int, scriptId: Long, action: String? = null) {
        // Kick off execution via WorkManager immediately
        scope.launch {
            com.scriptam.app.worker.ScriptWorker.enqueueOnce(context, scriptId, action)
        }
    }

    companion object {
        const val ACTION_RUN_SCRIPT = "com.scriptam.app.widget.ACTION_RUN_SCRIPT"
        const val EXTRA_SCRIPT_ID = "extra_script_id"
        const val EXTRA_ACTION = "extra_action"

        /**
         * Rebuild and push a widget update.
         */
        fun refreshWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            widgetId: Int,
            output: String? = null,
            timestamp: Long? = null,
            isWidgetUI: Boolean? = null
        ) {
            val resolvedOutput = output ?: WidgetPrefs.getOutput(context, widgetId)
            val resolvedTime = timestamp ?: WidgetPrefs.getLastRun(context, widgetId)
            val resolvedIsUI = isWidgetUI ?: WidgetPrefs.isWidgetUI(context, widgetId)
            val views = buildViews(context, widgetId, resolvedOutput, resolvedTime, resolvedIsUI)
            appWidgetManager.updateAppWidget(widgetId, views)
        }

        /** Update stored output and refresh the widget UI. */
        fun updateWidgetOutput(
            context: Context,
            widgetId: Int,
            output: String,
            timestamp: Long = System.currentTimeMillis(),
            isWidgetUI: Boolean = false
        ) {
            WidgetPrefs.saveOutput(context, widgetId, output, timestamp, isWidgetUI)
            val manager = AppWidgetManager.getInstance(context)
            refreshWidget(context, manager, widgetId, output, timestamp, isWidgetUI)
        }

        // -------------------------------------------------------------------------

        private fun buildViews(
            context: Context,
            widgetId: Int,
            output: String?,
            timestamp: Long?,
            isWidgetUI: Boolean
        ): RemoteViews {
            val scriptId = WidgetPrefs.getScriptId(context, widgetId)
            val scriptTitle = WidgetPrefs.getScriptTitle(context, widgetId)
            val accentColorInt = WidgetPrefs.getAccentColor(context, widgetId)

            val views = RemoteViews(context.packageName, R.layout.widget_script)

            // Title
            views.setTextViewText(R.id.widget_script_title, scriptTitle.ifBlank { "Script" })

            // Accent dot tint
            if (accentColorInt != 0) {
                views.setInt(R.id.widget_accent_dot, "setColorFilter", accentColorInt)
            }

            // Timestamp footer
            views.setTextViewText(
                R.id.widget_timestamp,
                if (timestamp != null && timestamp > 0)
                    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
                else "–"
            )

            // Render either structured WidgetUI or plain console output
            if (isWidgetUI && !output.isNullOrBlank()) {
                val payload = WidgetPayloadParser.parse(output)
                renderWidgetUI(context, views, payload, widgetId, scriptId)
            } else {
                renderConsoleOutput(views, output)
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
            views.setOnClickPendingIntent(R.id.widget_run_button, runPi)

            // Tap on widget body → open the app
            val openIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            if (openIntent != null) {
                val openPi = PendingIntent.getActivity(
                    context, widgetId + 1000, openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_root, openPi)
            }

            return views
        }

        private fun renderWidgetUI(
            context: Context,
            views: RemoteViews,
            payload: WidgetPayload,
            widgetId: Int,
            scriptId: Long
        ) {
            views.setViewVisibility(R.id.widget_ui_container, View.VISIBLE)
            views.setViewVisibility(R.id.widget_output, View.GONE)

            // Custom card background color
            payload.bg?.let { bgStr ->
                try {
                    val color = Color.parseColor(bgStr)
                    views.setInt(R.id.widget_root, "setBackgroundColor", color)
                } catch (_: Exception) {}
            }

            // Custom padding
            payload.padding?.let { p ->
                val density = context.resources.displayMetrics.density
                val leftPx = (p.left * density).toInt()
                val topPx = (p.top * density).toInt()
                val rightPx = (p.right * density).toInt()
                val bottomPx = (p.bottom * density).toInt()
                views.setViewPadding(R.id.widget_root, leftPx, topPx, rightPx, bottomPx)
            }

            // Action buttons setup (header & bottom toolbar)
            val allButtons = if (payload.buttons.isNotEmpty()) {
                payload.buttons
            } else if (!payload.buttonText.isNullOrBlank()) {
                listOf(
                    WidgetButton(
                        text = payload.buttonText,
                        action = payload.buttonAction ?: "button_click",
                        color = payload.buttonColor,
                        position = payload.buttonPosition ?: "header"
                    )
                )
            } else {
                emptyList()
            }

            val headerBtn = allButtons.firstOrNull { it.position.lowercase() == "header" }
            val bottomButtons = allButtons.filter { it.position.lowercase() != "header" }

            // 1. Header Button
            if (headerBtn != null) {
                val label = headerBtn.icon?.let { if (headerBtn.text != null) "$it ${headerBtn.text}" else it }
                    ?: headerBtn.text ?: ""
                views.setViewVisibility(R.id.widget_action_button, View.VISIBLE)
                views.setTextViewText(R.id.widget_action_button, label)
                headerBtn.color?.let {
                    try { views.setTextColor(R.id.widget_action_button, Color.parseColor(it)) } catch (_: Exception) {}
                }
                headerBtn.fontSize?.let { size ->
                    views.setTextViewTextSize(R.id.widget_action_button, TypedValue.COMPLEX_UNIT_SP, size)
                }
                headerBtn.bg?.let { bgStr ->
                    try { views.setInt(R.id.widget_action_button, "setBackgroundColor", Color.parseColor(bgStr)) } catch (_: Exception) {}
                }

                val actionIntent = Intent(context, ScriptWidget::class.java).apply {
                    action = ACTION_RUN_SCRIPT
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                    putExtra(EXTRA_SCRIPT_ID, scriptId)
                    putExtra(EXTRA_ACTION, headerBtn.action)
                }
                val actionPi = PendingIntent.getBroadcast(
                    context,
                    widgetId + 2000,
                    actionIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_action_button, actionPi)
            } else {
                views.setViewVisibility(R.id.widget_action_button, View.GONE)
            }

            // 2. Bottom Toolbar Buttons (1 to 4 buttons, e.g. music player or multi-action bar)
            if (bottomButtons.isNotEmpty()) {
                views.setViewVisibility(R.id.widget_button_bar, View.VISIBLE)
                views.setViewVisibility(R.id.widget_ui_button, View.GONE)

                val buttonSlotIds = intArrayOf(
                    R.id.widget_btn_1,
                    R.id.widget_btn_2,
                    R.id.widget_btn_3,
                    R.id.widget_btn_4
                )

                for (i in 0 until 4) {
                    val slotId = buttonSlotIds[i]
                    if (i < bottomButtons.size) {
                        val btn = bottomButtons[i]
                        views.setViewVisibility(slotId, View.VISIBLE)
                        val label = btn.icon?.let { if (btn.text != null) "$it ${btn.text}" else it }
                            ?: btn.text ?: ""
                        views.setTextViewText(slotId, label)
                        btn.color?.let {
                            try { views.setTextColor(slotId, Color.parseColor(it)) } catch (_: Exception) {}
                        }
                        btn.fontSize?.let { size ->
                            views.setTextViewTextSize(slotId, TypedValue.COMPLEX_UNIT_SP, size)
                        }
                        btn.bg?.let { bgStr ->
                            try { views.setInt(slotId, "setBackgroundColor", Color.parseColor(bgStr)) } catch (_: Exception) {}
                        }

                        val actionIntent = Intent(context, ScriptWidget::class.java).apply {
                            action = ACTION_RUN_SCRIPT
                            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                            putExtra(EXTRA_SCRIPT_ID, scriptId)
                            putExtra(EXTRA_ACTION, btn.action)
                        }
                        val actionPi = PendingIntent.getBroadcast(
                            context,
                            widgetId + 2100 + i,
                            actionIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        views.setOnClickPendingIntent(slotId, actionPi)
                    } else {
                        views.setViewVisibility(slotId, View.GONE)
                    }
                }
            } else {
                views.setViewVisibility(R.id.widget_button_bar, View.GONE)
                views.setViewVisibility(R.id.widget_ui_button, View.GONE)
            }

            when (payload) {
                is StatWidgetPayload -> {
                    views.setViewVisibility(R.id.widget_stat_value, View.VISIBLE)
                    views.setTextViewText(R.id.widget_stat_value, payload.value)
                    payload.color?.let {
                        try { views.setTextColor(R.id.widget_stat_value, Color.parseColor(it)) } catch (_: Exception) {}
                    }
                    payload.fontSize?.let { size ->
                        views.setTextViewTextSize(R.id.widget_stat_value, TypedValue.COMPLEX_UNIT_SP, size)
                    }

                    if (!payload.subtitle.isNullOrBlank()) {
                        views.setViewVisibility(R.id.widget_stat_subtitle, View.VISIBLE)
                        views.setTextViewText(R.id.widget_stat_subtitle, payload.subtitle)
                        payload.subtitleColor?.let {
                            try { views.setTextColor(R.id.widget_stat_subtitle, Color.parseColor(it)) } catch (_: Exception) {}
                        }
                    } else {
                        views.setViewVisibility(R.id.widget_stat_subtitle, View.GONE)
                    }

                    views.setViewVisibility(R.id.widget_progress_bar, View.GONE)
                    views.setViewVisibility(R.id.widget_list_container, View.GONE)
                }
                is ProgressWidgetPayload -> {
                    views.setViewVisibility(R.id.widget_stat_value, View.VISIBLE)
                    views.setTextViewText(R.id.widget_stat_value, payload.value ?: "${payload.progress}%")
                    payload.color?.let {
                        try { views.setTextColor(R.id.widget_stat_value, Color.parseColor(it)) } catch (_: Exception) {}
                    }

                    if (!payload.subtitle.isNullOrBlank()) {
                        views.setViewVisibility(R.id.widget_stat_subtitle, View.VISIBLE)
                        views.setTextViewText(R.id.widget_stat_subtitle, payload.subtitle)
                    } else {
                        views.setViewVisibility(R.id.widget_stat_subtitle, View.GONE)
                    }

                    views.setViewVisibility(R.id.widget_progress_bar, View.VISIBLE)
                    views.setProgressBar(R.id.widget_progress_bar, 100, payload.progress, false)

                    views.setViewVisibility(R.id.widget_list_container, View.GONE)
                }
                is ListWidgetPayload -> {
                    views.setViewVisibility(R.id.widget_stat_value, View.GONE)
                    views.setViewVisibility(R.id.widget_stat_subtitle, View.GONE)
                    views.setViewVisibility(R.id.widget_progress_bar, View.GONE)
                    views.setViewVisibility(R.id.widget_list_container, View.VISIBLE)
                    views.removeAllViews(R.id.widget_list_container)

                    for (item in payload.items.take(4)) {
                        val rowView = RemoteViews(context.packageName, R.layout.widget_row_item)
                        rowView.setTextViewText(R.id.row_label, item.label)
                        rowView.setTextViewText(R.id.row_value, item.value)
                        item.color?.let {
                            try { rowView.setTextColor(R.id.row_value, Color.parseColor(it)) } catch (_: Exception) {}
                        }
                        if (!item.statusColor.isNullOrBlank()) {
                            rowView.setViewVisibility(R.id.row_dot, View.VISIBLE)
                            try { rowView.setInt(R.id.row_dot, "setColorFilter", Color.parseColor(item.statusColor)) } catch (_: Exception) {}
                        } else {
                            rowView.setViewVisibility(R.id.row_dot, View.GONE)
                        }
                        views.addView(R.id.widget_list_container, rowView)
                    }
                }
                is CardWidgetPayload -> {
                    views.setViewVisibility(R.id.widget_stat_value, View.GONE)
                    views.setViewVisibility(R.id.widget_stat_subtitle, View.GONE)
                    views.setViewVisibility(R.id.widget_progress_bar, View.GONE)
                    views.setViewVisibility(R.id.widget_list_container, View.VISIBLE)
                    views.removeAllViews(R.id.widget_list_container)

                    for (item in payload.items.take(4)) {
                        val rowView = RemoteViews(context.packageName, R.layout.widget_row_item)
                        rowView.setTextViewText(R.id.row_label, item.label)
                        rowView.setTextViewText(R.id.row_value, item.value)
                        item.color?.let {
                            try { rowView.setTextColor(R.id.row_value, Color.parseColor(it)) } catch (_: Exception) {}
                        }
                        views.addView(R.id.widget_list_container, rowView)
                    }
                }
                is ConsoleWidgetPayload -> {
                    renderConsoleOutput(views, payload.output)
                }
            }
        }

        private fun renderConsoleOutput(views: RemoteViews, output: String?) {
            views.setViewVisibility(R.id.widget_ui_container, View.GONE)
            views.setViewVisibility(R.id.widget_action_button, View.GONE)
            views.setViewVisibility(R.id.widget_ui_button, View.GONE)
            views.setViewVisibility(R.id.widget_button_bar, View.GONE)
            views.setViewVisibility(R.id.widget_output, View.VISIBLE)
            views.setTextViewText(
                R.id.widget_output,
                output.takeIf { !it.isNullOrBlank() } ?: "Tap ▶ to run"
            )
        }
    }
}
