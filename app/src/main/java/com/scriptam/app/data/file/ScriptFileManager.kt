package com.scriptam.app.data.file

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Manages physical .js script files in the sandboxed [scriptsDir].
 * All I/O operations run on [Dispatchers.IO].
 */
class ScriptFileManager(context: Context) {

    private val scriptsDir: File = File(context.filesDir, "scripts").apply { mkdirs() }

    suspend fun readScript(fileName: String): String = withContext(Dispatchers.IO) {
        val file = File(scriptsDir, fileName)
        if (file.exists()) file.readText() else ""
    }

    suspend fun writeScript(fileName: String, content: String) = withContext(Dispatchers.IO) {
        File(scriptsDir, fileName).writeText(content)
    }

    suspend fun deleteScript(fileName: String): Boolean = withContext(Dispatchers.IO) {
        File(scriptsDir, fileName).delete()
    }

    suspend fun renameScript(oldName: String, newName: String): Boolean = withContext(Dispatchers.IO) {
        val old = File(scriptsDir, oldName)
        val new = File(scriptsDir, newName)
        if (old.exists() && !new.exists()) old.renameTo(new) else false
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
        fileName
    }
}
