package com.scriptam.app.widget.model

import org.json.JSONArray
import org.json.JSONObject

/**
 * Parses raw JSON string returned from `widgetUI()` or console text into [WidgetPayload].
 */
object WidgetPayloadParser {

    fun parse(raw: String?): WidgetPayload {
        if (raw.isNullOrBlank()) {
            return ConsoleWidgetPayload(output = "")
        }

        val trimmed = raw.trim()
        if (!trimmed.startsWith("{")) {
            return ConsoleWidgetPayload(output = trimmed)
        }

        return try {
            val json = JSONObject(trimmed)
            val type = json.optString("type", "").lowercase()

            when {
                type == "stat" || (type.isEmpty() && json.has("value") && !json.has("items")) -> {
                    parseStat(json)
                }
                type == "progress" || (type.isEmpty() && json.has("progress")) -> {
                    parseProgress(json)
                }
                type == "list" || (type.isEmpty() && json.has("items")) -> {
                    parseList(json)
                }
                type == "card" -> {
                    parseCard(json)
                }
                type == "console" -> {
                    ConsoleWidgetPayload(
                        output = json.optString("output", trimmed),
                        bg = json.optStringOrNull("bg"),
                        padding = parsePadding(json)
                    )
                }
                else -> {
                    // If JSON doesn't match standard models, display content as console
                    ConsoleWidgetPayload(output = trimmed)
                }
            }
        } catch (_: Exception) {
            ConsoleWidgetPayload(output = trimmed)
        }
    }

    private fun parseStat(json: JSONObject): StatWidgetPayload {
        val buttons = parseButtons(json)
        val primaryBtn = buttons.firstOrNull()
        return StatWidgetPayload(
            title = json.optString("title", "Script"),
            value = json.optString("value", ""),
            subtitle = json.optStringOrNull("subtitle"),
            color = json.optStringOrNull("color") ?: json.optStringOrNull("accentColor"),
            subtitleColor = json.optStringOrNull("subtitleColor"),
            fontSize = json.optDoubleOrNull("fontSize")?.toFloat(),
            bold = json.optBoolean("bold", true),
            fontFamily = parseFontFamily(json.optString("fontFamily")),
            align = parseTextAlign(json.optString("align")),
            buttonText = primaryBtn?.let { if (it.icon != null && it.text != null) "${it.icon} ${it.text}" else (it.icon ?: it.text) },
            buttonAction = primaryBtn?.action,
            buttonColor = primaryBtn?.color,
            buttonPosition = primaryBtn?.position,
            buttons = buttons,
            bg = json.optStringOrNull("bg") ?: json.optStringOrNull("background"),
            padding = parsePadding(json)
        )
    }

    private fun parseProgress(json: JSONObject): ProgressWidgetPayload {
        val rawProgress = json.optInt("progress", 0)
        val clampedProgress = rawProgress.coerceIn(0, 100)
        val buttons = parseButtons(json)
        val primaryBtn = buttons.firstOrNull()
        return ProgressWidgetPayload(
            title = json.optString("title", "Script"),
            value = json.optStringOrNull("value"),
            subtitle = json.optStringOrNull("subtitle"),
            progress = clampedProgress,
            color = json.optStringOrNull("color") ?: json.optStringOrNull("accentColor"),
            buttonText = primaryBtn?.let { if (it.icon != null && it.text != null) "${it.icon} ${it.text}" else (it.icon ?: it.text) },
            buttonAction = primaryBtn?.action,
            buttonColor = primaryBtn?.color,
            buttonPosition = primaryBtn?.position,
            buttons = buttons,
            bg = json.optStringOrNull("bg") ?: json.optStringOrNull("background"),
            padding = parsePadding(json)
        )
    }

    private fun parseList(json: JSONObject): ListWidgetPayload {
        val itemsArray = json.optJSONArray("items") ?: JSONArray()
        val items = mutableListOf<WidgetListItem>()
        for (i in 0 until itemsArray.length()) {
            val itemObj = itemsArray.optJSONObject(i) ?: continue
            items.add(
                WidgetListItem(
                    label = itemObj.optString("label", ""),
                    value = itemObj.optString("value", ""),
                    color = itemObj.optStringOrNull("color"),
                    statusColor = itemObj.optStringOrNull("statusColor")
                )
            )
        }
        val buttons = parseButtons(json)
        val primaryBtn = buttons.firstOrNull()
        return ListWidgetPayload(
            title = json.optString("title", "Script"),
            items = items,
            buttonText = primaryBtn?.let { if (it.icon != null && it.text != null) "${it.icon} ${it.text}" else (it.icon ?: it.text) },
            buttonAction = primaryBtn?.action,
            buttonColor = primaryBtn?.color,
            buttonPosition = primaryBtn?.position,
            buttons = buttons,
            bg = json.optStringOrNull("bg") ?: json.optStringOrNull("background"),
            padding = parsePadding(json)
        )
    }

