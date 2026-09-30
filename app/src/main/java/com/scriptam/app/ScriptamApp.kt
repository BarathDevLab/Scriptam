package com.scriptam.app

import android.app.Application

class ScriptamApp : Application() {

    lateinit var scriptRepository: com.scriptam.app.data.repository.ScriptRepository
        private set

    override fun onCreate() {
        super.onCreate()

        val database = com.scriptam.app.data.db.ScriptamDatabase.getInstance(this)
        val fileManager = com.scriptam.app.data.file.ScriptFileManager(this)
        scriptRepository = com.scriptam.app.data.repository.ScriptRepository(
            scriptDao = database.scriptDao(),
            fileManager = fileManager
        )
    }
}
