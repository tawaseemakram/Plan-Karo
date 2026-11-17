package com.example.data.repository

import android.content.Context
import com.example.data.local.HabitDao
import com.example.data.model.HabitItem
import com.example.reminder.RoutineReminderManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HabitRepository(
    private val habitDao: HabitDao,
    private val context: Context? = null
) {
    val allHabits: Flow<List<HabitItem>> = habitDao.getAllHabits()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    suspend fun insertHabit(habit: HabitItem): Long = withContext(Dispatchers.IO) {
        val newId = habitDao.insertHabit(habit)
        context?.let { ctx ->
            RoutineReminderManager.scheduleHabit(ctx, habit.copy(id = newId))
        }
        newId
    }

    suspend fun updateHabit(habit: HabitItem) = withContext(Dispatchers.IO) {
        habitDao.updateHabit(habit)
        context?.let { ctx ->
            RoutineReminderManager.scheduleHabit(ctx, habit)
        }
    }

    suspend fun deleteHabit(habit: HabitItem) = withContext(Dispatchers.IO) {
        habitDao.deleteHabit(habit)
        context?.let { ctx ->
            RoutineReminderManager.cancelHabit(ctx, habit.id)
        }
    }

    suspend fun toggleHabitForDate(habit: HabitItem, dateString: String) = withContext(Dispatchers.IO) {
        val completedSet = habit.getCompletedDatesSet().toMutableSet()
        if (completedSet.contains(dateString)) {
            completedSet.remove(dateString)
        } else {
            completedSet.add(dateString)
        }

        val updatedDatesString = completedSet.sorted().joinToString(",")
        val (currentStreak, bestStreak) = calculateStreaks(completedSet)

        val updated = habit.copy(
            completedDates = updatedDatesString,
            streakCount = currentStreak,
            bestStreak = maxOf(habit.bestStreak, bestStreak)
        )

        habitDao.updateHabit(updated)
    }

    private fun calculateStreaks(datesSet: Set<String>): Pair<Int, Int> {
        if (datesSet.isEmpty()) return Pair(0, 0)

        val cal = Calendar.getInstance()
        val todayStr = dateFormat.format(cal.time)
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayStr = dateFormat.format(cal.time)

        // Calculate current streak
        var streak = 0
        val checkCal = Calendar.getInstance()

        // If today is completed, start from today. Else if yesterday is completed, start from yesterday
        val hasToday = datesSet.contains(todayStr)
        val hasYesterday = datesSet.contains(yesterdayStr)

        if (hasToday) {
            streak = 1
            checkCal.add(Calendar.DAY_OF_YEAR, -1)
            while (datesSet.contains(dateFormat.format(checkCal.time))) {
                streak++
                checkCal.add(Calendar.DAY_OF_YEAR, -1)
            }
        } else if (hasYesterday) {
            streak = 1
            checkCal.add(Calendar.DAY_OF_YEAR, -2)
            while (datesSet.contains(dateFormat.format(checkCal.time))) {
                streak++
                checkCal.add(Calendar.DAY_OF_YEAR, -1)
            }
        }

        return Pair(streak, maxOf(streak, datesSet.size))
    }
}
