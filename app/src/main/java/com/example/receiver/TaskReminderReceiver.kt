package com.example.receiver

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.example.data.local.TaskDatabase
import com.example.data.model.RepeatMode
import com.example.data.repository.TaskRepository
import com.example.reminder.AlarmPreferenceManager
import com.example.reminder.AlarmSoundManager
import com.example.reminder.DailyReminderManager
import com.example.reminder.PermissionHelper
import com.example.reminder.ReminderManager
import com.example.reminder.RoutineReminderManager
import com.example.service.TaskAlarmService
import com.example.ui.alarm.TaskAlarmActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

class TaskReminderReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "TaskReminderReceiver"
        private const val RECEIVER_WAKE_LOCK_TAG = "PlanKaro:ReminderReceiverWakeLock"
        private const val RECEIVER_WAKE_LOCK_TIMEOUT_MS = 15_000L

        const val SNOOZE_REQUEST_CODE_OFFSET = 1_000_000
        const val COMPLETE_REQUEST_CODE_OFFSET = 1_500_000
        const val DISMISS_REQUEST_CODE_OFFSET = 2_000_000

        private val activeReceiverWakeLockCount = AtomicInteger(0)

        private val RESCHEDULE_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON",
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
            ReminderManager.ACTION_MIDNIGHT_ROLLOVER
        )

        @androidx.annotation.VisibleForTesting
        internal fun activeReceiverWakeLockCountForTest(): Int = activeReceiverWakeLockCount.get()

        private fun acquireReceiverWakeLock(context: Context): PowerManager.WakeLock? {
            return try {
                val pm = context.applicationContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
                    ?: return null
                pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, RECEIVER_WAKE_LOCK_TAG).apply {
                    setReferenceCounted(false)
                    acquire(RECEIVER_WAKE_LOCK_TIMEOUT_MS)
                }.also {
                    activeReceiverWakeLockCount.incrementAndGet()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Unable to acquire receiver WakeLock", e)
                null
            }
        }

        private fun releaseReceiverWakeLock(wakeLock: PowerManager.WakeLock?) {
            if (wakeLock == null) return
            try {
                if (wakeLock.isHeld) {
                    wakeLock.release()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Unable to release receiver WakeLock", e)
            } finally {
                activeReceiverWakeLockCount.updateAndGet { current -> (current - 1).coerceAtLeast(0) }
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action

        if (action in RESCHEDULE_ACTIONS) {
            rescheduleAllPendingReminders(context)
            return
        }

        if (action == DailyReminderManager.ACTION_DAILY_REMINDER) {
            val wakeLock = acquireReceiverWakeLock(context)
            try {
                val period = intent.getStringExtra(DailyReminderManager.EXTRA_PERIOD)
                    ?: DailyReminderManager.PERIOD_MORNING
                DailyReminderManager.showDailyNotification(context, period)
            } finally {
                releaseReceiverWakeLock(wakeLock)
            }
            return
        }

        if (action == RoutineReminderManager.ACTION_ROUTINE_REMINDER) {
            val routineId = intent.getLongExtra(RoutineReminderManager.EXTRA_ITEM_ID, -1L)
            if (routineId == -1L) return
            val title = intent.getStringExtra(RoutineReminderManager.EXTRA_ITEM_TITLE) ?: "Daily Routine"
            val desc = intent.getStringExtra(RoutineReminderManager.EXTRA_ITEM_DESC) ?: ""
            val timeStr = intent.getStringExtra(RoutineReminderManager.EXTRA_ITEM_TIME) ?: "08:00 AM"
            handleRoutineReminderAsync(context, routineId, title, desc, timeStr)
            return
        }

        if (action == RoutineReminderManager.ACTION_HABIT_REMINDER) {
            val habitId = intent.getLongExtra(RoutineReminderManager.EXTRA_ITEM_ID, -1L)
            if (habitId == -1L) return
            val title = intent.getStringExtra(RoutineReminderManager.EXTRA_ITEM_TITLE) ?: "Daily Habit"
            val desc = intent.getStringExtra(RoutineReminderManager.EXTRA_ITEM_DESC) ?: ""
            val timeStr = intent.getStringExtra(RoutineReminderManager.EXTRA_ITEM_TIME) ?: "08:00 AM"
            handleHabitReminderAsync(context, habitId, title, desc, timeStr)
            return
        }

        if (action == ReminderManager.ACTION_DISMISS) {
            val dismissTaskId = intent.getLongExtra(ReminderManager.EXTRA_TASK_ID, -1L)
            val notifId = intent.getIntExtra(RoutineReminderManager.EXTRA_NOTIFICATION_ID, -1)
            dismissTaskNotification(context, dismissTaskId, notifId)
            return
        }

        val taskId = intent.getLongExtra(ReminderManager.EXTRA_TASK_ID, -1L)
        if (taskId == -1L) return

        when (action) {
            ReminderManager.ACTION_REMINDER -> {
                val title = intent.getStringExtra(ReminderManager.EXTRA_TASK_TITLE) ?: "Task Reminder"
                val notes = intent.getStringExtra(ReminderManager.EXTRA_TASK_NOTES) ?: ""
                val priority = intent.getStringExtra(ReminderManager.EXTRA_TASK_PRIORITY) ?: "MEDIUM"

                showNotificationAndStartAlarm(context, taskId, title, notes, priority)
                scheduleNextRepeatOccurrenceInBackground(context, taskId)
            }
            ReminderManager.ACTION_SNOOZE -> {
                val minutes = intent.getIntExtra(ReminderManager.EXTRA_SNOOZE_MINUTES, 10)
                snoozeTask(context, taskId, minutes)
            }
            ReminderManager.ACTION_COMPLETE -> {
                completeTask(context, taskId)
            }
        }
    }

    private fun rescheduleAllPendingReminders(context: Context) {
        val wakeLock = acquireReceiverWakeLock(context)
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ReminderManager.rescheduleAllAlarms(context)
            } catch (e: Exception) {
                Log.e(TAG, "Error rescheduling alarms", e)
            } finally {
                releaseReceiverWakeLock(wakeLock)
                pendingResult?.finish()
            }
        }
    }

    private fun showNotificationAndStartAlarm(
        context: Context,
        taskId: Long,
        title: String,
        notes: String,
        priority: String
    ) {
        val isAlarmActive = AlarmPreferenceManager.isAlarmEnabled(context)
        val serviceStarted = if (isAlarmActive) {
            TaskAlarmService.acquireAlarmWakeLock(context)
            AlarmSoundManager.startAlarm(context)
            TaskAlarmService.startAlarmService(context, taskId, title, notes, priority)
        } else {
            AlarmSoundManager.stopAlarm(context)
            false
        }

        val notification = ReminderManager.buildTaskReminderNotification(
            context = context,
            taskId = taskId,
            title = title,
            notes = notes,
            priority = priority,
            isAlarmActive = isAlarmActive,
            audibleAlarmNotification = isAlarmActive && !serviceStarted
        )

        if (!PermissionHelper.hasNotificationPermission(context) ||
            !NotificationManagerCompat.from(context).areNotificationsEnabled()
        ) {
            Log.w(TAG, "Notifications blocked or missing permission; attempting safe notify for task $taskId")
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        try {
            notificationManager.notify(taskId.toInt(), notification)
        } catch (se: SecurityException) {
            Log.w(TAG, "Missing notification permission when posting task reminder $taskId", se)
        }
    }

    private fun scheduleNextRepeatOccurrenceInBackground(context: Context, taskId: Long) {
        val wakeLock = acquireReceiverWakeLock(context)
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = TaskDatabase.getDatabase(context)
                val task = db.taskDao().getTaskById(taskId)
                if (task != null && task.repeatMode != RepeatMode.NONE) {
                    val repository = TaskRepository(context, db.taskDao())
                    repository.ensureNextRepeatingOccurrenceScheduled(task)
                }
                ReminderManager.scheduleMidnightRollover(context)
            } catch (e: Exception) {
                Log.e(TAG, "Error advancing repeating task $taskId", e)
            } finally {
                releaseReceiverWakeLock(wakeLock)
                pendingResult?.finish()
            }
        }
    }

    private fun sendCloseAlarmScreenBroadcast(context: Context, taskId: Long) {
        runCatching {
            context.sendBroadcast(
                Intent(TaskAlarmActivity.ACTION_CLOSE_ALARM_SCREEN).apply {
                    setPackage(context.packageName)
                    putExtra(ReminderManager.EXTRA_TASK_ID, taskId)
                }
            )
        }
    }

    private fun snoozeTask(context: Context, taskId: Long, minutes: Int) {
        AlarmSoundManager.stopAlarm(context)
        sendCloseAlarmScreenBroadcast(context, taskId)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(taskId.toInt())

        val wakeLock = acquireReceiverWakeLock(context)
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = TaskDatabase.getDatabase(context)
                val task = db.taskDao().getTaskById(taskId)
                if (task != null) {
                    val repository = TaskRepository(context, db.taskDao())
                    repository.snoozeTask(task, minutes)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error snoozing task $taskId", e)
            } finally {
                releaseReceiverWakeLock(wakeLock)
                pendingResult?.finish()
            }
        }
    }

    private fun completeTask(context: Context, taskId: Long) {
        AlarmSoundManager.stopAlarm(context)
        sendCloseAlarmScreenBroadcast(context, taskId)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(taskId.toInt())

        val wakeLock = acquireReceiverWakeLock(context)
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = TaskDatabase.getDatabase(context)
                val task = db.taskDao().getTaskById(taskId)
                if (task != null) {
                    val repository = TaskRepository(context, db.taskDao())
                    repository.toggleTaskCompletion(task)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error completing task $taskId", e)
            } finally {
                releaseReceiverWakeLock(wakeLock)
                pendingResult?.finish()
            }
        }
    }

    private fun handleRoutineReminderAsync(
        context: Context,
        routineId: Long,
        fallbackTitle: String,
        fallbackDesc: String,
        fallbackTimeStr: String
    ) {
        val wakeLock = acquireReceiverWakeLock(context)
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = TaskDatabase.getDatabase(context)
                val routine = db.routineDao().getRoutineById(routineId)
                if (routine != null && !routine.isEnabled) {
                    return@launch
                }
                val title = routine?.title ?: fallbackTitle
                val desc = routine?.description ?: fallbackDesc
                val timeStr = routine?.scheduledTime ?: fallbackTimeStr
                RoutineReminderManager.showRoutineNotification(context, routineId, title, desc, timeStr)
            } catch (e: Exception) {
                Log.e(TAG, "Error handling routine reminder $routineId", e)
                RoutineReminderManager.showRoutineNotification(
                    context,
                    routineId,
                    fallbackTitle,
                    fallbackDesc,
                    fallbackTimeStr
                )
            } finally {
                releaseReceiverWakeLock(wakeLock)
                pendingResult?.finish()
            }
        }
    }

    private fun handleHabitReminderAsync(
        context: Context,
        habitId: Long,
        fallbackTitle: String,
        fallbackDesc: String,
        fallbackTimeStr: String
    ) {
        val wakeLock = acquireReceiverWakeLock(context)
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = TaskDatabase.getDatabase(context)
                val habit = db.habitDao().getHabitById(habitId)
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
                    .format(Date())
                if (habit != null && habit.isCompletedOn(todayStr)) {
                    // Already completed today — skip ringing today, reschedule tomorrow cleanly
                    RoutineReminderManager.rescheduleNextDayOccurrence(
                        context = context,
                        requestCode = 50000 + habitId.toInt(),
                        action = RoutineReminderManager.ACTION_HABIT_REMINDER,
                        itemId = habitId,
                        rawTitle = habit.title,
                        rawDesc = "${habit.category} • ${habit.targetDaysPerWeek} days/week",
                        timeStr = habit.reminderTime ?: fallbackTimeStr
                    )
                    return@launch
                }
                val title = habit?.title ?: fallbackTitle
                val desc = if (habit != null) {
                    "${habit.category} • ${habit.targetDaysPerWeek} days/week"
                } else {
                    fallbackDesc
                }
                val timeStr = habit?.reminderTime ?: fallbackTimeStr
                RoutineReminderManager.showHabitNotification(context, habitId, title, desc, timeStr)
            } catch (e: Exception) {
                Log.e(TAG, "Error handling habit reminder $habitId", e)
                RoutineReminderManager.showHabitNotification(
                    context,
                    habitId,
                    fallbackTitle,
                    fallbackDesc,
                    fallbackTimeStr
                )
            } finally {
                releaseReceiverWakeLock(wakeLock)
                pendingResult?.finish()
            }
        }
    }

    private fun dismissTaskNotification(context: Context, taskId: Long, extraNotifId: Int = -1) {
        AlarmSoundManager.stopAlarm(context)
        sendCloseAlarmScreenBroadcast(context, taskId)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (extraNotifId >= 0) {
            notificationManager.cancel(extraNotifId)
        }
        if (taskId == -1L) return

        notificationManager.cancel(taskId.toInt())

        val wakeLock = acquireReceiverWakeLock(context)
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = TaskDatabase.getDatabase(context)
                val task = db.taskDao().getTaskById(taskId)
                if (task != null && !task.isReminderDismissed) {
                    val repository = TaskRepository(context, db.taskDao())
                    repository.dismissReminder(task)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error dismissing task $taskId", e)
            } finally {
                releaseReceiverWakeLock(wakeLock)
                pendingResult?.finish()
            }
        }
    }
}
