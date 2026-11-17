package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "routines",
    indices = [
        Index(value = ["routineTime"]),
        Index(value = ["isEnabled"])
    ]
)
data class RoutineItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val description: String = "",
    val routineTime: RoutineTime = RoutineTime.MORNING,
    val scheduledTime: String = "08:00 AM",
    val daysOfWeek: String = "DAILY",
    val isEnabled: Boolean = true,
    val orderIndex: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
