package com.scriptam.app.data.bundle

import com.scriptam.app.data.db.ScriptEntity
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Data extracted from an imported `.scriptam` bundle.
 */
data class ScriptBundleData(
    val title: String,
    val code: String,
    val accentColor: Long = 0xFF6C63FF,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Packager and parser for the `.scriptam` portable archive format.
 *
 * Structure of a `.scriptam` bundle (ZIP):
 * - `manifest.json`: metadata (title, accent color, timestamps)
 * - `script.js`: full JavaScript source code
 */
object ScriptBundler {

    private const val MANIFEST_ENTRY = "manifest.json"
    private const val SCRIPT_ENTRY = "script.js"

    /**
     * Serializes a script into a `.scriptam` ZIP bundle written to [outputStream].
     */
    fun exportScript(
        script: ScriptEntity,
        code: String,
        outputStream: OutputStream
    ) {
        val manifestObj = JSONObject().apply {
            put("version", 1)
            put("title", script.title)
            put("accentColor", script.accentColor)
            put("createdAt", script.createdAt)
            put("lastModified", script.lastModified)
        }

        ZipOutputStream(outputStream).use { zipOut ->
            // Write manifest.json
            zipOut.putNextEntry(ZipEntry(MANIFEST_ENTRY))
            zipOut.write(manifestObj.toString(2).toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()

            // Write script.js
            zipOut.putNextEntry(ZipEntry(SCRIPT_ENTRY))
            zipOut.write(code.toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()
        }
    }

    /**
     * Reads a `.scriptam` ZIP bundle from [inputStream] and returns parsed [ScriptBundleData].
     */
    fun importScript(inputStream: InputStream): ScriptBundleData {
        var manifestJson: String? = null
        var scriptCode: String? = null

        ZipInputStream(inputStream).use { zipIn ->
            var entry = zipIn.nextEntry
            while (entry != null) {
                val bytes = zipIn.readBytes()
                when (entry.name) {
                    MANIFEST_ENTRY -> manifestJson = String(bytes, Charsets.UTF_8)
                    SCRIPT_ENTRY -> scriptCode = String(bytes, Charsets.UTF_8)
                }
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
        }

        if (scriptCode == null) {
            throw IllegalArgumentException("Invalid .scriptam bundle: missing script.js")
        }

        val manifest = if (manifestJson != null) JSONObject(manifestJson) else JSONObject()
        val title = manifest.optString("title", "Imported Script")
        val accentColor = manifest.optLong("accentColor", 0xFF6C63FF)
        val createdAt = manifest.optLong("createdAt", System.currentTimeMillis())

        return ScriptBundleData(
            title = title,
            code = scriptCode!!,
            accentColor = accentColor,
            createdAt = createdAt
        )
    }
}
