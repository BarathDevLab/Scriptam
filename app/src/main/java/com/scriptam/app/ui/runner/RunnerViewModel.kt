package com.scriptam.app.ui.runner

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
import com.scriptam.app.data.db.ScriptEntity
import com.scriptam.app.widget.model.WidgetPayload
import com.scriptam.app.widget.model.WidgetPayloadParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Script Runner popup.
 * Executes a script and produces both console output and interactive [WidgetPayload] output.
 */
class RunnerViewModel(application: Application) : AndroidViewModel(application) {

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

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    /** Parsed interactive output from widgetUI(). Null if script only has console output. */
    private val _interactiveOutput = MutableStateFlow<WidgetPayload?>(null)
    val interactiveOutput: StateFlow<WidgetPayload?> = _interactiveOutput.asStateFlow()

    /** Console log entries from script execution. */
    val consoleEntries: StateFlow<List<ConsoleEntry>> = console.entries

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /**
     * Loads and immediately runs a script by ID.
     */
    fun loadAndRun(scriptId: Long) {
        viewModelScope.launch {
            val entity = repository.getScriptById(scriptId) ?: run {
                _errorMessage.value = "Script not found"
                return@launch
            }
            _script.value = entity
            runScript(entity)
        }
    }

    /**
     * Re-runs the currently loaded script.
     */
    fun rerun() {
        val entity = _script.value ?: return
        viewModelScope.launch { runScript(entity) }
    }

    /**
     * Re-runs the script with a specific button action context.
     */
    fun runWithAction(action: String) {
        val entity = _script.value ?: return
        viewModelScope.launch { runScript(entity, action = action) }
    }

    private suspend fun runScript(entity: ScriptEntity, action: String? = null) {
        _isRunning.value = true
        _errorMessage.value = null
        _interactiveOutput.value = null
        console.clear()

        console.info("Running \"${entity.title}\"...")

        val code = repository.readScriptContent(entity.fileName)
        if (code.isBlank()) {
            console.warn("Script is empty.")
            _isRunning.value = false
            return
        }

        val result = engineManager.execute(
            script = code,
            console = console,
            action = action,
            bridgeInstaller = { quickJs -> bridge.install(quickJs) }
        )

        result.fold(
            onSuccess = { res ->
                // Check for interactive widget output
                if (res.widgetPayloadJson != null) {
                    val payload = WidgetPayloadParser.parse(res.widgetPayloadJson)
                    _interactiveOutput.value = payload
                    console.info("Interactive output rendered.")
                } else if (res.value != null && res.value != Unit) {
                    console.log("→ ${res.value}")
                }
                console.info("Script finished.")
            },
            onFailure = { error ->
                _errorMessage.value = error.message
                console.error("Script failed: ${error.message}")
            }
        )

        _isRunning.value = false
    }

    fun clearConsole() {
        console.clear()
    }
}
