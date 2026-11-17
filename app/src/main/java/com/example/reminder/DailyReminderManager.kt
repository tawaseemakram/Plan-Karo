package com.example.reminder

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.receiver.TaskReminderReceiver
import java.util.Calendar

object DailyReminderManager {
    private const val TAG = "DailyReminderManager"

    const val CHANNEL_ID = "daily_engagement_reminders_channel_v4"
    const val CHANNEL_NAME = "Daily Plan Karo Reminders"

    const val ACTION_DAILY_REMINDER = "com.example.plankaro.ACTION_DAILY_REMINDER"
    const val EXTRA_PERIOD = "extra_daily_period"

    const val PERIOD_MORNING = "MORNING"
    const val PERIOD_AFTERNOON = "AFTERNOON"
    const val PERIOD_EVENING = "EVENING"

    const val REQUEST_CODE_MORNING = 8001
    const val REQUEST_CODE_AFTERNOON = 8002
    const val REQUEST_CODE_EVENING = 8003

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            runCatching { manager.deleteNotificationChannel("daily_engagement_reminders_channel_v2") }
            runCatching { manager.deleteNotificationChannel("daily_engagement_reminders_channel_v3") }
            val notifAudioAttributes = android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Daily morning, afternoon, and evening reminders to check your tasks and goals"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 200, 100, 200)
                setSound(ReminderManager.getNotificationSoundUri(context), notifAudioAttributes)
                setShowBadge(true)
            }
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Schedules the 3 daily engagement reminders:
     * 1. Morning: 08:00 AM
     * 2. Afternoon: 02:00 PM
     * 3. Evening: 08:00 PM
     */
    fun scheduleAllDailyReminders(context: Context) {
        createChannel(context)
        schedulePeriodAlarm(context, PERIOD_MORNING, 8, 0, REQUEST_CODE_MORNING)
        schedulePeriodAlarm(context, PERIOD_AFTERNOON, 14, 0, REQUEST_CODE_AFTERNOON)
        schedulePeriodAlarm(context, PERIOD_EVENING, 20, 0, REQUEST_CODE_EVENING)
        Log.d(TAG, "All 3 daily reminders scheduled (Morning 8:00 AM, Afternoon 2:00 PM, Evening 8:00 PM)")
    }

    private fun schedulePeriodAlarm(
        context: Context,
        period: String,
        targetHour: Int,
        targetMinute: Int,
        requestCode: Int
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, TaskReminderReceiver::class.java).apply {
            action = ACTION_DAILY_REMINDER
            putExtra(EXTRA_PERIOD, period)
        }

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            flags
        )
        val showIntent = Intent(context, MainActivity::class.java).apply {
            this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val showPendingIntent = PendingIntent.getActivity(
            context,
            requestCode + 100000,
            showIntent,
            flags
        )

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // If time already passed today, schedule for tomorrow
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        if (PermissionHelper.canScheduleExactAlarms(context)) {
            try {
                alarmManager.setAlarmClock(
                    AlarmManager.AlarmClockInfo(cal.timeInMillis, showPendingIntent),
                    pendingIntent
                )
                return
            } catch (e: Exception) {
                Log.w(TAG, "setAlarmClock failed for $period, trying setExactAndAllowWhileIdle: ${e.message}")
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        cal.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        cal.timeInMillis,
                        pendingIntent
                    )
                }
                return
            } catch (e: Exception) {
                Log.w(TAG, "Exact daily reminder failed for $period, falling back to setAndAllowWhileIdle: ${e.message}")
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
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
        } catch (ex: Exception) {
            Log.e(TAG, "Failed to schedule daily reminder for $period", ex)
        }
    }

    fun getNotificationContent(period: String): Pair<String, String> {
        return when (period) {
            PERIOD_MORNING -> Pair(
                "🌅 Morning Plan! | Plan Karo",
                "Start your day with focus. Check today's top tasks and daily routines!"
            )
            PERIOD_AFTERNOON -> Pair(
                "☀️ Afternoon Check-in | Plan Karo",
                "Half of the day has passed! Take a quick 2-minute break and check your task progress."
            )
            PERIOD_EVENING -> Pair(
                "🌙 Evening Review & Plan Tomorrow | Plan Karo",
                "Review today's achievements and set tomorrow's top 3 priorities for a peaceful night!"
            )
            else -> Pair(
                "🎯 Plan Karo Reminder",
                "Check your daily tasks and goals to stay organized!"
            )
        }
    }

    fun showDailyNotification(context: Context, period: String) {
        createChannel(context)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val (title, body) = getNotificationContent(period)
        val notifId = when (period) {
            PERIOD_MORNING -> REQUEST_CODE_MORNING
            PERIOD_AFTERNOON -> REQUEST_CODE_AFTERNOON
            PERIOD_EVENING -> REQUEST_CODE_EVENING
            else -> REQUEST_CODE_MORNING
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notifId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setSound(ReminderManager.getNotificationSoundUri(context))
            .setVibrate(longArrayOf(0, 200, 100, 200))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        try {
            notificationManager.notify(notifId, notification)
        } catch (se: SecurityException) {
            Log.w(TAG, "Missing notification permission when posting daily reminder", se)
        }

        // Reschedule for next day to guarantee it continues
        when (period) {
            PERIOD_MORNING -> schedulePeriodAlarm(context, PERIOD_MORNING, 8, 0, REQUEST_CODE_MORNING)
            PERIOD_AFTERNOON -> schedulePeriodAlarm(context, PERIOD_AFTERNOON, 14, 0, REQUEST_CODE_AFTERNOON)
            PERIOD_EVENING -> schedulePeriodAlarm(context, PERIOD_EVENING, 20, 0, REQUEST_CODE_EVENING)
        }
    }
}
