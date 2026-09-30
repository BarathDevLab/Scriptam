package com.scriptam.app.ui.runner

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.scriptam.app.ui.console.ConsoleSheet

/**
 * A fullscreen popup overlay that runs a script and shows
 * interactive output + console in a dedicated container.
 *
 * Usage:
 * ```
 * if (showRunner) {
 *     RunnerPopup(scriptId = id, onDismiss = { showRunner = false })
 * }
 * ```
 */
@Composable
fun RunnerPopup(
    scriptId: Long,
    onDismiss: () -> Unit,
    viewModel: RunnerViewModel = viewModel()
) {
    val script by viewModel.script.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val interactiveOutput by viewModel.interactiveOutput.collectAsState()
    val consoleEntries by viewModel.consoleEntries.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var showConsole by remember { mutableStateOf(false) }

    // Auto-run on first appear
    LaunchedEffect(scriptId) {
        viewModel.loadAndRun(scriptId)
    }

    // Dimmed background overlay
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        // Popup card — blocks click-through
        Column(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(min = 200.dp, max = 560.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF121218))
                .border(
                    width = 1.dp,
                    color = Color(0xFF28283C),
                    shape = RoundedCornerShape(24.dp)
                )
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { /* consume click so it doesn't dismiss */ }
        ) {
            // ── Header ──────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF16161F))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Running indicator
                    if (isRunning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color(0xFF00E676)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(
                                    if (errorMessage != null) Color(0xFFFF5252)
                                    else Color(0xFF00E676)
                                )
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = script?.title ?: "Running…",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Console toggle
                    IconButton(onClick = { showConsole = !showConsole }) {
                        Icon(
                            Icons.Default.Terminal,
                            contentDescription = "Toggle Console",
                            tint = if (showConsole) Color(0xFF6C63FF) else Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Re-run
                    IconButton(
                        onClick = { viewModel.rerun() },
                        enabled = !isRunning
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Re-run",
                            tint = if (isRunning) Color.White.copy(alpha = 0.2f)
                            else Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Close
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // ── Body ────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Loading state
                if (isRunning && interactiveOutput == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = Color(0xFF6C63FF),
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Executing script…",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        }
                    }
                }

                // Error state
                if (errorMessage != null && interactiveOutput == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFF5252).copy(alpha = 0.1f))
                            .border(
                                1.dp,
                                Color(0xFFFF5252).copy(alpha = 0.3f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(
                                text = "Script Error",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFFF5252)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = errorMessage ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFF8A80),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                // Interactive output
                interactiveOutput?.let { payload ->
                    OutputRenderer(
                        payload = payload,
                        onButtonClick = { action -> viewModel.runWithAction(action) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Console output when there's no interactive output and no error
                if (interactiveOutput == null && errorMessage == null && !isRunning && consoleEntries.isNotEmpty()) {
                    // Show console entries inline as plain text
                    consoleEntries.forEach { entry ->
                        val color = when (entry.level) {
                            com.scriptam.app.core.ConsoleEntry.Level.LOG -> Color(0xFFE0E0E0)
                            com.scriptam.app.core.ConsoleEntry.Level.WARN -> Color(0xFFFFC107)
                            com.scriptam.app.core.ConsoleEntry.Level.ERROR -> Color(0xFFFF5252)
                            com.scriptam.app.core.ConsoleEntry.Level.INFO -> Color(0xFF448AFF)
                        }
                        Text(
                            text = entry.message,
                            fontSize = 13.sp,
                            color = color,
                            lineHeight = 18.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
            }

            // ── Console drawer ──────────────────────────────────
            AnimatedVisibility(
                visible = showConsole,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                ConsoleSheet(
                    entries = consoleEntries,
                    onClear = { viewModel.clearConsole() },
                    onClose = { showConsole = false },
                    modifier = Modifier.heightIn(min = 100.dp, max = 200.dp)
                )
            }
        }
    }
}
