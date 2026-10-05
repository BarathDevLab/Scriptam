package com.scriptam.app.data.file

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Manages physical .js script files in the sandboxed [scriptsDir].
 * All I/O operations run on [Dispatchers.IO].
 */
class ScriptFileManager(context: Context, private val maxCacheSize: Int = 20) {

    private val scriptsDir: File = File(context.filesDir, "scripts").apply { mkdirs() }

    private val lruCache = object : LinkedHashMap<String, String>(maxCacheSize, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean {
            return size > maxCacheSize
        }
    }

    private fun getFromCache(fileName: String): String? = synchronized(lruCache) {
        lruCache[fileName]
    }

    private fun putInCache(fileName: String, content: String) = synchronized(lruCache) {
        lruCache[fileName] = content
    }

    private fun removeFromCache(fileName: String) = synchronized(lruCache) {
        lruCache.remove(fileName)
    }

    suspend fun readScript(fileName: String): String = withContext(Dispatchers.IO) {
        getFromCache(fileName)?.let { return@withContext it }
        val file = File(scriptsDir, fileName)
        val content = if (file.exists()) file.readText() else ""
        if (content.isNotEmpty()) {
            putInCache(fileName, content)
        }
        content
    }

    suspend fun writeScript(fileName: String, content: String) = withContext(Dispatchers.IO) {
        File(scriptsDir, fileName).writeText(content)
        putInCache(fileName, content)
    }

    suspend fun deleteScript(fileName: String): Boolean = withContext(Dispatchers.IO) {
        removeFromCache(fileName)
        File(scriptsDir, fileName).delete()
    }

    suspend fun renameScript(oldName: String, newName: String): Boolean = withContext(Dispatchers.IO) {
        val old = File(scriptsDir, oldName)
        val new = File(scriptsDir, newName)
        if (old.exists() && !new.exists()) {
            val success = old.renameTo(new)
            if (success) {
                val cached = synchronized(lruCache) { lruCache.remove(oldName) }
                if (cached != null) {
                    putInCache(newName, cached)
                }
            }
            success
        } else false
    }

    suspend fun scriptExists(fileName: String): Boolean = withContext(Dispatchers.IO) {
        File(scriptsDir, fileName).exists()
    }

    /**
     * Creates a new script file with a starter template.
     * Returns the generated file name.
     */
    suspend fun createScript(fileName: String): String = withContext(Dispatchers.IO) {
        val template = """
            |// $fileName — Scriptam
            |// Write your JavaScript here. Use 'console.log()' for output.
            |
            |console.log("Hello from Scriptam!");
        """.trimMargin()
        File(scriptsDir, fileName).writeText(template)
        putInCache(fileName, template)
        fileName
    }
}
