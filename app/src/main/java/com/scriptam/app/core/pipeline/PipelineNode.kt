package com.scriptam.app.core.pipeline

/**
 * Node definition in a script execution DAG pipeline.
 *
 * @param id          Unique node identifier in the pipeline (e.g., "fetch_step").
 * @param scriptId    Room database ID of the script to execute.
 * @param scriptName  Optional human-readable title of the script.
 * @param dependsOn   List of node IDs that must finish executing before this node starts.
 */
data class PipelineNode(
    val id: String,
    val scriptId: Long,
    val scriptName: String = "",
    val dependsOn: List<String> = emptyList()
)

data class PipelineStepResult(
    val nodeId: String,
    val scriptId: Long,
    val output: Any?,
    val durationMs: Long,
    val isSuccess: Boolean,
    val error: String? = null
)

data class PipelineExecutionResult(
    val isSuccess: Boolean,
    val stepResults: List<PipelineStepResult>,
    val finalOutput: Any?,
    val totalDurationMs: Long,
    val error: String? = null
)
