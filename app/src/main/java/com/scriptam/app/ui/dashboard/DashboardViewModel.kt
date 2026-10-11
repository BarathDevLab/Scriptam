package com.scriptam.app.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scriptam.app.ScriptamApp
import com.scriptam.app.data.db.ScriptEntity
import com.scriptam.app.ui.theme.ScriptAccentColors
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as ScriptamApp).scriptRepository

    private val allScripts: StateFlow<List<ScriptEntity>> = repository.observeAllScripts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /** Filtered scripts based on search query. */
    val scripts: StateFlow<List<ScriptEntity>> = combine(
        allScripts,
        _searchQuery.debounce { if (it.isBlank()) 0L else 300L }
    ) { scripts, query ->
        if (query.isBlank()) scripts
        else scripts.filter { it.title.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _showCreateDialog = MutableStateFlow(false)
    val showCreateDialog: StateFlow<Boolean> = _showCreateDialog.asStateFlow()

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onCreateClick() {
        _showCreateDialog.value = true
    }

    fun onDismissCreateDialog() {
        _showCreateDialog.value = false
    }

    fun createScript(title: String) {
        viewModelScope.launch {
            val color = ScriptAccentColors.random()
            repository.createScript(title = title, accentColor = color)
            _showCreateDialog.value = false
        }
    }

    fun deleteScript(script: ScriptEntity) {
        viewModelScope.launch {
            repository.deleteScript(script)
        }
    }

    fun renameScript(script: ScriptEntity, newTitle: String) {
        viewModelScope.launch {
            repository.renameScript(script.id, newTitle)
        }
    }

    fun exportScript(context: android.content.Context, script: ScriptEntity, uri: android.net.Uri) {
        viewModelScope.launch {
            val code = repository.readScriptContent(script.fileName)
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                com.scriptam.app.data.bundle.ScriptBundler.exportScript(script, code, outputStream)
            }
        }
    }

    fun importScript(context: android.content.Context, uri: android.net.Uri) {
        viewModelScope.launch {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val bundle = com.scriptam.app.data.bundle.ScriptBundler.importScript(inputStream)
                    val newScript = repository.createScript(title = bundle.title, accentColor = bundle.accentColor)
                    repository.saveScriptContent(newScript.id, newScript.fileName, bundle.code)
                }
            } catch (_: Exception) {}
        }
    }
}
