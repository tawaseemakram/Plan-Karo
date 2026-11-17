package com.example.ui

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TaskCategory
import com.example.data.model.TaskItem
import com.example.ui.components.DailyProgressCard
import com.example.ui.components.TaskCard
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.PriorityHigh
import com.example.ui.theme.TealDeep
import com.example.ui.theme.TealNeon
import com.example.ui.theme.TealNeonDark
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(
    viewModel: TaskViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tasks by viewModel.uiTasks.collectAsStateWithLifecycle()
    val progress by viewModel.dailyProgress.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val currentFilter by viewModel.currentFilter.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val activeReminder by viewModel.activeReminderTask.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val selectedCategoryFilter by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()
    val bannerQuote by viewModel.bannerQuote.collectAsStateWithLifecycle()
    val todayDateString by viewModel.todayDateFlow.collectAsStateWithLifecycle()

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val displayDateFormatter = remember { SimpleDateFormat("EEE, MMM d", Locale.getDefault()) }

    // Date computation
    val isToday = selectedDate == todayDateString

    fun changeDay(offset: Int) {
        val cal = Calendar.getInstance()
        try {
            val parsedDate = dateFormat.parse(selectedDate)
            if (parsedDate != null) cal.time = parsedDate
        } catch (_: Exception) {}
        cal.add(Calendar.DAY_OF_YEAR, offset)
        viewModel.onDateSelected(dateFormat.format(cal.time))
    }

    fun openCalendarPicker() {
        val cal = Calendar.getInstance()
        try {
            val parsed = dateFormat.parse(selectedDate)
            if (parsed != null) cal.time = parsed
        } catch (_: Exception) {}

        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, month)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                viewModel.onDateSelected(dateFormat.format(cal.time))
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    LaunchedEffect(Unit) {
        viewModel.seedSampleTasksIfEmpty()
    }

    // Drag-and-drop state
    var draggedIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = DarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Image(
                            painter = painterResource(id = com.example.R.drawable.ic_plan_karo_logo),
                            contentDescription = "Plan Karo Logo",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )

                        Column {
                            Text(
                                text = stringResource(id = com.example.R.string.app_name),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontSize = 18.sp
                                )
                            )
                            Text(
                                text = stringResource(id = com.example.R.string.app_tagline),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TealNeon,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                maxLines = 1
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { openCalendarPicker() },
                        modifier = Modifier.testTag("calendar_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Select date",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBg
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openAddTask() },
                containerColor = TealNeon,
                contentColor = TealDeep,
                shape = CircleShape,
                modifier = Modifier
                    .size(56.dp)
                    .testTag("add_task_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Task",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .widthIn(max = 700.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Daily Inspirational Quote Card directly under Plan Karo Logo header
            val quoteAccentColor = remember(bannerQuote.category.hexColor) {
                try {
                    Color(android.graphics.Color.parseColor(bannerQuote.category.hexColor))
                } catch (_: Exception) {
                    TealNeon
                }
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .testTag("daily_quote_banner"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = DarkSurface
                ),
                border = BorderStroke(1.dp, quoteAccentColor.copy(alpha = 0.35f))
            ) {
                AnimatedContent(
                    targetState = bannerQuote,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                    },
                    label = "daily_quote_transition"
                ) { quote ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        quoteAccentColor.copy(alpha = 0.08f),
                                        DarkSurface
                                    )
                                )
                            )
                            .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(quoteAccentColor.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = quote.category.emoji,
                                fontSize = 15.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "\"${quote.text}\"",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontStyle = FontStyle.Italic,
                                    fontSize = 12.5.sp,
                                    lineHeight = 17.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "— ${quote.author}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = quoteAccentColor,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Text(
                                    text = "• ${quote.category.label}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                        fontSize = 9.5.sp
                                    ),
                                    maxLines = 1
                                )
                            }
                        }

                        IconButton(
                            onClick = { viewModel.refreshBannerQuote() },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("refresh_quote_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Next Quote",
                                tint = quoteAccentColor.copy(alpha = 0.9f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Permanent Search Bar at the top of the task list (as requested)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = {
                    Text(
                        "Search tasks by keyword, notes, or label…",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp)
                    )
                },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = TealNeon)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = TealNeon)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("search_text_field"),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TealNeon,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface
                ),
                shape = RoundedCornerShape(14.dp)
            )

            // When searching, show live feedback chip
            if (searchQuery.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Filtered by \"$searchQuery\" (${tasks.size} tasks found)",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TealNeon,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    )
                    Text(
                        text = "Clear filter",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.clickable { viewModel.onSearchQueryChanged("") }
                    )
                }
            }

            // Date Navigation Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { changeDay(-1) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous Day",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = DarkSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.clickable { openCalendarPicker() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = TealNeon,
                            modifier = Modifier.size(16.dp)
                        )

                        val parsedDate = try {
                            dateFormat.parse(selectedDate)
                        } catch (_: Exception) { null }
                        val displayStr = if (parsedDate != null) displayDateFormatter.format(parsedDate) else selectedDate

                        Text(
                            text = if (isToday) "Today, $displayStr" else displayStr,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (isToday) TealNeon else MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }

                IconButton(
                    onClick = { changeDay(1) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next Day",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Urgent Overdue Reminder Banner (if any)
            if (activeReminder != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .testTag("urgent_reminder_banner"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = PriorityHigh.copy(alpha = 0.2f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PriorityHigh)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = PriorityHigh,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "Reminder: ${activeReminder!!.title}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Text(
                                    text = "Tap Snooze to delay or Done to finish",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = { viewModel.openSnoozeDialog(activeReminder!!) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PriorityHigh,
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Snooze", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { viewModel.toggleTaskCompletion(activeReminder!!) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = TealNeon,
                                    contentColor = TealDeep
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Done", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Main Content LazyColumn
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("task_list"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Item 0: Daily Visual Progress Card
                item(key = "progress_card") {
                    DailyProgressCard(progress = progress)
                }

                // Item 1: Filter Chips Row
                item(key = "filter_chips") {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(TaskFilter.values().size) { idx ->
                            val filter = TaskFilter.values()[idx]
                            val isSelected = currentFilter == filter

                            val count = when (filter) {
                                TaskFilter.TASK -> progress.oneTimeTasksCount
                                TaskFilter.DAILY_TASK -> progress.dailyTasksCount
                                TaskFilter.WEEKLY_TASK -> progress.weeklyTasksCount
                                TaskFilter.MONTHLY_TASK -> progress.monthlyTasksCount
                                TaskFilter.PENDING -> progress.pendingCount
                                TaskFilter.COMPLETED -> progress.completedCount
                                TaskFilter.HIGH_PRIORITY -> progress.highPriorityPending
                                TaskFilter.ALL -> progress.allTasksCount
                            }

                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.onFilterSelected(filter) },
                                label = {
                                    Text(
                                        text = "${filter.title} ($count)",
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TealNeon,
                                    selectedLabelColor = TealDeep,
                                    containerColor = DarkSurface,
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = MaterialTheme.colorScheme.outlineVariant,
                                    selectedBorderColor = TealNeon
                                ),
                                modifier = Modifier.testTag("filter_${filter.name.lowercase()}")
                            )
                        }
                    }
                }

                // Item 1.5: Category Label Filter Chips Row
                item(key = "category_filter_chips") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LazyRow(
                            modifier = Modifier
                                .weight(1f)
                                .testTag("category_filter_row"),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // "All Labels" Chip
                            item(key = "all_categories") {
                                val isAllSelected = selectedCategoryFilter == null
                                FilterChip(
                                    selected = isAllSelected,
                                    onClick = { viewModel.selectCategoryFilter(null) },
                                    label = {
                                        Text(
                                            text = "All Labels",
                                            fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Label,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = if (isAllSelected) TealDeep else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = TealNeon,
                                        selectedLabelColor = TealDeep,
                                        containerColor = DarkSurface,
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isAllSelected,
                                        borderColor = MaterialTheme.colorScheme.outlineVariant,
                                        selectedBorderColor = TealNeon
                                    ),
                                    modifier = Modifier.testTag("filter_category_all")
                                )
                            }

                            // Dynamic Category Chips
                            items(categories) { cat ->
                                val isSelected = selectedCategoryFilter.equals(cat.name, ignoreCase = true)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        if (isSelected) {
                                            viewModel.selectCategoryFilter(null)
                                        } else {
                                            viewModel.selectCategoryFilter(cat.name)
                                        }
                                    },
                                    label = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
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
                                        containerColor = DarkSurface,
                                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderColor = MaterialTheme.colorScheme.outlineVariant,
                                        selectedBorderColor = cat.color
                                    ),
                                    modifier = Modifier.testTag("filter_category_${cat.name.lowercase()}")
                                )
                            }
                        }

                        // Button to open Category Management Sheet
                        Surface(
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .clickable { viewModel.openCategorySheet() }
                                .testTag("manage_categories_btn"),
                            shape = RoundedCornerShape(8.dp),
                            color = DarkSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, TealNeon.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Manage Categories",
                                    tint = TealNeon,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Label",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TealNeon,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }
                }

                // Tasks list or Empty State
                if (tasks.isEmpty()) {
                    item(key = "empty_state") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp, horizontal = 24.dp)
                                .testTag("empty_tasks_container"),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(DarkSurface)
                                    .border(1.dp, TealNeon.copy(alpha = 0.3f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = TealNeon,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            val emptyTitle = when {
                                searchQuery.isNotEmpty() -> "No matching tasks found"
                                currentFilter == TaskFilter.DAILY_TASK -> "No Daily Tasks yet"
                                currentFilter == TaskFilter.WEEKLY_TASK -> "No Weekly Tasks yet"
                                currentFilter == TaskFilter.MONTHLY_TASK -> "No Monthly Tasks yet"
                                currentFilter == TaskFilter.COMPLETED -> "No completed tasks yet"
                                currentFilter == TaskFilter.HIGH_PRIORITY -> "No high priority tasks"
                                else -> "No tasks for this day"
                            }
                            val emptySubtitle = when {
                                searchQuery.isNotEmpty() -> "Try a different search term or clear the filter."
                                currentFilter == TaskFilter.DAILY_TASK -> "Create a Daily repeating task here and it will automatically work every day."
                                currentFilter == TaskFilter.WEEKLY_TASK -> "Create a Weekly repeating task here to repeat automatically every week."
                                currentFilter == TaskFilter.MONTHLY_TASK -> "Create a Monthly repeating task here to repeat automatically every month."
                                else -> "Tap the + button below to create your first task with priority, reminders, notes, and photos."
                            }
                            val emptyButtonLabel = when (currentFilter) {
                                TaskFilter.DAILY_TASK -> "Add Daily Task"
                                TaskFilter.WEEKLY_TASK -> "Add Weekly Task"
                                TaskFilter.MONTHLY_TASK -> "Add Monthly Task"
                                else -> "Add First Task"
                            }

                            Text(
                                text = emptyTitle,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = emptySubtitle,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 20.sp
                                ),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = { viewModel.openAddTask() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = TealNeon,
                                    contentColor = TealDeep
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(emptyButtonLabel, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(40.dp))
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            // Empty-state banner ad with generous separation from CTA button
                            com.example.ads.AddFirstTaskBottomAd()
                        }
                    }
                } else {
                    itemsIndexed(
                        items = tasks,
                        key = { _, item -> item.id }
                    ) { index, task ->
                        val isBeingDragged = draggedIndex == index

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    if (isBeingDragged) {
                                        translationY = dragOffsetY
                                        shadowElevation = 8.dp.toPx()
                                        scaleX = 1.02f
                                        scaleY = 1.02f
                                    }
                                }
                        ) {
                            TaskCard(
                                task = task,
                                onToggleComplete = {
                                    val willBeCompleted = !task.isCompleted
                                    viewModel.toggleTaskCompletion(task)
                                    if (willBeCompleted) {
                                        val toastText = when (task.repeatMode) {
                                            com.example.data.model.RepeatMode.DAILY -> "Completed! Next Daily Task scheduled for tomorrow 🔁"
                                            com.example.data.model.RepeatMode.WEEKLY -> "Completed! Next Weekly Task scheduled 🔁"
                                            com.example.data.model.RepeatMode.MONTHLY -> "Completed! Next Monthly Task scheduled 🔁"
                                            com.example.data.model.RepeatMode.NONE -> "Task completed! 🎉"
                                        }
                                        Toast.makeText(context, toastText, Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onEdit = { viewModel.openEditTask(task) },
                                onDelete = { viewModel.deleteTask(task) },
                                onSnooze = { viewModel.openSnoozeDialog(task) },
                                onImageClick = { uri -> viewModel.openImagePreview(uri) },
                                onMoveUp = if (index > 0) { { viewModel.moveTaskUp(task) } } else null,
                                onMoveDown = if (index < tasks.size - 1) { { viewModel.moveTaskDown(task) } } else null,
                                modifier = Modifier.pointerInput(task.id) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            draggedIndex = index
                                            dragOffsetY = 0f
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragOffsetY += dragAmount.y

                                            // Reorder threshold (~80dp threshold)
                                            val threshold = 160f
                                            if (dragOffsetY > threshold && index < tasks.size - 1) {
                                                viewModel.moveTask(index, index + 1)
                                                draggedIndex = index + 1
                                                dragOffsetY = 0f
                                            } else if (dragOffsetY < -threshold && index > 0) {
                                                viewModel.moveTask(index, index - 1)
                                                draggedIndex = index - 1
                                                dragOffsetY = 0f
                                            }
                                        },
                                        onDragEnd = {
                                            draggedIndex = -1
                                            dragOffsetY = 0f
                                        },
                                        onDragCancel = {
                                            draggedIndex = -1
                                            dragOffsetY = 0f
                                        }
                                    )
                                }
                            )
                        }
                    }
                }

                // Banner ad for TaskScreen option
                item(key = "task_screen_banner_ad") {
                    com.example.ads.ScreenOptionBannerAd(modifier = Modifier.padding(top = 10.dp))
                }
            }
        }
    }
}
