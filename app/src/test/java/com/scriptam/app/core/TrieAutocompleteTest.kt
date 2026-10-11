package com.scriptam.app.core

import com.scriptam.app.core.autocomplete.CompletionCategory
import com.scriptam.app.core.autocomplete.CompletionItem
import com.scriptam.app.core.autocomplete.TrieAutocomplete
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TrieAutocompleteTest {

    private lateinit var trie: TrieAutocomplete

    @Before
    fun setUp() {
        trie = TrieAutocomplete()
    }

    @Test
    fun testDefaultCompletionsContainNativeApis() {
        val results = trie.findCompletions("Native.show")
        assertTrue(results.isNotEmpty())
        assertEquals("Native.showToast", results.first().label)
    }

    @Test
    fun testWidgetCompletions() {
        val results = trie.findCompletions("Widget.c")
        assertTrue(results.any { it.label == "Widget.column" })
    }

    @Test
    fun testCaseInsensitivePrefixMatching() {
        val resultsLower = trie.findCompletions("native.get")
        val resultsUpper = trie.findCompletions("NATIVE.GET")

        assertEquals(resultsLower.size, resultsUpper.size)
        assertTrue(resultsLower.any { it.label == "Native.getBatteryLevel" })
    }

    @Test
    fun testCustomItemInsertion() {
        trie.insert(
            CompletionItem(
                label = "myCustomFunction",
                detail = "User defined helper",
                insertText = "myCustomFunction()",
                category = CompletionCategory.GLOBAL
            )
        )

        val matches = trie.findCompletions("myCustom")
        assertEquals(1, matches.size)
        assertEquals("myCustomFunction", matches.first().label)
    }

    @Test
    fun testBlankQueryReturnsEmpty() {
        val matches = trie.findCompletions("")
        assertTrue(matches.isEmpty())
    }
}
