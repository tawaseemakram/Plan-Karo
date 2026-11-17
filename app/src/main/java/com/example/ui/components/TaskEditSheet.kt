package com.example.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import com.example.data.model.TaskCategory
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Repeat
import com.example.data.model.RepeatMode
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import android.widget.Toast
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.ImageAttachmentStorage
import com.example.data.model.Priority
import com.example.data.model.TaskItem
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.PriorityHigh
import com.example.ui.theme.TealNeon
import com.example.ui.theme.TealNeonDark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditSheet(
    initialTask: TaskItem?,
    categories: List<TaskCategory> = TaskCategory.DEFAULT_CATEGORIES,
    defaultRepeatMode: RepeatMode = RepeatMode.NONE,
    selectedDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
    alarmSoundEnabled: Boolean? = null,
    onAlarmSoundEnabledChange: ((Boolean) -> Unit)? = null,
    onDismiss: () -> Unit,
    onSave: (
        id: Long,
        title: String,
        notes: String,
        priority: Priority,
        category: String,
        reminderTimeMillis: Long?,
        imageUri: String?,
        audioUri: String?,
        audioDurationSeconds: Int,
        repeatMode: RepeatMode
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var title by remember(initialTask?.id) { mutableStateOf(initialTask?.title ?: "") }
    var notes by remember(initialTask?.id) { mutableStateOf(initialTask?.notes ?: "") }
    var priority by remember(initialTask?.id) { mutableStateOf(initialTask?.priority ?: Priority.MEDIUM) }
    var category by remember(initialTask?.id) { mutableStateOf(initialTask?.category ?: "Personal") }
    var reminderTime by remember(initialTask?.id) { mutableStateOf(initialTask?.reminderTimeMillis) }
    var repeatMode by remember(initialTask?.id, defaultRepeatMode) {
        mutableStateOf(initialTask?.repeatMode ?: defaultRepeatMode)
    }
    var imageUri by remember(initialTask?.id) { mutableStateOf(initialTask?.imageUri) }
    var audioUri by remember(initialTask?.id) { mutableStateOf(initialTask?.audioUri) }
    var audioDurationSeconds by remember(initialTask?.id) { mutableIntStateOf(initialTask?.audioDurationSeconds ?: 0) }
    var isAlarmEnabled by remember(alarmSoundEnabled) {
        mutableStateOf(alarmSoundEnabled ?: com.example.reminder.AlarmPreferenceManager.isAlarmEnabled(context))
    }
    var missingAlarmPermission by remember { mutableStateOf<com.example.reminder.PermissionPromptType?>(null) }
    var titleError by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    var permissionCheckTick by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionCheckTick++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val currentMissingPermission = remember(permissionCheckTick, isAlarmEnabled, reminderTime) {
        if (reminderTime != null || isAlarmEnabled) {
            com.example.reminder.PermissionHelper.firstMissingAlarmPermission(
                context = context,
                isAlarmSoundEnabled = isAlarmEnabled
            )
        } else null
    }

    val draftImageUris = remember { mutableSetOf<String>() }
    val draftAudioUris = remember { mutableSetOf<String>() }

    fun cleanupDraftMedia(keepImage: String? = null, keepAudio: String? = null) {
        val imagesToDelete = draftImageUris.filter { it != keepImage }
        val audiosToDelete = draftAudioUris.filter { it != keepAudio }
        if (imagesToDelete.isEmpty() && audiosToDelete.isEmpty()) return
        val appContext = context.applicationContext
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            imagesToDelete.forEach { ImageAttachmentStorage.deleteOwnedImageFile(appContext, it) }
            audiosToDelete.forEach { com.example.audio.AudioUtils.deleteOwnedAudioFile(appContext, it) }
        }
        draftImageUris.removeAll(imagesToDelete.toSet())
        draftAudioUris.removeAll(audiosToDelete.toSet())
    }

    fun handleDismiss() {
        cleanupDraftMedia()
        onDismiss()
    }

    if (missingAlarmPermission != null) {
        PermissionSettingsPromptDialog(
            permissionType = missingAlarmPermission!!,
            onDismiss = { missingAlarmPermission = null }
        )
    }

    // Android Photo Picker launcher (zero permissions needed per Play Policy)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val persisted = withContext(Dispatchers.IO) {
                    ImageAttachmentStorage.persistPickedImage(context, uri)
                }
                if (persisted != null) {
                    draftImageUris.add(persisted)
                    imageUri = persisted
                } else {
                    Toast.makeText(context, "Unable to attach selected image.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val timeFormatter = remember { SimpleDateFormat("EEE, MMM d • h:mm a", Locale.getDefault()) }

    fun createInitialCalendar(): Calendar {
        val calendar = Calendar.getInstance()
        if (reminderTime != null) {
            calendar.timeInMillis = reminderTime!!
        } else {
            val targetStr = initialTask?.taskDate ?: selectedDate
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val todayStr = sdf.format(Date())
            if (targetStr >= todayStr) {
                try {
                    val parsed = sdf.parse(targetStr)
                    if (parsed != null) {
                        val targetCal = Calendar.getInstance().apply { time = parsed }
                        calendar.set(Calendar.YEAR, targetCal.get(Calendar.YEAR))
                        calendar.set(Calendar.MONTH, targetCal.get(Calendar.MONTH))
                        calendar.set(Calendar.DAY_OF_MONTH, targetCal.get(Calendar.DAY_OF_MONTH))
                    }
                } catch (_: Exception) {}
            }
        }
        return calendar
    }

    fun openDateTimePicker() {
        val calendar = createInitialCalendar()

        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                TimePickerDialog(
                    context,
                    { _, hourOfDay, minute ->
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        calendar.set(Calendar.MINUTE, minute)
                        calendar.set(Calendar.SECOND, 0)
                        calendar.set(Calendar.MILLISECOND, 0)
                        if (calendar.timeInMillis <= System.currentTimeMillis()) {
                            calendar.add(Calendar.DAY_OF_YEAR, 1)
                        }
                        reminderTime = calendar.timeInMillis
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    false
                ).show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun openTimePickerDirectly() {
        val calendar = createInitialCalendar()

        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                calendar.set(Calendar.MINUTE, minute)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                if (calendar.timeInMillis <= System.currentTimeMillis()) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                    Toast.makeText(
                        context,
                        "That time has passed today — alarm scheduled for tomorrow",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                reminderTime = calendar.timeInMillis
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            false
        ).show()
    }

    ModalBottomSheet(
        onDismissRequest = { handleDismiss() },
        sheetState = sheetState,
        containerColor = DarkSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (initialTask == null) "New Daily Task" else "Edit Task",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                IconButton(onClick = { handleDismiss() }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Task Title Input
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    if (titleError && it.isNotBlank()) titleError = false
                },
                label = { Text("What needs to be done?") },
                placeholder = { Text("e.g., Complete project report") },
                isError = titleError,
                supportingText = if (titleError) {
                    { Text("Task title is required", color = PriorityHigh) }
                } else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_title_input"),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TealNeon,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedLabelColor = TealNeon
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Notes / Description field (as requested)
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes & Additional Details") },
                placeholder = { Text("Add subtasks, links, thoughts, checklist…") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Notes,
                        contentDescription = null,
                        tint = TealNeon
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .testTag("task_notes_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TealNeon,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedLabelColor = TealNeon
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Category Label Selector
            Text(
                text = "Category Label",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_category_selector_row"),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    val isSelected = category.equals(cat.name, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { category = cat.name },
                        label = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(cat.color)
                                )
                                Text(
                                    text = cat.name,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = cat.containerColor,
                            selectedLabelColor = cat.color,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = MaterialTheme.colorScheme.outlineVariant,
                            selectedBorderColor = cat.color
                        ),
                        modifier = Modifier.testTag("category_chip_${cat.name.lowercase()}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Priority Selector
            Text(
                text = "Priority Level",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Priority.values().forEach { p ->
                    val isSelected = priority == p
                    FilterChip(
                        selected = isSelected,
                        onClick = { priority = p },
                        label = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(p.color)
                                )
                                Text(
                                    text = p.label,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = p.containerColor,
                            selectedLabelColor = p.color,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = MaterialTheme.colorScheme.outlineVariant,
                            selectedBorderColor = p.color
                        ),
                        modifier = Modifier.testTag("priority_chip_${p.name.lowercase()}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Reminder Setting (as requested)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = null,
                        tint = TealNeon,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Task Reminder",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                if (reminderTime != null) {
                    IconButton(
                        onClick = { reminderTime = null },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove Reminder",
                            tint = PriorityHigh,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Reminder Display or Quick Picker
            if (reminderTime != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = TealNeonDark.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TealNeon.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { openDateTimePicker() }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Reminder Scheduled",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TealNeon,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = timeFormatter.format(Date(reminderTime!!)),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.Event,
                            contentDescription = "Change time",
                            tint = TealNeon
                        )
                    }
                }
            } else {
                // Quick Presets
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        QuickReminderChip(
                            label = "⚡ +1 min (Test)",
                            onClick = {
                                reminderTime = System.currentTimeMillis() + 60 * 1000L
                            }
                        )
                    }
                    item {
                        QuickReminderChip(
                            label = "+5 min",
                            onClick = {
                                reminderTime = System.currentTimeMillis() + 5 * 60 * 1000L
                            }
                        )
                    }
                    item {
                        QuickReminderChip(
                            label = "+15 min",
                            onClick = {
                                reminderTime = System.currentTimeMillis() + 15 * 60 * 1000L
                            }
                        )
                    }
                    item {
                        QuickReminderChip(
                            label = "+1 hour",
                            onClick = {
                                reminderTime = System.currentTimeMillis() + 60 * 60 * 1000L
                            }
                        )
                    }
                    item {
                        QuickReminderChip(
                            label = "Pick Time…",
                            onClick = { openTimePickerDirectly() }
                        )
                    }
                    item {
                        QuickReminderChip(
                            label = "Date & Time…",
                            onClick = { openDateTimePicker() }
                        )
                    }
                }
            }

            if (currentMissingPermission != null && reminderTime != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PriorityHigh.copy(alpha = 0.14f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PriorityHigh.copy(alpha = 0.65f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { missingAlarmPermission = currentMissingPermission }
                        .testTag("alarm_permission_warning_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Permission required",
                            tint = PriorityHigh,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentMissingPermission.title,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = PriorityHigh,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = "Tap here to enable in Settings so task alarms ring on time even when the app is closed.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Repeat Task Setting (No Repeat, Daily, Weekly, Monthly)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = null,
                        tint = TealNeon,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Repeat Task",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (repeatMode != RepeatMode.NONE) TealNeon.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.06f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (repeatMode != RepeatMode.NONE) TealNeon.copy(alpha = 0.5f) else Color.Transparent
                    )
                ) {
                    Text(
                        text = if (repeatMode != RepeatMode.NONE) "🔁 ${repeatMode.label}" else "Off",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (repeatMode != RepeatMode.NONE) TealNeon else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        ),
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Repeat Task Options: No Repeat, Daily, Weekly, Monthly
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("repeat_task_options_row"),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val repeatOptions = listOf(
                    RepeatMode.NONE to "No Repeat",
                    RepeatMode.DAILY to "Daily",
                    RepeatMode.WEEKLY to "Weekly",
                    RepeatMode.MONTHLY to "Monthly"
                )

                repeatOptions.forEach { (mode, label) ->
                    val isSelected = repeatMode == mode
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) TealNeon.copy(alpha = 0.2f) else DarkSurface,
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) TealNeon else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { repeatMode = mode }
                            .testTag("repeat_option_${mode.name.lowercase()}")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp, horizontal = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) TealNeon else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                ),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Explanatory hint for selected Repeat option
            Text(
                text = when (repeatMode) {
                    RepeatMode.NONE -> "• No Repeat: One-time task, will not re-occur after completion."
                    RepeatMode.DAILY -> "• Daily: Repeats every day. When completed, automatically schedules for tomorrow."
                    RepeatMode.WEEKLY -> "• Weekly: Repeats every 7 days on the same weekday."
                    RepeatMode.MONTHLY -> "• Monthly: Repeats on the same date every month."
                },
                style = MaterialTheme.typography.bodySmall.copy(
                    color = if (repeatMode != RepeatMode.NONE) TealNeon.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontSize = 11.sp
                ),
                modifier = Modifier.padding(start = 2.dp, top = 6.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Task Alarm Option in Task Edit Sheet
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = DarkSurface,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isAlarmEnabled) Color(0xFFFF9800).copy(alpha = 0.45f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("edit_task_alarm_option_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = "Task Alarm",
                            tint = if (isAlarmEnabled) Color(0xFFFF9800) else Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Task Alarm",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Text(
                                    text = if (isAlarmEnabled) "⏰ ON" else "🔕 BAR ONLY",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isAlarmEnabled) Color(0xFFFF9800) else Color.White.copy(alpha = 0.6f),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                )
                            }
                            Text(
                                text = if (isAlarmEnabled) {
                                    "Ring loudly for scheduled task alarms"
                                } else {
                                    "2-chime notification bar reminder (no loud alarm)"
                                },
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (isAlarmEnabled) Color(0xFFFF9800) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    Switch(
                        checked = isAlarmEnabled,
                        onCheckedChange = { enabled ->
                            isAlarmEnabled = enabled
                            if (onAlarmSoundEnabledChange != null) {
                                onAlarmSoundEnabledChange(enabled)
                            } else {
                                com.example.reminder.AlarmPreferenceManager.setAlarmEnabled(context, enabled)
                            }
                            val missing = com.example.reminder.PermissionHelper.firstMissingAlarmPermission(
                                context = context,
                                isAlarmSoundEnabled = enabled
                            )
                            if (missing != null) {
                                missingAlarmPermission = missing
                            } else {
                                val msg = if (enabled) {
                                    "Task Alarm enabled: Phone will ring at task time"
                                } else {
                                    "Notification mode active: 2-chime reminder on notification bar"
                                }
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DarkBg,
                            checkedTrackColor = Color(0xFFFF9800),
                            uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                            uncheckedTrackColor = DarkSurface
                        ),
                        modifier = Modifier.testTag("edit_task_alarm_switch")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Image Attachment (as requested)
            Text(
                text = "Attached Image",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (!imageUri.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, TealNeon.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                ) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = "Task image attachment",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(150.dp)
                    )

                    IconButton(
                        onClick = { imageUri = null },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(32.dp)
                            .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove photo",
                            tint = PriorityHigh,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                        .testTag("attach_image_button")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            tint = TealNeon,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Add photo or receipt…",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TealNeon,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Audio Voice Note Attachment
            TaskEditAudioSection(
                audioUri = audioUri,
                audioDurationSeconds = audioDurationSeconds,
                onAudioUpdated = { uri, dur ->
                    if (!uri.isNullOrBlank() && uri != initialTask?.audioUri) {
                        draftAudioUris.add(uri)
                    }
                    audioUri = uri
                    audioDurationSeconds = dur
                }
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Save / Update Button
            Button(
                onClick = {
                    if (title.isBlank()) {
                        titleError = true
                    } else {
                        cleanupDraftMedia(keepImage = imageUri, keepAudio = audioUri)
                        onSave(
                            initialTask?.id ?: 0L,
                            title,
                            notes,
                            priority,
                            category,
                            reminderTime,
                            imageUri,
                            audioUri,
                            audioDurationSeconds,
                            repeatMode
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_task_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TealNeon,
                    contentColor = Color(0xFF00382E)
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = if (initialTask == null) "Create Task" else "Save Changes",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun QuickReminderChip(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}
