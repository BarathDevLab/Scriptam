package com.scriptam.app.widget.model

/**
 * Padding specification in density-independent pixels (dp).
 */
data class WidgetPadding(
    val left: Int = 16,
    val top: Int = 16,
    val right: Int = 16,
    val bottom: Int = 16
)

enum class WidgetTextAlign {
    LEFT, CENTER, RIGHT
}

enum class WidgetFontFamily {
    DEFAULT, MONOSPACE, SERIF, SANS_SERIF, SANS_SERIF_MEDIUM
}

/**
 * Individual button specification with custom icon, text, size, color, background, action, and position.
 */
data class WidgetButton(
    val text: String? = null,
    val icon: String? = null,
    val action: String = "click",
    val color: String? = null,
    val bg: String? = null,
    val fontSize: Float? = null,
    val position: String = "bottom", // "header", "bottom", "inline"
    val align: String = "center"     // "left", "center", "right"
)

/**
 * Base sealed class for all widget UI representations parsed from `widgetUI()` or console logs.
 */
sealed class WidgetPayload {
    abstract val type: String
    abstract val bg: String?
    abstract val padding: WidgetPadding?
    open val buttonText: String? get() = null
    open val buttonAction: String? get() = null
    open val buttonColor: String? get() = null
    open val buttonPosition: String? get() = null
    open val buttons: List<WidgetButton> get() = emptyList()
}

/**
 * Monospace console log output fallback when `widgetUI()` is not defined.
 */
data class ConsoleWidgetPayload(
    val output: String,
    override val type: String = "console",
    override val bg: String? = null,
    override val padding: WidgetPadding? = null
) : WidgetPayload()

/**
 * Large metric / stat card (e.g. Crypto price, follower count, server ping).
 */
data class StatWidgetPayload(
    val title: String,
    val value: String,
    val subtitle: String? = null,
    val color: String? = null, // Main value color (hex)
    val subtitleColor: String? = null,
    val fontSize: Float? = null, // Value font size in sp
    val bold: Boolean = true,
    val fontFamily: WidgetFontFamily = WidgetFontFamily.DEFAULT,
    val align: WidgetTextAlign = WidgetTextAlign.LEFT,
    override val buttonText: String? = null,
    override val buttonAction: String? = null,
    override val buttonColor: String? = null,
    override val buttonPosition: String? = null,
    override val buttons: List<WidgetButton> = emptyList(),
    override val type: String = "stat",
    override val bg: String? = null,
    override val padding: WidgetPadding? = null
) : WidgetPayload()

/**
 * Progress indicator card (e.g. Battery %, storage usage, step goal).
 */
data class ProgressWidgetPayload(
    val title: String,
    val value: String? = null,
    val subtitle: String? = null,
    val progress: Int = 0, // 0 to 100
    val color: String? = null, // Progress bar color
    override val buttonText: String? = null,
    override val buttonAction: String? = null,
    override val buttonColor: String? = null,
    override val buttonPosition: String? = null,
    override val buttons: List<WidgetButton> = emptyList(),
    override val type: String = "progress",
    override val bg: String? = null,
    override val padding: WidgetPadding? = null
) : WidgetPayload()

/**
 * Single item inside a list or card view.
 */
data class WidgetListItem(
    val label: String,
    val value: String,
    val color: String? = null,
    val statusColor: String? = null
)

/**
 * Multi-item key-value or status list.
 */
data class ListWidgetPayload(
    val title: String,
    val items: List<WidgetListItem> = emptyList(),
    override val buttonText: String? = null,
    override val buttonAction: String? = null,
    override val buttonColor: String? = null,
    override val buttonPosition: String? = null,
    override val buttons: List<WidgetButton> = emptyList(),
    override val type: String = "list",
    override val bg: String? = null,
    override val padding: WidgetPadding? = null
) : WidgetPayload()

/**
 * General card with structured rows.
 */
data class CardWidgetPayload(
    val title: String? = null,
    val items: List<WidgetListItem> = emptyList(),
    val footerText: String? = null,
    override val buttonText: String? = null,
    override val buttonAction: String? = null,
    override val buttonColor: String? = null,
    override val buttonPosition: String? = null,
    override val buttons: List<WidgetButton> = emptyList(),
    override val type: String = "card",
    override val bg: String? = null,
    override val padding: WidgetPadding? = null
) : WidgetPayload()
