package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.Priority
import com.example.data.model.RepeatMode
import com.example.data.model.RoutineTime

class Converters {
    @TypeConverter
    fun fromPriority(priority: Priority): String {
        return priority.name
    }

    @TypeConverter
    fun toPriority(value: String): Priority {
        return try {
            Priority.valueOf(value)
        } catch (e: Exception) {
            Priority.MEDIUM
        }
    }

    @TypeConverter
    fun fromRoutineTime(routine: RoutineTime): String {
        return routine.name
    }

    @TypeConverter
    fun toRoutineTime(value: String): RoutineTime {
        return try {
            RoutineTime.valueOf(value)
        } catch (e: Exception) {
            RoutineTime.MORNING
        }
    }

    @TypeConverter
    fun fromRepeatMode(repeatMode: RepeatMode): String {
        return repeatMode.name
    }

    @TypeConverter
    fun toRepeatMode(value: String): RepeatMode {
        return try {
            RepeatMode.valueOf(value)
        } catch (e: Exception) {
            RepeatMode.NONE
        }
    }
}
