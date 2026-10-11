package com.scriptam.app.ui.monitor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scriptam.app.ScriptamApp
import com.scriptam.app.bridge.AndroidBridge
import com.scriptam.app.core.ScriptConsole
import com.scriptam.app.data.db.ScriptEntity
import com.scriptam.app.widget.model.WidgetPayloadParser
import com.scriptam.app.widget.model.ConsoleWidgetPayload
import com.scriptam.app.widget.model.WidgetPayload
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class WidgetSimulatorSize(val label: String, val grid: String, val heightDp: Int) {
    SMALL("2x2", "Small", 140),
    MEDIUM("4x2", "Medium", 140),
    LARGE("4x4", "Large", 280)
}

class WidgetSimulatorViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as ScriptamApp
    private val repository = app.scriptRepository
    private val engineManager = app.scriptEngineManager
    private val bridge = AndroidBridge.createDefault(application)
    private val console = ScriptConsole()

    private val _script = MutableStateFlow<ScriptEntity?>(null)
    val script: StateFlow<ScriptEntity?> = _script.asStateFlow()

    private val _payload = MutableStateFlow<WidgetPayload?>(null)
    val payload: StateFlow<WidgetPayload?> = _payload.asStateFlow()

    private val _rawOutput = MutableStateFlow("")
    val rawOutput: StateFlow<String> = _rawOutput.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedSize = MutableStateFlow(WidgetSimulatorSize.MEDIUM)
    val selectedSize: StateFlow<WidgetSimulatorSize> = _selectedSize.asStateFlow()

    fun setSize(size: WidgetSimulatorSize) {
        _selectedSize.value = size
    }

    fun loadScript(scriptId: Long) {
        viewModelScope.launch {
            val entity = repository.getScriptById(scriptId) ?: return@launch
            _script.value = entity
            runSimulation(entity)
        }
    }

    fun refresh() {
        val s = _script.value ?: return
        viewModelScope.launch {
            runSimulation(s)
        }
    }

    private suspend fun runSimulation(entity: ScriptEntity) {
        _isLoading.value = true
        console.clear()

        val code = repository.readScriptContent(entity.fileName)
        val result = engineManager.execute(
            script = code,
            console = console,
            bridgeInstaller = { quickJs -> bridge.install(quickJs) }
        )

        result.fold(
            onSuccess = { res ->
                val json = res.widgetPayloadJson
                if (!json.isNullOrBlank()) {
                    _rawOutput.value = json
                    try {
                        _payload.value = WidgetPayloadParser.parse(json)
                    } catch (e: Exception) {
                        _payload.value = ConsoleWidgetPayload("Error parsing widget: ${e.message}")
                    }
                } else {
                    val logs = console.entries.value.joinToString("\n") { it.message }
                    _rawOutput.value = logs.ifBlank { "Script executed (no Widget UI rendered)" }
                    _payload.value = ConsoleWidgetPayload(_rawOutput.value)
                }
            },
            onFailure = { err ->
                _rawOutput.value = "Error: ${err.message}"
                _payload.value = ConsoleWidgetPayload("Error: ${err.message}")
            }
        )

        _isLoading.value = false
    }
}
