package com.example.reminder

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.TaskDatabase
import com.example.data.model.TaskItem
import com.example.data.repository.TaskRepository
import com.example.receiver.TaskReminderReceiver
import com.example.ui.alarm.TaskAlarmActivity
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object ReminderManager {
    const val CHANNEL_ID_ALARM = "task_loud_alarm_v5_channel"
    const val CHANNEL_NAME_ALARM = "Plan Karo Task Alarms"

    const val CHANNEL_ID_NOTIFICATION_BAR = "task_notification_bar_v6_channel"
    const val CHANNEL_NAME_NOTIFICATION_BAR = "Plan Karo Task Notifications"

    // Backward-compatible alias
    const val CHANNEL_ID = CHANNEL_ID_ALARM
    const val CHANNEL_NAME = CHANNEL_NAME_ALARM

    const val ACTION_REMINDER = "com.example.plankaro.ACTION_REMINDER"
    const val ACTION_SNOOZE = "com.example.plankaro.ACTION_SNOOZE"
    const val ACTION_COMPLETE = "com.example.plankaro.ACTION_COMPLETE"
    const val ACTION_DISMISS = "com.example.plankaro.ACTION_DISMISS"
    const val ACTION_MIDNIGHT_ROLLOVER = "com.example.plankaro.ACTION_MIDNIGHT_ROLLOVER"

    const val EXTRA_TASK_ID = "extra_task_id"
    const val EXTRA_TASK_TITLE = "extra_task_title"
    const val EXTRA_TASK_NOTES = "extra_task_notes"
    const val EXTRA_TASK_PRIORITY = "extra_task_priority"
    const val EXTRA_SNOOZE_MINUTES = "extra_snooze_minutes"

    const val REQUEST_CODE_MIDNIGHT_ROLLOVER = 90001

    fun getAlarmSoundUri(context: Context): Uri {
        return Uri.parse(
            "${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/${R.raw.plankaro_alarm_ring}"
        )
    }

    fun getNotificationSoundUri(context: Context): Uri {
        return Uri.parse(
            "${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/${R.raw.plankaro_notify_chime}"
        )
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Remove legacy channels so cached OS importance/sound settings do not mute upgraded 2-chime sound
            listOf(
                "task_loud_alarm_v3_channel",
                "task_loud_alarm_v4_channel",
                "task_notification_bar_v3_channel",
                "task_notification_bar_v4_channel",
                "task_notification_bar_v5_channel"
            ).forEach { legacyId ->
                runCatching { manager.deleteNotificationChannel(legacyId) }
            }

            // 1. Loud Alarm Channel with unique Plan Karo alarm ringtone
            val alarmSound = getAlarmSoundUri(context)
            val alarmAudioAttributes = android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val alarmChannel = NotificationChannel(
                CHANNEL_ID_ALARM,
                CHANNEL_NAME_ALARM,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Loud ringtone, vibration, and lock-screen alarms for scheduled tasks"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 600, 300, 600, 300, 800)
                setSound(alarmSound, alarmAudioAttributes)
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
            }
            manager.createNotificationChannel(alarmChannel)

            // 2. Notification Bar Channel (IMPORTANCE_HIGH) with restored Plan Karo chime ringtone
            val notifSound = getNotificationSoundUri(context)
            val notifAudioAttributes = android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val barChannel = NotificationChannel(
                CHANNEL_ID_NOTIFICATION_BAR,
                CHANNEL_NAME_NOTIFICATION_BAR,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Heads-up notification chime reminders for tasks, daily habits, and routines"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 200, 100, 200)
                setSound(notifSound, notifAudioAttributes)
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setBypassDnd(false)
            }
            manager.createNotificationChannel(barChannel)
        }
    }

    fun buildTaskReminderNotification(
        context: Context,
        taskId: Long,
        title: String,
        notes: String,
        priority: String,
        isAlarmActive: Boolean,
        audibleAlarmNotification: Boolean = false
    ): Notification {
        createNotificationChannel(context)

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_TASK_ID, taskId)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            taskId.toInt(),
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmScreenIntent = Intent(context, TaskAlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_TASK_TITLE, title)
            putExtra(EXTRA_TASK_NOTES, notes)
            putExtra(EXTRA_TASK_PRIORITY, priority)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            TaskAlarmActivity.FULLSCREEN_REQUEST_CODE_OFFSET + taskId.toInt(),
            alarmScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, TaskReminderReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_SNOOZE_MINUTES, 10)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            TaskReminderReceiver.SNOOZE_REQUEST_CODE_OFFSET + taskId.toInt(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val completeIntent = Intent(context, TaskReminderReceiver::class.java).apply {
            action = ACTION_COMPLETE
            putExtra(EXTRA_TASK_ID, taskId)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            TaskReminderReceiver.COMPLETE_REQUEST_CODE_OFFSET + taskId.toInt(),
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(context, TaskReminderReceiver::class.java).apply {
            action = ACTION_DISMISS
            putExtra(EXTRA_TASK_ID, taskId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            TaskReminderReceiver.DISMISS_REQUEST_CODE_OFFSET + taskId.toInt(),
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = if (notes.isNotBlank()) notes else "Priority: $priority"

        return if (isAlarmActive) {
            NotificationCompat.Builder(context, CHANNEL_ID_ALARM)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("⏰ ALARM: $title")
                .setContentText(contentText)
                .setStyle(NotificationCompat.BigTextStyle().bigText("$contentText\nPriority: $priority"))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .apply {
                    if (audibleAlarmNotification) {
                        setSilent(false)
                        setSound(getAlarmSoundUri(context))
                        setOnlyAlertOnce(false)
                    } else {
                        setSilent(true)
                        setOnlyAlertOnce(true)
                    }
                }
                .setVibrate(longArrayOf(0, 600, 300, 600, 300, 800))
                .setAutoCancel(true)
                .setOngoing(false)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setContentIntent(fullScreenPendingIntent)
                .setDeleteIntent(dismissPendingIntent)
                .addAction(R.drawable.ic_notification, "Snooze 10m", snoozePendingIntent)
                .addAction(android.R.drawable.checkbox_on_background, "Mark Done", completePendingIntent)
                .build()
                .apply {
                    flags = flags or Notification.FLAG_INSISTENT
                }
        } else {
            val notifSound = getNotificationSoundUri(context)

            NotificationCompat.Builder(context, CHANNEL_ID_NOTIFICATION_BAR)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("📋 Task: $title")
                .setContentText(contentText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
                .setSound(notifSound)
                .setVibrate(longArrayOf(0, 200, 100, 200))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setContentIntent(contentPendingIntent)
                .setDeleteIntent(dismissPendingIntent)
                .addAction(R.drawable.ic_notification, "Snooze 10m", snoozePendingIntent)
                .addAction(android.R.drawable.checkbox_on_background, "Mark Done", completePendingIntent)
                .build()
        }
    }

    fun scheduleReminder(
        context: Context,
        task: TaskItem,
        allowRecentPastGrace: Boolean = false
    ) {
        val reminderTime = task.reminderTimeMillis ?: return
        val now = System.currentTimeMillis()
        val pastCutoffMs = if (allowRecentPastGrace) 90_000L else 5_000L
        if (reminderTime < now - pastCutoffMs) {
            return
        }
        val triggerAtMillis = if (allowRecentPastGrace && reminderTime <= now) {
            now + 500L
        } else {
            reminderTime
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            action = ACTION_REMINDER
            putExtra(EXTRA_TASK_ID, task.id)
            putExtra(EXTRA_TASK_TITLE, task.title)
            putExtra(EXTRA_TASK_NOTES, task.notes)
            putExtra(EXTRA_TASK_PRIORITY, task.priority.name)
        }

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.toInt(),
            intent,
            flags
        )

        val isAlarmMode = AlarmPreferenceManager.isAlarmEnabled(context)
        val showIntent = if (isAlarmMode) {
            Intent(context, TaskAlarmActivity::class.java).apply {
                this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_TASK_ID, task.id)
                putExtra(EXTRA_TASK_TITLE, task.title)
                putExtra(EXTRA_TASK_NOTES, task.notes)
                putExtra(EXTRA_TASK_PRIORITY, task.priority.name)
            }
        } else {
            Intent(context, MainActivity::class.java).apply {
                this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_TASK_ID, task.id)
            }
        }
        val showPendingIntent = PendingIntent.getActivity(
            context,
            task.id.toInt() + 100000,
            showIntent,
            flags
        )

        if (PermissionHelper.canScheduleExactAlarms(context)) {
            // setAlarmClock() explicitly bypasses Doze & App Standby throttling on all Android versions
            // without requiring the user to disable battery optimization.
            try {
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(triggerAtMillis, showPendingIntent),
                    pendingIntent
                )
                Log.d("ReminderManager", "setAlarmClock scheduled for ${task.title} (alarm=$isAlarmMode) at $triggerAtMillis")
                return
            } catch (e: Exception) {
                Log.w("ReminderManager", "setAlarmClock failed, trying setExactAndAllowWhileIdle: ${e.message}")
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                    Log.d("ReminderManager", "setExactAndAllowWhileIdle scheduled for ${task.title} at $triggerAtMillis")
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                    Log.d("ReminderManager", "setExact scheduled for ${task.title} at $triggerAtMillis")
                }
                return
            } catch (e: Exception) {
                Log.w("ReminderManager", "Exact alarm failed: ${e.message}")
            }
        } else {
            Log.w("ReminderManager", "Exact alarm permission missing; scheduling inexact alarm for ${task.title}")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } else {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    fun cancelReminder(context: Context, taskId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            action = ACTION_REMINDER
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            flags
        )
        alarmManager.cancel(pendingIntent)
        runCatching {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.cancel(taskId.toInt())
        }
    }

    /**
     * Schedules an automatic 00:01 AM rollover wake-up every night so Daily, Weekly, and Monthly
     * tasks advance onto their scheduled dates and arm their alarms automatically in the background.
     */
    fun scheduleMidnightRollover(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            action = ACTION_MIDNIGHT_ROLLOVER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_MIDNIGHT_ROLLOVER,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 1)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        try {
            if (PermissionHelper.canScheduleExactAlarms(context) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    cal.timeInMillis,
                    pendingIntent
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    cal.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    cal.timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            Log.w("ReminderManager", "Unable to schedule midnight rollover alarm", e)
        }
    }

    suspend fun rescheduleAllAlarms(context: Context) {
        val appContext = context.applicationContext
        val db = TaskDatabase.getDatabase(appContext)
        val repository = TaskRepository(appContext, db.taskDao())

        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date())
        repository.syncRepeatingTasksForToday(todayStr)

        val now = System.currentTimeMillis()
        val active = db.taskDao().getActiveReminders()
        active.filter { (it.reminderTimeMillis ?: 0L) > now - 5000L }.forEach { task ->
            scheduleReminder(appContext, task)
        }

        val routines = db.routineDao().getEnabledRoutines().first()
        routines.forEach { routine ->
            RoutineReminderManager.scheduleRoutine(appContext, routine)
        }

        val habits = db.habitDao().getAllHabits().first()
        habits.filter { !it.reminderTime.isNullOrBlank() }.forEach { habit ->
            RoutineReminderManager.scheduleHabit(appContext, habit)
        }

        DailyReminderManager.scheduleAllDailyReminders(appContext)
        scheduleMidnightRollover(appContext)
    }
}
