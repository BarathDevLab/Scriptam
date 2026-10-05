package com.scriptam.app.core

import com.dokar.quickjs.QuickJs
import com.dokar.quickjs.binding.function
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/**
 * Result of script execution containing the return value and optional widget UI JSON.
 */
data class ScriptExecutionResult(
    val value: Any?,
    val widgetPayloadJson: String? = null
)

/**
 * Manages the QuickJS virtual-machine lifecycle.
 *
 * All script executions are confined to a single background thread via
 * [Dispatchers.Default.limitedParallelism] to prevent data races and
 * keep the main thread stutter-free.
 */
class ScriptEngineManager {

    companion object {
        const val DEFAULT_TIMEOUT_MS = 30_000L

        private val CONSOLE_JS = """
            var console = {
                log:   function() { __scriptam_log.apply(null, arguments);   },
                warn:  function() { __scriptam_warn.apply(null, arguments);  },
                error: function() { __scriptam_error.apply(null, arguments); },
                info:  function() { __scriptam_info.apply(null, arguments);  }
            };
        """.trimIndent()

        private val WIDGET_CHECK_JS = """
            (function() {
                if (typeof widgetUI === 'function') {
                    try {
                        var res = widgetUI();
                        return (res && typeof res === 'object') ? JSON.stringify(res) : null;
                    } catch (e) {
                        console.error("widgetUI error: " + e);
                        return null;
                    }
                }
                return null;
            })()
        """.trimIndent()
    }

    private val engineDispatcher: CoroutineDispatcher =
        Dispatchers.Default.limitedParallelism(1)

    /**
     * Evaluates [script] in a fresh QuickJS context with [console], [Widget] DSL, and
     * [bridgeInstaller] bindings injected.
     *
     * @param script        The JavaScript source code to execute.
     * @param console       Console sink for `console.log()` / `.warn()` / `.error()`.
     * @param timeoutMs     Execution timeout in milliseconds (default 30 seconds).
     * @param bridgeInstaller Optional lambda that receives the [QuickJs] instance to install
     *                        native bridge modules (e.g., `Native.showToast()`).
     * @return [ScriptExecutionResult] containing the evaluated result and any widget UI JSON.
     */
    suspend fun execute(
        script: String,
        console: ScriptConsole,
        action: String? = null,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS,
        bridgeInstaller: (suspend (QuickJs) -> Unit)? = null
    ): Result<ScriptExecutionResult> = withContext(engineDispatcher) {
        try {
            withTimeout(timeoutMs) {
                val quickJs = QuickJs.create(jobDispatcher = engineDispatcher)
                try {
                    // Inject console.log / .warn / .error / .info
                    installConsole(quickJs, console)

                    // Inject declarative Widget DSL
                    installWidgetDsl(quickJs, action)

                    // Let the bridge layer install its own global modules
                    bridgeInstaller?.invoke(quickJs)

                    // Also expose action on Native if installed
                    val actionLiteral = if (action != null) "\"$action\"" else "null"
                    quickJs.evaluate<Any?>(
                        "(function() { if (typeof Native !== 'undefined') { Native.action = $actionLiteral; } })();"
                    )

                    // Execute the user's script
                    val result = quickJs.evaluate<Any?>(script)

                    // Check if widgetUI() was defined and returned a UI payload
                    val widgetJson = try {
                        quickJs.evaluate<String?>(WIDGET_CHECK_JS)
                    } catch (_: Exception) {
                        null
                    }

                    Result.success(ScriptExecutionResult(value = result, widgetPayloadJson = widgetJson))
                } finally {
                    quickJs.close()
                }
            }
        } catch (e: Exception) {
            val msg = if (e is kotlinx.coroutines.TimeoutCancellationException) {
                "Script execution timed out after ${timeoutMs}ms"
            } else {
                e.message ?: "Unknown script error"
            }
            console.error(msg)
            Result.failure(e)
        }
    }

    /**
     * Installs a `console` global with `.log()`, `.warn()`, `.error()`, `.info()` methods
     * that pipe output back to the Kotlin [ScriptConsole].
     */
    private suspend fun installConsole(quickJs: QuickJs, console: ScriptConsole) {
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

        quickJs.evaluate<Any?>(CONSOLE_JS)
    }

