package com.scriptam.app.ui.monitor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scriptam.app.widget.monitor.ExecutionLogEntry
import com.scriptam.app.widget.monitor.ExecutionLogStore
import com.scriptam.app.widget.monitor.WidgetSnapshot
import com.scriptam.app.widget.monitor.WidgetStateHistory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetWatcherScreen(
    scriptId: Long? = null,
    onNavigateBack: () -> Unit
) {
    var logs by remember {
        mutableStateOf(
            if (scriptId != null) ExecutionLogStore.getLogsForScript(scriptId)
            else ExecutionLogStore.getLogs()
        )
    }

    val snapshots = remember(scriptId) {
        if (scriptId != null) WidgetStateHistory.getSnapshots(scriptId)
        else WidgetStateHistory.getAllSnapshots()
    }

    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val totalRuns = logs.size
    val successRuns = logs.count { it.status == "SUCCESS" }
    val successRate = if (totalRuns > 0) (successRuns * 100) / totalRuns else 100
    val avgDuration = if (successRuns > 0) logs.filter { it.status == "SUCCESS" }.map { it.durationMs }.average().toLong() else 0L

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
        containerColor = Color(0xFF0F0F16),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Widget Watcher Suite",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
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
                    IconButton(onClick = {
                        ExecutionLogStore.clear()
                        logs = emptyList()
                    }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Clear logs",
                            tint = Color(0xFFFF5252)
                        )
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
        ) {
            // Metrics Summary Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricSummaryCard(
                    title = "Executions",
                    value = "$totalRuns",
                    icon = Icons.Default.Schedule,
                    color = Color(0xFF6C63FF),
                    modifier = Modifier.weight(1f)
                )
                MetricSummaryCard(
                    title = "Success Rate",
                    value = "$successRate%",
                    icon = Icons.Default.CheckCircle,
                    color = Color(0xFF00E676),
                    modifier = Modifier.weight(1f)
                )
                MetricSummaryCard(
                    title = "Avg Time",
                    value = "${avgDuration}ms",
                    icon = Icons.Default.Speed,
                    color = Color(0xFF00E5FF),
                    modifier = Modifier.weight(1f)
                )
            }

            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color(0xFF14141E),
                contentColor = Color(0xFF6C63FF)
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Run Logs (${logs.size})") }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Payload Snapshots (${snapshots.size})") }
                )
            }

            if (selectedTabIndex == 0) {
                // Run logs list
                if (logs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No execution logs recorded yet.", color = Color.Gray, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(logs, key = { it.id }) { log ->
                            ExecutionLogItemCard(log = log)
                        }
                    }
                }
            } else {
                // Snapshots list
                if (snapshots.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No widget payload snapshots recorded yet.", color = Color.Gray, fontSize = 13.sp)
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(snapshots, key = { it.id }) { snapshot ->
                            SnapshotItemCard(snapshot = snapshot)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricSummaryCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C28)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(text = title, fontSize = 10.sp, color = Color(0xFF8E8E93))
        }
    }
}

@Composable
private fun ExecutionLogItemCard(log: ExecutionLogEntry) {
    val (statusBg, statusColor) = when (log.status) {
        "SUCCESS" -> Pair(Color(0xFF00E676).copy(alpha = 0.15f), Color(0xFF00E676))
        "TIMEOUT" -> Pair(Color(0xFFFF9100).copy(alpha = 0.15f), Color(0xFFFF9100))
        else -> Pair(Color(0xFFFF5252).copy(alpha = 0.15f), Color(0xFFFF5252))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF191924)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = log.scriptTitle, fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = SimpleDateFormat("h:mm:ss a", Locale.getDefault()).format(Date(log.timestamp)),
                    color = Color(0xFF8E8E93),
                    fontSize = 11.sp
                )
                if (!log.error.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = log.error, color = Color(0xFFFF5252), fontSize = 10.sp, maxLines = 2)
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(statusBg)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (log.status == "SUCCESS") "${log.durationMs}ms" else log.status,
                    color = statusColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SnapshotItemCard(snapshot: WidgetSnapshot) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF191924)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Payload (${snapshot.payloadJson.length} bytes)",
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    fontSize = 12.sp
                )
                Text(
                    text = SimpleDateFormat("h:mm:ss a", Locale.getDefault()).format(Date(snapshot.timestamp)),
                    color = Color(0xFF8E8E93),
                    fontSize = 10.sp
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF0E0E14))
                        .padding(8.dp)
                ) {
                    Text(
                        text = snapshot.payloadJson,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF00E676),
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
