package com.toffice.app.data.task

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TaskPriority { LOW, NORMAL, HIGH }

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val notes: String = "",
    val isDone: Boolean = false,
    val priority: TaskPriority = TaskPriority.NORMAL,
    val dueDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
