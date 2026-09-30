package com.scriptam.app.core

import com.dokar.quickjs.QuickJs
import com.dokar.quickjs.binding.function
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Manages the QuickJS virtual-machine lifecycle.
 *
 * All script executions are confined to a single background thread via
 * [Dispatchers.Default.limitedParallelism] to prevent data races and
 * keep the main thread stutter-free.
 */
class ScriptEngineManager {

    private val engineDispatcher: CoroutineDispatcher =
        Dispatchers.Default.limitedParallelism(1)

    /**
     * Evaluates [script] in a fresh QuickJS context with [console] and
     * [bridgeInstaller] bindings injected.
     *
     * @param script        The JavaScript source code to execute.
     * @param console       Console sink for `console.log()` / `.warn()` / `.error()`.
     * @param bridgeInstaller Optional lambda that receives the [QuickJs] instance to install
     *                        native bridge modules (e.g., `Native.showToast()`).
     * @return The result of the evaluated script as a String, or an error message.
     */
    suspend fun execute(
        script: String,
        console: ScriptConsole,
        bridgeInstaller: (suspend (QuickJs) -> Unit)? = null
    ): Result<Any?> = withContext(engineDispatcher) {
        val quickJs = QuickJs.create(jobDispatcher = engineDispatcher)
        try {
            // Inject console.log / .warn / .error / .info
            installConsole(quickJs, console)

            // Let the bridge layer install its own global modules
            bridgeInstaller?.invoke(quickJs)

            // Execute the user's script
            val result = quickJs.evaluate<Any?>(script)
            Result.success(result)
        } catch (e: Exception) {
            console.error(e.message ?: "Unknown script error")
            Result.failure(e)
        } finally {
            quickJs.close()
        }
    }

    /**
     * Installs a `console` global with `.log()`, `.warn()`, `.error()`, `.info()` methods
     * that pipe output back to the Kotlin [ScriptConsole].
     */
    private suspend fun installConsole(quickJs: QuickJs, console: ScriptConsole) {
        // Build a JS-side console object that delegates to Kotlin functions
        quickJs.function<Unit>("__scriptam_log") { args ->
            console.log(args.joinToString(" ") { it.toString() })
        }
        quickJs.function<Unit>("__scriptam_warn") { args ->
            console.warn(args.joinToString(" ") { it.toString() })
        }
        quickJs.function<Unit>("__scriptam_error") { args ->
            console.error(args.joinToString(" ") { it.toString() })
        }
        quickJs.function<Unit>("__scriptam_info") { args ->
            console.info(args.joinToString(" ") { it.toString() })
        }

        // Wire the JS `console` object to our Kotlin callbacks
        quickJs.evaluate<Unit>(
            """
            var console = {
                log:   function() { __scriptam_log.apply(null, arguments);   },
                warn:  function() { __scriptam_warn.apply(null, arguments);  },
                error: function() { __scriptam_error.apply(null, arguments); },
                info:  function() { __scriptam_info.apply(null, arguments);  }
            };
            """.trimIndent()
        )
    }
}
