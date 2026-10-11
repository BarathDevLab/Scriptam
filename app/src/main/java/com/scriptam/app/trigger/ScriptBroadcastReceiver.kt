package com.scriptam.app.trigger

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.scriptam.app.worker.ScriptWorker

/**
 * Listens for system broadcasts (power, battery, boot) and custom trigger intents,
 * delegating script execution to [ScriptWorker].
 */
class ScriptBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val triggerManager = TriggerManager(context)

        // Custom direct trigger
        if (action == SystemTriggerEvent.CUSTOM.action) {
            val scriptId = intent.getLongExtra("script_id", -1L)
            if (scriptId != -1L) {
                ScriptWorker.enqueueOnce(context, scriptId, action = "custom_trigger")
            }
            return
        }

        // Standard system broadcast trigger
        val event = SystemTriggerEvent.fromAction(action) ?: return
        val boundScriptIds = triggerManager.getBindingsForEvent(event)

        for (scriptId in boundScriptIds) {
            ScriptWorker.enqueueOnce(context, scriptId, action = event.name)
        }
    }
}
