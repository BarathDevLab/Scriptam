package com.scriptam.app.core.autocomplete

/**
 * Autocomplete item returned by Trie prefix matching.
 */
data class CompletionItem(
    val label: String,
    val detail: String = "",
    val insertText: String = label,
    val category: CompletionCategory = CompletionCategory.KEYWORD
)

enum class CompletionCategory {
    NATIVE,
    WIDGET,
    KEYWORD,
    GLOBAL,
    SNIPPET
}

/**
 * Trie node supporting prefix queries and scored suggestions.
 */
class TrieNode(val char: Char? = null) {
    val children = mutableMapOf<Char, TrieNode>()
    var isWord: Boolean = false
    var item: CompletionItem? = null
}

/**
 * High-performance prefix-matching trie for Scriptam editor autocomplete.
 */
class TrieAutocomplete {

    private val root = TrieNode()

    init {
        loadDefaultCompletions()
    }

    fun insert(item: CompletionItem) {
        var current = root
        for (ch in item.label.lowercase()) {
            current = current.children.getOrPut(ch) { TrieNode(ch) }
        }
        current.isWord = true
        current.item = item
    }

    /**
     * Finds all completion items starting with the given prefix (case-insensitive),
     * sorted by category priority and label length.
     */
    fun findCompletions(prefix: String, maxResults: Int = 12): List<CompletionItem> {
        if (prefix.isBlank()) return emptyList()

        var current = root
        for (ch in prefix.lowercase()) {
            current = current.children[ch] ?: return emptyList()
        }

        val results = mutableListOf<CompletionItem>()
        collectItems(current, results, maxResults)

        return results.sortedWith(
            compareBy<CompletionItem> { it.category.ordinal }
                .thenBy { it.label.length }
                .thenBy { it.label }
        ).take(maxResults)
    }

    private fun collectItems(node: TrieNode, results: MutableList<CompletionItem>, maxResults: Int) {
        if (results.size >= maxResults * 2) return
        if (node.isWord && node.item != null) {
            results.add(node.item!!)
        }
        for (child in node.children.values) {
            collectItems(child, results, maxResults)
        }
    }

    private fun loadDefaultCompletions() {
        // Native bridge APIs
        val nativeApis = listOf(
            CompletionItem("Native.showToast", "Native.showToast(message)", "Native.showToast(\"\")", CompletionCategory.NATIVE),
            CompletionItem("Native.sendNotification", "Native.sendNotification(title, message)", "Native.sendNotification(\"\", \"\")", CompletionCategory.NATIVE),
            CompletionItem("Native.getBatteryLevel", "Native.getBatteryLevel() -> Int", "Native.getBatteryLevel()", CompletionCategory.NATIVE),
            CompletionItem("Native.getStorage", "Native.getStorage(key) -> String?", "Native.getStorage(\"\")", CompletionCategory.NATIVE),
            CompletionItem("Native.setStorage", "Native.setStorage(key, value)", "Native.setStorage(\"\", \"\")", CompletionCategory.NATIVE),
            CompletionItem("Native.fetch", "await Native.fetch(url, options)", "await Native.fetch(\"\")", CompletionCategory.NATIVE),
            CompletionItem("Native.getSecret", "Native.getSecret(key) -> String?", "Native.getSecret(\"\")", CompletionCategory.NATIVE),
            CompletionItem("Native.setSecret", "Native.setSecret(key, value)", "Native.setSecret(\"\", \"\")", CompletionCategory.NATIVE),
            CompletionItem("Native.runScript", "await Native.runScript(idOrName, input)", "await Native.runScript(\"\", {})", CompletionCategory.NATIVE)
        )
        nativeApis.forEach { insert(it) }

        // Widget builder APIs
        val widgetApis = listOf(
            CompletionItem("Widget.render", "Widget.render(rootWidget)", "Widget.render()", CompletionCategory.WIDGET),
            CompletionItem("Widget.column", "Widget.column({ children: [] })", "Widget.column({\n  children: [\n    \n  ]\n})", CompletionCategory.WIDGET),
            CompletionItem("Widget.row", "Widget.row({ children: [] })", "Widget.row({\n  children: [\n    \n  ]\n})", CompletionCategory.WIDGET),
            CompletionItem("Widget.box", "Widget.box({ children: [] })", "Widget.box({\n  children: []\n})", CompletionCategory.WIDGET),
            CompletionItem("Widget.text", "Widget.text(content, style)", "Widget.text(\"\", { size: 14, color: \"#FFFFFF\" })", CompletionCategory.WIDGET),
            CompletionItem("Widget.image", "Widget.image(url, { width, height })", "Widget.image(\"\", { width: 48, height: 48 })", CompletionCategory.WIDGET),
            CompletionItem("Widget.button", "Widget.button(text, action)", "Widget.button(\"\", { action: \"run\" })", CompletionCategory.WIDGET),
            CompletionItem("Widget.spacer", "Widget.spacer(size)", "Widget.spacer(8)", CompletionCategory.WIDGET),
            CompletionItem("Widget.divider", "Widget.divider(color)", "Widget.divider(\"#333333\")", CompletionCategory.WIDGET),
            CompletionItem("Widget.progress", "Widget.progress(value, max)", "Widget.progress(50, 100)", CompletionCategory.WIDGET)
        )
        widgetApis.forEach { insert(it) }

        // JavaScript core keywords & globals
        val jsKeywords = listOf(
            CompletionItem("const", "Declare constant", "const ", CompletionCategory.KEYWORD),
            CompletionItem("let", "Declare variable", "let ", CompletionCategory.KEYWORD),
            CompletionItem("async", "Async function", "async ", CompletionCategory.KEYWORD),
            CompletionItem("await", "Await promise", "await ", CompletionCategory.KEYWORD),
            CompletionItem("function", "Function declaration", "function () {\n  \n}", CompletionCategory.KEYWORD),
            CompletionItem("return", "Return statement", "return ", CompletionCategory.KEYWORD),
            CompletionItem("console.log", "console.log(...args)", "console.log();", CompletionCategory.GLOBAL),
            CompletionItem("console.info", "console.info(...args)", "console.info();", CompletionCategory.GLOBAL),
            CompletionItem("console.warn", "console.warn(...args)", "console.warn();", CompletionCategory.GLOBAL),
            CompletionItem("console.error", "console.error(...args)", "console.error();", CompletionCategory.GLOBAL),
            CompletionItem("JSON.stringify", "JSON.stringify(obj, null, 2)", "JSON.stringify()", CompletionCategory.GLOBAL),
            CompletionItem("JSON.parse", "JSON.parse(str)", "JSON.parse()", CompletionCategory.GLOBAL)
        )
        jsKeywords.forEach { insert(it) }
    }
}
