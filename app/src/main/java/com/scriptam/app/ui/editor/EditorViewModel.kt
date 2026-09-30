package com.scriptam.app.ui.editor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scriptam.app.ScriptamApp
import com.scriptam.app.bridge.AndroidBridge
import com.scriptam.app.bridge.NetworkModule
import com.scriptam.app.bridge.StorageModule
import com.scriptam.app.bridge.UIModule
import com.scriptam.app.core.ConsoleEntry
import com.scriptam.app.core.ScriptConsole
import com.scriptam.app.core.ScriptEngineManager
import com.dokar.quickjs.QuickJs
import com.scriptam.app.data.db.ScriptEntity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as ScriptamApp).scriptRepository
    private val engineManager = ScriptEngineManager()
    private val console = ScriptConsole()

    private val bridge = AndroidBridge(
        context = application,
        uiModule = UIModule(application),
        storageModule = StorageModule(application),
        networkModule = NetworkModule()
    )

    private val _script = MutableStateFlow<ScriptEntity?>(null)
    val script: StateFlow<ScriptEntity?> = _script.asStateFlow()

    private val _code = MutableStateFlow("")
    val code: StateFlow<String> = _code.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _showConsole = MutableStateFlow(false)
    val showConsole: StateFlow<Boolean> = _showConsole.asStateFlow()

    val consoleEntries: StateFlow<List<ConsoleEntry>> = console.entries

    /** Debounced auto-save job — cancels and restarts on each code change. */
    private var autoSaveJob: Job? = null

    fun loadScript(scriptId: Long) {
        viewModelScope.launch {
            val entity = repository.getScriptById(scriptId) ?: return@launch
            val content = repository.readScriptContent(entity.fileName)
            // Set code BEFORE script so the WebViewEditor gets the right initialCode
            // when it renders (triggered by script becoming non-null).
            _code.value = content
            _script.value = entity
        }
    }

    fun onCodeChange(newCode: String) {
        _code.value = newCode

        // Debounced auto-save: writes to disk 2s after the last change
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(2000)
            saveCodeInternal()
        }
    }

    fun saveCode() {
        autoSaveJob?.cancel()
        viewModelScope.launch { saveCodeInternal() }
    }

    private suspend fun saveCodeInternal() {
        val s = _script.value ?: return
        repository.saveScriptContent(s.id, s.fileName, _code.value)
    }

    fun runScript() {
        val currentCode = _code.value
        if (currentCode.isBlank()) return

        _isRunning.value = true
        _showConsole.value = true
        console.clear()

        viewModelScope.launch {
            // Force-save before running
            saveCodeInternal()

            console.info("Running script…")
            val result = engineManager.execute(
                script = currentCode,
                console = console,
                bridgeInstaller = { quickJs -> bridge.install(quickJs) }
            )

            result.fold(
                onSuccess = { res ->
                    if (res.value != null && res.value != Unit) {
                        console.log("→ ${res.value}")
                    }
                    if (res.widgetPayloadJson != null) {
                        console.info("Widget UI defined.")
                    }
                    console.info("Script finished.")
                },
                onFailure = { error ->
                    console.error("Script failed: ${error.message}")
                }
            )

            _isRunning.value = false
        }
    }

    fun toggleConsole() {
        _showConsole.value = !_showConsole.value
    }

    fun clearConsole() {
        console.clear()
    }

    override fun onCleared() {
        super.onCleared()
        autoSaveJob?.cancel()
    }
}
