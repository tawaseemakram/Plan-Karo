package com.example.data.repository

import android.content.Context
import com.example.data.local.TaskDao
import com.example.data.model.RepeatMode
import com.example.data.model.TaskItem
import com.example.reminder.ReminderManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TaskRepository(
    private val context: Context,
    private val taskDao: TaskDao
) {
    fun getAllTasks(): Flow<List<TaskItem>> = taskDao.getAllTasks()

    fun getTasksForDate(date: String): Flow<List<TaskItem>> = taskDao.getTasksForDate(date)

    suspend fun insertTask(task: TaskItem): Long = withContext(Dispatchers.IO) {
        val maxOrder = taskDao.getMaxOrderIndex(task.taskDate) ?: -1
        val taskWithOrder = if (task.orderIndex <= 0) {
            task.copy(orderIndex = maxOrder + 1)
        } else task

        val id = taskDao.insertTask(taskWithOrder)
        val inserted = taskWithOrder.copy(id = id)
        if (inserted.reminderTimeMillis != null && !inserted.isReminderDismissed) {
            ReminderManager.scheduleReminder(context, inserted, allowRecentPastGrace = true)
        }
        if (inserted.repeatMode != RepeatMode.NONE) {
            ReminderManager.scheduleMidnightRollover(context)
        }
        id
    }

    suspend fun updateTask(task: TaskItem) = withContext(Dispatchers.IO) {
        taskDao.updateTask(task)
        if (task.reminderTimeMillis != null && !task.isReminderDismissed && !task.isCompleted) {
            ReminderManager.scheduleReminder(context, task, allowRecentPastGrace = true)
        } else {
            ReminderManager.cancelReminder(context, task.id)
        }
        if (task.repeatMode != RepeatMode.NONE) {
            ReminderManager.scheduleMidnightRollover(context)
        }
    }

    suspend fun toggleTaskCompletion(task: TaskItem): Boolean = withContext(Dispatchers.IO) {
        val willBeCompleted = !task.isCompleted
        val updated = task.copy(
            isCompleted = willBeCompleted,
            completedAt = if (willBeCompleted) System.currentTimeMillis() else null
        )
        taskDao.updateTask(updated)

        val nextDateString: String? = computeNextOccurrenceDate(task.taskDate, task.repeatMode)

        if (willBeCompleted) {
            ReminderManager.cancelReminder(context, task.id)

            // If task has Repeat Mode (Daily/Weekly/Monthly), auto-create & arm next occurrence
            if (task.repeatMode != RepeatMode.NONE) {
                ensureNextRepeatingOccurrenceScheduled(updated)
            }
        } else {
            // Uncompleted: re-schedule reminder if valid
            if (updated.reminderTimeMillis != null && updated.reminderTimeMillis > System.currentTimeMillis()) {
                ReminderManager.scheduleReminder(context, updated)
            }
            // Only remove auto-cloned future occurrence if today's reminder hasn't already fired/been dismissed
            val todayReminderNotYetFired = !task.isReminderDismissed &&
                (task.reminderTimeMillis == null || task.reminderTimeMillis > System.currentTimeMillis())
            if (nextDateString != null && todayReminderNotYetFired) {
                val existingNext = taskDao.getTaskByDateAndTitle(nextDateString, task.title)
                if (existingNext != null && !existingNext.isCompleted) {
                    taskDao.deleteTask(existingNext)
                    ReminderManager.cancelReminder(context, existingNext.id)
                }
            }
        }
        willBeCompleted
    }

    /**
     * Ensures the next occurrence for a Daily / Weekly / Monthly repeating task exists in Room
     * and has its exact AlarmManager reminder scheduled in the background — whether the user
     * completed the task or the scheduled reminder fired without opening the app.
     */
    suspend fun ensureNextRepeatingOccurrenceScheduled(
        task: TaskItem,
        nowMillis: Long = System.currentTimeMillis()
    ): TaskItem? = withContext(Dispatchers.IO) {
        if (task.repeatMode == RepeatMode.NONE) return@withContext null

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayDateString = sdf.format(Date(nowMillis))

        var nextDateString = computeNextOccurrenceDate(task.taskDate, task.repeatMode)
            ?: return@withContext null
        var steps = 0
        while (nextDateString < todayDateString && steps < 366) {
            nextDateString = computeNextOccurrenceDate(nextDateString, task.repeatMode) ?: break
            steps++
        }

        val ensured = createOrArmOccurrenceForDate(task, nextDateString, nowMillis)

        // If today's shifted reminder time already passed (e.g. catch-up rollover late in the day),
        // also arm the following cycle immediately so tomorrow/next cycle won't be missed.
        if (ensured != null &&
            ensured.taskDate == todayDateString &&
            ensured.reminderTimeMillis != null &&
            ensured.reminderTimeMillis <= nowMillis
        ) {
            val followingDate = computeNextOccurrenceDate(nextDateString, task.repeatMode)
            if (followingDate != null) {
                createOrArmOccurrenceForDate(ensured, followingDate, nowMillis)
            }
        }
        ensured
    }

    private suspend fun createOrArmOccurrenceForDate(
        sourceTask: TaskItem,
        targetDate: String,
        nowMillis: Long
    ): TaskItem? {
        val exactMatch = taskDao.getTaskByDateAndTitle(targetDate, sourceTask.title)
        val normalizedTitle = sourceTask.title.trim().lowercase(Locale.getDefault())
        val existing = exactMatch ?: taskDao.getRepeatingTasks().firstOrNull {
            it.taskDate == targetDate &&
                it.repeatMode == sourceTask.repeatMode &&
                it.title.trim().lowercase(Locale.getDefault()) == normalizedTitle
        }

        if (existing != null) {
            if (!existing.isCompleted &&
                !existing.isReminderDismissed &&
                existing.reminderTimeMillis != null &&
                existing.reminderTimeMillis > nowMillis - 5000L
            ) {
                ReminderManager.scheduleReminder(context, existing)
            }
            return existing
        }

        val shiftedReminder = shiftReminderToDate(sourceTask.reminderTimeMillis, targetDate)
        if (!sourceTask.isCompleted &&
            !sourceTask.isReminderDismissed &&
            sourceTask.reminderTimeMillis != null
        ) {
            val sourceReminderDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(Date(sourceTask.reminderTimeMillis))
            if (sourceReminderDate == targetDate) {
                taskDao.updateTask(sourceTask.copy(isReminderDismissed = true))
                ReminderManager.cancelReminder(context, sourceTask.id)
            }
        }

        val nextTask = TaskItem(
            title = sourceTask.title,
            notes = sourceTask.notes,
            priority = sourceTask.priority,
            category = sourceTask.category,
            isCompleted = false,
            completedAt = null,
            orderIndex = (taskDao.getMaxOrderIndex(targetDate) ?: -1) + 1,
            reminderTimeMillis = shiftedReminder,
            isReminderDismissed = false,
            imageUri = sourceTask.imageUri,
            audioUri = sourceTask.audioUri,
            audioDurationSeconds = sourceTask.audioDurationSeconds,
            taskDate = targetDate,
            repeatMode = sourceTask.repeatMode
        )
        val newId = taskDao.insertTask(nextTask)
        val inserted = nextTask.copy(id = newId)
        if (shiftedReminder != null && shiftedReminder > nowMillis - 5000L) {
            ReminderManager.scheduleReminder(context, inserted)
        }
        return inserted
    }

    /**
     * Automatically rolls over Daily/Weekly/Monthly repeating tasks onto their scheduled dates
     * and keeps upcoming alarms armed even when the user never opened the app.
     */
    suspend fun syncRepeatingTasksForToday(
        todayDateString: String,
        nowMillis: Long = System.currentTimeMillis()
    ) = withContext(Dispatchers.IO) {
        val repeatingTasks = taskDao.getRepeatingTasks()
        if (repeatingTasks.isEmpty()) return@withContext

        val seriesByKey = repeatingTasks.groupBy {
            Pair(it.title.trim().lowercase(Locale.getDefault()), it.repeatMode)
        }

        for ((_, seriesTasks) in seriesByKey) {
            val latestTask = seriesTasks.maxByOrNull { it.taskDate } ?: continue

            when {
                latestTask.taskDate < todayDateString -> {
                    when (latestTask.repeatMode) {
                        RepeatMode.DAILY -> {
                            val todayTask = createOrArmOccurrenceForDate(latestTask, todayDateString, nowMillis)
                            if (todayTask != null &&
                                todayTask.reminderTimeMillis != null &&
                                todayTask.reminderTimeMillis <= nowMillis
                            ) {
                                ensureNextRepeatingOccurrenceScheduled(todayTask, nowMillis)
                            }
                        }
                        RepeatMode.WEEKLY, RepeatMode.MONTHLY -> {
                            var cursorDate = latestTask.taskDate
                            var steps = 0
                            while (cursorDate < todayDateString && steps < 366) {
                                val nextDate = computeNextOccurrenceDate(cursorDate, latestTask.repeatMode) ?: break
                                cursorDate = nextDate
                                steps++
                            }
                            if (cursorDate >= todayDateString) {
                                val targetTask = createOrArmOccurrenceForDate(latestTask, cursorDate, nowMillis)
                                if (targetTask != null &&
                                    cursorDate == todayDateString &&
                                    targetTask.reminderTimeMillis != null &&
                                    targetTask.reminderTimeMillis <= nowMillis
                                ) {
                                    ensureNextRepeatingOccurrenceScheduled(targetTask, nowMillis)
                                }
                            }
                        }
                        RepeatMode.NONE -> Unit
                    }
                }

                latestTask.taskDate == todayDateString -> {
                    val alreadyTriggeredToday = latestTask.isCompleted ||
                        latestTask.isReminderDismissed ||
                        (latestTask.reminderTimeMillis != null && latestTask.reminderTimeMillis <= nowMillis)
                    if (alreadyTriggeredToday) {
                        ensureNextRepeatingOccurrenceScheduled(latestTask, nowMillis)
                    } else if (latestTask.reminderTimeMillis != null) {
                        ReminderManager.scheduleReminder(context, latestTask)
                    }
                }

                else -> {
                    // Future occurrence already exists: ensure its alarm is armed
                    if (!latestTask.isCompleted &&
                        !latestTask.isReminderDismissed &&
                        latestTask.reminderTimeMillis != null &&
                        latestTask.reminderTimeMillis > nowMillis - 5000L
                    ) {
                        ReminderManager.scheduleReminder(context, latestTask)
                    }
                }
            }
        }
    }

    suspend fun snoozeTask(task: TaskItem, snoozeMinutes: Int) = withContext(Dispatchers.IO) {
        val newReminder = System.currentTimeMillis() + (snoozeMinutes * 60 * 1000L)
        val updated = task.copy(
            reminderTimeMillis = newReminder,
            isReminderDismissed = false
        )
        ReminderManager.cancelReminder(context, task.id)
        taskDao.updateTask(updated)
        ReminderManager.scheduleReminder(context, updated)
    }

    suspend fun dismissReminder(task: TaskItem) = withContext(Dispatchers.IO) {
        val updated = task.copy(isReminderDismissed = true)
        taskDao.updateTask(updated)
        ReminderManager.cancelReminder(context, task.id)
        if (updated.repeatMode != RepeatMode.NONE) {
            ensureNextRepeatingOccurrenceScheduled(updated)
        }
    }

    suspend fun deleteTask(task: TaskItem) = withContext(Dispatchers.IO) {
        taskDao.deleteTask(task)
        ReminderManager.cancelReminder(context, task.id)
    }

    suspend fun reorderTasks(newOrderedTasks: List<TaskItem>) = withContext(Dispatchers.IO) {
        val updatedList = newOrderedTasks.mapIndexed { index, task ->
            task.copy(orderIndex = index)
        }
        taskDao.updateTasks(updatedList)
    }

    companion object {
        fun computeNextOccurrenceDate(dateString: String, repeatMode: RepeatMode): String? {
            if (repeatMode == RepeatMode.NONE) return null
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val cal = Calendar.getInstance().apply {
                time = try {
                    sdf.parse(dateString) ?: Date()
                } catch (_: Exception) {
                    Date()
                }
            }
            when (repeatMode) {
                RepeatMode.DAILY -> cal.add(Calendar.DAY_OF_YEAR, 1)
                RepeatMode.WEEKLY -> cal.add(Calendar.DAY_OF_YEAR, 7)
                RepeatMode.MONTHLY -> cal.add(Calendar.MONTH, 1)
                RepeatMode.NONE -> return null
            }
            return sdf.format(cal.time)
        }

        fun shiftReminderToDate(prevReminderMillis: Long?, targetDateString: String): Long? {
            if (prevReminderMillis == null) return null
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val targetDate = try {
                sdf.parse(targetDateString) ?: return null
            } catch (_: Exception) {
                return null
            }
            val timeCal = Calendar.getInstance().apply { timeInMillis = prevReminderMillis }
            val resultCal = Calendar.getInstance().apply {
                time = targetDate
                set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY))
                set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE))
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return resultCal.timeInMillis
        }
    }
}
