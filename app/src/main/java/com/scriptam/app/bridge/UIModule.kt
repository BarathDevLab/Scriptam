package com.scriptam.app.bridge

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.dokar.quickjs.QuickJs
import com.dokar.quickjs.binding.function

/**
 * Exposes UI-related native functions to JavaScript:
 * - `Native.showToast(message)` — Shows an Android Toast.
 * - `Native.alert(title, message)` — Logs an alert (dialog support can be added later).
 */
class UIModule(private val context: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())

    fun install(quickJs: QuickJs) {
        quickJs.function<Unit>("__native_showToast") { args ->
            val message = args.firstOrNull()?.toString() ?: ""
            mainHandler.post {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }

        quickJs.function<Unit>("__native_alert") { args ->
            val title = args.getOrNull(0)?.toString() ?: "Alert"
            val message = args.getOrNull(1)?.toString() ?: ""
            // For now, show as a long toast. Full AlertDialog support requires
            // an Activity reference and will be added in Phase 5.
            mainHandler.post {
                Toast.makeText(context, "$title: $message", Toast.LENGTH_LONG).show()
            }
        }
    }
}
