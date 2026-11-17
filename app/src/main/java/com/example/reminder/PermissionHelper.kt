package com.example.reminder

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat

enum class PermissionPromptType(
    val title: String,
    val message: String,
    val settingsButtonLabel: String
) {
    MICROPHONE(
        title = "Microphone Permission Needed",
        message = "Microphone permission was denied. To record Audio Voice Notes for your tasks, please allow Microphone access in App Settings.",
        settingsButtonLabel = "Open Settings"
    ),
    EXACT_ALARM(
        title = "Alarms & Reminders Permission Needed",
        message = "Exact Alarm permission is turned off on this device. Without it, scheduled task, routine, and habit alarms may be delayed. Please allow 'Alarms & reminders' in Settings.",
        settingsButtonLabel = "Open Alarm Settings"
    ),
    NOTIFICATIONS(
        title = "Notification Permission Needed",
        message = "Notification permission was denied. Enable notifications in Settings so Plan Karo can deliver your daily reminders and alarms on time.",
        settingsButtonLabel = "Open Settings"
    )
}

object PermissionHelper {
    private const val TAG = "PermissionHelper"

    fun hasRecordAudioPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasNotificationPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    @androidx.annotation.VisibleForTesting
    internal var exactAlarmPermissionOverrideForTest: Boolean? = null

    fun canScheduleExactAlarms(context: Context): Boolean {
        exactAlarmPermissionOverrideForTest?.let { return it }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return true
        return try {
            alarmManager.canScheduleExactAlarms()
        } catch (e: Exception) {
            Log.w(TAG, "Unable to query canScheduleExactAlarms", e)
            true
        }
    }

    fun openAppDetailsSettingsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun openExactAlarmSettingsIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            openAppDetailsSettingsIntent(context)
        }
    }

    fun openNotificationSettingsIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            openAppDetailsSettingsIntent(context)
        }
    }

    fun getSettingsIntentForType(context: Context, type: PermissionPromptType): Intent {
        return when (type) {
            PermissionPromptType.MICROPHONE -> openAppDetailsSettingsIntent(context)
            PermissionPromptType.EXACT_ALARM -> openExactAlarmSettingsIntent(context)
            PermissionPromptType.NOTIFICATIONS -> openNotificationSettingsIntent(context)
        }
    }

    fun launchSettingsForType(context: Context, type: PermissionPromptType): Boolean {
        val primary = getSettingsIntentForType(context, type)
        return try {
            context.startActivity(primary)
            true
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "Primary settings intent not found for $type, falling back to app details", e)
            try {
                context.startActivity(openAppDetailsSettingsIntent(context))
                true
            } catch (ex: Exception) {
                Log.e(TAG, "Failed to open app details settings", ex)
                false
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to open settings for $type, trying app details", e)
            try {
                context.startActivity(openAppDetailsSettingsIntent(context))
                true
            } catch (ex: Exception) {
                Log.e(TAG, "Failed to open fallback app details settings", ex)
                false
            }
        }
    }

    fun canUseFullScreenIntent(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return true
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return true
        return try {
            nm.canUseFullScreenIntent()
        } catch (e: Exception) {
            Log.w(TAG, "Unable to query canUseFullScreenIntent", e)
            true
        }
    }

    fun launchFullScreenIntentSettings(context: Context): Boolean {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            openNotificationSettingsIntent(context)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.w(TAG, "Unable to open full screen intent settings, falling back to app details", e)
            try {
                context.startActivity(openAppDetailsSettingsIntent(context))
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    fun firstMissingAlarmPermission(
        context: Context,
        @Suppress("UNUSED_PARAMETER") isAlarmSoundEnabled: Boolean = false
    ): PermissionPromptType? {
        if (!hasNotificationPermission(context)) return PermissionPromptType.NOTIFICATIONS
        if (!canScheduleExactAlarms(context)) return PermissionPromptType.EXACT_ALARM
        return null
    }
}
