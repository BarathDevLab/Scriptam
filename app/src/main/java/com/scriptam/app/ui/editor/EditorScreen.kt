package com.scriptam.app.ui.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.scriptam.app.ui.console.ConsoleSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    scriptId: Long,
    onNavigateBack: () -> Unit,
    viewModel: EditorViewModel = viewModel()
) {
    val script by viewModel.script.collectAsState()
    val code by viewModel.code.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val showConsole by viewModel.showConsole.collectAsState()
    val consoleEntries by viewModel.consoleEntries.collectAsState()

    LaunchedEffect(scriptId) {
        viewModel.loadScript(scriptId)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .imePadding(),
        containerColor = Color(0xFF0D0D14),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = script?.title ?: "Loading…",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.saveCode()
                        onNavigateBack()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // Save button
                    IconButton(onClick = { viewModel.saveCode() }) {
                        Icon(
                            Icons.Default.Save,
                            contentDescription = "Save",
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }

                    // Console toggle
                    IconButton(onClick = { viewModel.toggleConsole() }) {
                        Icon(
                            Icons.Default.Terminal,
                            contentDescription = "Toggle Console",
                            tint = if (showConsole) MaterialTheme.colorScheme.primary
                            else Color.White.copy(alpha = 0.7f)
                        )
                    }

                    // Run button
                    IconButton(
                        onClick = { viewModel.runScript() },
                        enabled = !isRunning
                    ) {
                        if (isRunning) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp,
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding(2.dp)
                            )
                        } else {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = "Run Script",
                                tint = Color(0xFF00E676)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF121218)
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Code editor area — show loading state until script is loaded
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (script != null) {
                    // WebView-based CodeMirror editor with syntax highlighting
                    androidx.compose.runtime.key(script?.id) {
                        WebViewEditor(
                            initialCode = code,
                            onCodeChange = { viewModel.onCodeChange(it) },
                            isReadOnly = isRunning,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    // Loading state
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0D0D14)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
                    }
                }
            }

            // Console drawer with slide animation
            AnimatedVisibility(
                visible = showConsole,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                ConsoleSheet(
                    entries = consoleEntries,
                    onClear = { viewModel.clearConsole() },
                    onClose = { viewModel.toggleConsole() },
                    modifier = Modifier.heightIn(min = 120.dp, max = 300.dp)
                )
            }
        }
    }
}
