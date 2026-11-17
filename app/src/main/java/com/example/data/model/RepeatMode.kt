package com.example.data.model

enum class RepeatMode(
    val label: String,
    val shortLabel: String,
    val description: String
) {
    NONE("No Repeat", "No Repeat", "Task does not repeat after completion"),
    DAILY("Daily", "Daily", "Repeats every day automatically"),
    WEEKLY("Weekly", "Weekly", "Repeats every week on this day"),
    MONTHLY("Monthly", "Monthly", "Repeats every month on this date");

    companion object {
        fun fromString(value: String?): RepeatMode {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: NONE
        }
    }
}
