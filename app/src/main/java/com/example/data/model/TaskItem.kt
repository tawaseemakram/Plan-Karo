package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["taskDate"]),
        Index(value = ["isCompleted"]),
        Index(value = ["priority"]),
        Index(value = ["category"]),
        Index(value = ["taskDate", "orderIndex"])
    ]
)
data class TaskItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val notes: String = "",
    val priority: Priority = Priority.MEDIUM,
    val category: String = "Personal",
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val orderIndex: Int = 0,
    val reminderTimeMillis: Long? = null,
    val isReminderDismissed: Boolean = false,
    val imageUri: String? = null,
    val audioUri: String? = null,
    val audioDurationSeconds: Int = 0,
    val taskDate: String, // Format: YYYY-MM-DD
    val repeatMode: RepeatMode = RepeatMode.NONE,
    val createdAt: Long = System.currentTimeMillis()
)
