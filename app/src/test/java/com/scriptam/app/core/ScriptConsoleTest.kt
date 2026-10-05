package com.scriptam.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScriptConsoleTest {

    @Test
    fun append_respectsMaxEntries_evictsOldest() {
        val console = ScriptConsole(maxEntries = 5)

        for (i in 1..10) {
            console.log("Message $i")
        }

        val entries = console.entries.value
        assertEquals(5, entries.size)
        // Oldest (1..5) should be evicted; 6..10 should remain
        assertEquals("Message 6", entries[0].message)
        assertEquals("Message 7", entries[1].message)
        assertEquals("Message 8", entries[2].message)
        assertEquals("Message 9", entries[3].message)
        assertEquals("Message 10", entries[4].message)
    }

    @Test
    fun append_allLevels_correctlyRecorded() {
        val console = ScriptConsole()
        console.log("Log msg")
        console.warn("Warn msg")
        console.error("Error msg")
        console.info("Info msg")

        val entries = console.entries.value
        assertEquals(4, entries.size)
        assertEquals(ConsoleEntry.Level.LOG, entries[0].level)
        assertEquals(ConsoleEntry.Level.WARN, entries[1].level)
        assertEquals(ConsoleEntry.Level.ERROR, entries[2].level)
        assertEquals(ConsoleEntry.Level.INFO, entries[3].level)
    }

    @Test
    fun clear_emptiesAllEntries() {
        val console = ScriptConsole()
        console.log("A")
        console.log("B")
        assertEquals(2, console.entries.value.size)

        console.clear()
        assertTrue(console.entries.value.isEmpty())
    }
}
