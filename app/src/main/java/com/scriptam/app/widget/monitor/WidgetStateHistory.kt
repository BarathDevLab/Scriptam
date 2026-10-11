package com.scriptam.app.widget.monitor

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque

data class WidgetSnapshot(
    val id: String = java.util.UUID.randomUUID().toString(),
    val widgetId: Int,
    val scriptId: Long,
    val payloadJson: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Stores the last 10 visual payload snapshots per script for inspection and regression tracking.
 */
object WidgetStateHistory {
    private const val MAX_SNAPSHOTS = 10
    private val history = ConcurrentHashMap<Long, ConcurrentLinkedDeque<WidgetSnapshot>>()

    fun recordSnapshot(widgetId: Int, scriptId: Long, payloadJson: String) {
        val deque = history.computeIfAbsent(scriptId) { ConcurrentLinkedDeque() }
        deque.addFirst(
            WidgetSnapshot(
                widgetId = widgetId,
                scriptId = scriptId,
                payloadJson = payloadJson
            )
        )
        while (deque.size > MAX_SNAPSHOTS) {
            deque.removeLast()
        }
    }

    fun getSnapshots(scriptId: Long): List<WidgetSnapshot> =
        history[scriptId]?.toList() ?: emptyList()

    fun getAllSnapshots(): List<WidgetSnapshot> =
        history.values.flatMap { it.toList() }.sortedByDescending { it.timestamp }
}
