package com.scriptam.app.core

import android.content.Context
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicInteger

data class BytecodeCacheStats(
    val hitCount: Int,
    val missCount: Int,
    val cachedFiles: Int,
    val totalSizeBytes: Long
)

/**
 * Disk and memory cache for precompiled QuickJS bytecode (.qbc).
 *
 * Eliminates JS parsing and AST overhead on warm executions by caching
 * bytecode keyed by the SHA-256 hash of the script source code.
 */
class BytecodeCache(context: Context) {

    private val cacheDir = File(context.cacheDir, "bytecode_cache").apply { mkdirs() }
    private val hits = AtomicInteger(0)
    private val misses = AtomicInteger(0)

    fun get(script: String): ByteArray? {
        val hash = sha256(script)
        val file = File(cacheDir, "$hash.qbc")
        return if (file.exists() && file.length() > 0) {
            hits.incrementAndGet()
            file.readBytes()
        } else {
            misses.incrementAndGet()
            null
        }
    }

    fun put(script: String, bytecode: ByteArray) {
        val hash = sha256(script)
        val file = File(cacheDir, "$hash.qbc")
        try {
            file.writeBytes(bytecode)
        } catch (_: Exception) {}
    }

    fun clear() {
        cacheDir.listFiles()?.forEach { it.delete() }
        hits.set(0)
        misses.set(0)
    }

    fun getStats(): BytecodeCacheStats {
        val files = cacheDir.listFiles() ?: emptyArray()
        val totalBytes = files.sumOf { it.length() }
        return BytecodeCacheStats(
            hitCount = hits.get(),
            missCount = misses.get(),
            cachedFiles = files.size,
            totalSizeBytes = totalBytes
        )
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
