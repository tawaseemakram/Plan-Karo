package com.example.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ads.AdManager
import com.example.data.model.QuoteTriggerReason
import com.example.data.model.RepeatMode
import com.example.ui.calendar.SmartCalendarScreen
import com.example.ui.components.CategoryManagementSheet
import com.example.ui.components.ImagePreviewDialog
import com.example.ui.components.InspirationalQuoteDialog
import com.example.ui.components.PermissionSettingsPromptDialog
import com.example.ui.components.SnoozeDialog
import com.example.ui.components.TaskAlarmDialog
import com.example.ui.components.TaskEditSheet
import com.example.ui.habits.HabitsScreen
import com.example.ui.navigation.AppBottomBar
import com.example.ui.notes.NoteEditorSheet
import com.example.ui.notes.NotesScreen
import com.example.ui.profile.ProfileScreen
import com.example.ui.theme.DarkBg

@Composable
fun MainAppScreen(
    viewModel: TaskViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val currentFilter by viewModel.currentFilter.collectAsStateWithLifecycle()

    val editingTask by viewModel.editingTask.collectAsStateWithLifecycle()
    val isAddSheetOpen by viewModel.isAddSheetOpen.collectAsStateWithLifecycle()
    val editingNote by viewModel.editingNote.collectAsStateWithLifecycle()
    val isNoteEditorOpen by viewModel.isNoteEditorOpen.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val isCategorySheetOpen by viewModel.isCategorySheetOpen.collectAsStateWithLifecycle()
    val taskForSnooze by viewModel.taskForSnooze.collectAsStateWithLifecycle()
    val previewImageUri by viewModel.previewImageUri.collectAsStateWithLifecycle()
    val activeReminderTask by viewModel.activeReminderTask.collectAsStateWithLifecycle()
    val isTaskAlarmEnabled by viewModel.isTaskAlarmEnabled.collectAsStateWithLifecycle()
    val quoteDialogState by viewModel.quoteDialogState.collectAsStateWithLifecycle()
    val permissionPromptType by viewModel.permissionPromptType.collectAsStateWithLifecycle()

    // BackHandler: returns to TASKS screen if on other tabs
    BackHandler(enabled = currentScreen != AppScreen.TASKS) {
        viewModel.setScreen(AppScreen.TASKS)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBg,
        bottomBar = {
            AppBottomBar(
                currentScreen = currentScreen,
                onScreenSelected = { viewModel.setScreen(it) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            Crossfade(
                targetState = currentScreen,
                label = "ScreenTransition"
            ) { screen ->
                when (screen) {
                    AppScreen.TASKS -> TaskScreen(viewModel = viewModel)
                    AppScreen.NOTES -> NotesScreen(viewModel = viewModel)
                    AppScreen.CALENDAR -> SmartCalendarScreen(viewModel = viewModel)
                    AppScreen.HABITS -> HabitsScreen(viewModel = viewModel)
                    AppScreen.PROFILE -> ProfileScreen(viewModel = viewModel)
                }
            }
        }
    }

    // Global Note Editor BottomSheet
    if (isNoteEditorOpen) {
        NoteEditorSheet(
            initialNote = editingNote,
            onDismiss = { viewModel.closeNoteEditor() },
            onSave = { id, title, content, category, colorHex, isPinned, audioUri, audioDurationSeconds, imageUri, isChecklist, checklistItems ->
                val isNewNote = id == 0L
                viewModel.saveNote(
                    id = id,
                    title = title,
                    content = content,
                    category = category,
                    colorHex = colorHex,
                    isPinned = isPinned,
                    audioUri = audioUri,
                    audioDurationSeconds = audioDurationSeconds,
                    imageUri = imageUri,
                    isChecklist = isChecklist,
                    checklistItems = checklistItems
                )
                // Interstitial ad & quote trigger ONLY on new note creation (not edit)
                if (isNewNote) {
                    (context as? Activity)?.let { act ->
                        AdManager.showInterstitialAfterTaskEntry(act) {
                            viewModel.showQuote(QuoteTriggerReason.NOTE_CREATED)
                        }
                    } ?: run {
                        viewModel.showQuote(QuoteTriggerReason.NOTE_CREATED)
                    }
                }
            }
        )
    }

    // Global Add / Edit Modal BottomSheet
    if (isAddSheetOpen) {
        val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
        val defaultRepeatMode = when (currentFilter) {
            TaskFilter.DAILY_TASK -> RepeatMode.DAILY
            TaskFilter.WEEKLY_TASK -> RepeatMode.WEEKLY
            TaskFilter.MONTHLY_TASK -> RepeatMode.MONTHLY
            else -> RepeatMode.NONE
        }
        TaskEditSheet(
            initialTask = editingTask,
            categories = categories,
            defaultRepeatMode = defaultRepeatMode,
            selectedDate = selectedDate,
            alarmSoundEnabled = isTaskAlarmEnabled,
            onAlarmSoundEnabledChange = { viewModel.setTaskAlarmEnabled(it) },
            onDismiss = { viewModel.closeAddEditSheet() },
            onSave = { id, title, notes, priority, category, reminderTime, imageUri, audioUri, audioDurationSeconds, repeatMode ->
                val isNewTask = id == 0L
                viewModel.saveTask(
                    id = id,
                    title = title,
                    notes = notes,
                    priority = priority,
                    category = category,
                    reminderTimeMillis = reminderTime,
                    imageUri = imageUri,
                    audioUri = audioUri,
                    audioDurationSeconds = audioDurationSeconds,
                    repeatMode = repeatMode
                )
                // Interstitial ad (subject to cooldown) & quote trigger ONLY on new task creation (not edit)
                if (isNewTask) {
                    (context as? Activity)?.let { act ->
                        AdManager.showInterstitialAfterTaskEntry(act) {
                            viewModel.showQuote(QuoteTriggerReason.TASK_CREATED)
                        }
                    } ?: run {
                        viewModel.showQuote(QuoteTriggerReason.TASK_CREATED)
                    }
                }
            }
        )
    }

    // Category Management Sheet
    if (isCategorySheetOpen) {
        CategoryManagementSheet(
            categories = categories,
            onAddCategory = { name, colorHex ->
                viewModel.addCustomCategory(name, colorHex)
            },
            onDeleteCategory = { categoryId ->
                viewModel.deleteCustomCategory(categoryId)
            },
            onDismiss = { viewModel.closeCategorySheet() }
        )
    }

    // Global Snooze Dialog
    if (taskForSnooze != null) {
        SnoozeDialog(
            task = taskForSnooze!!,
            onDismiss = { viewModel.closeSnoozeDialog() },
            onSnoozeMinutes = { minutes ->
                viewModel.snoozeTask(taskForSnooze!!, minutes)
            }
        )
    }

    // Global Image Preview Dialog
    if (!previewImageUri.isNullOrBlank()) {
        ImagePreviewDialog(
            imageUri = previewImageUri!!,
            onDismiss = { viewModel.closeImagePreview() }
        )
    }

    // Global Screen Alarm Dialog: Rings and vibrates on screen when task time arrives ONLY if Alarm is enabled
    if (activeReminderTask != null && isTaskAlarmEnabled) {
        TaskAlarmDialog(
            task = activeReminderTask!!,
            onDismiss = { viewModel.dismissReminder(activeReminderTask!!) },
            onSnooze = { minutes -> viewModel.snoozeTask(activeReminderTask!!, minutes) },
            onMarkDone = { viewModel.toggleTaskCompletion(activeReminderTask!!) }
        )
    }

    // 6. Android 14+ Denied Permission -> Direct Open Settings Prompt (takes priority over Quote dialog)
    if (permissionPromptType != null) {
        PermissionSettingsPromptDialog(
            permissionType = permissionPromptType!!,
            onDismiss = { viewModel.dismissPermissionSettingsPrompt() }
        )
    } else if (quoteDialogState != null) {
        // Inspirational Quote Dialog (App launch, and after task/note creation after ad completion)
        InspirationalQuoteDialog(
            state = quoteDialogState!!,
            onNextQuote = { viewModel.nextQuote() },
            onDismiss = { viewModel.dismissQuote() }
        )
    }
}
