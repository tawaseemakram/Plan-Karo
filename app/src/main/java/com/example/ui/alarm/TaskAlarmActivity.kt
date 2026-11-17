package com.example.ui.alarm

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.data.local.TaskDatabase
import com.example.data.repository.TaskRepository
import com.example.reminder.AlarmPreferenceManager
import com.example.reminder.AlarmSoundManager
import com.example.reminder.ReminderManager
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.PlanKaroTheme
import com.example.ui.theme.PriorityHigh
import com.example.ui.theme.TealDeep
import com.example.ui.theme.TealNeon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TaskAlarmActivity : ComponentActivity() {

    companion object {
        const val FULLSCREEN_REQUEST_CODE_OFFSET = 400_000
        const val ACTION_CLOSE_ALARM_SCREEN = "com.example.plankaro.ACTION_CLOSE_ALARM_SCREEN"
    }

    private var currentTaskId: Long = -1L

    private val closeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val targetId = intent?.getLongExtra(ReminderManager.EXTRA_TASK_ID, -1L) ?: -1L
            if (targetId == -1L || targetId == currentTaskId) {
                finishSafely()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureLockScreenFlags()

        ContextCompat.registerReceiver(
            this,
            closeReceiver,
            IntentFilter(ACTION_CLOSE_ALARM_SCREEN),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        renderAlarmIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        renderAlarmIntent(intent)
    }

    private fun configureLockScreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )
    }

    private fun renderAlarmIntent(intent: Intent?) {
        currentTaskId = intent?.getLongExtra(ReminderManager.EXTRA_TASK_ID, -1L) ?: -1L
        val title = intent?.getStringExtra(ReminderManager.EXTRA_TASK_TITLE) ?: "Task Alarm"
        val notes = intent?.getStringExtra(ReminderManager.EXTRA_TASK_NOTES) ?: ""
        val priority = intent?.getStringExtra(ReminderManager.EXTRA_TASK_PRIORITY) ?: "HIGH"

        if (AlarmPreferenceManager.isAlarmEnabled(this) && !AlarmSoundManager.isPlaying()) {
            lifecycleScope.launch {
                val stillActive = withContext(Dispatchers.IO) {
                    if (currentTaskId <= 0L) true
                    else {
                        val task = TaskDatabase.getDatabase(applicationContext).taskDao().getTaskById(currentTaskId)
                        task == null || (!task.isCompleted && !task.isReminderDismissed)
                    }
                }
                if (stillActive && AlarmPreferenceManager.isAlarmEnabled(this@TaskAlarmActivity)) {
                    AlarmSoundManager.startAlarm(this@TaskAlarmActivity)
                }
            }
        }

        setContent {
            PlanKaroTheme {
                LockScreenAlarmView(
                    title = title,
                    notes = notes,
                    priority = priority,
                    onMarkDone = { completeCurrentTaskAndFinish() },
                    onSnooze = { minutes -> snoozeCurrentTaskAndFinish(minutes) },
                    onDismiss = { dismissCurrentTaskAndFinish() }
                )
            }
        }
    }

    private fun cancelActiveNotification() {
        if (currentTaskId > 0L) {
            runCatching {
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.cancel(currentTaskId.toInt())
            }
        }
    }

    private fun completeCurrentTaskAndFinish() {
        AlarmSoundManager.stopAlarm(this)
        cancelActiveNotification()
        val taskId = currentTaskId
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                if (taskId > 0L) {
                    val db = TaskDatabase.getDatabase(applicationContext)
                    val task = db.taskDao().getTaskById(taskId)
                    if (task != null && !task.isCompleted) {
                        TaskRepository(applicationContext, db.taskDao()).toggleTaskCompletion(task)
                    }
                }
            }
            finishSafely()
        }
    }

    private fun snoozeCurrentTaskAndFinish(minutes: Int) {
        AlarmSoundManager.stopAlarm(this)
        cancelActiveNotification()
        val taskId = currentTaskId
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                if (taskId > 0L) {
                    val db = TaskDatabase.getDatabase(applicationContext)
                    val task = db.taskDao().getTaskById(taskId)
                    if (task != null) {
                        TaskRepository(applicationContext, db.taskDao()).snoozeTask(task, minutes)
                    }
                }
            }
            finishSafely()
        }
    }

    private fun dismissCurrentTaskAndFinish() {
        AlarmSoundManager.stopAlarm(this)
        cancelActiveNotification()
        val taskId = currentTaskId
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                if (taskId > 0L) {
                    val db = TaskDatabase.getDatabase(applicationContext)
                    val task = db.taskDao().getTaskById(taskId)
                    if (task != null && !task.isReminderDismissed) {
                        TaskRepository(applicationContext, db.taskDao()).dismissReminder(task)
                    }
                }
            }
            finishSafely()
        }
    }

    private fun finishSafely() {
        if (!isFinishing && !isDestroyed) {
            finish()
        }
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(closeReceiver) }
        super.onDestroy()
    }
}

@Composable
private fun LockScreenAlarmView(
    title: String,
    notes: String,
    priority: String,
    onMarkDone: () -> Unit,
    onSnooze: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "lock_alarm_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "lock_alarm_scale"
    )
    val timeText = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DarkBg, Color(0xFF141824), DarkBg)
                )
            )
            .safeDrawingPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .testTag("lock_screen_alarm_card"),
            shape = RoundedCornerShape(28.dp),
            color = DarkSurface,
            border = BorderStroke(2.dp, PriorityHigh.copy(alpha = 0.8f)),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = TealNeon
                    )
                )

                Spacer(modifier = Modifier.height(18.dp))

                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .scale(scale)
                        .clip(CircleShape)
                        .background(PriorityHigh.copy(alpha = 0.2f))
                        .border(2.dp, PriorityHigh, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = "Task Alarm Ringing",
                        tint = PriorityHigh,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PriorityHigh.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = "⏰ TASK ALARM • $priority",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = PriorityHigh,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp
                        ),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = notes,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        ),
                        maxLines = 4
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onMarkDone,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TealNeon,
                        contentColor = TealDeep
                    ),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 52.dp)
                        .testTag("lock_alarm_done_button")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Mark Completed & Stop Alarm",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onSnooze(5) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("lock_alarm_snooze_5m"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TealNeon)
                    ) {
                        Text("+5m", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = { onSnooze(15) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("lock_alarm_snooze_15m"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TealNeon)
                    ) {
                        Text("+15m", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = { onSnooze(60) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("lock_alarm_snooze_1h"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TealNeon)
                    ) {
                        Text("+1h", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("lock_alarm_dismiss_button")
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Dismiss Alarm",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
