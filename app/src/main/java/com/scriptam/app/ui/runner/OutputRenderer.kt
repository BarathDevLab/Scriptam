package com.scriptam.app.ui.runner

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scriptam.app.widget.model.CardWidgetPayload
import com.scriptam.app.widget.model.ConsoleWidgetPayload
import com.scriptam.app.widget.model.ListWidgetPayload
import com.scriptam.app.widget.model.ProgressWidgetPayload
import com.scriptam.app.widget.model.StatWidgetPayload
import com.scriptam.app.widget.model.WidgetButton
import com.scriptam.app.widget.model.WidgetFontFamily
import com.scriptam.app.widget.model.WidgetListItem
import com.scriptam.app.widget.model.WidgetPayload
import com.scriptam.app.widget.model.WidgetTextAlign

/**
 * Renders a [WidgetPayload] as an interactive in-app Compose UI card.
 * Reuses the same parsed models from the widget system.
 */
@Composable
fun OutputRenderer(
    payload: WidgetPayload,
    onButtonClick: (action: String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val bgColor = payload.bg?.let { parseHexColor(it) } ?: Color(0xFF1A1A2E)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .animateContentSize()
            .padding(
                start = (payload.padding?.left ?: 16).dp,
                top = (payload.padding?.top ?: 16).dp,
                end = (payload.padding?.right ?: 16).dp,
                bottom = (payload.padding?.bottom ?: 16).dp
            )
    ) {
        when (payload) {
            is StatWidgetPayload -> StatCard(payload, onButtonClick)
            is ProgressWidgetPayload -> ProgressCard(payload, onButtonClick)
            is ListWidgetPayload -> ListCard(payload, onButtonClick)
            is CardWidgetPayload -> GenericCard(payload, onButtonClick)
            is ConsoleWidgetPayload -> ConsoleCard(payload)
        }
    }
}

@Composable
private fun StatCard(
    payload: StatWidgetPayload,
    onButtonClick: (String) -> Unit
) {
    val valueColor = payload.color?.let { parseHexColor(it) } ?: Color(0xFF6C63FF)
    val textAlign = payload.align.toComposeAlign()
    val fontFamily = payload.fontFamily.toComposeFontFamily()

    Column(modifier = Modifier.fillMaxWidth()) {
        // Header buttons
        val headerButtons = payload.buttons.filter { it.position == "header" }
        if (headerButtons.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = payload.title,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.weight(1f)
                )
                ButtonRow(headerButtons, onButtonClick)
            }
        } else {
            Text(
                text = payload.title,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.6f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Value
        Text(
            text = payload.value,
            fontSize = (payload.fontSize ?: 32f).sp,
            fontWeight = if (payload.bold) FontWeight.Bold else FontWeight.Normal,
            fontFamily = fontFamily,
            color = valueColor,
            textAlign = textAlign,
            modifier = Modifier.fillMaxWidth()
        )

        // Subtitle
        if (!payload.subtitle.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = payload.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = payload.subtitleColor?.let { parseHexColor(it) }
                    ?: Color.White.copy(alpha = 0.5f)
            )
        }

        // Bottom buttons
        val bottomButtons = payload.buttons.filter { it.position != "header" }
        if (bottomButtons.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            ButtonRow(bottomButtons, onButtonClick)
        }
    }
}

@Composable
private fun ProgressCard(
    payload: ProgressWidgetPayload,
    onButtonClick: (String) -> Unit
) {
    val progressColor = payload.color?.let { parseHexColor(it) } ?: Color(0xFF6C63FF)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = payload.title,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.6f)
            )
            payload.value?.let { value ->
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = progressColor
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Progress bar
        LinearProgressIndicator(
            progress = { payload.progress / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = progressColor,
            trackColor = progressColor.copy(alpha = 0.15f),
        )

        Text(
            text = "${payload.progress}%",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.padding(top = 6.dp)
        )

        if (!payload.subtitle.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = payload.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.5f)
            )
        }

        val bottomButtons = payload.buttons.filter { it.position != "header" }
        if (bottomButtons.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            ButtonRow(bottomButtons, onButtonClick)
        }
    }
}

