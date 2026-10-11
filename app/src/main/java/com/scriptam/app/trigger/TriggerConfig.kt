package com.scriptam.app.trigger

import android.content.Context
import android.content.Intent

/**
 * Supported system and custom broadcast trigger events.
 */
enum class SystemTriggerEvent(val action: String, val displayName: String) {
    POWER_CONNECTED(Intent.ACTION_POWER_CONNECTED, "Power Connected"),
    POWER_DISCONNECTED(Intent.ACTION_POWER_DISCONNECTED, "Power Disconnected"),
    BATTERY_LOW(Intent.ACTION_BATTERY_LOW, "Battery Low"),
    BOOT_COMPLETED(Intent.ACTION_BOOT_COMPLETED, "Device Booted"),
    CUSTOM("com.scriptam.app.TRIGGER", "Custom Broadcast");

    companion object {
        fun fromAction(action: String?): SystemTriggerEvent? =
            values().firstOrNull { it.action == action }
    }
}

data class ScriptTriggerBinding(
    val scriptId: Long,
    val event: SystemTriggerEvent,
    val isEnabled: Boolean = true
)

/**
 * Manages persisted trigger bindings between system broadcasts and scripts.
 */
class TriggerManager(context: Context) {
    private val prefs = context.getSharedPreferences("scriptam_triggers", Context.MODE_PRIVATE)

    fun bindTrigger(scriptId: Long, event: SystemTriggerEvent) {
        val current = getBindingsForEvent(event).toMutableSet()
        current.add(scriptId)
        prefs.edit().putStringSet(event.action, current.map { it.toString() }.toSet()).apply()
    }

    fun unbindTrigger(scriptId: Long, event: SystemTriggerEvent) {
        val current = getBindingsForEvent(event).toMutableSet()
        current.remove(scriptId)
        prefs.edit().putStringSet(event.action, current.map { it.toString() }.toSet()).apply()
    }

    fun getBindingsForEvent(event: SystemTriggerEvent): Set<Long> {
        val set = prefs.getStringSet(event.action, emptySet()) ?: emptySet()
        return set.mapNotNull { it.toLongOrNull() }.toSet()
    }
}
