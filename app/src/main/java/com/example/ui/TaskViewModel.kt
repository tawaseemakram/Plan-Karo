package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.QuotePreferenceManager
import com.example.data.local.TaskDatabase
import com.example.data.model.HabitItem
import com.example.data.model.Priority
import com.example.data.model.NoteItem
import com.example.data.model.QuoteCategory
import com.example.data.model.QuoteDialogState
import com.example.data.model.QuoteItem
import com.example.data.model.QuoteTriggerReason
import com.example.data.model.QuotesCatalog
import com.example.data.model.RepeatMode
import com.example.data.model.RoutineItem
import com.example.data.model.RoutineTime
import com.example.data.model.TaskCategory
import com.example.data.model.TaskItem
import com.example.data.repository.HabitRepository
import com.example.data.repository.NoteRepository
import com.example.data.repository.RoutineRepository
import com.example.data.repository.TaskRepository
import com.example.reminder.AlarmPreferenceManager
import com.example.reminder.PermissionHelper
import com.example.reminder.PermissionPromptType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import android.app.NotificationManager
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class AppScreen(val title: String) {
    TASKS("Tasks"),
    NOTES("Notes"),
    CALENDAR("Calendar"),
    HABITS("Habits"),
    PROFILE("Profile")
}

enum class TaskFilter(val title: String) {
    TASK("Task"),
    DAILY_TASK("Daily Task"),
    WEEKLY_TASK("Weekly Task"),
    MONTHLY_TASK("Monthly Task"),
    PENDING("Pending"),
    COMPLETED("Done"),
    HIGH_PRIORITY("High Priority"),
    ALL("All Tasks")
}

data class DailyProgress(
    val totalCount: Int = 0,
    val oneTimeTasksCount: Int = 0,
    val completedCount: Int = 0,
    val pendingCount: Int = 0,
    val percentage: Float = 0f,
    val highPriorityPending: Int = 0,
    val dailyTasksCount: Int = 0,
    val weeklyTasksCount: Int = 0,
    val monthlyTasksCount: Int = 0,
    val allTasksCount: Int = 0
)

