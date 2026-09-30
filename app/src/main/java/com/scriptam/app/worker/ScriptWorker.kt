package com.scriptam.app.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.scriptam.app.ScriptamApp
import com.scriptam.app.bridge.AndroidBridge
import com.scriptam.app.bridge.NetworkModule
import com.scriptam.app.bridge.StorageModule
import com.scriptam.app.bridge.UIModule
import com.scriptam.app.core.ScriptConsole
import com.scriptam.app.core.ScriptEngineManager
import com.scriptam.app.widget.WidgetOutputUpdater
import com.dokar.quickjs.QuickJs
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker that executes a Scriptam script in the background.
 * After execution, the console output is pushed to any home-screen widgets
 * bound to the same script via [WidgetOutputUpdater].
 *
 * Usage:
 * ```
 * ScriptWorker.enqueueOnce(context, scriptId = 42L)
 * ScriptWorker.enqueueRepeating(context, scriptId = 42L, intervalMinutes = 15)
 * ScriptWorker.cancel(context, scriptId = 42L)
 * ```
 */
class ScriptWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val scriptId = inputData.getLong(KEY_SCRIPT_ID, -1L)
        if (scriptId == -1L) return Result.failure()

        val app = applicationContext as ScriptamApp
        val repository = app.scriptRepository

        val script = repository.getScriptById(scriptId) ?: return Result.failure()
        val code = repository.readScriptContent(script.fileName)
        if (code.isBlank()) return Result.failure()

        val engineManager = ScriptEngineManager()
        val console = ScriptConsole()
        val bridge = AndroidBridge(
            context = applicationContext,
            uiModule = UIModule(applicationContext),
            storageModule = StorageModule(applicationContext),
            networkModule = NetworkModule()
        )

        val action = inputData.getString(KEY_ACTION)

        val result = engineManager.execute(
            script = code,
            console = console,
            action = action,
            bridgeInstaller = { quickJs -> bridge.install(quickJs) }
        )

        val widgetJson = result.getOrNull()?.widgetPayloadJson
        if (widgetJson != null) {
            WidgetOutputUpdater.pushOutput(
                context = applicationContext,
                scriptId = scriptId,
                output = widgetJson,
                isWidgetUI = true
            )
        } else {
            // Collect the console output as a single string for the widget
            val outputText = console.entries.value.joinToString("\n") { entry ->
                "${entry.level.name}: ${entry.message}"
            }
            WidgetOutputUpdater.pushOutput(
                context = applicationContext,
                scriptId = scriptId,
                output = outputText,
                isWidgetUI = false
            )
        }

        return if (result.isSuccess) Result.success() else Result.retry()
    }

    companion object {
        const val KEY_SCRIPT_ID = "script_id"
        const val KEY_ACTION = "action"
        private const val TAG_PREFIX = "scriptam_script_"

        /** Schedule a one-time background execution of a script. */
        fun enqueueOnce(context: Context, scriptId: Long, action: String? = null) {
            val data = Data.Builder()
                .putLong(KEY_SCRIPT_ID, scriptId)
                .apply {
                    if (action != null) {
                        putString(KEY_ACTION, action)
                    }
                }
                .build()

            val request = OneTimeWorkRequestBuilder<ScriptWorker>()
                .setInputData(data)
                .addTag("$TAG_PREFIX$scriptId")
                .build()

            WorkManager.getInstance(context).enqueue(request)
        }

        /** Schedule a repeating background execution (minimum 15 min interval). */
        fun enqueueRepeating(
            context: Context,
            scriptId: Long,
            intervalMinutes: Long = 15
        ) {
            val data = Data.Builder()
                .putLong(KEY_SCRIPT_ID, scriptId)
                .build()

            val request = PeriodicWorkRequestBuilder<ScriptWorker>(
                intervalMinutes, TimeUnit.MINUTES
            )
                .setInputData(data)
                .addTag("$TAG_PREFIX$scriptId")
                .build()

            WorkManager.getInstance(context).enqueue(request)
        }

        /** Cancel all scheduled executions for a specific script. */
        fun cancel(context: Context, scriptId: Long) {
            WorkManager.getInstance(context)
                .cancelAllWorkByTag("$TAG_PREFIX$scriptId")
        }
    }
}