    /**
     * Installs the declarative `Widget` DSL for building rich widget UIs.
     */
    private suspend fun installWidgetDsl(quickJs: QuickJs, action: String? = null) {
        val actionLiteral = if (action != null) "\"$action\"" else "null"
        quickJs.evaluate<Any?>(
            """
            var Widget = {
                action: $actionLiteral,
                Button: function(opt) {
                    opt = opt || {};
                    return {
                        text: opt.text || opt.title || opt.label || null,
                        icon: opt.icon || null,
                        action: opt.action || opt.buttonAction || opt.btnAction || "click",
                        color: opt.color || opt.buttonColor || opt.btnColor || null,
                        bg: opt.bg || opt.background || null,
                        size: opt.size || opt.fontSize || null,
                        fontSize: opt.fontSize || opt.size || null,
                        position: opt.position || opt.buttonPosition || opt.btnPosition || "bottom",
                        align: opt.align || "center"
                    };
                },
                Stat: function(opt) {
                    opt = opt || {};
                    return {
                        type: "stat",
                        title: opt.title || "Script",
                        value: String(opt.value != null ? opt.value : ""),
                        subtitle: opt.subtitle || null,
                        color: opt.color || opt.accentColor || null,
                        subtitleColor: opt.subtitleColor || null,
                        fontSize: opt.fontSize || null,
                        bold: opt.bold !== false,
                        fontFamily: opt.fontFamily || opt.font || "default",
                        align: opt.align || "left",
                        buttonText: opt.buttonText || opt.btnText || null,
                        buttonAction: opt.buttonAction || opt.btnAction || null,
                        buttonColor: opt.buttonColor || opt.btnColor || null,
                        buttonPosition: opt.buttonPosition || opt.btnPosition || null,
                        button: opt.button || opt.btn || null,
                        buttons: Array.isArray(opt.buttons) ? opt.buttons : null,
                        bg: opt.bg || opt.background || null,
                        padding: opt.padding != null ? opt.padding : null
                    };
                },
                Progress: function(opt) {
                    opt = opt || {};
                    return {
                        type: "progress",
                        title: opt.title || "Script",
                        value: opt.value != null ? String(opt.value) : null,
                        subtitle: opt.subtitle || null,
                        progress: typeof opt.progress === 'number' ? opt.progress : 0,
                        color: opt.color || opt.accentColor || null,
                        buttonText: opt.buttonText || opt.btnText || null,
                        buttonAction: opt.buttonAction || opt.btnAction || null,
                        buttonColor: opt.buttonColor || opt.btnColor || null,
                        buttonPosition: opt.buttonPosition || opt.btnPosition || null,
                        button: opt.button || opt.btn || null,
                        buttons: Array.isArray(opt.buttons) ? opt.buttons : null,
                        bg: opt.bg || opt.background || null,
                        padding: opt.padding != null ? opt.padding : null
                    };
                },
                List: function(opt) {
                    opt = opt || {};
                    return {
                        type: "list",
                        title: opt.title || "Script",
                        items: Array.isArray(opt.items) ? opt.items : [],
                        buttonText: opt.buttonText || opt.btnText || null,
                        buttonAction: opt.buttonAction || opt.btnAction || null,
                        buttonColor: opt.buttonColor || opt.btnColor || null,
                        buttonPosition: opt.buttonPosition || opt.btnPosition || null,
                        button: opt.button || opt.btn || null,
                        buttons: Array.isArray(opt.buttons) ? opt.buttons : null,
                        bg: opt.bg || opt.background || null,
                        padding: opt.padding != null ? opt.padding : null
                    };
                },
                Card: function(opt) {
                    opt = opt || {};
                    return {
                        type: "card",
                        title: opt.title || null,
                        items: Array.isArray(opt.items) ? opt.items : (Array.isArray(opt.rows) ? opt.rows : []),
                        footerText: opt.footerText || opt.footer || null,
                        buttonText: opt.buttonText || opt.btnText || null,
                        buttonAction: opt.buttonAction || opt.btnAction || null,
                        buttonColor: opt.buttonColor || opt.btnColor || null,
                        buttonPosition: opt.buttonPosition || opt.btnPosition || null,
                        button: opt.button || opt.btn || null,
                        buttons: Array.isArray(opt.buttons) ? opt.buttons : null,
                        bg: opt.bg || opt.background || null,
                        padding: opt.padding != null ? opt.padding : null
                    };
                },
                Text: function(text, style) {
                    style = style || {};
                    return {
                        type: "text",
                        text: String(text != null ? text : ""),
                        color: style.color || null,
                        fontSize: style.fontSize || null,
                        bold: !!style.bold,
                        fontFamily: style.fontFamily || style.font || "default",
                        align: style.align || "left"
                    };
                },
                Badge: function(text, style) {
                    style = style || {};
                    return {
                        type: "badge",
                        text: String(text != null ? text : ""),
                        color: style.color || null,
                        bg: style.bg || style.background || null
                    };
                },
                ProgressBar: function(opt) {
                    opt = opt || {};
                    return {
                        type: "progressBar",
                        progress: typeof opt.progress === 'number' ? opt.progress : 0,
                        color: opt.color || null
                    };
                },
                Spacer: function(height) {
                    return {
                        type: "spacer",
                        height: typeof height === 'number' ? height : 8
                    };
                },
                Divider: function(color) {
                    return {
                        type: "divider",
                        color: color || null
                    };
                }
            };
            """.trimIndent()
        )
    }
}
