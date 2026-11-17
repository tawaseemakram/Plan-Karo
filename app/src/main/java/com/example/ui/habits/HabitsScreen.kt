package com.example.ui.habits

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.HabitItem
import com.example.data.model.QuoteTriggerReason
import com.example.data.model.RoutineItem
import com.example.data.model.RoutineTime
import com.example.ui.TaskViewModel
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.PriorityMedium
import com.example.ui.theme.TealDeep
import com.example.ui.theme.TealNeon
import com.example.ui.theme.TealNeonDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsScreen(
    viewModel: TaskViewModel,
    modifier: Modifier = Modifier
) {
    val habits by viewModel.habits.collectAsStateWithLifecycle()
    val routines by viewModel.routines.collectAsStateWithLifecycle()
    val todayString = viewModel.todayDateString

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Habits, 1 = Daily Routines
    var selectedRoutineFilter by remember { mutableStateOf<RoutineTime?>(null) }
    var isAddRoutineDialogOpen by remember { mutableStateOf(false) }

    val isAddDialogOpen by viewModel.isAddHabitDialogOpen.collectAsStateWithLifecycle()
    val editingHabit by viewModel.editingHabit.collectAsStateWithLifecycle()

    val completedTodayCount = habits.count { it.isCompletedOn(todayString) }
    val totalCount = habits.size
    val habitProgressPct = if (totalCount > 0) completedTodayCount.toFloat() / totalCount else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = habitProgressPct,
        animationSpec = tween(500),
        label = "habitProgress"
    )

    val maxStreak = habits.maxOfOrNull { it.streakCount } ?: 0

    val filteredHabits = if (selectedRoutineFilter == null) {
        habits
    } else {
        habits.filter { it.routineTime == selectedRoutineFilter }
    }

    val filteredRoutines = if (selectedRoutineFilter == null) {
        routines
    } else {
        routines.filter { it.routineTime == selectedRoutineFilter }
    }

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
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(TealNeon),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SelfImprovement,
                                contentDescription = null,
                                tint = TealDeep,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Habits & Routines",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontSize = 18.sp
                                )
                            )
                            Text(
                                text = "Locally persisted with Room Database",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TealNeon,
                                    fontSize = 9.sp
                                )
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedTab == 0) {
                        viewModel.openAddHabitDialog(null)
                    } else {
                        isAddRoutineDialogOpen = true
                    }
                },
                containerColor = TealNeon,
                contentColor = TealDeep,
                shape = CircleShape,
                modifier = Modifier
                    .size(56.dp)
                    .testTag(if (selectedTab == 0) "add_habit_fab" else "add_routine_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = if (selectedTab == 0) "New Habit" else "New Routine",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Segmented Tab Row: Habits vs Routines
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = DarkBg,
                contentColor = TealNeon,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = TealNeon
                    )
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = "Daily Habits (${habits.size})",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 0) TealNeon else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = "Routines (${routines.size})",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 1) TealNeon else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("habits_and_routines_list"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (selectedTab == 0) {
                    // TAB 0: HABITS
                    item(key = "habit_header_card") {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.dp,
                                    brush = Brush.horizontalGradient(
                                        listOf(TealNeon.copy(alpha = 0.5f), Color.Transparent)
                                    ),
                                    shape = RoundedCornerShape(20.dp)
                                ),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "Today's Habit Mastery",
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            )
                                            Text(
                                                text = "$completedTodayCount of $totalCount completed today",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = PriorityMedium.copy(alpha = 0.15f),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, PriorityMedium.copy(alpha = 0.4f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.LocalFireDepartment,
                                                    contentDescription = null,
                                                    tint = PriorityMedium,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = "$maxStreak Day Streak",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        color = PriorityMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Habit Progress Bar
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(10.dp)
                                            .clip(RoundedCornerShape(5.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        LinearProgressIndicator(
                                            progress = { animatedProgress },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(10.dp)
                                                .clip(RoundedCornerShape(5.dp)),
                                            color = TealNeon,
                                            trackColor = Color(0xFF162529)
                                        )
                                    }
                                }
                            }
                        }

                        // Routine Filter Chips
                        item(key = "routine_filters_habits") {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                item {
                                    FilterChip(
                                        selected = selectedRoutineFilter == null,
                                        onClick = { selectedRoutineFilter = null },
                                        label = { Text("All Segments") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = TealNeon,
                                            selectedLabelColor = TealDeep,
                                            containerColor = DarkSurface,
                                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }

                                items(RoutineTime.values()) { rTime ->
                                    val isSelected = selectedRoutineFilter == rTime
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { selectedRoutineFilter = if (isSelected) null else rTime },
                                        label = { Text("${rTime.icon} ${rTime.title}") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = TealNeon,
                                            selectedLabelColor = TealDeep,
                                            containerColor = DarkSurface,
                                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    )
                                }
                            }
                        }

                        if (filteredHabits.isEmpty()) {
                            item(key = "empty_habits") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 48.dp, horizontal = 24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Repeat,
                                        contentDescription = null,
                                        tint = TealNeon,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "No habits found for this filter",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = { viewModel.openAddHabitDialog(null) },
                                        colors = ButtonDefaults.buttonColors(containerColor = TealNeon, contentColor = TealDeep)
                                    ) {
                                        Text("+ Add Habit")
                                    }
                                }
                            }
                        } else {
                            items(
                                items = filteredHabits,
                                key = { it.id }
                            ) { habit ->
                                HabitCard(
                                    habit = habit,
                                    targetDate = todayString,
                                    onToggleCompletion = { viewModel.toggleHabitToday(habit) },
                                    onEdit = { viewModel.openAddHabitDialog(habit) },
                                    onDelete = { viewModel.deleteHabit(habit) },
                                    onTogglePastDate = { pastDate ->
                                        viewModel.toggleHabitForSpecificDate(habit, pastDate)
                                    }
                                )
                            }
                        }
                } else {
                    // TAB 1: DAILY ROUTINES
                    item(key = "routine_overview_banner") {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, TealNeon.copy(alpha = 0.4f), RoundedCornerShape(20.dp)),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard)
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Default.Schedule, contentDescription = null, tint = TealNeon, modifier = Modifier.size(24.dp))
                                    Column {
                                        Text(
                                            text = "Scheduled Daily Routines",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Text(
                                            text = "${routines.count { it.isEnabled }} active routines in local database",
                                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Routine filter chips
                    item(key = "routine_filter_chips_tab1") {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(
                                    selected = selectedRoutineFilter == null,
                                    onClick = { selectedRoutineFilter = null },
                                    label = { Text("All Times") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = TealNeon,
                                        selectedLabelColor = TealDeep
                                    )
                                )
                            }
                            items(RoutineTime.values()) { rTime ->
                                val isSelected = selectedRoutineFilter == rTime
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedRoutineFilter = if (isSelected) null else rTime },
                                    label = { Text("${rTime.icon} ${rTime.title}") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = TealNeon,
                                        selectedLabelColor = TealDeep
                                    )
                                )
                            }
                        }
                    }

                    if (filteredRoutines.isEmpty()) {
                        item(key = "empty_routines") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 48.dp, horizontal = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = TealNeon, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No routines scheduled",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { isAddRoutineDialogOpen = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = TealNeon, contentColor = TealDeep)
                                ) {
                                    Text("+ Add Routine Schedule")
                                }
                            }
                        }
                    } else {
                        items(
                            items = filteredRoutines,
                            key = { it.id }
                        ) { routine ->
                            RoutineCard(
                                routine = routine,
                                onToggleEnabled = { viewModel.toggleRoutineEnabled(routine) },
                                onDelete = { viewModel.deleteRoutine(routine) }
                            )
                        }
                    }
                }

                // Banner ad for Habits option
                item(key = "habits_option_banner_ad") {
                    com.example.ads.ScreenOptionBannerAd(modifier = Modifier.padding(top = 10.dp))
                }
            }
        }
    }

    // Add / Edit Habit Dialog
    if (isAddDialogOpen) {
        HabitAddDialog(
            initialHabit = editingHabit,
            onDismiss = { viewModel.closeAddHabitDialog() },
            onSave = { id, title, category, routineTime, targetDays, reminderTime ->
                viewModel.saveHabit(id, title, category, routineTime, targetDays, reminderTime)
            },
            onHabitCreatedQuote = {
                viewModel.showQuote(QuoteTriggerReason.HABIT_CREATED)
            }
        )
    }

    // Add Routine Dialog
    if (isAddRoutineDialogOpen) {
        RoutineAddDialog(
            onDismiss = { isAddRoutineDialogOpen = false },
            onSave = { title, description, routineTime, scheduledTime ->
                viewModel.insertRoutine(title, description, routineTime, scheduledTime)
            },
            onRoutineCreatedQuote = {
                viewModel.showQuote(QuoteTriggerReason.ROUTINE_CREATED)
            }
        )
    }
}