@Composable
private fun ListCard(
    payload: ListWidgetPayload,
    onButtonClick: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = payload.title,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(10.dp))

        payload.items.forEachIndexed { index, item ->
            ListItemRow(item)
            if (index < payload.items.lastIndex) {
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        val bottomButtons = payload.buttons.filter { it.position != "header" }
        if (bottomButtons.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            ButtonRow(bottomButtons, onButtonClick)
        }
    }
}

@Composable
private fun ListItemRow(item: WidgetListItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status dot
        if (item.statusColor != null) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(parseHexColor(item.statusColor))
            )
            Spacer(modifier = Modifier.width(10.dp))
        }

        Text(
            text = item.label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = item.value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = item.color?.let { parseHexColor(it) } ?: Color.White.copy(alpha = 0.9f)
        )
    }
}

@Composable
private fun GenericCard(
    payload: CardWidgetPayload,
    onButtonClick: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (!payload.title.isNullOrBlank()) {
            Text(
                text = payload.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        payload.items.forEachIndexed { index, item ->
            ListItemRow(item)
            if (index < payload.items.lastIndex) {
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        if (!payload.footerText.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = payload.footerText,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.4f)
            )
        }

        val bottomButtons = payload.buttons.filter { it.position != "header" }
        if (bottomButtons.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            ButtonRow(bottomButtons, onButtonClick)
        }
    }
}

@Composable
private fun ConsoleCard(payload: ConsoleWidgetPayload) {
    if (payload.output.isBlank()) return
    Text(
        text = payload.output,
        fontFamily = FontFamily.Monospace,
        fontSize = 13.sp,
        color = Color(0xFFE0E0E0),
        lineHeight = 20.sp,
        modifier = Modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ButtonRow(
    buttons: List<WidgetButton>,
    onButtonClick: (String) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        buttons.forEach { button ->
            val btnBg = button.bg?.let { parseHexColor(it) } ?: Color(0xFF6C63FF).copy(alpha = 0.15f)
            val btnColor = button.color?.let { parseHexColor(it) } ?: Color(0xFF6C63FF)

            val displayText = buildString {
                button.icon?.let { append(it); append(" ") }
                button.text?.let { append(it) }
            }.trim()

            if (displayText.isNotBlank()) {
                Text(
                    text = displayText,
                    fontSize = (button.fontSize ?: 13f).sp,
                    fontWeight = FontWeight.SemiBold,
                    color = btnColor,
                    textAlign = when (button.align) {
                        "center" -> TextAlign.Center
                        "right" -> TextAlign.End
                        else -> TextAlign.Start
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(btnBg)
                        .clickable { onButtonClick(button.action) }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }
    }
}

// ── Utility helpers ────────────────────────────────────────────

fun parseHexColor(hex: String): Color {
    return try {
        val clean = hex.removePrefix("#")
        val argb = when (clean.length) {
            6 -> "FF$clean"
            8 -> clean
            3 -> "FF${clean[0]}${clean[0]}${clean[1]}${clean[1]}${clean[2]}${clean[2]}"
            else -> return Color(0xFF6C63FF)
        }
        Color(argb.toLong(16))
    } catch (_: Exception) {
        Color(0xFF6C63FF)
    }
}

private fun WidgetTextAlign.toComposeAlign(): TextAlign = when (this) {
    WidgetTextAlign.CENTER -> TextAlign.Center
    WidgetTextAlign.RIGHT -> TextAlign.End
    WidgetTextAlign.LEFT -> TextAlign.Start
}

private fun WidgetFontFamily.toComposeFontFamily(): FontFamily = when (this) {
    WidgetFontFamily.MONOSPACE -> FontFamily.Monospace
    WidgetFontFamily.SERIF -> FontFamily.Serif
    WidgetFontFamily.SANS_SERIF -> FontFamily.SansSerif
    WidgetFontFamily.SANS_SERIF_MEDIUM -> FontFamily.SansSerif
    WidgetFontFamily.DEFAULT -> FontFamily.Default
}
