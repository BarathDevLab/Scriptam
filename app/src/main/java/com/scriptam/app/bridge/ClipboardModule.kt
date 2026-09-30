package com.scriptam.app.bridge

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.dokar.quickjs.QuickJs
import com.dokar.quickjs.binding.function

/**
 * Exposes clipboard operations to JavaScript:
 * - `Native.clipboard.copy(text)` — Copies text to the system clipboard.
 * - `Native.clipboard.paste()` — Returns the current clipboard text content.
 */
class ClipboardModule(private val context: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())

    fun install(quickJs: QuickJs) {
        quickJs.function<Unit>("__native_clipboard_copy") { args ->
            val text = args.firstOrNull()?.toString() ?: ""
            mainHandler.post {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Scriptam", text))
            }
        }

        quickJs.function<String>("__native_clipboard_paste") { _ ->
            try {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
            } catch (e: Exception) {
                ""
            }
        }
    }
}
