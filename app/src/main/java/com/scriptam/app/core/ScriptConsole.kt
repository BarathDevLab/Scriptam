package com.scriptam.app.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Captures `console.log()`, `console.warn()`, `console.error()`, and `console.info()`
 * calls from the QuickJS context and exposes them as a [StateFlow] for the UI.
 */
class ScriptConsole {

    private val _entries = MutableStateFlow<List<ConsoleEntry>>(emptyList())
    val entries: StateFlow<List<ConsoleEntry>> = _entries.asStateFlow()

    fun log(message: String) = append(message, ConsoleEntry.Level.LOG)
    fun warn(message: String) = append(message, ConsoleEntry.Level.WARN)
    fun error(message: String) = append(message, ConsoleEntry.Level.ERROR)
    fun info(message: String) = append(message, ConsoleEntry.Level.INFO)

    fun clear() {
        _entries.value = emptyList()
    }

    private fun append(message: String, level: ConsoleEntry.Level) {
        _entries.update { current ->
            current + ConsoleEntry(message = message, level = level)
        }
    }
}
