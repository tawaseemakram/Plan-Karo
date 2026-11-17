package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.reminder.DailyReminderManager
import com.example.reminder.ReminderManager
import com.example.ui.MainAppScreen
import com.example.ui.TaskViewModel
import com.example.ui.theme.PlanKaroTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val taskViewModel: TaskViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Notification channel for reminders & midnight repeating rollover
        ReminderManager.createNotificationChannel(this)
        ReminderManager.scheduleMidnightRollover(this)

        // Schedule 3x daily app engagement reminders (Morning 08:30 AM, Afternoon 02:00 PM, Evening 08:30 PM)
        DailyReminderManager.scheduleAllDailyReminders(this)

        // Initialize Google AdMob SDK with UMP GDPR consent check
        com.example.ads.AdManager.initializeWithConsent(this)

        handleNotificationIntent(intent)

        setContent {
            PlanKaroTheme {
                val viewModel = taskViewModel

                // Request Notification Permission on Android 13+, then check Exact Alarm once
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (!isGranted) {
                        viewModel.showPermissionSettingsPrompt(
                            com.example.reminder.PermissionPromptType.NOTIFICATIONS
                        )
                    } else if (!com.example.reminder.PermissionHelper.canScheduleExactAlarms(this@MainActivity)) {
                        val prefs = getSharedPreferences("plankaro_permission_prefs", MODE_PRIVATE)
                        if (!prefs.getBoolean("prompted_exact_alarm_once_v1", false)) {
                            prefs.edit().putBoolean("prompted_exact_alarm_once_v1", true).apply()
                            viewModel.showPermissionSettingsPrompt(
                                com.example.reminder.PermissionPromptType.EXACT_ALARM
                            )
                        }
                    }
                }

                LaunchedEffect(Unit) {
                    val hasNotifPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                    } else {
                        true
                    }
                    if (!hasNotifPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else if (!com.example.reminder.PermissionHelper.canScheduleExactAlarms(this@MainActivity)) {
                        val prefs = getSharedPreferences("plankaro_permission_prefs", MODE_PRIVATE)
                        if (!prefs.getBoolean("prompted_exact_alarm_once_v1", false)) {
                            prefs.edit().putBoolean("prompted_exact_alarm_once_v1", true).apply()
                            viewModel.showPermissionSettingsPrompt(
                                com.example.reminder.PermissionPromptType.EXACT_ALARM
                            )
                        }
                    }
                }

                MainAppScreen(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        val taskId = intent?.getLongExtra(ReminderManager.EXTRA_TASK_ID, -1L) ?: -1L
        if (taskId > 0L) {
            taskViewModel.handleNotificationTap(taskId)
            intent?.removeExtra(ReminderManager.EXTRA_TASK_ID)
        }
    }

    override fun onResume() {
        super.onResume()
        val appContext = applicationContext
        lifecycleScope.launch(Dispatchers.IO) {
            runCatching { ReminderManager.rescheduleAllAlarms(appContext) }
        }
    }
}
