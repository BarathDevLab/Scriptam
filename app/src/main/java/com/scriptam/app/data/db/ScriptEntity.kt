package com.scriptam.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scripts")
data class ScriptEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fileName: String,
    val title: String,
    val accentColor: Long = 0xFF6C63FF,
    val lastModified: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    /** Last execution status: "SUCCESS", "ERROR", "TIMEOUT", or "NEVER" */
    val lastRunStatus: String = "NEVER",
    /** Last execution duration in milliseconds (-1 = never run) */
    val lastRunDurationMs: Long = -1L
)
