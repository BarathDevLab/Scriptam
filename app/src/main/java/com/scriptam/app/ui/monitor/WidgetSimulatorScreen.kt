package com.scriptam.app.ui.monitor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.scriptam.app.widget.model.CardWidgetPayload
import com.scriptam.app.widget.model.ConsoleWidgetPayload
import com.scriptam.app.widget.model.ListWidgetPayload
import com.scriptam.app.widget.model.ProgressWidgetPayload
import com.scriptam.app.widget.model.StatWidgetPayload
import com.scriptam.app.widget.model.WidgetPayload

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetSimulatorScreen(
    scriptId: Long,
    onNavigateBack: () -> Unit,
    viewModel: WidgetSimulatorViewModel = viewModel()
) {
    val script by viewModel.script.collectAsState()
    val payload by viewModel.payload.collectAsState()
    val rawOutput by viewModel.rawOutput.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedSize by viewModel.selectedSize.collectAsState()

    var showJsonDrawer by remember { mutableStateOf(false) }

    LaunchedEffect(scriptId) {
        viewModel.loadScript(scriptId)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
        containerColor = Color(0xFF0F0F16),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Widget Live Simulator",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Text(
                            text = script?.title ?: "Loading...",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF8E8E93)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refresh() },
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color(0xFF6C63FF),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF14141E)
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Size Selector Segmented Controls
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1C1C28))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                WidgetSimulatorSize.values().forEach { size ->
                    val isSelected = size == selectedSize
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Color(0xFF6C63FF) else Color.Transparent)
                            .clickable { viewModel.setSize(size) }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "${size.label} (${size.grid})",
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else Color(0xFF8E8E93)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Simulated Android Homescreen Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF1F1D36), Color(0xFF14142B), Color(0xFF0B0B14))
                        )
                    )
                    .border(1.dp, Color(0xFF2E2E48), RoundedCornerShape(24.dp))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                // The Simulated Widget Card
                val widthModifier = when (selectedSize) {
                    WidgetSimulatorSize.SMALL -> Modifier.width(160.dp)
                    else -> Modifier.fillMaxWidth()
                }

                Box(
                    modifier = widthModifier
                        .height(selectedSize.heightDp.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            try {
                                if (payload?.bg != null) Color(android.graphics.Color.parseColor(payload!!.bg))
                                else Color(0xFF1E1E2C)
                            } catch (_: Exception) {
                                Color(0xFF1E1E2C)
                            }
                        )
                        .border(1.dp, Color(0xFF383852), RoundedCornerShape(18.dp))
                        .padding(16.dp)
                ) {
                    WidgetPreviewContent(payload = payload, scriptTitle = script?.title ?: "Script")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Toggle payload JSON preview
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1A1A26))
                    .clickable { showJsonDrawer = !showJsonDrawer }
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Raw Widget JSON / Output",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                Text(
                    text = if (showJsonDrawer) "Hide ▲" else "View ▼",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF6C63FF)
                )
            }

            AnimatedVisibility(visible = showJsonDrawer) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0A0A10))
                        .padding(12.dp)
                ) {
                    Text(
                        text = rawOutput.ifBlank { "No output payload" },
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color(0xFF00E676),
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun WidgetPreviewContent(
    payload: WidgetPayload?,
    scriptTitle: String
) {
    if (payload == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Simulating...", color = Color.Gray, fontSize = 12.sp)
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Widget Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF6C63FF))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = scriptTitle,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFCCCCCC)
                )
            }
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color(0xFF00E676),
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Body Content by Payload Type
        when (payload) {
            is StatWidgetPayload -> {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                    Text(
                        text = payload.title,
                        fontSize = 11.sp,
                        color = Color(0xFF9E9EA8)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = payload.value,
                        fontSize = (payload.fontSize ?: 24f).sp,
                        fontWeight = FontWeight.Bold,
                        color = try {
                            if (payload.color != null) Color(android.graphics.Color.parseColor(payload.color))
                            else Color.White
                        } catch (_: Exception) { Color.White }
                    )
                    if (!payload.subtitle.isNullOrBlank()) {
                        Text(
                            text = payload.subtitle!!,
                            fontSize = 10.sp,
                            color = Color(0xFF6C63FF)
                        )
                    }
                }
            }
            is ProgressWidgetPayload -> {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                    Text(text = payload.title, fontSize = 12.sp, color = Color(0xFF9E9EA8))
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (payload.progress / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFF00E676),
                        trackColor = Color(0xFF333344)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${payload.progress}%",
                        fontSize = 11.sp,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            is ListWidgetPayload -> {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = payload.title, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    payload.items.take(4).forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = item.label, fontSize = 11.sp, color = Color(0xFF9E9EA8))
                            Text(text = item.value, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
            is CardWidgetPayload -> {
                Column(modifier = Modifier.weight(1f)) {
                    if (!payload.title.isNullOrBlank()) {
                        Text(text = payload.title!!, fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    payload.items.take(3).forEach { item ->
                        Text(text = "• ${item.label}: ${item.value}", fontSize = 11.sp, color = Color(0xFFCCCCCC))
                    }
                }
            }
            is ConsoleWidgetPayload -> {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF101018))
                        .padding(6.dp)
                ) {
                    Text(
                        text = payload.output.take(150),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = Color(0xFF00E676)
                    )
                }
            }
        }
    }
}