    private fun parseCard(json: JSONObject): CardWidgetPayload {
        val itemsArray = json.optJSONArray("items") ?: JSONArray()
        val items = mutableListOf<WidgetListItem>()
        for (i in 0 until itemsArray.length()) {
            val itemObj = itemsArray.optJSONObject(i) ?: continue
            items.add(
                WidgetListItem(
                    label = itemObj.optString("label", ""),
                    value = itemObj.optString("value", ""),
                    color = itemObj.optStringOrNull("color"),
                    statusColor = itemObj.optStringOrNull("statusColor")
                )
            )
        }
        val buttons = parseButtons(json)
        val primaryBtn = buttons.firstOrNull()
        return CardWidgetPayload(
            title = json.optStringOrNull("title"),
            items = items,
            footerText = json.optStringOrNull("footerText"),
            buttonText = primaryBtn?.let { if (it.icon != null && it.text != null) "${it.icon} ${it.text}" else (it.icon ?: it.text) },
            buttonAction = primaryBtn?.action,
            buttonColor = primaryBtn?.color,
            buttonPosition = primaryBtn?.position,
            buttons = buttons,
            bg = json.optStringOrNull("bg") ?: json.optStringOrNull("background"),
            padding = parsePadding(json)
        )
    }

    private fun parseButtons(json: JSONObject): List<WidgetButton> {
        val result = mutableListOf<WidgetButton>()

        // 1. Check for `buttons: [...]` array
        val buttonsArray = json.optJSONArray("buttons")
        if (buttonsArray != null && buttonsArray.length() > 0) {
            for (i in 0 until buttonsArray.length()) {
                val item = buttonsArray.opt(i)
                when (item) {
                    is JSONObject -> {
                        result.add(parseSingleButton(item))
                    }
                    is String -> {
                        if (item.isNotBlank()) {
                            result.add(WidgetButton(text = item))
                        }
                    }
                }
            }
            return result
        }

        // 2. Check for `button: { ... }` or `btn: { ... }` object
        val buttonObj = json.optJSONObject("button") ?: json.optJSONObject("btn")
        if (buttonObj != null) {
            result.add(parseSingleButton(buttonObj))
            return result
        }

        // 3. Fallback to scalar button properties: `buttonText`, `buttonAction`, etc.
        val legacyText = json.optStringOrNull("buttonText")
            ?: json.optStringOrNull("btnText")
            ?: json.optStringOrNull("button")
            ?: json.optStringOrNull("btn")

        if (!legacyText.isNullOrBlank()) {
            result.add(
                WidgetButton(
                    text = legacyText,
                    action = json.optStringOrNull("buttonAction") ?: json.optStringOrNull("btnAction") ?: "button_click",
                    color = json.optStringOrNull("buttonColor") ?: json.optStringOrNull("btnColor"),
                    position = json.optStringOrNull("buttonPosition") ?: json.optStringOrNull("btnPosition") ?: "bottom"
                )
            )
        }

        return result
    }

    private fun parseSingleButton(obj: JSONObject): WidgetButton {
        val text = obj.optStringOrNull("text") ?: obj.optStringOrNull("title") ?: obj.optStringOrNull("label")
        val icon = obj.optStringOrNull("icon")
        val action = obj.optStringOrNull("action") ?: obj.optStringOrNull("buttonAction") ?: "button_click"
        val color = obj.optStringOrNull("color")
        val bg = obj.optStringOrNull("bg") ?: obj.optStringOrNull("background")
        val fontSize = obj.optDoubleOrNull("fontSize")?.toFloat()
            ?: obj.optDoubleOrNull("size")?.toFloat()
        val position = obj.optStringOrNull("position") ?: "bottom"
        val align = obj.optStringOrNull("align") ?: "center"

        return WidgetButton(
            text = text,
            icon = icon,
            action = action,
            color = color,
            bg = bg,
            fontSize = fontSize,
            position = position,
            align = align
        )
    }

    private fun parsePadding(json: JSONObject): WidgetPadding? {
        if (!json.has("padding")) return null
        return when (val paddingVal = json.get("padding")) {
            is Number -> {
                val p = paddingVal.toInt()
                WidgetPadding(left = p, top = p, right = p, bottom = p)
            }
            is JSONObject -> {
                WidgetPadding(
                    left = paddingVal.optInt("left", 16),
                    top = paddingVal.optInt("top", 16),
                    right = paddingVal.optInt("right", 16),
                    bottom = paddingVal.optInt("bottom", 16)
                )
            }
            else -> null
        }
    }

    private fun parseTextAlign(alignStr: String?): WidgetTextAlign {
        return when (alignStr?.lowercase()) {
            "center" -> WidgetTextAlign.CENTER
            "right" -> WidgetTextAlign.RIGHT
            else -> WidgetTextAlign.LEFT
        }
    }

    private fun parseFontFamily(fontStr: String?): WidgetFontFamily {
        return when (fontStr?.lowercase()) {
            "monospace", "mono" -> WidgetFontFamily.MONOSPACE
            "serif" -> WidgetFontFamily.SERIF
            "sans-serif", "sans" -> WidgetFontFamily.SANS_SERIF
            "sans-serif-medium", "medium" -> WidgetFontFamily.SANS_SERIF_MEDIUM
            else -> WidgetFontFamily.DEFAULT
        }
    }

    private fun JSONObject.optStringOrNull(key: String): String? {
        if (!has(key)) return null
        val str = optString(key)
        return str.takeIf { it.isNotBlank() }
    }

    private fun JSONObject.optDoubleOrNull(key: String): Double? {
        if (!has(key)) return null
        val d = optDouble(key)
        return if (d.isNaN()) null else d
    }
}
