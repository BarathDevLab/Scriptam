package com.scriptam.app.core.pipeline

import com.scriptam.app.core.ScriptConsole
import com.scriptam.app.core.ScriptEngineManager
import com.scriptam.app.data.repository.ScriptRepository
import org.json.JSONObject

/**
 * Directed Acyclic Graph (DAG) executor for chaining multiple Scriptam scripts.
 *
 * Automatically resolves execution order using topological sorting, detects circular
 * dependencies, and passes outputs from upstream nodes to downstream dependencies.
 */
class DagPipelineManager(
    private val engineManager: ScriptEngineManager? = null,
    private val repository: ScriptRepository? = null
) {

    /**
     * Resolves nodes into a valid topological execution order.
     * Throws [IllegalStateException] if a dependency cycle is detected or an unknown dependency is referenced.
     */
    fun topologicalSort(nodes: List<PipelineNode>): List<PipelineNode> {
        val nodeMap = nodes.associateBy { it.id }
        val inDegree = mutableMapOf<String, Int>()
        val adjList = mutableMapOf<String, MutableList<String>>()

        for (node in nodes) {
            inDegree[node.id] = 0
            adjList[node.id] = mutableListOf()
        }

        for (node in nodes) {
            for (dep in node.dependsOn) {
                if (!nodeMap.containsKey(dep)) {
                    throw IllegalStateException("Pipeline node '${node.id}' depends on missing node '$dep'")
                }
                adjList[dep]?.add(node.id)
                inDegree[node.id] = (inDegree[node.id] ?: 0) + 1
            }
        }

        val queue = ArrayDeque<String>()
        for ((nodeId, degree) in inDegree) {
            if (degree == 0) {
                queue.add(nodeId)
            }
        }

        val sorted = mutableListOf<PipelineNode>()
        while (queue.isNotEmpty()) {
            val curr = queue.removeFirst()
            nodeMap[curr]?.let { sorted.add(it) }

            for (neighbor in adjList[curr] ?: emptyList()) {
                val newDegree = (inDegree[neighbor] ?: 1) - 1
                inDegree[neighbor] = newDegree
                if (newDegree == 0) {
                    queue.add(neighbor)
                }
            }
        }

        if (sorted.size != nodes.size) {
            throw IllegalStateException("Circular dependency cycle detected in script pipeline")
        }

        return sorted
    }

    /**
     * Executes the pipeline nodes in topological order.
     * The output of each step is recorded and injected into downstream dependencies via `Native.pipelineInput`.
     */
    suspend fun executePipeline(
        nodes: List<PipelineNode>,
        initialInput: Any? = null,
        bridgeInstaller: (suspend (com.dokar.quickjs.QuickJs) -> Unit)? = null
    ): PipelineExecutionResult {
        val startAll = System.currentTimeMillis()
        val sortedNodes = try {
            topologicalSort(nodes)
        } catch (e: Exception) {
            return PipelineExecutionResult(
                isSuccess = false,
                stepResults = emptyList(),
                finalOutput = null,
                totalDurationMs = 0L,
                error = e.message
            )
        }

        if (repository == null || engineManager == null) {
            return PipelineExecutionResult(
                isSuccess = false,
                stepResults = emptyList(),
                finalOutput = null,
                totalDurationMs = 0L,
                error = "Repository and EngineManager must be provided for pipeline execution"
            )
        }

        val stepOutputs = mutableMapOf<String, Any?>()
        val stepResults = mutableListOf<PipelineStepResult>()
        var lastOutput: Any? = initialInput

        for (node in sortedNodes) {
            val scriptEntity = repository.getScriptById(node.scriptId)
            if (scriptEntity == null) {
                val err = "Script #${node.scriptId} not found in database for step '${node.id}'"
                stepResults.add(
                    PipelineStepResult(node.id, node.scriptId, null, 0L, false, err)
                )
                return PipelineExecutionResult(
                    isSuccess = false,
                    stepResults = stepResults,
                    finalOutput = null,
                    totalDurationMs = System.currentTimeMillis() - startAll,
                    error = err
                )
            }

            val scriptCode = repository.readScriptContent(scriptEntity.fileName)
            val stepConsole = ScriptConsole()

            // Prepare dependency input payload
            val inputPayload = if (node.dependsOn.isEmpty()) {
                lastOutput
            } else {
                node.dependsOn.associateWith { stepOutputs[it] }
            }

            val inputJson = try {
                if (inputPayload is String) JSONObject.quote(inputPayload)
                else JSONObject.wrap(inputPayload)?.toString() ?: "null"
            } catch (_: Exception) { "null" }

            // Inject input before execution
            val wrappedScript = """
                (function() {
                    if (typeof Native === 'undefined') { var Native = {}; }
                    Native.pipelineInput = $inputJson;
                })();
                $scriptCode
            """.trimIndent()

            val stepStart = System.currentTimeMillis()
            val result = engineManager.execute(
                script = wrappedScript,
                console = stepConsole,
                bridgeInstaller = bridgeInstaller
            )
            val stepDuration = System.currentTimeMillis() - stepStart

            if (result.isSuccess) {
                val output = result.getOrNull()?.value
                stepOutputs[node.id] = output
                lastOutput = output
                stepResults.add(
                    PipelineStepResult(
                        nodeId = node.id,
                        scriptId = node.scriptId,
                        output = output,
                        durationMs = stepDuration,
                        isSuccess = true
                    )
                )
            } else {
                val err = result.exceptionOrNull()?.message ?: "Step failed"
                stepResults.add(
                    PipelineStepResult(
                        nodeId = node.id,
                        scriptId = node.scriptId,
                        output = null,
                        durationMs = stepDuration,
                        isSuccess = false,
                        error = err
                    )
                )
                return PipelineExecutionResult(
                    isSuccess = false,
                    stepResults = stepResults,
                    finalOutput = null,
                    totalDurationMs = System.currentTimeMillis() - startAll,
                    error = "Pipeline aborted at step '${node.id}': $err"
                )
            }
        }

        return PipelineExecutionResult(
            isSuccess = true,
            stepResults = stepResults,
            finalOutput = lastOutput,
            totalDurationMs = System.currentTimeMillis() - startAll
        )
    }
}
