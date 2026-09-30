package com.scriptam.app.core

/**
 * Represents a single console output entry captured from JavaScript execution.
 */
data class ConsoleEntry(
    val message: String,
    val level: Level,
    val timestamp: Long = System.currentTimeMillis()
) {
    enum class Level { LOG, WARN, ERROR, INFO }
}
