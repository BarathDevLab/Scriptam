package com.scriptam.app.widget

import com.scriptam.app.widget.diff.WidgetTreeDiffer
import com.scriptam.app.widget.model.ProgressWidgetPayload
import com.scriptam.app.widget.model.StatWidgetPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetTreeDifferTest {

    @Test
    fun testIdenticalPayloadsProduceNoChanges() {
        val payload1 = StatWidgetPayload(title = "Battery", value = "95%")
        val payload2 = StatWidgetPayload(title = "Battery", value = "95%")

        val diff = WidgetTreeDiffer.diff(payload1, payload2)
        assertFalse(diff.hasChanges)
        assertEquals(0, diff.changeCount)
    }

    @Test
    fun testStatValueMutationDetected() {
        val oldPayload = StatWidgetPayload(title = "BTC", value = "$65,000")
        val newPayload = StatWidgetPayload(title = "BTC", value = "$67,200")

        val diff = WidgetTreeDiffer.diff(oldPayload, newPayload)
        assertTrue(diff.hasChanges)
        assertTrue(diff.contentChanged)
        assertFalse(diff.bgChanged)
    }

    @Test
    fun testProgressMutationDetected() {
        val oldPayload = ProgressWidgetPayload(title = "Download", progress = 40)
        val newPayload = ProgressWidgetPayload(title = "Download", progress = 80)

        val diff = WidgetTreeDiffer.diff(oldPayload, newPayload)
        assertTrue(diff.hasChanges)
        assertTrue(diff.contentChanged)
    }

    @Test
    fun testTypeChangeDetected() {
        val oldPayload = StatWidgetPayload(title = "Status", value = "OK")
        val newPayload = ProgressWidgetPayload(title = "Status", progress = 100)

        val diff = WidgetTreeDiffer.diff(oldPayload, newPayload)
        assertTrue(diff.hasChanges)
    }
}
