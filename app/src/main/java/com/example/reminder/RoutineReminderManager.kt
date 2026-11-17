package com.example.reminder

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.HabitItem
import com.example.data.model.RoutineItem
import com.example.receiver.TaskReminderReceiver
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object RoutineReminderManager {
    private const val TAG = "RoutineReminderManager"

    const val ACTION_ROUTINE_REMINDER = "com.example.plankaro.ACTION_ROUTINE_REMINDER"
    const val ACTION_HABIT_REMINDER = "com.example.plankaro.ACTION_HABIT_REMINDER"

    const val EXTRA_ITEM_ID = "extra_routine_habit_id"
    const val EXTRA_ITEM_TITLE = "extra_routine_habit_title"
    const val EXTRA_ITEM_DESC = "extra_routine_habit_desc"
    const val EXTRA_ITEM_TIME = "extra_routine_habit_time"
    const val EXTRA_NOTIFICATION_ID = "extra_routine_habit_notif_id"

    private const val ROUTINE_REQUEST_CODE_OFFSET = 30000
    private const val HABIT_REQUEST_CODE_OFFSET = 50000

    fun parseTimeHourMinute(timeStr: String?): Pair<Int, Int>? {
        if (timeStr.isNullOrBlank()) return null
        val trimmed = timeStr.trim()
        val formats = listOf("hh:mm a", "h:mm a", "HH:mm")
        for (pattern in formats) {
            for (locale in listOf(Locale.ENGLISH, Locale.getDefault())) {
                try {
                    val sdf = SimpleDateFormat(pattern, locale)
                    sdf.isLenient = false
                    val parsed = sdf.parse(trimmed)
                    if (parsed != null) {
                        val cal = Calendar.getInstance().apply { time = parsed }
                        return Pair(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
                    }
                } catch (_: Exception) {
                }
            }
        }
        return null
    }

    fun computeNextTriggerMillis(
        hourOfDay: Int,
        minute: Int,
        nowMillis: Long = System.currentTimeMillis()
    ): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= nowMillis) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return cal.timeInMillis
    }

    fun scheduleRoutine(context: Context, routine: RoutineItem) {
        if (!routine.isEnabled || routine.id <= 0L) {
            cancelRoutine(context, routine.id)
            return
        }
        val (hour, minute) = parseTimeHourMinute(routine.scheduledTime) ?: return
        val triggerAt = computeNextTriggerMillis(hour, minute)
        val requestCode = ROUTINE_REQUEST_CODE_OFFSET + routine.id.toInt()

        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            action = ACTION_ROUTINE_REMINDER
            putExtra(EXTRA_ITEM_ID, routine.id)
            putExtra(EXTRA_ITEM_TITLE, routine.title)
            putExtra(EXTRA_ITEM_DESC, routine.description)
            putExtra(EXTRA_ITEM_TIME, routine.scheduledTime)
        }
        scheduleExactDailyAlarm(context, requestCode, intent, triggerAt, "Routine: ${routine.title}")
    }

    fun cancelRoutine(context: Context, routineId: Long) {
        if (routineId <= 0L) return
        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            action = ACTION_ROUTINE_REMINDER
        }
        cancelAlarm(context, ROUTINE_REQUEST_CODE_OFFSET + routineId.toInt(), intent)
    }

    fun scheduleHabit(context: Context, habit: HabitItem) {
        val reminderTime = habit.reminderTime
        if (reminderTime.isNullOrBlank() || habit.id <= 0L) {
            cancelHabit(context, habit.id)
            return
        }
        val (hour, minute) = parseTimeHourMinute(reminderTime) ?: return
        val triggerAt = computeNextTriggerMillis(hour, minute)
        val requestCode = HABIT_REQUEST_CODE_OFFSET + habit.id.toInt()

        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            action = ACTION_HABIT_REMINDER
            putExtra(EXTRA_ITEM_ID, habit.id)
            putExtra(EXTRA_ITEM_TITLE, habit.title)
            putExtra(EXTRA_ITEM_DESC, "${habit.category} • ${habit.targetDaysPerWeek} days/week")
            putExtra(EXTRA_ITEM_TIME, reminderTime)
        }
        scheduleExactDailyAlarm(context, requestCode, intent, triggerAt, "Habit: ${habit.title}")
    }

    fun cancelHabit(context: Context, habitId: Long) {
        if (habitId <= 0L) return
        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            action = ACTION_HABIT_REMINDER
        }
        cancelAlarm(context, HABIT_REQUEST_CODE_OFFSET + habitId.toInt(), intent)
    }

    private fun scheduleExactDailyAlarm(
        context: Context,
        requestCode: Int,
        intent: Intent,
        triggerAtMillis: Long,
        label: String
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags)

        val showIntent = Intent(context, MainActivity::class.java).apply {
            this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val showPendingIntent = PendingIntent.getActivity(context, requestCode + 100000, showIntent, flags)

        if (PermissionHelper.canScheduleExactAlarms(context)) {
            // setAlarmClock() bypasses Doze throttling so daily Habits & Routines fire on time
            // even under default Android battery optimization.
            try {
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(triggerAtMillis, showPendingIntent),
                    pendingIntent
                )
                Log.d(TAG, "Scheduled Doze-exempt alarm via setAlarmClock for $label at $triggerAtMillis")
                return
            } catch (e: Exception) {
                Log.w(TAG, "setAlarmClock failed for $label, trying setExactAndAllowWhileIdle: ${e.message}")
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
                Log.d(TAG, "Scheduled exact alarm via setExactAndAllowWhileIdle for $label at $triggerAtMillis")
                return
            } catch (ex: Exception) {
                Log.w(TAG, "Exact alarm failed for $label: ${ex.message}")
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    private fun cancelAlarm(context: Context, requestCode: Int, intent: Intent) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun showRoutineNotification(
        context: Context,
        routineId: Long,
        title: String,
        description: String,
        scheduledTime: String
    ) {
        showNotificationAndReschedule(
            context = context,
            notifId = ROUTINE_REQUEST_CODE_OFFSET + routineId.toInt(),
            notifTitle = "🔁 Routine: $title",
            notifBody = if (description.isNotBlank()) "$description • Scheduled at $scheduledTime" else "Time for your daily routine ($scheduledTime)",
            timeStr = scheduledTime,
            action = ACTION_ROUTINE_REMINDER,
            itemId = routineId,
            rawTitle = title,
            rawDesc = description
        )
    }

    fun showHabitNotification(
        context: Context,
        habitId: Long,
        title: String,
        description: String,
        reminderTime: String
    ) {
        showNotificationAndReschedule(
            context = context,
            notifId = HABIT_REQUEST_CODE_OFFSET + habitId.toInt(),
            notifTitle = "🔥 Daily Habit: $title",
            notifBody = if (description.isNotBlank()) "$description • Reminder at $reminderTime" else "Stay consistent! Time for your daily habit ($reminderTime)",
            timeStr = reminderTime,
            action = ACTION_HABIT_REMINDER,
            itemId = habitId,
            rawTitle = title,
            rawDesc = description
        )
    }

    private fun showNotificationAndReschedule(
        context: Context,
        notifId: Int,
        notifTitle: String,
        notifBody: String,
        timeStr: String,
        action: String,
        itemId: Long,
        rawTitle: String,
        rawDesc: String
    ) {
        ReminderManager.createNotificationChannel(context)

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notifId,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(context, TaskReminderReceiver::class.java).apply {
            this.action = ReminderManager.ACTION_DISMISS
            putExtra(ReminderManager.EXTRA_TASK_ID, -1L)
            putExtra(EXTRA_NOTIFICATION_ID, notifId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            2_500_000 + notifId,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Daily Habits & Routines always deliver a gentle notification sound (never loud Task Alarm)
        val notification = NotificationCompat.Builder(context, ReminderManager.CHANNEL_ID_NOTIFICATION_BAR)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(notifTitle)
            .setContentText(notifBody)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notifBody))
            .setSound(ReminderManager.getNotificationSoundUri(context))
            .setVibrate(longArrayOf(0, 200, 100, 200))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDeleteIntent(dismissPendingIntent)
            .build()

        if (!PermissionHelper.hasNotificationPermission(context) ||
            !NotificationManagerCompat.from(context).areNotificationsEnabled()
        ) {
            Log.w(TAG, "Notifications blocked or missing permission; attempting safe notify for $notifId")
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        try {
            notificationManager.notify(notifId, notification)
        } catch (se: SecurityException) {
            Log.w(TAG, "Missing notification permission when posting reminder $notifId", se)
        }

        // Reschedule for the next day so daily routines and habits keep firing automatically
        rescheduleNextDayOccurrence(
            context = context,
            requestCode = notifId,
            action = action,
            itemId = itemId,
            rawTitle = rawTitle,
            rawDesc = rawDesc,
            timeStr = timeStr
        )
    }

    fun rescheduleNextDayOccurrence(
        context: Context,
        requestCode: Int,
        action: String,
        itemId: Long,
        rawTitle: String,
        rawDesc: String,
        timeStr: String
    ) {
        val (hour, minute) = parseTimeHourMinute(timeStr) ?: return
        val nextTrigger = computeNextTriggerMillis(hour, minute, System.currentTimeMillis() + 60_000L)
        val rescheduleIntent = Intent(context, TaskReminderReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_ITEM_ID, itemId)
            putExtra(EXTRA_ITEM_TITLE, rawTitle)
            putExtra(EXTRA_ITEM_DESC, rawDesc)
            putExtra(EXTRA_ITEM_TIME, timeStr)
        }
        scheduleExactDailyAlarm(context, requestCode, rescheduleIntent, nextTrigger, rawTitle)
    }
}
