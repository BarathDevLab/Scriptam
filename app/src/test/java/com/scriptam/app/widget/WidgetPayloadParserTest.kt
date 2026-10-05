package com.scriptam.app.widget

import com.scriptam.app.widget.model.CardWidgetPayload
import com.scriptam.app.widget.model.ConsoleWidgetPayload
import com.scriptam.app.widget.model.ListWidgetPayload
import com.scriptam.app.widget.model.ProgressWidgetPayload
import com.scriptam.app.widget.model.StatWidgetPayload
import com.scriptam.app.widget.model.WidgetFontFamily
import com.scriptam.app.widget.model.WidgetPayloadParser
import com.scriptam.app.widget.model.WidgetTextAlign
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetPayloadParserTest {

    @Test
    fun parse_plainText_returnsConsolePayload() {
        val result = WidgetPayloadParser.parse("Hello from console.log")
        assertTrue(result is ConsoleWidgetPayload)
        assertEquals("Hello from console.log", (result as ConsoleWidgetPayload).output)
    }

    @Test
    fun parse_nullOrEmpty_returnsEmptyConsolePayload() {
        val r1 = WidgetPayloadParser.parse(null)
        val r2 = WidgetPayloadParser.parse("")
        assertTrue(r1 is ConsoleWidgetPayload)
        assertEquals("", (r1 as ConsoleWidgetPayload).output)
        assertTrue(r2 is ConsoleWidgetPayload)
        assertEquals("", (r2 as ConsoleWidgetPayload).output)
    }

    @Test
    fun parse_statWithStyles_returnsStatPayload() {
        val json = """
            {
                "type": "stat",
                "title": "Bitcoin",
                "value": "$64,250",
                "subtitle": "+5.2% today",
                "color": "#00E676",
                "bg": "#1A1A24",
                "fontSize": 24,
                "bold": true,
                "fontFamily": "mono",
                "align": "center",
                "padding": 20
            }
        """.trimIndent()

        val result = WidgetPayloadParser.parse(json)
        assertTrue(result is StatWidgetPayload)
        val stat = result as StatWidgetPayload

        assertEquals("Bitcoin", stat.title)
        assertEquals("$64,250", stat.value)
        assertEquals("+5.2% today", stat.subtitle)
        assertEquals("#00E676", stat.color)
        assertEquals("#1A1A24", stat.bg)
        assertEquals(24f, stat.fontSize)
        assertEquals(true, stat.bold)
        assertEquals(WidgetFontFamily.MONOSPACE, stat.fontFamily)
        assertEquals(WidgetTextAlign.CENTER, stat.align)
        assertNotNull(stat.padding)
        assertEquals(20, stat.padding?.left)
    }

    @Test
    fun parse_progressWithColor_returnsProgressPayload() {
        val json = """
            {
                "type": "progress",
                "title": "Battery",
                "value": "84%",
                "progress": 84,
                "color": "#6C63FF",
                "bg": "#0D0D14"
            }
        """.trimIndent()

        val result = WidgetPayloadParser.parse(json)
        assertTrue(result is ProgressWidgetPayload)
        val p = result as ProgressWidgetPayload

        assertEquals("Battery", p.title)
        assertEquals("84%", p.value)
        assertEquals(84, p.progress)
        assertEquals("#6C63FF", p.color)
    }

    @Test
    fun parse_listWithItems_returnsListPayload() {
        val json = """
            {
                "type": "list",
                "title": "Cluster Status",
                "items": [
                    { "label": "Node 1", "value": "Healthy", "statusColor": "#00E676" },
                    { "label": "Node 2", "value": "Busy", "statusColor": "#FF9800" }
                ]
            }
        """.trimIndent()

        val result = WidgetPayloadParser.parse(json)
        assertTrue(result is ListWidgetPayload)
        val list = result as ListWidgetPayload

        assertEquals("Cluster Status", list.title)
        assertEquals(2, list.items.size)
        assertEquals("Node 1", list.items[0].label)
        assertEquals("Healthy", list.items[0].value)
        assertEquals("#00E676", list.items[0].statusColor)
    }

    @Test
    fun parse_statWithCustomButtonObject_returnsConfiguredButton() {
        val json = """
            {
                "type": "stat",
                "title": "Hydration",
                "value": "1500 ml",
                "button": {
                    "icon": "💧",
                    "text": "+250ml",
                    "action": "add_water",
                    "color": "#00E5FF",
                    "size": 14,
                    "position": "header"
                }
            }
        """.trimIndent()

        val result = WidgetPayloadParser.parse(json)
        assertTrue(result is StatWidgetPayload)
        val stat = result as StatWidgetPayload

        assertEquals("💧 +250ml", stat.buttonText)
        assertEquals("add_water", stat.buttonAction)
        assertEquals("header", stat.buttonPosition)
        assertEquals(1, stat.buttons.size)

        val btn = stat.buttons.first()
        assertEquals("💧", btn.icon)
        assertEquals("+250ml", btn.text)
        assertEquals("add_water", btn.action)
        assertEquals("#00E5FF", btn.color)
        assertEquals(14f, btn.fontSize)
        assertEquals("header", btn.position)
    }

    @Test
    fun parse_cardWithMultiButtons_returnsAllButtons() {
        val json = """
            {
                "type": "card",
                "title": "Music Player",
                "items": [
                    { "label": "Track", "value": "Starboy" },
                    { "label": "Artist", "value": "The Weeknd" }
                ],
                "buttons": [
                    { "icon": "⏮", "action": "prev", "size": 18 },
                    { "icon": "⏯", "action": "toggle", "size": 22, "color": "#00E676" },
                    { "icon": "⏭", "action": "next", "size": 18 },
                    { "icon": "❤️", "action": "like", "size": 16, "color": "#FF5252" }
                ]
            }
        """.trimIndent()

        val result = WidgetPayloadParser.parse(json)
        assertTrue(result is CardWidgetPayload)
        val card = result as CardWidgetPayload

        assertEquals("Music Player", card.title)
        assertEquals(4, card.buttons.size)
        assertEquals("⏮", card.buttons[0].icon)
        assertEquals("prev", card.buttons[0].action)
        assertEquals(18f, card.buttons[0].fontSize)

        assertEquals("⏯", card.buttons[1].icon)
        assertEquals("toggle", card.buttons[1].action)
        assertEquals("#00E676", card.buttons[1].color)
        assertEquals(22f, card.buttons[1].fontSize)

        assertEquals("⏭", card.buttons[2].icon)
        assertEquals("next", card.buttons[2].action)

        assertEquals("❤️", card.buttons[3].icon)
        assertEquals("like", card.buttons[3].action)
        assertEquals("#FF5252", card.buttons[3].color)
    }

    @Test
    fun parse_listPayloadWithColorsAndStatus_returnsParsedItems() {
        val json = """
            {
                "type": "list",
                "title": "Server Status",
                "items": [
                    { "label": "API", "value": "Online", "color": "#00E676", "statusColor": "#00FF00" },
                    { "label": "DB", "value": "Degraded", "color": "#FFD600", "statusColor": "#FFAA00" }
                ]
            }
        """.trimIndent()

        val result = WidgetPayloadParser.parse(json)
        assertTrue(result is ListWidgetPayload)
        val list = result as ListWidgetPayload

        assertEquals("Server Status", list.title)
        assertEquals(2, list.items.size)
        assertEquals("API", list.items[0].label)
        assertEquals("Online", list.items[0].value)
        assertEquals("#00E676", list.items[0].color)
        assertEquals("#00FF00", list.items[0].statusColor)

        assertEquals("DB", list.items[1].label)
        assertEquals("Degraded", list.items[1].value)
        assertEquals("#FFD600", list.items[1].color)
        assertEquals("#FFAA00", list.items[1].statusColor)
    }
}
