package com.scriptam.app.widget.monitor

import java.util.concurrent.ConcurrentLinkedDeque

data class ExecutionLogEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val scriptId: Long,
    val scriptTitle: String,
    val status: String, // "SUCCESS", "ERROR", "TIMEOUT"
    val durationMs: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val error: String? = null
)

/**
 * Thread-safe ring buffer store capturing the last N executions for diagnostics.
 */
object ExecutionLogStore {
    private const val MAX_LOGS = 50
    private val logs = ConcurrentLinkedDeque<ExecutionLogEntry>()

    fun record(
        scriptId: Long,
        scriptTitle: String,
        status: String,
        durationMs: Long,
        error: String? = null
    ) {
        logs.addFirst(
            ExecutionLogEntry(
                scriptId = scriptId,
                scriptTitle = scriptTitle,
                status = status,
                durationMs = durationMs,
                error = error
            )
        )
        while (logs.size > MAX_LOGS) {
            logs.removeLast()
        }
    }

    fun getLogs(): List<ExecutionLogEntry> = logs.toList()

    fun getLogsForScript(scriptId: Long): List<ExecutionLogEntry> =
        logs.filter { it.scriptId == scriptId }

    fun clear() {
        logs.clear()
    }
}
