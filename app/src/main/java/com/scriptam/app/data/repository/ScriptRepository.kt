package com.scriptam.app.data.repository

import com.scriptam.app.data.db.ScriptDao
import com.scriptam.app.data.db.ScriptEntity
import com.scriptam.app.data.file.ScriptFileManager
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * Single source of truth combining Room metadata with filesystem .js content.
 */
class ScriptRepository(
    private val scriptDao: ScriptDao,
    private val fileManager: ScriptFileManager
) {

    /** Observe all scripts as a reactive Flow for the dashboard. */
    fun observeAllScripts(): Flow<List<ScriptEntity>> = scriptDao.observeAll()

    /** Create a new script: generates a file on disk and a metadata row in Room. */
    suspend fun createScript(title: String, accentColor: Long = 0xFF6C63FF): ScriptEntity {
        val fileName = "${UUID.randomUUID()}.js"
        fileManager.createScript(fileName)

        val entity = ScriptEntity(
            fileName = fileName,
            title = title,
            accentColor = accentColor
        )
        val id = scriptDao.insert(entity)
        return entity.copy(id = id)
    }

    /** Read .js file content for the editor. */
    suspend fun readScriptContent(fileName: String): String {
        return fileManager.readScript(fileName)
    }

    /** Save .js file content and touch the metadata timestamp. */
    suspend fun saveScriptContent(id: Long, fileName: String, content: String) {
        fileManager.writeScript(fileName, content)
        scriptDao.touchLastModified(id)
    }

    /** Update script metadata (title, color). */
    suspend fun updateScript(script: ScriptEntity) {
        scriptDao.update(script.copy(lastModified = System.currentTimeMillis()))
    }

    /** Delete both the metadata row and the filesystem file. */
    suspend fun deleteScript(script: ScriptEntity) {
        scriptDao.delete(script)
        fileManager.deleteScript(script.fileName)
    }

    /** Rename script title (metadata only — file name stays as UUID). */
    suspend fun renameScript(id: Long, newTitle: String) {
        val entity = scriptDao.getById(id) ?: return
        scriptDao.update(entity.copy(title = newTitle, lastModified = System.currentTimeMillis()))
    }

    suspend fun getScriptById(id: Long): ScriptEntity? = scriptDao.getById(id)
}
