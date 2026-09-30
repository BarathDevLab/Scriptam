package com.scriptam.app.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scriptam.app.ScriptamApp
import com.scriptam.app.data.db.ScriptEntity
import com.scriptam.app.ui.theme.ScriptAccentColors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as ScriptamApp).scriptRepository

    private val allScripts: StateFlow<List<ScriptEntity>> = repository.observeAllScripts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /** Filtered scripts based on search query. */
    val scripts: StateFlow<List<ScriptEntity>> = combine(
        allScripts,
        _searchQuery
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
}
