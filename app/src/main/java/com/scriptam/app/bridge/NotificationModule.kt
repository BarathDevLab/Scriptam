package com.scriptam.app.bridge

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import com.dokar.quickjs.QuickJs
import com.dokar.quickjs.binding.function

/**
 * Exposes notification capabilities to JavaScript:
 * - `Native.notify(title, body)` — Shows a system notification.
 */
class NotificationModule(private val context: Context) {

    companion object {
        private const val CHANNEL_ID = "scriptam_scripts"
        private const val CHANNEL_NAME = "Script Notifications"
    }

    init {
        createChannel()
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifications triggered by Scriptam scripts"
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    fun install(quickJs: QuickJs) {
        quickJs.function<Unit>("__native_notify") { args ->
            val title = args.getOrNull(0)?.toString() ?: "Scriptam"
            val body = args.getOrNull(1)?.toString() ?: ""
            val notifId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()

            val manager = context.getSystemService(NotificationManager::class.java)
            manager.notify(notifId, notification)
        }
    }
}