data class DayProductivityTrend(
    val dateString: String,
    val dayLabel: String,
    val totalTasks: Int,
    val completedTasks: Int,
    val completionPercentage: Float // 0f to 100f
)

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val db = TaskDatabase.getDatabase(application)
    private val taskRepository = TaskRepository(application, db.taskDao())
    private val habitRepository = HabitRepository(db.habitDao(), application)
    private val routineRepository = RoutineRepository(db.routineDao(), application)
    private val noteRepository = NoteRepository(db.noteDao())

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val todayDateFlow = MutableStateFlow(dateFormat.format(Date()))
    val todayDateString: String
        get() = todayDateFlow.value

    private val clockTicker = flow {
        while (true) {
            val nowMillis = System.currentTimeMillis()
            val currentToday = dateFormat.format(Date(nowMillis))
            val prevToday = todayDateFlow.value
            if (currentToday != prevToday) {
                todayDateFlow.value = currentToday
                if (selectedDate.value == prevToday) {
                    selectedDate.value = currentToday
                }
                taskRepository.syncRepeatingTasksForToday(currentToday)
            }
            emit(nowMillis)
            delay(1000L)
        }
    }

    val currentScreen = MutableStateFlow(AppScreen.TASKS)

    val selectedDate = MutableStateFlow(todayDateFlow.value)
    val currentFilter = MutableStateFlow(TaskFilter.TASK)
    val selectedCategoryFilter = MutableStateFlow<String?>(null)
    val searchQuery = MutableStateFlow("")

    private val categoryPrefs = application.getSharedPreferences("plankaro_categories_prefs", android.content.Context.MODE_PRIVATE)

    private fun loadCategories(): List<TaskCategory> {
        val jsonRaw = categoryPrefs.getString("custom_categories_json", null)
        val customList = if (!jsonRaw.isNullOrBlank()) {
            try {
                val arr = org.json.JSONArray(jsonRaw)
                buildList {
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        val id = obj.optString("id")
                        val name = obj.optString("name")
                        val colorHex = obj.optString("colorHex")
                        if (id.isNotBlank() && name.isNotBlank() && colorHex.isNotBlank()) {
                            add(TaskCategory(id = id, name = name, colorHex = colorHex, isCustom = true))
                        }
                    }
                }
            } catch (_: Exception) {
                emptyList()
            }
        } else {
            val legacySaved = categoryPrefs.getStringSet("custom_categories", emptySet()) ?: emptySet()
            val migrated = legacySaved.mapNotNull { entry ->
                val firstSep = entry.indexOf('|')
                val lastSep = entry.lastIndexOf('|')
                if (firstSep > 0 && lastSep > firstSep) {
                    TaskCategory(
                        id = entry.substring(0, firstSep),
                        name = entry.substring(firstSep + 1, lastSep),
                        colorHex = entry.substring(lastSep + 1),
                        isCustom = true
                    )
                } else null
            }
            if (migrated.isNotEmpty()) {
                saveCustomCategories(migrated)
                categoryPrefs.edit().remove("custom_categories").apply()
            }
            migrated
        }
        return TaskCategory.DEFAULT_CATEGORIES + customList
    }

    private fun saveCustomCategories(customList: List<TaskCategory>) {
        val arr = org.json.JSONArray()
        customList.filter { it.isCustom }.forEach { cat ->
            arr.put(
                org.json.JSONObject().apply {
                    put("id", cat.id)
                    put("name", cat.name)
                    put("colorHex", cat.colorHex)
                }
            )
        }
        categoryPrefs.edit()
            .putString("custom_categories_json", arr.toString())
            .remove("custom_categories")
            .apply()
    }

    val categories = MutableStateFlow<List<TaskCategory>>(loadCategories())
    val isCategorySheetOpen = MutableStateFlow(false)

    // Raw tasks observed from Room for selected date
    val allTasks: StateFlow<List<TaskItem>> = taskRepository.getAllTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered and searched list for selectedDate & active filter
    val uiTasks: StateFlow<List<TaskItem>> = combine(
        combine(allTasks, selectedDate, todayDateFlow) { tasks, date, today ->
            Triple(tasks, date, today)
        },
        combine(currentFilter, selectedCategoryFilter, searchQuery) { filter, categoryFilter, query ->
            Triple(filter, categoryFilter, query)
        }
    ) { (tasks, date, today), (filter, categoryFilter, query) ->
        filterTasksForUi(
            tasks = tasks,
            selectedDate = date,
            todayDate = today,
            filter = filter,
            categoryFilter = categoryFilter,
            query = query
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Progress computation based on selected date + repeating filter counts
    val dailyProgress: StateFlow<DailyProgress> = combine(
        allTasks,
        selectedDate,
        todayDateFlow
    ) { tasks, date, today ->
        val dateTasks = tasks.filter { it.taskDate == date }
        val total = dateTasks.size
        val completed = dateTasks.count { it.isCompleted }
        val pending = total - completed
        val pct = if (total > 0) completed.toFloat() / total.toFloat() else 0f
        val highPending = dateTasks.count { !it.isCompleted && it.priority == Priority.HIGH }
        val oneTimeCount = dateTasks.count { it.repeatMode == RepeatMode.NONE }
        val dailyCount = selectRepeatingSeriesTasks(tasks, RepeatMode.DAILY, date, today).size
        val weeklyCount = selectRepeatingSeriesTasks(tasks, RepeatMode.WEEKLY, date, today).size
        val monthlyCount = selectRepeatingSeriesTasks(tasks, RepeatMode.MONTHLY, date, today).size
        val allCount = filterTasksForUi(
            tasks = tasks,
            selectedDate = date,
            todayDate = today,
            filter = TaskFilter.ALL,
            categoryFilter = null,
            query = ""
        ).size

        DailyProgress(
            totalCount = total,
            oneTimeTasksCount = oneTimeCount,
            completedCount = completed,
            pendingCount = pending,
            percentage = pct,
            highPriorityPending = highPending,
            dailyTasksCount = dailyCount,
            weeklyTasksCount = weeklyCount,
            monthlyTasksCount = monthlyCount,
            allTasksCount = allCount
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DailyProgress())

    // 7-Day Completion Rate Productivity Trend
    val last7DaysTrend: StateFlow<List<DayProductivityTrend>> = allTasks.map { tasks ->
        val dayLabelFormat = SimpleDateFormat("EEE", Locale.getDefault())
        val result = mutableListOf<DayProductivityTrend>()
        val todayStr = todayDateString

        for (offset in 6 downTo 0) {
            val dayCal = Calendar.getInstance()
            dayCal.add(Calendar.DAY_OF_YEAR, -offset)
            val dateStr = dateFormat.format(dayCal.time)
            val label = if (dateStr == todayStr) "Today" else dayLabelFormat.format(dayCal.time)

            val dayTasks = tasks.filter { it.taskDate == dateStr }
            val total = dayTasks.size
            val completed = dayTasks.count { it.isCompleted }
            val pct = if (total > 0) (completed.toFloat() / total.toFloat()) * 100f else 0f

            result.add(
                DayProductivityTrend(
                    dateString = dateStr,
                    dayLabel = label,
                    totalTasks = total,
                    completedTasks = completed,
                    completionPercentage = pct
                )
            )
        }
        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active reminders evaluated with live clock ticker within a 2-minute trigger window
    // so stale missed alarms hours later do not re-ring when opening the app
    val activeReminderTask: StateFlow<TaskItem?> = combine(allTasks, clockTicker) { tasks, now ->
        tasks.filter { task ->
            !task.isCompleted &&
                    !task.isReminderDismissed &&
                    task.reminderTimeMillis != null &&
                    task.reminderTimeMillis <= now &&
                    (now - task.reminderTimeMillis) < 2 * 60 * 1000L
        }.maxByOrNull { it.reminderTimeMillis ?: 0L }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Habits Stream
    val habits: StateFlow<List<HabitItem>> = habitRepository.allHabits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Routines Stream (persisted locally via Room)
    val routines: StateFlow<List<RoutineItem>> = routineRepository.allRoutines
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Notes Stream & Filtering State
    val allNotes: StateFlow<List<NoteItem>> = noteRepository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchNotesQuery = MutableStateFlow("")
    val selectedNoteCategory = MutableStateFlow<String?>(null)

    val uiNotes: StateFlow<List<NoteItem>> = combine(
        allNotes,
        searchNotesQuery,
        selectedNoteCategory
    ) { notes, query, cat ->
        var list = notes
        if (!cat.isNullOrBlank()) {
            list = list.filter { it.category.equals(cat, ignoreCase = true) }
        }
        if (query.isNotBlank()) {
            val q = query.trim().lowercase(Locale.getDefault())
            list = list.filter {
                it.title.lowercase(Locale.getDefault()).contains(q) ||
                        it.content.lowercase(Locale.getDefault()).contains(q) ||
                        it.checklistItems.lowercase(Locale.getDefault()).contains(q)
            }
        }
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val editingNote = MutableStateFlow<NoteItem?>(null)
    val isNoteEditorOpen = MutableStateFlow(false)

    // Smart Calendar State
    private val calendarInstance = Calendar.getInstance()
    val calendarYear = MutableStateFlow(calendarInstance.get(Calendar.YEAR))
    val calendarMonth = MutableStateFlow(calendarInstance.get(Calendar.MONTH))

    val calendarMonthTitle: StateFlow<String> = combine(
        calendarYear,
        calendarMonth
    ) { year, month ->
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        monthYearFormat.format(cal.time)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    // Map of dates to tasks for Smart Calendar dot plotting
    val tasksByDateMap: StateFlow<Map<String, List<TaskItem>>> = allTasks.map { tasks ->
        tasks.groupBy { it.taskDate }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // UI Dialog & Sheet States
    val editingTask = MutableStateFlow<TaskItem?>(null)
    val isAddSheetOpen = MutableStateFlow(false)
    val taskForSnooze = MutableStateFlow<TaskItem?>(null)
    val previewImageUri = MutableStateFlow<String?>(null)

    // Habit creation dialog state
    val isAddHabitDialogOpen = MutableStateFlow(false)
    val editingHabit = MutableStateFlow<HabitItem?>(null)

    // Alarm Preference: When true, alarms ring loudly like an alarm clock. When false, only notification bar.
    val isTaskAlarmEnabled = MutableStateFlow(AlarmPreferenceManager.isAlarmEnabled(application))
    val isAlarmTesting = MutableStateFlow(false)

    // Inspirational Quote Dialog State (shown after task/note creation after ad completion)
    val quoteDialogState = MutableStateFlow<QuoteDialogState?>(null)

    // Daily Inspirational Quote displayed inline under the Plan Karo logo (resumes from saved quote)
    val bannerQuote = MutableStateFlow(
        QuotesCatalog.getQuoteById(QuotePreferenceManager.getSavedQuoteId(application)).also { initial ->
            QuotePreferenceManager.saveQuoteId(application, initial.id)
        }
    )

    private var quoteRotationJob: Job? = null

    init {
        // Watch active task reminders and trigger in-app alarm ringtone & vibration on transitions only,
        // so unrelated Habit/Routine alarms ringing in TaskAlarmService are never cancelled by null task state.
        viewModelScope.launch {
            var previousTaskAlarmId: Long? = null
            var previousAlarmOn: Boolean = isTaskAlarmEnabled.value
            combine(activeReminderTask, isTaskAlarmEnabled) { task, isAlarmOn ->
                Pair(task, isAlarmOn)
            }.collect { (task, isAlarmOn) ->
                if (task != null && isAlarmOn) {
                    if (task.id != previousTaskAlarmId || !previousAlarmOn) {
                        com.example.reminder.AlarmSoundManager.startAlarm(application)
                    }
                    previousTaskAlarmId = task.id
                } else {
                    val wasTaskAlarmActive = previousTaskAlarmId != null
                    val userDisabledAlarm = previousAlarmOn && !isAlarmOn
                    if (wasTaskAlarmActive || userDisabledAlarm) {
                        com.example.reminder.AlarmSoundManager.stopAlarm(application)
                    }
                    previousTaskAlarmId = null
                }
                previousAlarmOn = isAlarmOn
            }
        }

        // Automatically advance the banner quote once every 5 minutes and persist it
        startQuoteAutoRotation()

        // Sweep stale orphaned internal media files older than 60s from crashed/cancelled drafts
        viewModelScope.launch {
            cleanupOrphanedInternalMediaFiles(minAgeMs = 60_000L)
        }
    }

    companion object {
        const val QUOTE_ROTATION_INTERVAL_MS = 5 * 60 * 1000L // 5 minutes

        /**
         * Returns one representative occurrence per repeating series so Daily/Weekly/Monthly tasks
         * stay cleanly isolated in their dedicated filter tabs without duplicate rows.
         */
        fun selectRepeatingSeriesTasks(
            tasks: List<TaskItem>,
            mode: RepeatMode,
            selectedDate: String,
            todayDate: String
        ): List<TaskItem> {
            if (mode == RepeatMode.NONE) return emptyList()
            val matching = tasks.filter { it.repeatMode == mode }
            if (matching.isEmpty()) return emptyList()

            val seriesMap = matching.groupBy { it.title.trim().lowercase(Locale.getDefault()) }
            val result = mutableListOf<TaskItem>()

            for ((_, series) in seriesMap) {
                // 1. Exact occurrence for the selected date
                val exactForDate = series.firstOrNull { it.taskDate == selectedDate }
                if (exactForDate != null) {
                    result.add(exactForDate)
                    continue
                }

                // 2. Active/upcoming series occurrence
                when (mode) {
                    RepeatMode.DAILY -> {
                        if (selectedDate == todayDate) {
                            val latest = series.maxByOrNull { it.taskDate }
                            if (latest != null) result.add(latest)
                        }
                    }
                    RepeatMode.WEEKLY, RepeatMode.MONTHLY -> {
                        if (selectedDate >= todayDate) {
                            val upcomingPending = series
                                .filter { it.taskDate >= selectedDate && !it.isCompleted }
                                .minByOrNull { it.taskDate }
                            val upcomingAny = series
                                .filter { it.taskDate >= selectedDate }
                                .minByOrNull { it.taskDate }
                            val overduePending = series
                                .filter { !it.isCompleted }
                                .maxByOrNull { it.taskDate }
                            val chosen = upcomingPending ?: upcomingAny ?: overduePending
                            if (chosen != null) result.add(chosen)
                        }
                    }
                    RepeatMode.NONE -> Unit
                }
            }

            return result.sortedWith(compareBy<TaskItem> { it.orderIndex }.thenBy { it.createdAt })
        }

        fun filterTasksForUi(
            tasks: List<TaskItem>,
            selectedDate: String,
            todayDate: String,
            filter: TaskFilter,
            categoryFilter: String?,
            query: String
        ): List<TaskItem> {
            val dateTasks = tasks.filter { it.taskDate == selectedDate }
            val dailyTasks = selectRepeatingSeriesTasks(tasks, RepeatMode.DAILY, selectedDate, todayDate)
            val weeklyTasks = selectRepeatingSeriesTasks(tasks, RepeatMode.WEEKLY, selectedDate, todayDate)
            val monthlyTasks = selectRepeatingSeriesTasks(tasks, RepeatMode.MONTHLY, selectedDate, todayDate)

            val targetList = when (filter) {
                TaskFilter.TASK -> dateTasks.filter { it.repeatMode == RepeatMode.NONE }
                TaskFilter.DAILY_TASK -> dailyTasks
                TaskFilter.WEEKLY_TASK -> weeklyTasks
                TaskFilter.MONTHLY_TASK -> monthlyTasks
                TaskFilter.PENDING -> dateTasks.filter { !it.isCompleted }
                TaskFilter.COMPLETED -> dateTasks.filter { it.isCompleted }
                TaskFilter.HIGH_PRIORITY -> dateTasks.filter { it.priority == Priority.HIGH }
                TaskFilter.ALL -> (dateTasks + dailyTasks + weeklyTasks + monthlyTasks)
                    .distinctBy { it.id }
                    .sortedWith(compareBy<TaskItem> { it.orderIndex }.thenBy { it.createdAt })
            }

            val categoryFiltered = if (!categoryFilter.isNullOrBlank()) {
                targetList.filter { it.category.equals(categoryFilter, ignoreCase = true) }
            } else {
                targetList
            }

            return if (query.isNotBlank()) {
                val q = query.trim().lowercase(Locale.getDefault())
                categoryFiltered.filter {
                    it.title.lowercase(Locale.getDefault()).contains(q) ||
                        it.notes.lowercase(Locale.getDefault()).contains(q) ||
                        it.category.lowercase(Locale.getDefault()).contains(q)
                }
            } else {
                categoryFiltered
            }
        }
    }

    fun setScreen(screen: AppScreen) {
        currentScreen.value = screen
    }

    fun onDateSelected(dateString: String) {
        selectedDate.value = dateString
    }

    fun onFilterSelected(filter: TaskFilter) {
        currentFilter.value = filter
    }

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
    }

    fun openAddTask() {
        editingTask.value = null
        isAddSheetOpen.value = true
    }

    fun openEditTask(task: TaskItem) {
        editingTask.value = task
        isAddSheetOpen.value = true
    }

    fun closeAddEditSheet() {
        editingTask.value = null
        isAddSheetOpen.value = false
    }

    fun openSnoozeDialog(task: TaskItem) {
        taskForSnooze.value = task
    }

    fun closeSnoozeDialog() {
        taskForSnooze.value = null
    }

    fun openImagePreview(uri: String) {
        previewImageUri.value = uri
    }

    fun closeImagePreview() {
        previewImageUri.value = null
    }

    fun openAddHabitDialog(habit: HabitItem? = null) {
        editingHabit.value = habit
        isAddHabitDialogOpen.value = true
    }

    fun closeAddHabitDialog() {
        editingHabit.value = null
        isAddHabitDialogOpen.value = false
    }

    fun previousCalendarMonth() {
        if (calendarMonth.value == 0) {
            calendarMonth.value = 11
            calendarYear.value -= 1
        } else {
            calendarMonth.value -= 1
        }
    }

    fun nextCalendarMonth() {
        if (calendarMonth.value == 11) {
            calendarMonth.value = 0
            calendarYear.value += 1
        } else {
            calendarMonth.value += 1
        }
    }

    fun jumpToTodayCalendar() {
        val cal = Calendar.getInstance()
        calendarYear.value = cal.get(Calendar.YEAR)
        calendarMonth.value = cal.get(Calendar.MONTH)
        selectedDate.value = todayDateString
    }

    fun openCategorySheet() {
        isCategorySheetOpen.value = true
    }

    fun closeCategorySheet() {
        isCategorySheetOpen.value = false
    }

    fun selectCategoryFilter(categoryName: String?) {
        selectedCategoryFilter.value = categoryName
    }

    fun addCustomCategory(name: String, colorHex: String): Boolean {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return false
        val exists = categories.value.any { it.name.equals(trimmed, ignoreCase = true) }
        if (exists) return false

        val id = "custom_${System.currentTimeMillis()}"
        val newCat = TaskCategory(
            id = id,
            name = trimmed,
            colorHex = colorHex,
            isCustom = true
        )
        val updated = categories.value + newCat
        categories.value = updated
        saveCustomCategories(updated)
        return true
    }

    fun deleteCustomCategory(categoryId: String) {
        val updated = categories.value.filter { it.id != categoryId }
        categories.value = updated
        saveCustomCategories(updated)

        if (selectedCategoryFilter.value != null && categories.value.none { it.name.equals(selectedCategoryFilter.value, ignoreCase = true) }) {
            selectedCategoryFilter.value = null
        }
    }

    fun saveTask(
        id: Long = 0L,
        title: String,
        notes: String,
        priority: Priority,
        category: String = "Personal",
        reminderTimeMillis: Long?,
        imageUri: String?,
        audioUri: String? = null,
        audioDurationSeconds: Int = 0,
        repeatMode: RepeatMode = RepeatMode.NONE,
        date: String = selectedDate.value
    ) {
        viewModelScope.launch {
            if (id == 0L) {
                val newTask = TaskItem(
                    title = title.trim(),
                    notes = notes.trim(),
                    priority = priority,
                    category = category.trim().ifBlank { "Personal" },
                    reminderTimeMillis = reminderTimeMillis,
                    isReminderDismissed = false,
                    imageUri = imageUri,
                    audioUri = audioUri,
                    audioDurationSeconds = audioDurationSeconds,
                    repeatMode = repeatMode,
                    taskDate = date
                )
                taskRepository.insertTask(newTask)
            } else {
                val existing = editingTask.value
                val reminderUnchanged = existing != null && existing.reminderTimeMillis == reminderTimeMillis
                val updated = (existing ?: TaskItem(id = id, title = title, taskDate = date)).copy(
                    title = title.trim(),
                    notes = notes.trim(),
                    priority = priority,
                    category = category.trim().ifBlank { existing?.category ?: "Personal" },
                    reminderTimeMillis = reminderTimeMillis,
                    isReminderDismissed = if (reminderUnchanged) existing!!.isReminderDismissed else false,
                    imageUri = imageUri,
                    audioUri = audioUri,
                    audioDurationSeconds = audioDurationSeconds,
                    repeatMode = repeatMode,
                    taskDate = existing?.taskDate ?: date
                )
                taskRepository.updateTask(updated)
                val replacedImage = existing?.imageUri?.takeIf { it != imageUri }
                val replacedAudio = existing?.audioUri?.takeIf { it != audioUri }
                deleteAssociatedMediaFilesIfExists(replacedImage, replacedAudio)
            }
            if (reminderTimeMillis != null) {
                checkAndPromptAlarmPermissionsIfNeeded()
            }
            closeAddEditSheet()
        }
    }

    fun handleNotificationTap(taskId: Long) {
        if (taskId <= 0L) return
        currentScreen.value = AppScreen.TASKS
        viewModelScope.launch {
            val task = withContext(Dispatchers.IO) {
                db.taskDao().getTaskById(taskId)
            } ?: return@launch

            val now = System.currentTimeMillis()
            val reminderAt = task.reminderTimeMillis
            val inActiveAlarmWindow = !task.isCompleted &&
                !task.isReminderDismissed &&
                reminderAt != null &&
                reminderAt <= now &&
                (now - reminderAt) < 2 * 60 * 1000L

            if (!inActiveAlarmWindow) {
                com.example.reminder.AlarmSoundManager.stopAlarm(getApplication())
                if (!task.isCompleted && !task.isReminderDismissed && reminderAt != null && reminderAt <= now) {
                    taskForSnooze.value = task
                }
            }
        }
    }

    fun rescheduleTask(task: TaskItem, newDate: String) {
        viewModelScope.launch {
            val updated = task.copy(taskDate = newDate)
            taskRepository.updateTask(updated)
        }
    }

    fun toggleTaskCompletion(task: TaskItem) {
        com.example.reminder.AlarmSoundManager.stopAlarm(getApplication())
        viewModelScope.launch {
            taskRepository.toggleTaskCompletion(task)
        }
    }

    fun snoozeTask(task: TaskItem, minutes: Int) {
        com.example.reminder.AlarmSoundManager.stopAlarm(getApplication())
        viewModelScope.launch {
            taskRepository.snoozeTask(task, minutes)
            closeSnoozeDialog()
        }
    }

    fun dismissReminder(task: TaskItem) {
        com.example.reminder.AlarmSoundManager.stopAlarm(getApplication())
        viewModelScope.launch {
            taskRepository.dismissReminder(task)
        }
    }

    fun setTaskAlarmEnabled(enabled: Boolean) {
        isTaskAlarmEnabled.value = enabled
        AlarmPreferenceManager.setAlarmEnabled(getApplication(), enabled)
        if (!enabled) {
            com.example.reminder.AlarmSoundManager.stopAlarm(getApplication())
        }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                com.example.reminder.ReminderManager.rescheduleAllAlarms(getApplication())
            }
        }
    }

    fun testAlarmSound(play: Boolean) {
        isAlarmTesting.value = play
        if (play) {
            com.example.reminder.AlarmSoundManager.startAlarm(getApplication())
        } else {
            com.example.reminder.AlarmSoundManager.stopAlarm(getApplication())
        }
    }

    /**
     * Shows an inspirational quote based on Time, Discipline, Motivation, Success, or Consistency.
     * Triggered on app open and after task/note creation when ad completes.
     */
    fun showQuote(reason: QuoteTriggerReason, category: QuoteCategory? = null) {
        val quote = QuotesCatalog.getRandomQuote(category = category)
        quoteDialogState.value = QuoteDialogState(quote = quote, triggerReason = reason)
    }

    fun nextQuote() {
        val current = quoteDialogState.value ?: return
        val next = QuotesCatalog.getRandomQuote(excludeId = current.quote.id)
        quoteDialogState.value = current.copy(quote = next)
    }

    fun dismissQuote() {
        quoteDialogState.value = null
    }

    val permissionPromptType = MutableStateFlow<PermissionPromptType?>(null)

    fun showPermissionSettingsPrompt(type: PermissionPromptType) {
        permissionPromptType.value = type
    }

    fun dismissPermissionSettingsPrompt() {
        permissionPromptType.value = null
    }

    fun checkAndPromptAlarmPermissionsIfNeeded() {
        val missing = PermissionHelper.firstMissingAlarmPermission(
            context = getApplication(),
            isAlarmSoundEnabled = isTaskAlarmEnabled.value
        )
        if (missing != null) {
            permissionPromptType.value = missing
        }
    }

    private fun startQuoteAutoRotation() {
        quoteRotationJob?.cancel()
        quoteRotationJob = viewModelScope.launch {
            while (true) {
                delay(QUOTE_ROTATION_INTERVAL_MS)
                advanceBannerQuoteInternal()
            }
        }
    }

    private fun advanceBannerQuoteInternal() {
        val next = QuotesCatalog.getNextQuote(bannerQuote.value.id)
        bannerQuote.value = next
        QuotePreferenceManager.saveQuoteId(getApplication(), next.id)
    }

    fun refreshBannerQuote() {
        advanceBannerQuoteInternal()
        // Reset the 5-minute auto-rotation timer after manual refresh
        startQuoteAutoRotation()
    }

    fun deleteTask(task: TaskItem) {
        viewModelScope.launch {
            taskRepository.deleteTask(task)
            deleteAssociatedMediaFilesIfExists(task.imageUri, task.audioUri)
        }
    }

    fun moveTask(fromIndex: Int, toIndex: Int) {
        val current = uiTasks.value.toMutableList()
        if (fromIndex !in current.indices || toIndex !in current.indices) return
        val item = current.removeAt(fromIndex)
        current.add(toIndex, item)

        viewModelScope.launch {
            taskRepository.reorderTasks(current)
        }
    }

    fun moveTaskUp(task: TaskItem) {
        val current = uiTasks.value
        val index = current.indexOfFirst { it.id == task.id }
        if (index > 0) {
            moveTask(index, index - 1)
        }
    }

    fun moveTaskDown(task: TaskItem) {
        val current = uiTasks.value
        val index = current.indexOfFirst { it.id == task.id }
        if (index >= 0 && index < current.size - 1) {
            moveTask(index, index + 1)
        }
    }

    // Habit operations
    fun toggleHabitToday(habit: HabitItem) {
        viewModelScope.launch {
            habitRepository.toggleHabitForDate(habit, selectedDate.value)
        }
    }

    fun toggleHabitForSpecificDate(habit: HabitItem, dateString: String) {
        viewModelScope.launch {
            habitRepository.toggleHabitForDate(habit, dateString)
        }
    }

    fun saveHabit(
        id: Long = 0L,
        title: String,
        category: String,
        routineTime: RoutineTime,
        targetDaysPerWeek: Int = 7,
        reminderTime: String? = null
    ) {
        viewModelScope.launch {
            if (id == 0L) {
                habitRepository.insertHabit(
                    HabitItem(
                        title = title.trim(),
                        category = category.trim(),
                        routineTime = routineTime,
                        targetDaysPerWeek = targetDaysPerWeek,
                        reminderTime = reminderTime
                    )
                )
            } else {
                val existing = editingHabit.value ?: HabitItem(id = id, title = title)
                habitRepository.updateHabit(
                    existing.copy(
                        title = title.trim(),
                        category = category.trim(),
                        routineTime = routineTime,
                        targetDaysPerWeek = targetDaysPerWeek,
                        reminderTime = reminderTime
                    )
                )
            }
            if (!reminderTime.isNullOrBlank()) {
                checkAndPromptAlarmPermissionsIfNeeded()
            }
            closeAddHabitDialog()
        }
    }

    fun deleteHabit(habit: HabitItem) {
        viewModelScope.launch {
            habitRepository.deleteHabit(habit)
        }
    }

    // -------------------------------------------------------------
    // Routine Local Persistence Operations
    // -------------------------------------------------------------
    fun insertRoutine(
        title: String,
        description: String = "",
        routineTime: RoutineTime = RoutineTime.MORNING,
        scheduledTime: String = "08:00 AM",
        daysOfWeek: String = "DAILY"
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            routineRepository.insertRoutine(
                RoutineItem(
                    title = title.trim(),
                    description = description.trim(),
                    routineTime = routineTime,
                    scheduledTime = scheduledTime,
                    daysOfWeek = daysOfWeek
                )
            )
            checkAndPromptAlarmPermissionsIfNeeded()
        }
    }

    fun updateRoutine(routine: RoutineItem) {
        viewModelScope.launch {
            routineRepository.updateRoutine(routine)
            if (routine.isEnabled) {
                checkAndPromptAlarmPermissionsIfNeeded()
            }
        }
    }

    fun toggleRoutineEnabled(routine: RoutineItem) {
        viewModelScope.launch {
            routineRepository.toggleRoutineEnabled(routine)
            if (!routine.isEnabled) {
                checkAndPromptAlarmPermissionsIfNeeded()
            }
        }
    }

    fun deleteRoutine(routine: RoutineItem) {
        viewModelScope.launch {
            routineRepository.deleteRoutine(routine)
        }
    }

    fun reorderRoutines(orderedList: List<RoutineItem>) {
        viewModelScope.launch {
            routineRepository.reorderRoutines(orderedList)
        }
    }

    // -------------------------------------------------------------
    // Note Operations (Audio Notes, Checklists, Convert to Task)
    // -------------------------------------------------------------
    fun openAddNote() {
        editingNote.value = null
        isNoteEditorOpen.value = true
    }

    fun openEditNote(note: NoteItem) {
        editingNote.value = note
        isNoteEditorOpen.value = true
    }

    fun closeNoteEditor() {
        editingNote.value = null
        isNoteEditorOpen.value = false
    }

    fun onSearchNotesChanged(query: String) {
        searchNotesQuery.value = query
    }

    fun selectNoteCategory(category: String?) {
        selectedNoteCategory.value = category
    }

    fun saveNote(
        id: Long = 0L,
        title: String,
        content: String,
        category: String = "Idea",
        colorHex: String = "#00E5BE",
        isPinned: Boolean = false,
        audioUri: String? = null,
        audioDurationSeconds: Int = 0,
        imageUri: String? = null,
        isChecklist: Boolean = false,
        checklistItems: String = ""
    ) {
        viewModelScope.launch {
            if (id == 0L) {
                noteRepository.insertNote(
                    NoteItem(
                        title = title.trim(),
                        content = content.trim(),
                        category = category.trim(),
                        colorHex = colorHex,
                        isPinned = isPinned,
                        audioUri = audioUri,
                        audioDurationSeconds = audioDurationSeconds,
                        imageUri = imageUri,
                        isChecklist = isChecklist,
                        checklistItems = checklistItems
                    )
                )
            } else {
                val existing = editingNote.value
                noteRepository.updateNote(
                    (existing ?: NoteItem(id = id, title = title)).copy(
                        title = title.trim(),
                        content = content.trim(),
                        category = category.trim(),
                        colorHex = colorHex,
                        isPinned = isPinned,
                        audioUri = audioUri,
                        audioDurationSeconds = audioDurationSeconds,
                        imageUri = imageUri,
                        isChecklist = isChecklist,
                        checklistItems = checklistItems,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                val replacedImage = existing?.imageUri?.takeIf { it != imageUri }
                val replacedAudio = existing?.audioUri?.takeIf { it != audioUri }
                deleteAssociatedMediaFilesIfExists(replacedImage, replacedAudio)
            }
            closeNoteEditor()
        }
    }

    fun deleteNote(note: NoteItem) {
        viewModelScope.launch {
            noteRepository.deleteNote(note)
            deleteAssociatedMediaFilesIfExists(note.imageUri, note.audioUri)
        }
    }

    /**
     * Checks whether the given image/audio paths exist inside app-private internal storage
     * (`filesDir/attached_images` or `filesDir/audio_notes`), ensures no remaining Task or Note
     * in Room still references them, and deletes them to free up disk space.
     */
    internal suspend fun deleteAssociatedMediaFilesIfExists(
        imageUri: String?,
        audioUri: String?
    ): Int {
        if (imageUri.isNullOrBlank() && audioUri.isNullOrBlank()) return 0
        val app = getApplication<Application>()
        return withContext(Dispatchers.IO) {
            var deletedCount = 0

            if (!imageUri.isNullOrBlank()) {
                val targetPath = resolveCanonicalInternalPath(imageUri)
                val referencedPaths = (db.taskDao().getAllImageUris() + db.noteDao().getAllImageUris())
                    .mapNotNull { resolveCanonicalInternalPath(it) }
                    .toSet()
                val exactRefs = db.taskDao().countImageUriReferences(imageUri) +
                    db.noteDao().countImageUriReferences(imageUri)
                val stillReferenced = exactRefs > 0 || (targetPath != null && targetPath in referencedPaths)
                if (!stillReferenced && com.example.data.local.ImageAttachmentStorage.deleteOwnedImageFile(app, imageUri)) {
                    deletedCount++
                }
            }

            if (!audioUri.isNullOrBlank()) {
                val targetPath = resolveCanonicalInternalPath(audioUri)
                val referencedPaths = (db.taskDao().getAllAudioUris() + db.noteDao().getAllAudioUris())
                    .mapNotNull { resolveCanonicalInternalPath(it) }
                    .toSet()
                val exactRefs = db.taskDao().countAudioUriReferences(audioUri) +
                    db.noteDao().countAudioUriReferences(audioUri)
                val stillReferenced = exactRefs > 0 || (targetPath != null && targetPath in referencedPaths)
                if (!stillReferenced && com.example.audio.AudioUtils.deleteOwnedAudioFile(app, audioUri)) {
                    deletedCount++
                }
            }

            deletedCount
        }
    }

    /**
     * Sweeps internal storage media directories and removes any unreferenced files left behind
     * by cancelled drafts or deleted entries (respecting [minAgeMs] so active drafts are spared).
     */
    internal suspend fun cleanupOrphanedInternalMediaFiles(minAgeMs: Long = 0L): Int {
        val app = getApplication<Application>()
        return withContext(Dispatchers.IO) {
            var deletedCount = 0
            val cutoffMs = System.currentTimeMillis() - minAgeMs

            val referencedImages = (db.taskDao().getAllImageUris() + db.noteDao().getAllImageUris())
                .mapNotNull { resolveCanonicalInternalPath(it) }
                .toSet()
            val imagesDir = com.example.data.local.ImageAttachmentStorage.getImagesDir(app)
            imagesDir.listFiles()?.forEach { file ->
                val canonical = runCatching { file.canonicalPath }.getOrNull()
                val oldEnough = minAgeMs <= 0L || file.lastModified() <= cutoffMs
                if (file.isFile && oldEnough && canonical != null && canonical !in referencedImages && file.delete()) {
                    deletedCount++
                }
            }

            val referencedAudios = (db.taskDao().getAllAudioUris() + db.noteDao().getAllAudioUris())
                .mapNotNull { resolveCanonicalInternalPath(it) }
                .toSet()
            val audioDir = File(app.filesDir, "audio_notes")
            if (audioDir.exists()) {
                audioDir.listFiles()?.forEach { file ->
                    val canonical = runCatching { file.canonicalPath }.getOrNull()
                    val oldEnough = minAgeMs <= 0L || file.lastModified() <= cutoffMs
                    if (file.isFile && oldEnough && canonical != null && canonical !in referencedAudios && file.delete()) {
                        deletedCount++
                    }
                }
            }

            deletedCount
        }
    }

    private fun resolveCanonicalInternalPath(uriOrPath: String): String? {
        return try {
            val path = if (uriOrPath.startsWith("file://")) {
                android.net.Uri.parse(uriOrPath).path
            } else if (!uriOrPath.contains("://")) {
                uriOrPath
            } else {
                null
            } ?: return null
            File(path).canonicalPath
        } catch (_: Exception) {
            null
        }
    }

    fun toggleNotePin(note: NoteItem) {
        viewModelScope.launch {
            noteRepository.togglePin(note.id)
        }
    }

    fun toggleChecklistItem(note: NoteItem, itemIndex: Int) {
        val lines = note.checklistItems.lines().toMutableList()
        if (itemIndex in lines.indices) {
            val line = lines[itemIndex]
            val parts = line.split("|", limit = 2)
            if (parts.size == 2) {
                val isDone = parts[0] == "Done"
                val text = parts[1]
                lines[itemIndex] = if (isDone) "Todo|$text" else "Done|$text"
                viewModelScope.launch {
                    noteRepository.updateNote(note.copy(checklistItems = lines.joinToString("\n")))
                }
            }
        }
    }

    fun convertNoteToTask(
        note: NoteItem,
        targetDate: String = todayDateString,
        priority: Priority = Priority.MEDIUM
    ) {
        viewModelScope.launch {
            saveTask(
                title = note.title,
                notes = if (note.isChecklist && note.checklistItems.isNotBlank()) {
                    note.checklistItems.lines().joinToString("\n") { line ->
                        val parts = line.split("|", limit = 2)
                        if (parts.size == 2) (if (parts[0] == "Done") "✓ " else "○ ") + parts[1] else line
                    }
                } else note.content,
                priority = priority,
                category = when (note.category.lowercase()) {
                    "work" -> "Work"
                    "study" -> "Learning"
                    "personal" -> "Personal"
                    "meeting" -> "Urgent"
                    else -> "Personal"
                },
                reminderTimeMillis = null,
                imageUri = note.imageUri,
                audioUri = note.audioUri,
                audioDurationSeconds = note.audioDurationSeconds,
                date = targetDate
            )
        }
    }

    fun seedSampleTasksIfEmpty() {
        viewModelScope.launch {
            // Clean up any previously seeded automatic sample tasks as requested by the user
            val currentTasks = allTasks.first()
            val sampleTitles = setOf(
                "Review project deliverables",
                "Team sync & design review",
                "Grocery shopping & fresh fruits"
            )
            val toDelete = currentTasks.filter { it.title in sampleTitles }
            for (t in toDelete) {
                taskRepository.deleteTask(t)
            }

            // Automatically roll over Daily/Weekly/Monthly repeating tasks for today even if missed earlier
            taskRepository.syncRepeatingTasksForToday(todayDateString)

            // Ensure all enabled routines & habits with reminderTime have active AlarmManager schedules
            routines.first().filter { it.isEnabled }.forEach { routine ->
                com.example.reminder.RoutineReminderManager.scheduleRoutine(getApplication(), routine)
            }
            habits.first().filter { !it.reminderTime.isNullOrBlank() }.forEach { habit ->
                com.example.reminder.RoutineReminderManager.scheduleHabit(getApplication(), habit)
            }

            val seedPrefs = getApplication<Application>().getSharedPreferences("plankaro_seed_prefs", android.content.Context.MODE_PRIVATE)

            // Seed initial habits: EXACTLY 1 example only once, preventing automatic recreation on app reopen
            val hasSeededHabits = seedPrefs.getBoolean("has_seeded_initial_habits_v3", false)
            val currentHabits = habits.first()
            val sampleHabitTitles = setOf(
                "Morning Meditation & Breathing",
                "Hydrate: Drink 2.5L Water",
                "Read 20 Pages of Book",
                "Nightly Reflection & Plan Tomorrow"
            )

            // Deduplicate any previously seeded extra sample habits so only 1 example remains
            val existingSampleHabits = currentHabits.filter { it.title in sampleHabitTitles }
            if (existingSampleHabits.size > 1) {
                val toKeep = existingSampleHabits.firstOrNull { it.title == "Morning Meditation & Breathing" }
                    ?: existingSampleHabits.first()
                val toRemove = existingSampleHabits.filter { it.id != toKeep.id }
                for (extra in toRemove) {
                    habitRepository.deleteHabit(extra)
                }
            }

            if (!hasSeededHabits) {
                if (currentHabits.none { it.title in sampleHabitTitles }) {
                    habitRepository.insertHabit(
                        HabitItem(
                            title = "Morning Meditation & Breathing",
                            category = "Mindfulness",
                            routineTime = RoutineTime.MORNING,
                            streakCount = 1,
                            bestStreak = 1,
                            completedDates = todayDateString
                        )
                    )
                }
                seedPrefs.edit().putBoolean("has_seeded_initial_habits_v3", true).apply()
            }

            // Seed initial routines: EXACTLY 1 example only once, as requested by user
            val hasSeededRoutines = seedPrefs.getBoolean("has_seeded_initial_routines_v3", false)
            val currentRoutines = routines.first()
            val sampleRoutineTitles = setOf(
                "Morning Kickstart Routine",
                "Afternoon Reset & Walk",
                "Evening Wind-Down & Digital Sunset"
            )

            // Deduplicate/clean up any extra sample routines from previous launches so exactly 1 remains
            val existingSampleRoutines = currentRoutines.filter { it.title in sampleRoutineTitles }
            if (existingSampleRoutines.size > 1) {
                val toKeep = existingSampleRoutines.firstOrNull { it.title == "Morning Kickstart Routine" }
                    ?: existingSampleRoutines.first()
                val toRemove = existingSampleRoutines.filter { it.id != toKeep.id }
                for (extra in toRemove) {
                    routineRepository.deleteRoutine(extra)
                }
            }

            if (!hasSeededRoutines) {
                if (currentRoutines.none { it.title in sampleRoutineTitles }) {
                    routineRepository.insertRoutine(
                        RoutineItem(
                            title = "Morning Kickstart Routine",
                            description = "Wake up, 10-min sunlight, hydrate & plan daily top 3",
                            routineTime = RoutineTime.MORNING,
                            scheduledTime = "07:00 AM",
                            orderIndex = 0
                        )
                    )
                }
                seedPrefs.edit().putBoolean("has_seeded_initial_routines_v3", true).apply()
            }

            // Seed initial notes: EXACTLY 2 examples only once, preventing automatic recreation on every launch
            val hasSeededNotes = seedPrefs.getBoolean("has_seeded_initial_notes_v3", false)
            val currentNotes = allNotes.first()

            // Deduplicate if duplicates were created by previous app opens
            val sampleNoteTitles = setOf("Plan Karo Notes 🎯", "Productivity Checklist ✅", "Daily Productivity Checklist ✅")
            val sampleNotesFound = currentNotes.filter { it.title in sampleNoteTitles }
            if (sampleNotesFound.size > 2) {
                val toKeep = sampleNotesFound.distinctBy { it.title }.take(2)
                val toRemove = sampleNotesFound - toKeep.toSet()
                for (extra in toRemove) {
                    noteRepository.deleteNote(extra)
                }
            }

            if (!hasSeededNotes) {
                if (currentNotes.none { it.title in sampleNoteTitles }) {
                    noteRepository.insertNote(
                        NoteItem(
                            title = "Plan Karo Notes 🎯",
                            content = "Welcome to Plan Karo Notes! Use this space for rapid thoughts, meeting points, voice memos, and converting spontaneous ideas into daily tasks with 1 tap.",
                            category = "Idea",
                            colorHex = "#00E5BE",
                            isPinned = true
                        )
                    )
                    noteRepository.insertNote(
                        NoteItem(
                            title = "Daily Productivity Checklist ✅",
                            category = "Checklist",
                            colorHex = "#FFB800",
                            isChecklist = true,
                            checklistItems = "Done|Drink morning water\nDone|10-min sunlight walk\nTodo|Set daily top 3 priorities\nTodo|Record voice notes for quick memos"
                        )
                    )
                }
                seedPrefs.edit().putBoolean("has_seeded_initial_notes_v3", true).apply()
            }
        }
    }

    fun clearAllLocalData(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val app = getApplication<Application>()

            // 1. Stop active alarm playback & cancel scheduled reminders before clearing records
            runCatching { com.example.reminder.AlarmSoundManager.stopAlarm(app) }
            val snapshotTasks = allTasks.value
            val snapshotHabits = habits.value
            val snapshotRoutines = routines.value
            snapshotTasks.forEach { task ->
                runCatching { com.example.reminder.ReminderManager.cancelReminder(app, task.id) }
            }
            snapshotHabits.forEach { habit ->
                runCatching { com.example.reminder.RoutineReminderManager.cancelHabit(app, habit.id) }
            }
            snapshotRoutines.forEach { routine ->
                runCatching { com.example.reminder.RoutineReminderManager.cancelRoutine(app, routine.id) }
            }
            runCatching {
                (app.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)?.cancelAll()
            }

            // 2. Wipe Room database tables & local audio voice note files on IO dispatcher
            withContext(Dispatchers.IO) {
                runCatching {
                    db.clearAllTables()
                }.onFailure { err ->
                    Log.e("TaskViewModel", "clearAllTables failed during local data reset", err)
                }

                runCatching {
                    val audioDir = File(app.filesDir, "audio_notes")
                    if (audioDir.exists()) {
                        audioDir.deleteRecursively()
                    }
                }.onFailure { err ->
                    Log.e("TaskViewModel", "Failed deleting audio_notes directory", err)
                }

                runCatching {
                    com.example.data.local.ImageAttachmentStorage.deleteAllAttachedImages(app)
                }.onFailure { err ->
                    Log.e("TaskViewModel", "Failed deleting attached_images directory", err)
                }
            }

            // 3. Clear local SharedPreferences (custom categories, legacy prefs, alarm prefs, quotes, legal consent)
            categoryPrefs.edit().clear().apply()
            app.getSharedPreferences("plankaro_api_key_prefs", Context.MODE_PRIVATE).edit().clear().apply()
            app.getSharedPreferences("plan_karo_alarm_prefs", Context.MODE_PRIVATE).edit().clear().apply()
            app.getSharedPreferences("plan_karo_quote_prefs", Context.MODE_PRIVATE).edit().clear().apply()
            app.getSharedPreferences("plankaro_legal_prefs", Context.MODE_PRIVATE).edit().clear().apply()

            // Keep initial-seed flags marked true so sample items are not re-inserted after explicit reset
            app.getSharedPreferences("plankaro_seed_prefs", Context.MODE_PRIVATE)
                .edit()
                .putBoolean("has_seeded_initial_habits_v3", true)
                .putBoolean("has_seeded_initial_routines_v3", true)
                .putBoolean("has_seeded_initial_notes_v3", true)
                .apply()

            // 4. Reset in-memory ViewModel state
            categories.value = TaskCategory.DEFAULT_CATEGORIES
            selectedCategoryFilter.value = null
            searchQuery.value = ""
            searchNotesQuery.value = ""
            selectedNoteCategory.value = null
            isTaskAlarmEnabled.value = AlarmPreferenceManager.isAlarmEnabled(app)
            isAlarmTesting.value = false

            editingTask.value = null
            isAddSheetOpen.value = false
            taskForSnooze.value = null
            previewImageUri.value = null
            editingNote.value = null
            isNoteEditorOpen.value = false
            editingHabit.value = null
            isAddHabitDialogOpen.value = false
            quoteDialogState.value = null

            onComplete()
        }
    }

    override fun onCleared() {
        super.onCleared()
        com.example.reminder.AlarmSoundManager.stopAlarm(getApplication())
    }
}
