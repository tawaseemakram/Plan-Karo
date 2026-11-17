package com.example

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.example.ui.TaskViewModel
import com.example.ui.profile.PrivacyPolicyScreen
import com.example.ui.theme.PlanKaroTheme
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MainActivityTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testTaskViewModelInitialization() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = TaskViewModel(app)
        assertNotNull(vm)
        assertNotNull(vm.allTasks.value)
    }

    @Test
    fun testMainActivityLaunch() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertNotNull(activity)
            }
        }
    }

    @Test
    fun testPrivacyPolicyScreenDisplaysAndCloses() {
        var closed = false
        composeTestRule.setContent {
            PlanKaroTheme {
                PrivacyPolicyScreen(onClose = { closed = true })
            }
        }

        composeTestRule.onNodeWithTag("privacy_policy_screen").assertIsDisplayed()
        composeTestRule.onNodeWithText("Privacy Policy").assertIsDisplayed()
        composeTestRule.onNodeWithTag("privacy_policy_close_button").performClick()
        assertTrue(closed)
    }

    @Test
    fun testTermsOfServiceScreenDisplaysAndAccepts() {
        var accepted = false
        composeTestRule.setContent {
            PlanKaroTheme {
                com.example.ui.profile.TermsOfServiceScreen(onAccept = { accepted = true })
            }
        }

        composeTestRule.onNodeWithTag("terms_of_service_screen").assertIsDisplayed()
        composeTestRule.onNodeWithText("Terms of Service").assertIsDisplayed()
        composeTestRule.onNodeWithTag("terms_accept_button").performClick()
        assertTrue(accepted)
    }

    @Test
    fun testTaskReminderReceiverActionsExecuteSafely() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val receiver = com.example.receiver.TaskReminderReceiver()

        receiver.onReceive(
            context,
            android.content.Intent(android.content.Intent.ACTION_BOOT_COMPLETED)
        )
        receiver.onReceive(
            context,
            android.content.Intent(com.example.reminder.ReminderManager.ACTION_SNOOZE).apply {
                putExtra(com.example.reminder.ReminderManager.EXTRA_TASK_ID, 9999L)
                putExtra(com.example.reminder.ReminderManager.EXTRA_SNOOZE_MINUTES, 10)
            }
        )
        receiver.onReceive(
            context,
            android.content.Intent(com.example.reminder.ReminderManager.ACTION_COMPLETE).apply {
                putExtra(com.example.reminder.ReminderManager.EXTRA_TASK_ID, 9999L)
            }
        )
    }

    @Test
    fun testProfilePermissionsCardOpensThreeOptionsSheet() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val vm = TaskViewModel(app)
        composeTestRule.setContent {
            PlanKaroTheme {
                com.example.ui.profile.ProfileScreen(viewModel = vm)
            }
        }

        composeTestRule.onNodeWithTag("profile_screen_list")
            .performScrollToNode(hasTestTag("profile_permissions_card"))
        composeTestRule.onNodeWithTag("profile_permissions_card").assertIsDisplayed()
        composeTestRule.onNodeWithText("Permissions & Alarm Access").assertIsDisplayed()

        composeTestRule.onNodeWithTag("profile_permissions_card").performClick()

        composeTestRule.onNodeWithTag("permissions_detail_sheet").assertIsDisplayed()
        composeTestRule.onNodeWithText("Notifications").assertIsDisplayed()
        composeTestRule.onNodeWithText("Exact Alarms (Task & Routine)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Microphone (Audio Notes)").assertIsDisplayed()
        composeTestRule.onNodeWithText("Background Battery (Unrestricted)").assertDoesNotExist()
    }

    @Test
    fun testTaskHabitAndRoutineCardsRenderBadgesWithoutSqueezing() {
        val futureTime = System.currentTimeMillis() + 3_600_000L
        val task = com.example.data.model.TaskItem(
            id = 1L,
            title = "Bike Charge",
            notes = "Bike charge",
            priority = com.example.data.model.Priority.MEDIUM,
            category = "Personal",
            reminderTimeMillis = futureTime,
            taskDate = "2026-11-14",
            repeatMode = com.example.data.model.RepeatMode.DAILY
        )
        val habit = com.example.data.model.HabitItem(
            id = 2L,
            title = "Morning Meditation & Deep Breathing",
            category = "Personal",
            routineTime = com.example.data.model.RoutineTime.MORNING,
            streakCount = 12,
            targetDaysPerWeek = 5,
            reminderTime = "07:30 AM"
        )
        val routine = com.example.data.model.RoutineItem(
            id = 3L,
            title = "Evening Wind Down",
            description = "Plan next day priorities",
            routineTime = com.example.data.model.RoutineTime.EVENING,
            scheduledTime = "09:30 PM"
        )

        composeTestRule.setContent {
            PlanKaroTheme {
                Column(
                    modifier = Modifier
                        .width(320.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    com.example.ui.components.TaskCard(
                        task = task,
                        onToggleComplete = {},
                        onEdit = {},
                        onDelete = {},
                        onSnooze = {},
                        onImageClick = {}
                    )
                    com.example.ui.habits.HabitCard(
                        habit = habit,
                        targetDate = "2026-11-14",
                        onToggleCompletion = {},
                        onEdit = {},
                        onDelete = {}
                    )
                    com.example.ui.habits.RoutineCard(
                        routine = routine,
                        onToggleEnabled = {},
                        onDelete = {}
                    )
                }
            }
        }

        val catNode = composeTestRule
            .onNodeWithTag("task_category_badge", useUnmergedTree = true)
            .fetchSemanticsNode()
        val repeatNode = composeTestRule
            .onNodeWithTag("task_repeat_badge", useUnmergedTree = true)
            .fetchSemanticsNode()
        assertTrue(
            "Category badge should be visible: ${catNode.boundsInRoot}",
            catNode.boundsInRoot.width > 0f && catNode.boundsInRoot.height > 0f
        )
        assertTrue(
            "Repeat badge should align on same row with single-line height: cat=${catNode.boundsInRoot}, repeat=${repeatNode.boundsInRoot}",
            kotlin.math.abs(repeatNode.boundsInRoot.top - catNode.boundsInRoot.top) <= 2f &&
                kotlin.math.abs(repeatNode.boundsInRoot.height - catNode.boundsInRoot.height) <= 2f &&
                repeatNode.boundsInRoot.left > catNode.boundsInRoot.right
        )
        composeTestRule.onNodeWithTag("task_category_badge", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("task_repeat_badge", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Medium", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("5d/wk", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("09:30 PM", useUnmergedTree = true).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun testCustomCategoryWithPipePersistsViaJson() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        app.getSharedPreferences("plankaro_categories_prefs", android.content.Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()

        val vm1 = TaskViewModel(app)
        val added = vm1.addCustomCategory("Work | Deep Focus", "#00E5BE")
        assertTrue(added)

        val vm2 = TaskViewModel(app)
        val loaded = vm2.categories.value.find { it.name == "Work | Deep Focus" }
        assertNotNull(loaded)
        org.junit.Assert.assertEquals("#00E5BE", loaded?.colorHex)
    }

    @Test
    fun testDailyNotificationShowsWithMonochromeIcon() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        com.example.reminder.DailyReminderManager.showDailyNotification(
            context,
            com.example.reminder.DailyReminderManager.PERIOD_MORNING
        )
        val drawableId = context.resources.getIdentifier(
            "ic_notification",
            "drawable",
            context.packageName
        )
        assertTrue(drawableId != 0)
    }

    @Test
    fun testClearAllLocalDataCardOpensDialogAndResetsData() {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        app.getSharedPreferences("plankaro_api_key_prefs", android.content.Context.MODE_PRIVATE)
            .edit()
            .putString("legacy_key", "temp-user-key-987654")
            .commit()
        app.getSharedPreferences("plankaro_legal_prefs", android.content.Context.MODE_PRIVATE)
            .edit()
            .putBoolean("terms_accepted_v1", true)
            .commit()

        val audioDir = java.io.File(app.filesDir, "audio_notes").apply { mkdirs() }
        val dummyAudio = java.io.File(audioDir, "sample.m4a").apply { writeText("audio") }
        val imageDir = com.example.data.local.ImageAttachmentStorage.getImagesDir(app)
        val dummyImage = java.io.File(imageDir, "sample.jpg").apply { writeText("image") }
        assertTrue(dummyAudio.exists())
        assertTrue(dummyImage.exists())

        val vm = TaskViewModel(app)
        vm.addCustomCategory("Side Hustle", "#FFB800")
        assertTrue(vm.categories.value.any { it.name == "Side Hustle" })

        composeTestRule.setContent {
            PlanKaroTheme {
                com.example.ui.profile.ProfileScreen(viewModel = vm)
            }
        }

        composeTestRule.onNodeWithTag("profile_screen_list")
            .performScrollToNode(hasTestTag("profile_clear_local_data_card"))
        composeTestRule.onNodeWithTag("profile_clear_local_data_card").assertIsDisplayed()
        composeTestRule.onNodeWithText("Clear All Local Data").assertIsDisplayed()

        composeTestRule.onNodeWithTag("profile_clear_local_data_card").performClick()
        composeTestRule.onNodeWithTag("clear_local_data_confirm_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithTag("clear_local_data_confirm_button").performClick()

        composeTestRule.waitUntil(timeoutMillis = 5_000L) {
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            vm.categories.value.none { it.isCustom } &&
                !dummyAudio.exists() &&
                !dummyImage.exists()
        }

        org.junit.Assert.assertNull(
            app.getSharedPreferences("plankaro_api_key_prefs", android.content.Context.MODE_PRIVATE)
                .getString("legacy_key", null)
        )
        org.junit.Assert.assertFalse(
            app.getSharedPreferences("plankaro_legal_prefs", android.content.Context.MODE_PRIVATE)
                .getBoolean("terms_accepted_v1", false)
        )
        assertTrue(
            app.getSharedPreferences("plankaro_seed_prefs", android.content.Context.MODE_PRIVATE)
                .getBoolean("has_seeded_initial_habits_v3", false)
        )
    }

    @Test
    fun testCustomNotificationRingtonesAndTaskReceiverIcon() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()

        val notifyResId = context.resources.getIdentifier(
            "plankaro_notify_chime",
            "raw",
            context.packageName
        )
        val alarmResId = context.resources.getIdentifier(
            "plankaro_alarm_ring",
            "raw",
            context.packageName
        )
        assertTrue(notifyResId != 0)
        assertTrue(alarmResId != 0)

        val notifyUri = com.example.reminder.ReminderManager.getNotificationSoundUri(context)
        val alarmUri = com.example.reminder.ReminderManager.getAlarmSoundUri(context)
        assertTrue(notifyUri.toString().endsWith("/$notifyResId"))
        assertTrue(alarmUri.toString().endsWith("/$alarmResId"))

        com.example.reminder.ReminderManager.createNotificationChannel(context)
        com.example.reminder.DailyReminderManager.createChannel(context)

        val nm = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val alarmChannel = nm.getNotificationChannel(com.example.reminder.ReminderManager.CHANNEL_ID_ALARM)
        val barChannel = nm.getNotificationChannel(com.example.reminder.ReminderManager.CHANNEL_ID_NOTIFICATION_BAR)
        val dailyChannel = nm.getNotificationChannel(com.example.reminder.DailyReminderManager.CHANNEL_ID)
        assertNotNull(alarmChannel)
        assertNotNull(barChannel)
        assertNotNull(dailyChannel)
        org.junit.Assert.assertEquals(alarmUri, alarmChannel.sound)
        org.junit.Assert.assertEquals(notifyUri, barChannel.sound)
        org.junit.Assert.assertEquals(notifyUri, dailyChannel.sound)
        org.junit.Assert.assertEquals(
            android.app.NotificationManager.IMPORTANCE_HIGH,
            alarmChannel.importance
        )
        org.junit.Assert.assertEquals(
            android.app.NotificationManager.IMPORTANCE_HIGH,
            barChannel.importance
        )
        org.junit.Assert.assertEquals(
            android.app.NotificationManager.IMPORTANCE_HIGH,
            dailyChannel.importance
        )

        val receiver = com.example.receiver.TaskReminderReceiver()
        receiver.onReceive(
            context,
            android.content.Intent(com.example.reminder.ReminderManager.ACTION_REMINDER).apply {
                putExtra(com.example.reminder.ReminderManager.EXTRA_TASK_ID, 42L)
                putExtra(com.example.reminder.ReminderManager.EXTRA_TASK_TITLE, "Evening Workout")
                putExtra(com.example.reminder.ReminderManager.EXTRA_TASK_NOTES, "30 min run")
                putExtra(com.example.reminder.ReminderManager.EXTRA_TASK_PRIORITY, "HIGH")
            }
        )
        com.example.reminder.AlarmSoundManager.stopAlarm()

        // Verify unique 2-chime notification WAV has two audible pulses separated by a silent gap
        val wavBytes = context.resources.openRawResource(notifyResId).use { it.readBytes() }
        assertTrue(wavBytes.size > 44)
        val sampleRate = java.nio.ByteBuffer.wrap(wavBytes, 24, 4)
            .order(java.nio.ByteOrder.LITTLE_ENDIAN).int
        val pcmSamples = (wavBytes.size - 44) / 2
        val shortBuf = java.nio.ByteBuffer.wrap(wavBytes, 44, pcmSamples * 2)
            .order(java.nio.ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        fun maxAmp(startSec: Double, endSec: Double): Int {
            val s0 = (startSec * sampleRate).toInt().coerceIn(0, pcmSamples)
            val s1 = (endSec * sampleRate).toInt().coerceIn(s0, pcmSamples)
            var peak = 0
            for (i in s0 until s1) {
                val v = kotlin.math.abs(shortBuf.get(i).toInt())
                if (v > peak) peak = v
            }
            return peak
        }
        assertTrue("First notification chime pulse must be audible", maxAmp(0.01, 0.35) > 2000)
        assertTrue("Silent separation gap must distinguish 2 chimes", maxAmp(0.46, 0.58) < 50)
        assertTrue("Second notification chime pulse must be audible", maxAmp(0.62, 1.00) > 2000)
    }

    @Test
    fun testTaskAlarmDialogFitsCompleteButtonWithoutClipping() {
        val sampleTask = com.example.data.model.TaskItem(
            id = 101L,
            title = "Bike Charge",
            priority = com.example.data.model.Priority.HIGH,
            taskDate = "2026-11-17"
        )
        composeTestRule.setContent {
            PlanKaroTheme {
                com.example.ui.components.TaskAlarmDialog(
                    task = sampleTask,
                    onDismiss = {},
                    onSnooze = {},
                    onMarkDone = {}
                )
            }
        }

        val doneButtonNode = composeTestRule.onNodeWithTag("alarm_done_button")
        doneButtonNode.assertIsDisplayed()
        composeTestRule.onNodeWithText("Mark Completed & Stop Alarm", useUnmergedTree = true).assertIsDisplayed()

        val buttonBounds = doneButtonNode.fetchSemanticsNode().boundsInRoot
        val textBounds = composeTestRule
            .onNodeWithText("Mark Completed & Stop Alarm", useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot
        assertTrue(
            "Button height (${buttonBounds.height}) must fully contain text height (${textBounds.height}) without clipping",
            buttonBounds.height >= textBounds.height &&
                textBounds.top >= buttonBounds.top &&
                textBounds.bottom <= buttonBounds.bottom
        )
    }

    @Test
    fun testImageAttachmentStorageCopiesUriIntoAppPrivateDir() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val tempSource = java.io.File(context.cacheDir, "picked_photo.png").apply {
            writeBytes(byteArrayOf(1, 2, 3, 4, 5, 6))
        }
        val sourceUri = android.net.Uri.fromFile(tempSource)

        val persisted = com.example.data.local.ImageAttachmentStorage.persistPickedImage(context, sourceUri)
        assertNotNull(persisted)
        val persistedFile = java.io.File(android.net.Uri.parse(persisted).path!!)
        assertTrue(persistedFile.exists())
        assertTrue(persistedFile.absolutePath.contains(com.example.data.local.ImageAttachmentStorage.DIRECTORY_NAME))

        // Delete original source to verify copied file stays intact
        tempSource.delete()
        assertTrue(persistedFile.exists())

        com.example.data.local.ImageAttachmentStorage.deleteAllAttachedImages(context)
        org.junit.Assert.assertFalse(persistedFile.exists())
    }

    @Test
    fun testAlarmNotificationSetsDeleteIntentAndDismissStopsAlarm() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        com.example.reminder.AlarmPreferenceManager.setAlarmEnabled(context, true)

        val receiver = com.example.receiver.TaskReminderReceiver()
        receiver.onReceive(
            context,
            android.content.Intent(com.example.reminder.ReminderManager.ACTION_REMINDER).apply {
                putExtra(com.example.reminder.ReminderManager.EXTRA_TASK_ID, 1L)
                putExtra(com.example.reminder.ReminderManager.EXTRA_TASK_TITLE, "Take Medicine")
                putExtra(com.example.reminder.ReminderManager.EXTRA_TASK_NOTES, "After lunch")
                putExtra(com.example.reminder.ReminderManager.EXTRA_TASK_PRIORITY, "HIGH")
            }
        )

        assertTrue(com.example.reminder.AlarmSoundManager.isPlaying())

        val nm = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val shadowNm = org.robolectric.Shadows.shadowOf(nm)
        val posted = shadowNm.getNotification(1)
        assertNotNull(posted)
        assertNotNull(posted.deleteIntent)

        // Verify Task #1's deleteIntent does NOT collide with Task #13's scheduled reminder PendingIntent
        val reminderPiTask13 = android.app.PendingIntent.getBroadcast(
            context,
            13,
            android.content.Intent(context, com.example.receiver.TaskReminderReceiver::class.java).apply {
                action = com.example.reminder.ReminderManager.ACTION_REMINDER
            },
            android.app.PendingIntent.FLAG_NO_CREATE or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        org.junit.Assert.assertNull(reminderPiTask13)

        // Simulate swipe-dismissing the notification via deleteIntent
        posted.deleteIntent.send()
        org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        // Also deliver ACTION_DISMISS directly to verify receiver cleanup
        receiver.onReceive(
            context,
            android.content.Intent(com.example.reminder.ReminderManager.ACTION_DISMISS).apply {
                putExtra(com.example.reminder.ReminderManager.EXTRA_TASK_ID, 1L)
            }
        )

        org.junit.Assert.assertFalse(com.example.reminder.AlarmSoundManager.isPlaying())
        org.junit.Assert.assertNull(shadowNm.getNotification(1))
    }

    @Test
    fun testSingleDeleteCleansUpUnreferencedMediaAndPreservesSharedRefs() = kotlinx.coroutines.runBlocking {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val db = com.example.data.local.TaskDatabase.getDatabase(app)
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            db.clearAllTables()
        }

        val imageDir = com.example.data.local.ImageAttachmentStorage.getImagesDir(app)
        val imageFile = java.io.File(imageDir, "shared_img.jpg").apply { writeText("img") }
        val imageUri = android.net.Uri.fromFile(imageFile).toString()

        val audioDir = java.io.File(app.filesDir, "audio_notes").apply { mkdirs() }
        val audioFile = java.io.File(audioDir, "task_voice.m4a").apply { writeText("aud") }
        val audioPath = audioFile.absolutePath

        val noteId = db.noteDao().insertNote(
            com.example.data.model.NoteItem(
                title = "Note With Shared Image",
                content = "Body",
                imageUri = imageUri
            )
        )
        val taskId = db.taskDao().insertTask(
            com.example.data.model.TaskItem(
                title = "Task With Media",
                taskDate = "2026-11-15",
                imageUri = imageUri,
                audioUri = audioPath
            )
        )

        val vm = TaskViewModel(app)
        val insertedNote = db.noteDao().getNoteById(noteId)!!
        val insertedTask = db.taskDao().getTaskById(taskId)!!

        // 1. Deleting the note must NOT delete shared image still referenced by task
        vm.deleteNote(insertedNote)
        val noteDeadline = System.currentTimeMillis() + 3_000L
        while (db.noteDao().getNoteById(noteId) != null && System.currentTimeMillis() < noteDeadline) {
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            kotlinx.coroutines.delay(25)
        }
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        assertTrue(imageFile.exists())

        // 2. Deleting the task removes the final references to both image & audio files
        vm.deleteTask(insertedTask)
        val mediaDeadline = System.currentTimeMillis() + 3_000L
        while ((imageFile.exists() || audioFile.exists()) && System.currentTimeMillis() < mediaDeadline) {
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            kotlinx.coroutines.delay(25)
        }
        org.junit.Assert.assertFalse(imageFile.exists())
        org.junit.Assert.assertFalse(audioFile.exists())

        // 3. Orphan sweep deletes unreferenced leftover files while keeping active Task attachments
        val activeImg = java.io.File(imageDir, "active_img.jpg").apply { writeText("active") }
        val orphanImg = java.io.File(imageDir, "orphan_img.jpg").apply { writeText("orphan") }
        val orphanAud = java.io.File(audioDir, "orphan_aud.m4a").apply { writeText("orphan_aud") }
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            db.taskDao().insertTask(
                com.example.data.model.TaskItem(
                    title = "Active Task",
                    taskDate = "2026-11-15",
                    imageUri = android.net.Uri.fromFile(activeImg).toString()
                )
            )
        }

        val pruned = vm.cleanupOrphanedInternalMediaFiles()
        assertTrue(pruned >= 2)
        assertTrue(activeImg.exists())
        org.junit.Assert.assertFalse(orphanImg.exists())
        org.junit.Assert.assertFalse(orphanAud.exists())
    }

    @Test
    fun testBackgroundAlarmStartsTaskAlarmServiceWithInsistentFlagAndStopsCleanly() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        com.example.reminder.AlarmPreferenceManager.setAlarmEnabled(context, true)
        com.example.reminder.AlarmSoundManager.stopAlarm(context)

        // Verify ReminderManager schedules AlarmManager.setAlarmClock when Task Alarm is enabled
        val futureTrigger = System.currentTimeMillis() + 60_000L
        val scheduledTask = com.example.data.model.TaskItem(
            id = 77L,
            title = "Morning Walk",
            notes = "6:00 AM daily walk",
            priority = com.example.data.model.Priority.HIGH,
            reminderTimeMillis = futureTrigger,
            taskDate = "2026-11-17"
        )
        com.example.reminder.PermissionHelper.exactAlarmPermissionOverrideForTest = true
        try {
            com.example.reminder.ReminderManager.scheduleReminder(context, scheduledTask)
        } finally {
            com.example.reminder.PermissionHelper.exactAlarmPermissionOverrideForTest = null
        }
        val am = context.getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
        val shadowAm = org.robolectric.Shadows.shadowOf(am)
        val scheduledAlarm = shadowAm.scheduledAlarms.firstOrNull { it.triggerAtTime == futureTrigger }
        assertNotNull("Expected AlarmManager.setAlarmClock to be scheduled", scheduledAlarm)
        assertNotNull("Expected AlarmClockInfo showIntent for TaskAlarmActivity", scheduledAlarm!!.showIntent)

        val shadowApp = org.robolectric.Shadows.shadowOf(context)
        while (shadowApp.nextStartedService != null) {
            // drain prior started services
        }

        val receiver = com.example.receiver.TaskReminderReceiver()
        receiver.onReceive(
            context,
            android.content.Intent(com.example.reminder.ReminderManager.ACTION_REMINDER).apply {
                putExtra(com.example.reminder.ReminderManager.EXTRA_TASK_ID, 77L)
                putExtra(com.example.reminder.ReminderManager.EXTRA_TASK_TITLE, "Morning Walk")
                putExtra(com.example.reminder.ReminderManager.EXTRA_TASK_NOTES, "6:00 AM daily walk")
                putExtra(com.example.reminder.ReminderManager.EXTRA_TASK_PRIORITY, "HIGH")
            }
        )

        val startedService = shadowApp.nextStartedService
        assertNotNull("Expected TaskAlarmService to start for background alarm", startedService)
        org.junit.Assert.assertEquals(
            com.example.service.TaskAlarmService::class.java.name,
            startedService.component?.className
        )
        org.junit.Assert.assertEquals(
            com.example.service.TaskAlarmService.ACTION_START_ALARM,
            startedService.action
        )
        assertTrue(
            "Expected PARTIAL_WAKE_LOCK to be held during alarm handoff",
            com.example.service.TaskAlarmService.isWakeLockHeld()
        )

        val nm = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val posted = org.robolectric.Shadows.shadowOf(nm).getNotification(77)
        assertNotNull(posted)
        assertNotNull("Alarm notification must attach fullScreenIntent for lock-screen UI", posted.fullScreenIntent)
        assertTrue(
            "Alarm notification must include FLAG_INSISTENT",
            (posted.flags and android.app.Notification.FLAG_INSISTENT) != 0
        )

        receiver.onReceive(
            context,
            android.content.Intent(com.example.reminder.ReminderManager.ACTION_DISMISS).apply {
                putExtra(com.example.reminder.ReminderManager.EXTRA_TASK_ID, 77L)
            }
        )
        org.junit.Assert.assertFalse(com.example.reminder.AlarmSoundManager.isPlaying())
        org.junit.Assert.assertFalse(
            "Expected PARTIAL_WAKE_LOCK to be released on dismiss",
            com.example.service.TaskAlarmService.isWakeLockHeld()
        )
    }

    @Test
    fun testTaskAlarmActivityLaunchesAndClosesOnBroadcast() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        com.example.reminder.AlarmPreferenceManager.setAlarmEnabled(context, false)

        val alarmIntent = android.content.Intent(context, com.example.ui.alarm.TaskAlarmActivity::class.java).apply {
            putExtra(com.example.reminder.ReminderManager.EXTRA_TASK_ID, 501L)
            putExtra(com.example.reminder.ReminderManager.EXTRA_TASK_TITLE, "Client Meeting Alarm")
            putExtra(com.example.reminder.ReminderManager.EXTRA_TASK_NOTES, "Join call on time")
            putExtra(com.example.reminder.ReminderManager.EXTRA_TASK_PRIORITY, "HIGH")
        }

        ActivityScenario.launch<com.example.ui.alarm.TaskAlarmActivity>(alarmIntent).use { scenario ->
            scenario.onActivity { activity ->
                assertNotNull(activity)
                org.junit.Assert.assertFalse(activity.isFinishing)
            }

            context.sendBroadcast(
                android.content.Intent(com.example.ui.alarm.TaskAlarmActivity.ACTION_CLOSE_ALARM_SCREEN).apply {
                    setPackage(context.packageName)
                    putExtra(com.example.reminder.ReminderManager.EXTRA_TASK_ID, 501L)
                }
            )
            org.robolectric.shadows.ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

            scenario.onActivity { activity ->
                assertTrue(activity.isFinishing)
            }
        }
    }

    @Test
    fun testRepeatingDailyWeeklyMonthlyAutoScheduleAndPreserveExactCadence() = kotlinx.coroutines.runBlocking {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val db = com.example.data.local.TaskDatabase.getDatabase(app)
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            db.clearAllTables()
        }
        val repo = com.example.data.repository.TaskRepository(app, db.taskDao())

        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val baseCal = java.util.Calendar.getInstance().apply {
            set(2026, java.util.Calendar.NOVEMBER, 10, 6, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val baseDate = sdf.format(baseCal.time) // 2026-11-10
        val reminderAt6Am = baseCal.timeInMillis

        val dailyId = db.taskDao().insertTask(
            com.example.data.model.TaskItem(
                title = "Morning Walk",
                taskDate = baseDate,
                reminderTimeMillis = reminderAt6Am,
                repeatMode = com.example.data.model.RepeatMode.DAILY
            )
        )
        db.taskDao().insertTask(
            com.example.data.model.TaskItem(
                title = "Weekly Review",
                taskDate = baseDate,
                reminderTimeMillis = reminderAt6Am,
                repeatMode = com.example.data.model.RepeatMode.WEEKLY
            )
        )
        db.taskDao().insertTask(
            com.example.data.model.TaskItem(
                title = "Monthly Rent",
                taskDate = baseDate,
                reminderTimeMillis = reminderAt6Am,
                repeatMode = com.example.data.model.RepeatMode.MONTHLY
            )
        )

        // 1. On 2026-11-11 (one day later): Daily rolls onto 2026-11-11, while Weekly advances cleanly to 2026-11-17 (not 11-11!)
        val nov11Noon = java.util.Calendar.getInstance().apply {
            set(2026, java.util.Calendar.NOVEMBER, 11, 12, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        repo.syncRepeatingTasksForToday("2026-11-11", nowMillis = nov11Noon)

        assertNotNull(db.taskDao().getTaskByDateAndTitle("2026-11-11", "Morning Walk"))
        // Since 6:00 AM on Nov 11 already passed at Nov 11 Noon, Nov 12 Daily occurrence must also be pre-armed
        assertNotNull(db.taskDao().getTaskByDateAndTitle("2026-11-12", "Morning Walk"))
        org.junit.Assert.assertNull(db.taskDao().getTaskByDateAndTitle("2026-11-11", "Weekly Review"))
        assertNotNull(db.taskDao().getTaskByDateAndTitle("2026-11-17", "Weekly Review"))
        org.junit.Assert.assertNull(db.taskDao().getTaskByDateAndTitle("2026-11-11", "Monthly Rent"))
        assertNotNull(db.taskDao().getTaskByDateAndTitle("2026-12-10", "Monthly Rent"))

        // 2. Simulating reminder trigger for a repeating task schedules next cycle automatically without duplicates
        val insertedDaily = db.taskDao().getTaskById(dailyId)!!
        repo.ensureNextRepeatingOccurrenceScheduled(insertedDaily, nowMillis = reminderAt6Am + 1000L)
        assertNotNull(db.taskDao().getTaskByDateAndTitle("2026-11-11", "Morning Walk"))

        // 3. Receiver handles exact alarm permission grant, package replacement, time shift, and midnight rollover
        val receiver = com.example.receiver.TaskReminderReceiver()
        listOf(
            android.app.AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
            android.content.Intent.ACTION_MY_PACKAGE_REPLACED,
            android.content.Intent.ACTION_TIME_CHANGED,
            android.content.Intent.ACTION_TIMEZONE_CHANGED,
            com.example.reminder.ReminderManager.ACTION_MIDNIGHT_ROLLOVER
        ).forEach { action ->
            receiver.onReceive(app, android.content.Intent(action))
        }
    }

    @Test
    fun testStaleMissedTaskDoesNotTriggerInAppAlarmAndHabitAlarmIsPreserved() = kotlinx.coroutines.runBlocking {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val db = com.example.data.local.TaskDatabase.getDatabase(app)
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            db.clearAllTables()
        }

        com.example.reminder.AlarmPreferenceManager.setAlarmEnabled(app, true)
        com.example.reminder.AlarmSoundManager.stopAlarm(app)

        // 1. Insert a stale task whose reminder was 10 minutes ago (older than 2-minute trigger window)
        val tenMinutesAgo = System.currentTimeMillis() - 10 * 60 * 1000L
        val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            db.taskDao().insertTask(
                com.example.data.model.TaskItem(
                    title = "Old Missed Alarm",
                    taskDate = todayStr,
                    reminderTimeMillis = tenMinutesAgo,
                    isCompleted = false,
                    isReminderDismissed = false
                )
            )
        }

        // Start a Habit/Routine alarm BEFORE creating TaskViewModel
        com.example.reminder.AlarmSoundManager.startAlarm(app)
        assertTrue(com.example.reminder.AlarmSoundManager.isPlaying())

        val vm = TaskViewModel(app)
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        kotlinx.coroutines.delay(100)
        org.robolectric.shadows.ShadowLooper.idleMainLooper()

        // Stale (>2m) task must NOT pop up as activeReminderTask, AND active Habit/Routine alarm must keep playing
        org.junit.Assert.assertNull(vm.activeReminderTask.value)
        assertTrue(com.example.reminder.AlarmSoundManager.isPlaying())

        // Completed habit on today's date must NOT post a notification when habit alarm fires
        val habitId = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            db.habitDao().insertHabit(
                com.example.data.model.HabitItem(
                    title = "Evening Reading",
                    category = "Growth",
                    routineTime = com.example.data.model.RoutineTime.EVENING,
                    completedDates = todayStr,
                    reminderTime = "08:00 PM"
                )
            )
        }
        com.example.reminder.AlarmSoundManager.stopAlarm(app)
        val nm = app.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        nm.cancelAll()

        val receiver = com.example.receiver.TaskReminderReceiver()
        receiver.onReceive(
            app,
            android.content.Intent(com.example.reminder.RoutineReminderManager.ACTION_HABIT_REMINDER).apply {
                putExtra(com.example.reminder.RoutineReminderManager.EXTRA_ITEM_ID, habitId)
                putExtra(com.example.reminder.RoutineReminderManager.EXTRA_ITEM_TITLE, "Evening Reading")
                putExtra(com.example.reminder.RoutineReminderManager.EXTRA_ITEM_TIME, "08:00 PM")
            }
        )
        kotlinx.coroutines.delay(250)
        org.robolectric.shadows.ShadowLooper.idleMainLooper()

        org.junit.Assert.assertNull(org.robolectric.Shadows.shadowOf(nm).getNotification(50000 + habitId.toInt()))
        org.junit.Assert.assertFalse(com.example.reminder.AlarmSoundManager.isPlaying())
    }

    @Test
    fun testHabitAndRoutineUseNotificationSoundOnlyWithoutAlarmService() = kotlinx.coroutines.runBlocking {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val db = com.example.data.local.TaskDatabase.getDatabase(app)
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            db.clearAllTables()
        }

        com.example.reminder.AlarmPreferenceManager.setAlarmEnabled(app, true)
        com.example.reminder.AlarmSoundManager.stopAlarm(app)

        val shadowApp = org.robolectric.Shadows.shadowOf(app)
        while (shadowApp.nextStartedService != null) {
            // drain prior started services
        }

        val routineId = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            db.routineDao().insertRoutine(
                com.example.data.model.RoutineItem(
                    title = "Morning Hydration",
                    description = "Drink 2 glasses of water",
                    routineTime = com.example.data.model.RoutineTime.MORNING,
                    scheduledTime = "07:00 AM",
                    isEnabled = true
                )
            )
        }
        val habitId = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            db.habitDao().insertHabit(
                com.example.data.model.HabitItem(
                    title = "Stretch 10 mins",
                    category = "Health",
                    routineTime = com.example.data.model.RoutineTime.MORNING,
                    reminderTime = "07:30 AM"
                )
            )
        }

        val nm = app.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        nm.cancelAll()
        val receiver = com.example.receiver.TaskReminderReceiver()

        receiver.onReceive(
            app,
            android.content.Intent(com.example.reminder.RoutineReminderManager.ACTION_ROUTINE_REMINDER).apply {
                putExtra(com.example.reminder.RoutineReminderManager.EXTRA_ITEM_ID, routineId)
                putExtra(com.example.reminder.RoutineReminderManager.EXTRA_ITEM_TITLE, "Morning Hydration")
                putExtra(com.example.reminder.RoutineReminderManager.EXTRA_ITEM_TIME, "07:00 AM")
            }
        )
        receiver.onReceive(
            app,
            android.content.Intent(com.example.reminder.RoutineReminderManager.ACTION_HABIT_REMINDER).apply {
                putExtra(com.example.reminder.RoutineReminderManager.EXTRA_ITEM_ID, habitId)
                putExtra(com.example.reminder.RoutineReminderManager.EXTRA_ITEM_TITLE, "Stretch 10 mins")
                putExtra(com.example.reminder.RoutineReminderManager.EXTRA_ITEM_TIME, "07:30 AM")
            }
        )
        kotlinx.coroutines.delay(250)
        org.robolectric.shadows.ShadowLooper.idleMainLooper()

        val routineNotif = org.robolectric.Shadows.shadowOf(nm).getNotification(30000 + routineId.toInt())
        val habitNotif = org.robolectric.Shadows.shadowOf(nm).getNotification(50000 + habitId.toInt())
        assertNotNull(routineNotif)
        assertNotNull(habitNotif)
        org.junit.Assert.assertEquals(
            com.example.reminder.ReminderManager.CHANNEL_ID_NOTIFICATION_BAR,
            routineNotif.channelId
        )
        org.junit.Assert.assertEquals(
            com.example.reminder.ReminderManager.CHANNEL_ID_NOTIFICATION_BAR,
            habitNotif.channelId
        )
        org.junit.Assert.assertEquals(0, routineNotif.flags and android.app.Notification.FLAG_INSISTENT)
        org.junit.Assert.assertEquals(0, habitNotif.flags and android.app.Notification.FLAG_INSISTENT)
        org.junit.Assert.assertFalse(com.example.reminder.AlarmSoundManager.isPlaying())
        org.junit.Assert.assertNull(shadowApp.nextStartedService)
    }

    @Test
    fun testTaskAlarmGraceSchedulingAudibleFallbackAndDatePreservation() = kotlinx.coroutines.runBlocking {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val db = com.example.data.local.TaskDatabase.getDatabase(app)
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            db.clearAllTables()
        }

        // 1. Recent-past reminder (15s ago) must still be scheduled when saved by user (allowRecentPastGrace = true)
        val now = System.currentTimeMillis()
        val fifteenSecondsAgo = now - 15_000L
        val taskWithRecentReminder = com.example.data.model.TaskItem(
            id = 88L,
            title = "Pay Electricity Bill",
            reminderTimeMillis = fifteenSecondsAgo,
            taskDate = "2026-11-17"
        )
        val am = app.getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
        val shadowAm = org.robolectric.Shadows.shadowOf(am)
        shadowAm.scheduledAlarms.clear()

        com.example.reminder.AlarmPreferenceManager.setAlarmEnabled(app, false)
        com.example.reminder.ReminderManager.scheduleReminder(app, taskWithRecentReminder, allowRecentPastGrace = false)
        org.junit.Assert.assertTrue(
            "Reschedule without grace window should skip past reminder",
            shadowAm.scheduledAlarms.isEmpty()
        )

        com.example.reminder.ReminderManager.scheduleReminder(app, taskWithRecentReminder, allowRecentPastGrace = true)
        org.junit.Assert.assertTrue(
            "User-saved reminder within 90s grace window must schedule immediately instead of being dropped",
            shadowAm.scheduledAlarms.isNotEmpty()
        )

        // Check that notification-bar-only mode builds HIGH priority reminder on CHANNEL_ID_NOTIFICATION_BAR
        val barOnlyNotif = com.example.reminder.ReminderManager.buildTaskReminderNotification(
            context = app,
            taskId = 89L,
            title = "Water Plants",
            notes = "Balcony",
            priority = "MEDIUM",
            isAlarmActive = false
        )
        org.junit.Assert.assertEquals(
            com.example.reminder.ReminderManager.CHANNEL_ID_NOTIFICATION_BAR,
            barOnlyNotif.channelId
        )
        org.junit.Assert.assertEquals(0, barOnlyNotif.flags and android.app.Notification.FLAG_INSISTENT)

        // 2. Notification fallback: audibleAlarmNotification=true sets sound & FLAG_INSISTENT; false stays silent for TaskAlarmService
        val silentServiceNotif = com.example.reminder.ReminderManager.buildTaskReminderNotification(
            context = app,
            taskId = 88L,
            title = "Pay Electricity Bill",
            notes = "",
            priority = "HIGH",
            isAlarmActive = true,
            audibleAlarmNotification = false
        )
        val audibleFallbackNotif = com.example.reminder.ReminderManager.buildTaskReminderNotification(
            context = app,
            taskId = 88L,
            title = "Pay Electricity Bill",
            notes = "",
            priority = "HIGH",
            isAlarmActive = true,
            audibleAlarmNotification = true
        )
        org.junit.Assert.assertTrue(
            (silentServiceNotif.flags and android.app.Notification.FLAG_ONLY_ALERT_ONCE) != 0
        )
        org.junit.Assert.assertEquals(
            0,
            audibleFallbackNotif.flags and android.app.Notification.FLAG_ONLY_ALERT_ONCE
        )
        org.junit.Assert.assertEquals(
            com.example.reminder.ReminderManager.CHANNEL_ID_ALARM,
            audibleFallbackNotif.channelId
        )
        val nm = app.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val alarmChannel = nm.getNotificationChannel(com.example.reminder.ReminderManager.CHANNEL_ID_ALARM)
        assertNotNull(alarmChannel)
        org.junit.Assert.assertEquals(
            com.example.reminder.ReminderManager.getAlarmSoundUri(app),
            alarmChannel.sound
        )
        org.junit.Assert.assertTrue((audibleFallbackNotif.flags and android.app.Notification.FLAG_INSISTENT) != 0)

        // 3. Editing an existing repeating task preserves its original taskDate instead of overwriting with today's selectedDate
        val originalTaskId = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            db.taskDao().insertTask(
                com.example.data.model.TaskItem(
                    title = "Weekly Sync",
                    taskDate = "2026-11-24",
                    repeatMode = com.example.data.model.RepeatMode.WEEKLY,
                    reminderTimeMillis = now + 3_600_000L
                )
            )
        }
        val originalTask = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            db.taskDao().getTaskById(originalTaskId)!!
        }
        val vm = TaskViewModel(app)
        vm.onDateSelected("2026-11-17")
        vm.openEditTask(originalTask)
        vm.saveTask(
            id = originalTask.id,
            title = "Weekly Sync Updated",
            notes = "Updated notes",
            priority = com.example.data.model.Priority.HIGH,
            category = originalTask.category,
            reminderTimeMillis = originalTask.reminderTimeMillis,
            imageUri = null,
            repeatMode = com.example.data.model.RepeatMode.WEEKLY
        )
        val deadline = System.currentTimeMillis() + 3_000L
        while (System.currentTimeMillis() < deadline) {
            val updated = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                db.taskDao().getTaskById(originalTaskId)
            }
            if (updated?.title == "Weekly Sync Updated") break
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            kotlinx.coroutines.delay(25)
        }
        val finalTask = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            db.taskDao().getTaskById(originalTaskId)!!
        }
        org.junit.Assert.assertEquals("2026-11-24", finalTask.taskDate)
    }

    @Test
    fun testQuickbootLocaleSafetyAndReceiverWakeLockCleanup() = kotlinx.coroutines.runBlocking {
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        val db = com.example.data.local.TaskDatabase.getDatabase(app)
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            db.clearAllTables()
        }

        // 1. Verify QUICKBOOT_POWERON triggers reschedule safely and releases receiver WakeLock
        val receiver = com.example.receiver.TaskReminderReceiver()
        listOf(
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON"
        ).forEach { action ->
            receiver.onReceive(app, android.content.Intent(action))
        }

        // 2. Verify Daily notification posts VISIBILITY_PUBLIC + CATEGORY_REMINDER and releases WakeLock
        val nm = app.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        nm.cancelAll()
        receiver.onReceive(
            app,
            android.content.Intent(com.example.reminder.DailyReminderManager.ACTION_DAILY_REMINDER).apply {
                putExtra(
                    com.example.reminder.DailyReminderManager.EXTRA_PERIOD,
                    com.example.reminder.DailyReminderManager.PERIOD_EVENING
                )
            }
        )
        val dailyNotif = org.robolectric.Shadows.shadowOf(nm)
            .getNotification(com.example.reminder.DailyReminderManager.REQUEST_CODE_EVENING)
        assertNotNull(dailyNotif)
        org.junit.Assert.assertEquals(
            android.app.Notification.VISIBILITY_PUBLIC,
            dailyNotif.visibility
        )
        org.junit.Assert.assertEquals(
            android.app.Notification.CATEGORY_REMINDER,
            dailyNotif.category
        )

        // 3. Verify time formatting/parsing remains valid under non-Latin-digit default locale
        val originalLocale = java.util.Locale.getDefault()
        try {
            java.util.Locale.setDefault(java.util.Locale("ar", "EG"))
            val formatted = String.format(java.util.Locale.ENGLISH, "%02d:%02d %s", 7, 5, "PM")
            val parsed = com.example.reminder.RoutineReminderManager.parseTimeHourMinute(formatted)
            assertNotNull(parsed)
            org.junit.Assert.assertEquals(19, parsed!!.first)
            org.junit.Assert.assertEquals(5, parsed.second)
        } finally {
            java.util.Locale.setDefault(originalLocale)
        }

        // Wait for async QUICKBOOT_POWERON reschedule coroutines to settle cleanly before isolated Step 4 assertions
        val preDeadline = System.currentTimeMillis() + 3_000L
        while (com.example.receiver.TaskReminderReceiver.activeReceiverWakeLockCountForTest() > 0 &&
            System.currentTimeMillis() < preDeadline
        ) {
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            kotlinx.coroutines.delay(25)
        }

        // 4. Verify Notification-bar-only tasks, Routines, and Daily reminders schedule Doze-exempt setAlarmClock
        val am = app.getSystemService(android.content.Context.ALARM_SERVICE) as android.app.AlarmManager
        val shadowAm = org.robolectric.Shadows.shadowOf(am)
        com.example.reminder.PermissionHelper.exactAlarmPermissionOverrideForTest = true
        try {
            com.example.reminder.AlarmPreferenceManager.setAlarmEnabled(app, false)
            val futureTrigger = System.currentTimeMillis() + 120_000L
            com.example.reminder.ReminderManager.scheduleReminder(
                app,
                com.example.data.model.TaskItem(
                    id = 910L,
                    title = "Notify Only Task",
                    reminderTimeMillis = futureTrigger,
                    taskDate = "2026-11-17"
                )
            )
            val notifyClock = shadowAm.scheduledAlarms.firstOrNull { it.triggerAtTime == futureTrigger }
            assertNotNull("Expected notification-only task to use setAlarmClock", notifyClock)
            assertNotNull("Expected AlarmClockInfo showIntent for notification-only task", notifyClock!!.showIntent)

            val routineCountBefore = shadowAm.scheduledAlarms.count { it.showIntent != null }
            com.example.reminder.RoutineReminderManager.scheduleRoutine(
                app,
                com.example.data.model.RoutineItem(
                    id = 911L,
                    title = "Morning Stretches",
                    routineTime = com.example.data.model.RoutineTime.MORNING,
                    scheduledTime = "07:15 AM",
                    isEnabled = true
                )
            )
            val routineCountAfter = shadowAm.scheduledAlarms.count { it.showIntent != null }
            org.junit.Assert.assertEquals(routineCountBefore + 1, routineCountAfter)

            val dailyCountBefore = routineCountAfter
            com.example.reminder.DailyReminderManager.scheduleAllDailyReminders(app)
            org.junit.Assert.assertEquals(
                dailyCountBefore + 3,
                shadowAm.scheduledAlarms.count { it.showIntent != null }
            )
        } finally {
            com.example.reminder.PermissionHelper.exactAlarmPermissionOverrideForTest = null
        }

        val deadline = System.currentTimeMillis() + 3_000L
        while (com.example.receiver.TaskReminderReceiver.activeReceiverWakeLockCountForTest() > 0 &&
            System.currentTimeMillis() < deadline
        ) {
            org.robolectric.shadows.ShadowLooper.idleMainLooper()
            kotlinx.coroutines.delay(25)
        }
        org.junit.Assert.assertEquals(
            0,
            com.example.receiver.TaskReminderReceiver.activeReceiverWakeLockCountForTest()
        )
    }
}
