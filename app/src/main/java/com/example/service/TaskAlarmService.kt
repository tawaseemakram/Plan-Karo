package com.example.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.reminder.AlarmPreferenceManager
import com.example.reminder.AlarmSoundManager
import com.example.reminder.ReminderManager

/**
 * Foreground Service that holds a bounded PARTIAL_WAKE_LOCK from BroadcastReceiver handoff
 * through alarm playback/vibration so scheduled task alarms ring reliably even when the
 * device screen is off, locked, or the app is in the background.
 */
class TaskAlarmService : Service() {

    companion object {
        private const val TAG = "TaskAlarmService"
        const val ACTION_START_ALARM = "com.example.plankaro.service.START_ALARM"
        const val ACTION_STOP_ALARM = "com.example.plankaro.service.STOP_ALARM"

        private const val DEFAULT_NOTIFICATION_ID = 70001
        private const val WAKE_LOCK_TAG = "PlanKaro:TaskAlarmWakeLock"
        private const val WAKE_LOCK_TIMEOUT_MS = AlarmSoundManager.MAX_ALARM_DURATION_MS + 10_000L

        private val wakeLockLock = Any()
        @Volatile
        private var sharedWakeLock: PowerManager.WakeLock? = null

        fun acquireAlarmWakeLock(context: Context) {
            synchronized(wakeLockLock) {
                try {
                    val existing = sharedWakeLock
                    if (existing != null && existing.isHeld) {
                        existing.acquire(WAKE_LOCK_TIMEOUT_MS)
                        return
                    }
                    val pm = context.applicationContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
                        ?: return
                    sharedWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG).apply {
                        setReferenceCounted(false)
                        acquire(WAKE_LOCK_TIMEOUT_MS)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Unable to acquire alarm WakeLock", e)
                }
            }
        }

        fun releaseAlarmWakeLock() {
            synchronized(wakeLockLock) {
                try {
                    sharedWakeLock?.let { wl ->
                        if (wl.isHeld) {
                            wl.release()
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Unable to release alarm WakeLock", e)
                } finally {
                    sharedWakeLock = null
                }
            }
        }

        fun isWakeLockHeld(): Boolean {
            synchronized(wakeLockLock) {
                return sharedWakeLock?.isHeld == true
            }
        }

        fun startAlarmService(
            context: Context,
            taskId: Long,
            title: String,
            notes: String,
            priority: String
        ): Boolean {
            // Acquire bounded WakeLock immediately during receiver->service handoff so the CPU
            // stays awake on locked/Doze devices until startForeground() runs.
            acquireAlarmWakeLock(context)

            val intent = Intent(context, TaskAlarmService::class.java).apply {
                action = ACTION_START_ALARM
                putExtra(ReminderManager.EXTRA_TASK_ID, taskId)
                putExtra(ReminderManager.EXTRA_TASK_TITLE, title)
                putExtra(ReminderManager.EXTRA_TASK_NOTES, notes)
                putExtra(ReminderManager.EXTRA_TASK_PRIORITY, priority)
            }
            return try {
                ContextCompat.startForegroundService(context, intent)
                true
            } catch (e: Exception) {
                Log.w(TAG, "Unable to start TaskAlarmService from background: ${e.message}")
                if (!AlarmSoundManager.isPlaying()) {
                    releaseAlarmWakeLock()
                }
                false
            }
        }

        fun stopAlarmService(context: Context) {
            releaseAlarmWakeLock()
            try {
                val appContext = context.applicationContext
                appContext.stopService(Intent(appContext, TaskAlarmService::class.java))
            } catch (e: Exception) {
                Log.w(TAG, "Unable to stop TaskAlarmService: ${e.message}")
            }
        }
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val timeoutRunnable = Runnable {
        Log.d(TAG, "Alarm foreground service timeout reached, stopping self")
        stopAlarmAndSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_ALARM) {
            stopAlarmAndSelf()
            return START_NOT_STICKY
        }

        val taskId = intent?.getLongExtra(ReminderManager.EXTRA_TASK_ID, -1L) ?: -1L
        val title = intent?.getStringExtra(ReminderManager.EXTRA_TASK_TITLE) ?: "Task Reminder"
        val notes = intent?.getStringExtra(ReminderManager.EXTRA_TASK_NOTES) ?: ""
        val priority = intent?.getStringExtra(ReminderManager.EXTRA_TASK_PRIORITY) ?: "HIGH"
        val notificationId = if (taskId > 0L) taskId.toInt() else DEFAULT_NOTIFICATION_ID

        ReminderManager.createNotificationChannel(this)
        val notification = ReminderManager.buildTaskReminderNotification(
            context = this,
            taskId = if (taskId > 0L) taskId else DEFAULT_NOTIFICATION_ID.toLong(),
            title = title,
            notes = notes,
            priority = priority,
            isAlarmActive = true
        )

        try {
            val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            } else {
                0
            }
            ServiceCompat.startForeground(this, notificationId, notification, fgsType)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to promote TaskAlarmService to foreground; falling back to audible alarm notification", e)
            val fallbackNotification = ReminderManager.buildTaskReminderNotification(
                context = this,
                taskId = if (taskId > 0L) taskId else DEFAULT_NOTIFICATION_ID.toLong(),
                title = title,
                notes = notes,
                priority = priority,
                isAlarmActive = true,
                audibleAlarmNotification = true
            )
            runCatching {
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                nm.notify(notificationId, fallbackNotification)
            }
            stopAlarmAndSelf()
            return START_NOT_STICKY
        }

        if (!AlarmPreferenceManager.isAlarmEnabled(this)) {
            stopAlarmAndSelf()
            return START_NOT_STICKY
        }

        acquireAlarmWakeLock(this)
        AlarmSoundManager.startAlarm(this)

        mainHandler.removeCallbacks(timeoutRunnable)
        mainHandler.postDelayed(timeoutRunnable, AlarmSoundManager.MAX_ALARM_DURATION_MS)

        return START_NOT_STICKY
    }

    private fun stopAlarmAndSelf() {
        mainHandler.removeCallbacks(timeoutRunnable)
        AlarmSoundManager.stopLocalPlaybackOnly(this)
        releaseAlarmWakeLock()
        try {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_DETACH)
        } catch (_: Exception) {
        }
        stopSelf()
    }

    override fun onDestroy() {
        mainHandler.removeCallbacks(timeoutRunnable)
        AlarmSoundManager.stopLocalPlaybackOnly(this)
        releaseAlarmWakeLock()
        super.onDestroy()
    }
}
