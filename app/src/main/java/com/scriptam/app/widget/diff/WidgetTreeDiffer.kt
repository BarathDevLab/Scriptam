package com.scriptam.app.widget.diff

import android.graphics.Color
import android.widget.RemoteViews
import com.scriptam.app.R
import com.scriptam.app.widget.model.CardWidgetPayload
import com.scriptam.app.widget.model.ConsoleWidgetPayload
import com.scriptam.app.widget.model.ListWidgetPayload
import com.scriptam.app.widget.model.ProgressWidgetPayload
import com.scriptam.app.widget.model.StatWidgetPayload
import com.scriptam.app.widget.model.WidgetPayload

/**
 * Result of comparing two [WidgetPayload] states.
 */
data class WidgetDiffResult(
    val hasChanges: Boolean,
    val bgChanged: Boolean = false,
    val paddingChanged: Boolean = false,
    val contentChanged: Boolean = false,
    val buttonsChanged: Boolean = false,
    val changeCount: Int = 0
)

/**
 * Surgical tree-differ for [WidgetPayload] and [RemoteViews].
 *
 * Avoids full RemoteViews teardown and rebuild when only atomic values
 * (such as stat value, progress percentage, or timestamp) change,
 * preventing homescreen widget flicker.
 */
object WidgetTreeDiffer {

    /**
     * Compares [oldPayload] and [newPayload] to identify exact mutated segments.
     */
    fun diff(oldPayload: WidgetPayload?, newPayload: WidgetPayload): WidgetDiffResult {
        if (oldPayload == null) {
            return WidgetDiffResult(
                hasChanges = true,
                bgChanged = true,
                paddingChanged = true,
                contentChanged = true,
                buttonsChanged = true,
                changeCount = 4
            )
        }

        if (oldPayload::class != newPayload::class) {
            return WidgetDiffResult(
                hasChanges = true,
                bgChanged = true,
                paddingChanged = true,
                contentChanged = true,
                buttonsChanged = true,
                changeCount = 4
            )
        }

        var changes = 0
        val bgChanged = oldPayload.bg != newPayload.bg
        if (bgChanged) changes++

        val paddingChanged = oldPayload.padding != newPayload.padding
        if (paddingChanged) changes++

        val buttonsChanged = oldPayload.buttons != newPayload.buttons ||
                oldPayload.buttonText != newPayload.buttonText

        if (buttonsChanged) changes++

        val contentChanged = when {
            oldPayload is StatWidgetPayload && newPayload is StatWidgetPayload -> {
                oldPayload.title != newPayload.title ||
                        oldPayload.value != newPayload.value ||
                        oldPayload.subtitle != newPayload.subtitle ||
                        oldPayload.color != newPayload.color
            }
            oldPayload is ProgressWidgetPayload && newPayload is ProgressWidgetPayload -> {
                oldPayload.title != newPayload.title ||
                        oldPayload.progress != newPayload.progress ||
                        oldPayload.value != newPayload.value ||
                        oldPayload.color != newPayload.color
            }
            oldPayload is ListWidgetPayload && newPayload is ListWidgetPayload -> {
                oldPayload.title != newPayload.title ||
                        oldPayload.items != newPayload.items
            }
            oldPayload is CardWidgetPayload && newPayload is CardWidgetPayload -> {
                oldPayload.title != newPayload.title ||
                        oldPayload.items != newPayload.items ||
                        oldPayload.footerText != newPayload.footerText
            }
            oldPayload is ConsoleWidgetPayload && newPayload is ConsoleWidgetPayload -> {
                oldPayload.output != newPayload.output
            }
            else -> oldPayload != newPayload
        }

        if (contentChanged) changes++

        return WidgetDiffResult(
            hasChanges = changes > 0,
            bgChanged = bgChanged,
            paddingChanged = paddingChanged,
            contentChanged = contentChanged,
            buttonsChanged = buttonsChanged,
            changeCount = changes
        )
    }

    /**
     * Applies surgical updates to [views] based on the diff result.
     * Returns true if surgical application succeeded, or false if a full rebuild is required.
     */
    fun applySurgicalUpdate(
        views: RemoteViews,
        diff: WidgetDiffResult,
        oldPayload: WidgetPayload?,
        newPayload: WidgetPayload
    ): Boolean {
        if (!diff.hasChanges) return true

        // If background changed, apply directly
        if (diff.bgChanged && newPayload.bg != null) {
            try {
                val color = Color.parseColor(newPayload.bg)
                views.setInt(R.id.widget_root, "setBackgroundColor", color)
            } catch (_: Exception) {}
        }

        // Surgical updates for stat widgets
        if (oldPayload is StatWidgetPayload && newPayload is StatWidgetPayload) {
            if (oldPayload.title != newPayload.title) {
                views.setTextViewText(R.id.widget_script_title, newPayload.title)
            }
            if (oldPayload.value != newPayload.value) {
                views.setTextViewText(R.id.widget_stat_value, newPayload.value)
            }
            if (oldPayload.subtitle != newPayload.subtitle) {
                views.setTextViewText(R.id.widget_stat_subtitle, newPayload.subtitle ?: "")
            }
            return true
        }

        // Surgical updates for progress widgets
        if (oldPayload is ProgressWidgetPayload && newPayload is ProgressWidgetPayload) {
            if (oldPayload.title != newPayload.title) {
                views.setTextViewText(R.id.widget_script_title, newPayload.title)
            }
            if (oldPayload.progress != newPayload.progress) {
                views.setProgressBar(R.id.widget_progress_bar, 100, newPayload.progress.coerceIn(0, 100), false)
                views.setTextViewText(R.id.widget_stat_value, newPayload.value ?: "${newPayload.progress}%")
            }
            return true
        }

        // Complex lists or layout transitions require full rebuild
        return false
    }
}
