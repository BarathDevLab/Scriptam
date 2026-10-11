package com.scriptam.app.core

import com.scriptam.app.core.pipeline.DagPipelineManager
import com.scriptam.app.core.pipeline.PipelineNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DagPipelineTest {

    private val manager = DagPipelineManager()

    @Test
    fun testLinearPipelineTopologicalSort() {
        val nodes = listOf(
            PipelineNode(id = "stepC", scriptId = 3L, dependsOn = listOf("stepB")),
            PipelineNode(id = "stepA", scriptId = 1L, dependsOn = emptyList()),
            PipelineNode(id = "stepB", scriptId = 2L, dependsOn = listOf("stepA"))
        )

        val sorted = manager.topologicalSort(nodes)
        assertEquals(3, sorted.size)
        assertEquals("stepA", sorted[0].id)
        assertEquals("stepB", sorted[1].id)
        assertEquals("stepC", sorted[2].id)
    }

    @Test(expected = IllegalStateException::class)
    fun testCircularDependencyThrowsException() {
        val circularNodes = listOf(
            PipelineNode(id = "stepA", scriptId = 1L, dependsOn = listOf("stepB")),
            PipelineNode(id = "stepB", scriptId = 2L, dependsOn = listOf("stepA"))
        )

        manager.topologicalSort(circularNodes)
    }

    @Test(expected = IllegalStateException::class)
    fun testMissingDependencyThrowsException() {
        val orphanNodes = listOf(
            PipelineNode(id = "stepA", scriptId = 1L, dependsOn = listOf("nonExistentStep"))
        )

        manager.topologicalSort(orphanNodes)
    }
}
