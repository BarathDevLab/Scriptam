package com.scriptam.app.bridge

import android.content.Context
import com.dokar.quickjs.QuickJs
import com.dokar.quickjs.binding.function

/**
 * Exposes a simple key-value storage API to JavaScript:
 * - `Native.storage.get(key)` — Returns the stored string or null.
 * - `Native.storage.set(key, value)` — Persists a string value.
 * - `Native.storage.remove(key)` — Deletes a stored key.
 *
 * Backed by Android SharedPreferences (scoped to "scriptam_js_storage").
 */
class StorageModule(context: Context) {

    private val prefs = context.getSharedPreferences("scriptam_js_storage", Context.MODE_PRIVATE)

    fun install(quickJs: QuickJs) {
        quickJs.function<Any?>("__native_storage_get") { args ->
            val key = args.firstOrNull()?.toString() ?: return@function null
            prefs.getString(key, null)
        }

        quickJs.function<Unit>("__native_storage_set") { args ->
            val key = args.getOrNull(0)?.toString() ?: return@function
            val value = args.getOrNull(1)?.toString() ?: ""
            prefs.edit().putString(key, value).apply()
        }

        quickJs.function<Unit>("__native_storage_remove") { args ->
            val key = args.firstOrNull()?.toString() ?: return@function
            prefs.edit().remove(key).apply()
        }
    }
}
