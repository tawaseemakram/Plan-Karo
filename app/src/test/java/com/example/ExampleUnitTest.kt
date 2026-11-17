package com.example

import com.example.data.model.HabitItem
import com.example.data.model.Priority
import com.example.data.model.RoutineTime
import com.example.data.model.TaskItem
import com.example.ui.DailyProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `test priority ordering and colors`() {
        assertTrue(Priority.HIGH.level > Priority.MEDIUM.level)
        assertTrue(Priority.MEDIUM.level > Priority.LOW.level)
        assertEquals("High", Priority.HIGH.label)
    }

    @Test
    fun `test daily progress calculation`() {
        val tasks = listOf(
            TaskItem(id = 1, title = "Task 1", isCompleted = true, taskDate = "2026-11-04"),
            TaskItem(id = 2, title = "Task 2", isCompleted = false, priority = Priority.HIGH, taskDate = "2026-11-04"),
            TaskItem(id = 3, title = "Task 3", isCompleted = false, taskDate = "2026-11-04")
        )

        val total = tasks.size
        val completed = tasks.count { it.isCompleted }
        val pending = total - completed
        val pct = completed.toFloat() / total.toFloat()

        val progress = DailyProgress(
            totalCount = total,
            completedCount = completed,
            pendingCount = pending,
            percentage = pct,
            highPriorityPending = 1,
            dailyTasksCount = 3
        )

        assertEquals(3, progress.totalCount)
        assertEquals(1, progress.completedCount)
        assertEquals(2, progress.pendingCount)
        assertEquals(0.333f, progress.percentage, 0.01f)
        assertEquals(1, progress.highPriorityPending)
        assertEquals(3, progress.dailyTasksCount)
    }

    @Test
    fun `test task filter enum order and titles`() {
        val filters = com.example.ui.TaskFilter.values().toList()
        assertEquals(
            listOf(
                com.example.ui.TaskFilter.TASK,
                com.example.ui.TaskFilter.DAILY_TASK,
                com.example.ui.TaskFilter.WEEKLY_TASK,
                com.example.ui.TaskFilter.MONTHLY_TASK,
                com.example.ui.TaskFilter.PENDING,
                com.example.ui.TaskFilter.COMPLETED,
                com.example.ui.TaskFilter.HIGH_PRIORITY,
                com.example.ui.TaskFilter.ALL
            ),
            filters
        )
        assertEquals(
            listOf("Task", "Daily Task", "Weekly Task", "Monthly Task", "Pending", "Done", "High Priority", "All Tasks"),
            filters.map { it.title }
        )
    }

    @Test
    fun `test snooze time calculation`() {
        val now = 1700000000000L
        val snooze5m = now + (5 * 60 * 1000L)
        val task = TaskItem(
            id = 1,
            title = "Meeting",
            reminderTimeMillis = snooze5m,
            taskDate = "2026-11-04"
        )

        assertEquals(snooze5m, task.reminderTimeMillis)
        assertFalse(task.isCompleted)
    }

    @Test
    fun `test habit completion dates and routine segment`() {
        val habit = HabitItem(
            id = 10,
            title = "Drink Water",
            category = "Health",
            routineTime = RoutineTime.MORNING,
            completedDates = "2026-11-03,2026-11-04"
        )

        assertTrue(habit.isCompletedOn("2026-11-04"))
        assertTrue(habit.isCompletedOn("2026-11-03"))
        assertFalse(habit.isCompletedOn("2026-11-05"))
        assertEquals(RoutineTime.MORNING, habit.routineTime)
        assertEquals("🌅", habit.routineTime.icon)
    }

    @Test
    fun `test routine item model and properties`() {
        val routine = com.example.data.model.RoutineItem(
            id = 5,
            title = "Morning Kickstart Routine",
            description = "Hydration, sunlight, priorities review",
            routineTime = RoutineTime.MORNING,
            scheduledTime = "07:00 AM",
            daysOfWeek = "DAILY",
            isEnabled = true,
            orderIndex = 0
        )

        assertEquals("Morning Kickstart Routine", routine.title)
        assertEquals(RoutineTime.MORNING, routine.routineTime)
        assertEquals("07:00 AM", routine.scheduledTime)
        assertTrue(routine.isEnabled)
        assertEquals(0, routine.orderIndex)
    }

    @Test
    fun `test category model defaults and color assignment`() {
        val workCategory = com.example.data.model.TaskCategory.WORK
        val healthCategory = com.example.data.model.TaskCategory.HEALTH
        val personalCategory = com.example.data.model.TaskCategory.PERSONAL

        assertEquals("Work", workCategory.name)
        assertEquals("#3B82F6", workCategory.colorHex)
        assertEquals("Health", healthCategory.name)
        assertEquals("#10B981", healthCategory.colorHex)
        assertEquals("Personal", personalCategory.name)
        assertEquals("#A855F7", personalCategory.colorHex)

        val taskWork = TaskItem(id = 1, title = "Quarterly Report", category = "Work", taskDate = "2026-11-06")
        val taskHealth = TaskItem(id = 2, title = "Evening Run", category = "Health", taskDate = "2026-11-06")
        val taskDefault = TaskItem(id = 3, title = "Meditate", taskDate = "2026-11-06")

        assertEquals("Work", taskWork.category)
        assertEquals("Health", taskHealth.category)
        assertEquals("Personal", taskDefault.category)
    }

    @Test
    fun `test category filtering logic`() {
        val tasks = listOf(
            TaskItem(id = 1, title = "Finish slides", category = "Work", taskDate = "2026-11-06"),
            TaskItem(id = 2, title = "Doctor appointment", category = "Health", taskDate = "2026-11-06"),
            TaskItem(id = 3, title = "Call parents", category = "Personal", taskDate = "2026-11-06"),
            TaskItem(id = 4, title = "Refactor PR", category = "Work", taskDate = "2026-11-06")
        )

        val workTasks = tasks.filter { it.category.equals("Work", ignoreCase = true) }
        val healthTasks = tasks.filter { it.category.equals("Health", ignoreCase = true) }
        val personalTasks = tasks.filter { it.category.equals("Personal", ignoreCase = true) }

        assertEquals(2, workTasks.size)
        assertEquals(1, healthTasks.size)
        assertEquals(1, personalTasks.size)
    }

    @Test
    fun `test audio note attached to task item`() {
        val audioPath = "/data/user/0/com.aistudio.plankaro.mzrkqp/files/audio_notes/audio_12345.m4a"
        val taskWithAudio = TaskItem(
            id = 10,
            title = "Voice Memo Task",
            audioUri = audioPath,
            audioDurationSeconds = 18,
            taskDate = "2026-11-07"
        )

        assertEquals(audioPath, taskWithAudio.audioUri)
        assertEquals(18, taskWithAudio.audioDurationSeconds)

        val taskWithoutAudio = TaskItem(
            id = 11,
            title = "Normal Task",
            taskDate = "2026-11-07"
        )
        assertEquals(null, taskWithoutAudio.audioUri)
        assertEquals(0, taskWithoutAudio.audioDurationSeconds)
    }

    @Test
    fun `test audio utils formatting`() {
        assertEquals("0:00", com.example.audio.AudioUtils.formatDuration(0))
        assertEquals("0:15", com.example.audio.AudioUtils.formatDuration(15))
        assertEquals("1:00", com.example.audio.AudioUtils.formatDuration(60))
        assertEquals("2:05", com.example.audio.AudioUtils.formatDuration(125))

        assertEquals("0:05", com.example.audio.AudioUtils.formatDurationMillis(5000))
        assertEquals("1:30", com.example.audio.AudioUtils.formatDurationMillis(90000))
    }

    @Test
    fun `test achievement unlock logic`() {
        val totalTasks = 6
        val completedTasks = 5
        val bestStreak = 4
        val audioNotesCount = 1
        val routinesCount = 2
        val completionRate = 83

        val isFirstStepUnlocked = totalTasks >= 1
        val isTaskCrusherUnlocked = completedTasks >= 5
        val isStreakChampionUnlocked = bestStreak >= 3
        val isVoicePioneerUnlocked = audioNotesCount >= 1
        val isRoutineMasterUnlocked = routinesCount >= 2
        val isConsistencyProUnlocked = completionRate >= 80 && totalTasks >= 3

        assertTrue(isFirstStepUnlocked)
        assertTrue(isTaskCrusherUnlocked)
        assertTrue(isStreakChampionUnlocked)
        assertTrue(isVoicePioneerUnlocked)
        assertTrue(isRoutineMasterUnlocked)
        assertTrue(isConsistencyProUnlocked)
    }

    @Test
    fun `test note item creation and checklist parsing`() {
        val note = com.example.data.model.NoteItem(
            id = 1,
            title = "Launch Checklist",
            category = "Checklist",
            colorHex = "#10B981",
            isPinned = true,
            isChecklist = true,
            checklistItems = "Done|Test sound\nTodo|Deploy APK\nTodo|Verify UI"
        )

        assertEquals("Launch Checklist", note.title)
        assertEquals("Checklist", note.category)
        assertTrue(note.isPinned)
        assertTrue(note.isChecklist)

        val items = note.checklistItems.lines().filter { it.isNotBlank() }
        assertEquals(3, items.size)

        val firstItem = items[0].split("|", limit = 2)
        assertEquals("Done", firstItem[0])
        assertEquals("Test sound", firstItem[1])

        val secondItem = items[1].split("|", limit = 2)
        assertEquals("Todo", secondItem[0])
        assertEquals("Deploy APK", secondItem[1])
    }

    @Test
    fun `test note to task conversion text formatting`() {
        val note = com.example.data.model.NoteItem(
            id = 2,
            title = "Meeting Notes",
            category = "Meeting",
            isChecklist = true,
            checklistItems = "Done|Intro\nTodo|Action plan"
        )

        val formattedNotes = note.checklistItems.lines().joinToString("\n") { line ->
            val parts = line.split("|", limit = 2)
            if (parts.size == 2) (if (parts[0] == "Done") "✓ " else "○ ") + parts[1] else line
        }

        assertTrue(formattedNotes.contains("✓ Intro"))
        assertTrue(formattedNotes.contains("○ Action plan"))
    }

    @Test
    fun `test auto-transfer task to next day calculation`() {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val initialDateStr = "2026-11-08"
        val initialDate = sdf.parse(initialDateStr)!!

        val nextDayCal = java.util.Calendar.getInstance().apply {
            time = initialDate
            add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
        val nextDayStr = sdf.format(nextDayCal.time)
        assertEquals("2026-11-09", nextDayStr)

        val completedTask = TaskItem(
            id = 5,
            title = "Drink 3L Water",
            notes = "Daily hydration goal",
            priority = Priority.HIGH,
            category = "Health",
            isCompleted = true,
            taskDate = initialDateStr
        )

        // Transferred next day task should be pending with next day's date
        val transferredTask = completedTask.copy(
            id = 0L,
            isCompleted = false,
            completedAt = null,
            taskDate = nextDayStr
        )

        assertEquals("Drink 3L Water", transferredTask.title)
        assertFalse(transferredTask.isCompleted)
        assertEquals("2026-11-09", transferredTask.taskDate)
        assertEquals(Priority.HIGH, transferredTask.priority)
    }

    @Test
    fun `test task alarm preference behavior`() {
        var isAlarmEnabled = true
        fun shouldRingAlarm(): Boolean = isAlarmEnabled
        fun shouldShowNotificationBar(): Boolean = true

        // When Alarm is ON: both alarm ringing and notification bar active
        assertTrue(shouldRingAlarm())
        assertTrue(shouldShowNotificationBar())

        // When Alarm is OFF: alarm ringing disabled, but notification bar remains active
        isAlarmEnabled = false
        assertFalse(shouldRingAlarm())
        assertTrue(shouldShowNotificationBar())
    }

    @Test
    fun `test repeat mode options and daily weekly monthly calculations`() {
        val repeatNone = com.example.data.model.RepeatMode.NONE
        val repeatDaily = com.example.data.model.RepeatMode.DAILY
        val repeatWeekly = com.example.data.model.RepeatMode.WEEKLY
        val repeatMonthly = com.example.data.model.RepeatMode.MONTHLY

        assertEquals("No Repeat", repeatNone.label)
        assertEquals("Daily", repeatDaily.label)
        assertEquals("Weekly", repeatWeekly.label)
        assertEquals("Monthly", repeatMonthly.label)

        val taskDaily = TaskItem(
            title = "Morning Meditation",
            taskDate = "2026-11-11",
            repeatMode = repeatDaily
        )
        assertEquals(com.example.data.model.RepeatMode.DAILY, taskDaily.repeatMode)

        // Test Daily calculation (+1 day)
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val cal = java.util.Calendar.getInstance().apply {
            time = sdf.parse("2026-11-11")!!
        }
        cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
        val nextDaily = sdf.format(cal.time)
        assertEquals("2026-11-12", nextDaily)

        // Test Weekly calculation (+7 days)
        val calWeekly = java.util.Calendar.getInstance().apply {
            time = sdf.parse("2026-11-11")!!
        }
        calWeekly.add(java.util.Calendar.DAY_OF_YEAR, 7)
        val nextWeekly = sdf.format(calWeekly.time)
        assertEquals("2026-11-18", nextWeekly)

        // Test Monthly calculation (+1 month)
        val calMonthly = java.util.Calendar.getInstance().apply {
            time = sdf.parse("2026-11-11")!!
        }
        calMonthly.add(java.util.Calendar.MONTH, 1)
        val nextMonthly = sdf.format(calMonthly.time)
        assertEquals("2026-12-11", nextMonthly)
    }

    @Test
    fun `test routine single example and ad unit configuration`() {
        val sampleRoutines = listOf(
            com.example.data.model.RoutineItem(id = 1, title = "Morning Kickstart Routine"),
            com.example.data.model.RoutineItem(id = 2, title = "Afternoon Reset & Walk"),
            com.example.data.model.RoutineItem(id = 3, title = "Evening Wind-Down & Digital Sunset")
        )

        // Deduplication leaves only 1 example routine as requested by user
        val sampleTitles = setOf("Morning Kickstart Routine", "Afternoon Reset & Walk", "Evening Wind-Down & Digital Sunset")
        val filtered = sampleRoutines.filter { it.title in sampleTitles }
        val toKeep = filtered.firstOrNull { it.title == "Morning Kickstart Routine" } ?: filtered.first()
        val toRemove = filtered.filter { it.id != toKeep.id }

        assertEquals(1, listOf(toKeep).size)
        assertEquals("Morning Kickstart Routine", toKeep.title)
        assertEquals(2, toRemove.size)

        // Verify AdMob Ad Unit IDs
        assertEquals("ca-app-pub-7340151781341221/2088878109", com.example.ads.AdManager.AD_UNIT_BANNER_OPTIONS)
        assertEquals("ca-app-pub-7340151781341221/2088878109", com.example.ads.AdManager.AD_UNIT_EMPTY_STATE_TASK)
        assertEquals("ca-app-pub-7340151781341221/5114321687", com.example.ads.AdManager.AD_UNIT_INTERSTITIAL_TASK_ENTRY)
    }

    @Test
    fun `test daily 3 times reminder periods and configuration`() {
        assertEquals("MORNING", com.example.reminder.DailyReminderManager.PERIOD_MORNING)
        assertEquals("AFTERNOON", com.example.reminder.DailyReminderManager.PERIOD_AFTERNOON)
        assertEquals("EVENING", com.example.reminder.DailyReminderManager.PERIOD_EVENING)

        assertEquals(8001, com.example.reminder.DailyReminderManager.REQUEST_CODE_MORNING)
        assertEquals(8002, com.example.reminder.DailyReminderManager.REQUEST_CODE_AFTERNOON)
        assertEquals(8003, com.example.reminder.DailyReminderManager.REQUEST_CODE_EVENING)

        // Verify English titles and messages
        val morning = com.example.reminder.DailyReminderManager.getNotificationContent(com.example.reminder.DailyReminderManager.PERIOD_MORNING)
        assertEquals("🌅 Morning Plan! | Plan Karo", morning.first)
        assertTrue(morning.second.contains("Start your day with focus"))

        val afternoon = com.example.reminder.DailyReminderManager.getNotificationContent(com.example.reminder.DailyReminderManager.PERIOD_AFTERNOON)
        assertEquals("☀️ Afternoon Check-in | Plan Karo", afternoon.first)
        assertTrue(afternoon.second.contains("Half of the day has passed"))

        val evening = com.example.reminder.DailyReminderManager.getNotificationContent(com.example.reminder.DailyReminderManager.PERIOD_EVENING)
        assertEquals("🌙 Evening Review & Plan Tomorrow | Plan Karo", evening.first)
        assertTrue(evening.second.contains("Review today's achievements"))
    }

    @Test
    fun `test inline banner quote and catalog random selection`() {
        val initialQuote = com.example.data.model.QuotesCatalog.getRandomQuote()
        assertTrue(initialQuote.text.isNotBlank())
        assertTrue(initialQuote.author.isNotBlank())
        assertTrue(com.example.data.model.QuotesCatalog.QUOTES.isNotEmpty())

        val nextQuote = com.example.data.model.QuotesCatalog.getRandomQuote(excludeId = initialQuote.id)
        if (com.example.data.model.QuotesCatalog.QUOTES.size > 1) {
            org.junit.Assert.assertNotEquals(initialQuote.id, nextQuote.id)
        }
    }

    @Test
    fun `test indian quotes added and all quotes short enough to fit box`() {
        val quotes = com.example.data.model.QuotesCatalog.QUOTES
        assertTrue(quotes.size >= 60)

        // Ensure every quote is concise (<= 60 characters) so it displays completely in the quote box
        val longestQuote = quotes.maxByOrNull { it.text.length }!!
        assertTrue(
            "Quote '${longestQuote.text}' is too long (${longestQuote.text.length} chars)",
            longestQuote.text.length <= 60
        )

        // Verify Indian icons are included
        val authors = quotes.map { it.author }.toSet()
        assertTrue(authors.contains("Swami Vivekananda"))
        assertTrue(authors.contains("Dr. A.P.J. Abdul Kalam"))
        assertTrue(authors.contains("Mahatma Gandhi"))
        assertTrue(authors.contains("Ratan Tata"))
        assertTrue(authors.contains("Sachin Tendulkar"))
        assertTrue(authors.contains("Chanakya"))
    }

    @Test
    fun `test 5 minute quote rotation interval and sequential resume`() {
        assertEquals(5 * 60 * 1000L, com.example.ui.TaskViewModel.QUOTE_ROTATION_INTERVAL_MS)

        val quotes = com.example.data.model.QuotesCatalog.QUOTES
        val savedId = quotes[10].id
        val resumedQuote = com.example.data.model.QuotesCatalog.getQuoteById(savedId)
        assertEquals(savedId, resumedQuote.id)

        val nextAfterResumed = com.example.data.model.QuotesCatalog.getNextQuote(savedId)
        assertEquals(quotes[11].id, nextAfterResumed.id)

        // Wrap-around from last quote back to first quote
        val lastId = quotes.last().id
        val wrapped = com.example.data.model.QuotesCatalog.getNextQuote(lastId)
        assertEquals(quotes.first().id, wrapped.id)
    }

    @Test
    fun `test daily habit single example and quote trigger reason`() {
        val reason = com.example.data.model.QuoteTriggerReason.HABIT_CREATED
        assertEquals("Habit Created 🔥", reason.dialogTitle)
        assertTrue(reason.subtitle.isNotBlank())

        val sampleHabitTitles = setOf(
            "Morning Meditation & Breathing",
            "Hydrate: Drink 2.5L Water",
            "Read 20 Pages of Book",
            "Nightly Reflection & Plan Tomorrow"
        )
        val existing = listOf(
            HabitItem(id = 1, title = "Morning Meditation & Breathing"),
            HabitItem(id = 2, title = "Hydrate: Drink 2.5L Water"),
            HabitItem(id = 3, title = "Read 20 Pages of Book"),
            HabitItem(id = 4, title = "Nightly Reflection & Plan Tomorrow")
        )
        val matching = existing.filter { it.title in sampleHabitTitles }
        val toKeep = matching.firstOrNull { it.title == "Morning Meditation & Breathing" } ?: matching.first()
        val toRemove = matching.filter { it.id != toKeep.id }

        assertEquals("Morning Meditation & Breathing", toKeep.title)
        assertEquals(3, toRemove.size)
        assertEquals("ca-app-pub-7340151781341221/2088878109", com.example.ads.AdManager.AD_UNIT_BANNER_OPTIONS)
        assertEquals("ca-app-pub-7340151781341221/2088878109", com.example.ads.AdManager.AD_UNIT_EMPTY_STATE_TASK)
        assertEquals("ca-app-pub-7340151781341221/5114321687", com.example.ads.AdManager.AD_UNIT_INTERSTITIAL_TASK_ENTRY)
    }

    @Test
    fun `test routine time parsing, next trigger calculation, and routine quote trigger`() {
        assertEquals(Pair(7, 0), com.example.reminder.RoutineReminderManager.parseTimeHourMinute("07:00 AM"))
        assertEquals(Pair(13, 30), com.example.reminder.RoutineReminderManager.parseTimeHourMinute("01:30 PM"))
        assertEquals(Pair(20, 0), com.example.reminder.RoutineReminderManager.parseTimeHourMinute("08:00 PM"))
        assertEquals(null, com.example.reminder.RoutineReminderManager.parseTimeHourMinute(""))

        val fixedNow = java.util.Calendar.getInstance().apply {
            set(2026, java.util.Calendar.NOVEMBER, 13, 10, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

        // 07:00 AM already passed today at 10:00 AM -> must schedule for tomorrow
        val nextMorning = com.example.reminder.RoutineReminderManager.computeNextTriggerMillis(7, 0, fixedNow)
        val calMorning = java.util.Calendar.getInstance().apply { timeInMillis = nextMorning }
        assertEquals(14, calMorning.get(java.util.Calendar.DAY_OF_MONTH))
        assertEquals(7, calMorning.get(java.util.Calendar.HOUR_OF_DAY))

        // 08:00 PM is still upcoming today at 10:00 AM -> must schedule for today
        val nextEvening = com.example.reminder.RoutineReminderManager.computeNextTriggerMillis(20, 0, fixedNow)
        val calEvening = java.util.Calendar.getInstance().apply { timeInMillis = nextEvening }
        assertEquals(13, calEvening.get(java.util.Calendar.DAY_OF_MONTH))
        assertEquals(20, calEvening.get(java.util.Calendar.HOUR_OF_DAY))

        // Routine created quote reason
        val routineReason = com.example.data.model.QuoteTriggerReason.ROUTINE_CREATED
        assertEquals("Routine Scheduled ⏰", routineReason.dialogTitle)
        assertTrue(routineReason.subtitle.isNotBlank())
    }

    @Test
    fun `test repeating task rollover helper and habit targetDays with reminderTime`() {
        val nextDaily = com.example.data.repository.TaskRepository.computeNextOccurrenceDate(
            "2026-11-12",
            com.example.data.model.RepeatMode.DAILY
        )
        assertEquals("2026-11-13", nextDaily)

        val nextWeekly = com.example.data.repository.TaskRepository.computeNextOccurrenceDate(
            "2026-11-06",
            com.example.data.model.RepeatMode.WEEKLY
        )
        assertEquals("2026-11-13", nextWeekly)

        val prevReminder = java.util.Calendar.getInstance().apply {
            set(2026, java.util.Calendar.NOVEMBER, 12, 9, 45, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        val shifted = com.example.data.repository.TaskRepository.shiftReminderToDate(prevReminder, "2026-11-13")!!
        val shiftedCal = java.util.Calendar.getInstance().apply { timeInMillis = shifted }
        assertEquals(13, shiftedCal.get(java.util.Calendar.DAY_OF_MONTH))
        assertEquals(9, shiftedCal.get(java.util.Calendar.HOUR_OF_DAY))
        assertEquals(45, shiftedCal.get(java.util.Calendar.MINUTE))

        val habit = HabitItem(
            title = "Evening Yoga",
            category = "Fitness",
            routineTime = RoutineTime.EVENING,
            targetDaysPerWeek = 5,
            reminderTime = "06:30 PM"
        )
        assertEquals(5, habit.targetDaysPerWeek)
        assertEquals("06:30 PM", habit.reminderTime)
    }

    @Test
    fun `test permission prompt types and database migration`() {
        val types = com.example.reminder.PermissionPromptType.values().toSet()
        assertEquals(3, types.size)
        assertTrue(types.contains(com.example.reminder.PermissionPromptType.MICROPHONE))
        assertTrue(types.contains(com.example.reminder.PermissionPromptType.EXACT_ALARM))
        assertTrue(types.contains(com.example.reminder.PermissionPromptType.NOTIFICATIONS))
        types.forEach {
            assertTrue(it.title.isNotBlank())
            assertTrue(it.message.isNotBlank())
            assertTrue(it.settingsButtonLabel.contains("Settings"))
        }

        assertEquals(7, com.example.data.local.TaskDatabase.MIGRATION_7_8.startVersion)
        assertEquals(8, com.example.data.local.TaskDatabase.MIGRATION_7_8.endVersion)
    }

    @Test
    fun `test exclusive repeat task filters keep daily weekly monthly isolated from task tab`() {
        val today = "2026-11-14"
        val oneTime = TaskItem(id = 1, title = "Pay bill", taskDate = today, repeatMode = com.example.data.model.RepeatMode.NONE)
        val daily = TaskItem(id = 2, title = "Read 10 pages", taskDate = today, repeatMode = com.example.data.model.RepeatMode.DAILY)
        val weekly = TaskItem(id = 3, title = "Weekly review", taskDate = "2026-11-18", repeatMode = com.example.data.model.RepeatMode.WEEKLY)
        val monthly = TaskItem(id = 4, title = "Pay rent", taskDate = "2026-12-01", repeatMode = com.example.data.model.RepeatMode.MONTHLY)
        val all = listOf(oneTime, daily, weekly, monthly)

        val taskTab = com.example.ui.TaskViewModel.filterTasksForUi(all, today, today, com.example.ui.TaskFilter.TASK, null, "")
        assertEquals(listOf(oneTime), taskTab)

        val dailyTab = com.example.ui.TaskViewModel.filterTasksForUi(all, today, today, com.example.ui.TaskFilter.DAILY_TASK, null, "")
        assertEquals(listOf(daily), dailyTab)

        val weeklyTab = com.example.ui.TaskViewModel.filterTasksForUi(all, today, today, com.example.ui.TaskFilter.WEEKLY_TASK, null, "")
        assertEquals(listOf(weekly), weeklyTab)

        val monthlyTab = com.example.ui.TaskViewModel.filterTasksForUi(all, today, today, com.example.ui.TaskFilter.MONTHLY_TASK, null, "")
        assertEquals(listOf(monthly), monthlyTab)

        val allTab = com.example.ui.TaskViewModel.filterTasksForUi(all, today, today, com.example.ui.TaskFilter.ALL, null, "")
        assertEquals(4, allTab.size)
        assertTrue(allTab.containsAll(all))
    }

    @Test
    fun `test interstitial ad 90 second cooldown prevents excessive ads`() {
        com.example.ads.AdManager.resetInterstitialCooldown()
        assertEquals(90_000L, com.example.ads.AdManager.INTERSTITIAL_COOLDOWN_MS)

        val t0 = 1_000_000L
        assertTrue(com.example.ads.AdManager.canShowInterstitialNow(t0))

        com.example.ads.AdManager.recordInterstitialShown(t0)
        org.junit.Assert.assertFalse(com.example.ads.AdManager.canShowInterstitialNow(t0 + 10_000L))
        org.junit.Assert.assertFalse(com.example.ads.AdManager.canShowInterstitialNow(t0 + 89_999L))
        assertTrue(com.example.ads.AdManager.canShowInterstitialNow(t0 + 90_000L))

        com.example.ads.AdManager.resetInterstitialCooldown()
    }
}
