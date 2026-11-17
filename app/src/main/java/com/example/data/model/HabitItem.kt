package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "habits",
    indices = [
        Index(value = ["routineTime"]),
        Index(value = ["category"])
    ]
)
data class HabitItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val category: String = "Health",
    val routineTime: RoutineTime = RoutineTime.MORNING,
    val streakCount: Int = 0,
    val bestStreak: Int = 0,
    val completedDates: String = "", // Comma-separated list of YYYY-MM-DD
    val targetDaysPerWeek: Int = 7,
    val reminderTime: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun isCompletedOn(dateString: String): Boolean {
        if (completedDates.isBlank()) return false
        val set = completedDates.split(",").map { it.trim() }.toSet()
        return set.contains(dateString)
    }

    fun getCompletedDatesSet(): Set<String> {
        if (completedDates.isBlank()) return emptySet()
        return completedDates.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }
}
