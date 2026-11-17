package com.example.ui.calendar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.example.data.model.Priority
import com.example.data.model.TaskItem
import com.example.ui.TaskViewModel
import com.example.ui.components.TaskCard
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.PriorityHigh
import com.example.ui.theme.PriorityMedium
import com.example.ui.theme.TealDeep
import com.example.ui.theme.TealNeon
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartCalendarScreen(
    viewModel: TaskViewModel,
    modifier: Modifier = Modifier
) {
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val calYear by viewModel.calendarYear.collectAsStateWithLifecycle()
    val calMonth by viewModel.calendarMonth.collectAsStateWithLifecycle()
    val monthTitle by viewModel.calendarMonthTitle.collectAsStateWithLifecycle()
    val tasksByDate by viewModel.tasksByDateMap.collectAsStateWithLifecycle()

    val todayStr = viewModel.todayDateString
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val prettyDateFormatter = remember { SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()) }

    // Selected day tasks
    val dayTasks = tasksByDate[selectedDate] ?: emptyList()
    val dayTotal = dayTasks.size
    val dayDone = dayTasks.count { it.isCompleted }
    val dayProgressPct = if (dayTotal > 0) dayDone.toFloat() / dayTotal else 0f

    // Days in current month grid calculation
    val daysGrid = remember(calYear, calMonth) {
        val list = mutableListOf<CalendarDay>()
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, calYear)
        cal.set(Calendar.MONTH, calMonth)
        cal.set(Calendar.DAY_OF_MONTH, 1)

        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1 (Sun) to 7 (Sat)
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Previous month filler days
        val prevCal = Calendar.getInstance()
        prevCal.set(Calendar.YEAR, calYear)
        prevCal.set(Calendar.MONTH, calMonth)
        prevCal.add(Calendar.MONTH, -1)
        val prevMaxDays = prevCal.getActualMaximum(Calendar.DAY_OF_MONTH)

        for (i in (firstDayOfWeek - 1) downTo 1) {
            prevCal.set(Calendar.DAY_OF_MONTH, prevMaxDays - i + 1)
            list.add(
                CalendarDay(
                    dayNumber = prevMaxDays - i + 1,
                    dateString = dateFormat.format(prevCal.time),
                    isCurrentMonth = false
                )
            )
        }

        // Current month days
        for (day in 1..maxDays) {
            cal.set(Calendar.DAY_OF_MONTH, day)
            list.add(
                CalendarDay(
                    dayNumber = day,
                    dateString = dateFormat.format(cal.time),
                    isCurrentMonth = true
                )
            )
        }

        // Next month filler days to complete rows (up to multiple of 7)
        val remainder = list.size % 7
        if (remainder != 0) {
            val nextDays = 7 - remainder
            val nextCal = Calendar.getInstance()
            nextCal.set(Calendar.YEAR, calYear)
            nextCal.set(Calendar.MONTH, calMonth)
            nextCal.add(Calendar.MONTH, 1)
            for (day in 1..nextDays) {
                nextCal.set(Calendar.DAY_OF_MONTH, day)
                list.add(
                    CalendarDay(
                        dayNumber = day,
                        dateString = dateFormat.format(nextCal.time),
                        isCurrentMonth = false
                    )
                )
            }
        }

        list
    }

    val daysOfWeek = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    // Workload evaluation
    val workloadText = when {
        dayTotal == 0 -> "🏖️ Free Day • No tasks scheduled"
        dayTotal <= 2 -> "🌱 Light Day • Easily manageable"
        dayTotal <= 4 -> "⚡ Balanced • Optimal productivity"
        else -> "🔥 Heavy Day • $dayTotal tasks scheduled"
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(TealNeon),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = TealDeep,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = "Smart Calendar",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.jumpToTodayCalendar() },
                        colors = ButtonDefaults.textButtonColors(contentColor = TealNeon),
                        modifier = Modifier.testTag("jump_to_today_button")
                    ) {
                        Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Today", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("calendar_scroll_container"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Month Header Selector
            item(key = "month_header") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        // Month switcher row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { viewModel.previousCalendarMonth() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Previous Month",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Text(
                                text = monthTitle,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 17.sp
                                )
                            )

                            IconButton(onClick = { viewModel.nextCalendarMonth() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Next Month",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Day of week headers
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            daysOfWeek.forEach { dayName ->
                                Text(
                                    text = dayName,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (dayName == "Sun" || dayName == "Sat") {
                                            TealNeon.copy(alpha = 0.7f)
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Calendar Grid (rows of 7 days)
                        val chunkedDays = daysGrid.chunked(7)
                        chunkedDays.forEach { week ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                week.forEach { cDay ->
                                    val isSelected = cDay.dateString == selectedDate
                                    val isToday = cDay.dateString == todayStr
                                    val dayTaskList = tasksByDate[cDay.dateString] ?: emptyList()
                                    val hasHighPriority = dayTaskList.any { it.priority == Priority.HIGH && !it.isCompleted }
                                    val hasPending = dayTaskList.any { !it.isCompleted }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isSelected -> TealNeon
                                                    isToday -> TealNeon.copy(alpha = 0.15f)
                                                    else -> Color.Transparent
                                                }
                                            )
                                            .border(
                                                width = if (isToday && !isSelected) 1.5.dp else 0.dp,
                                                color = if (isToday && !isSelected) TealNeon else Color.Transparent,
                                                shape = CircleShape
                                            )
                                            .clickable {
                                                viewModel.onDateSelected(cDay.dateString)
                                            }
                                            .testTag("cal_day_${cDay.dateString}"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = "${cDay.dayNumber}",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                                    color = when {
                                                        isSelected -> TealDeep
                                                        !cDay.isCurrentMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                                        isToday -> TealNeon
                                                        else -> MaterialTheme.colorScheme.onSurface
                                                    },
                                                    fontSize = 12.sp
                                                )
                                            )

                                            // Task density dots
                                            if (dayTaskList.isNotEmpty()) {
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                    modifier = Modifier.padding(top = 2.dp)
                                                ) {
                                                    if (hasHighPriority) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(4.dp)
                                                                .clip(CircleShape)
                                                                .background(if (isSelected) PriorityHigh else PriorityHigh)
                                                        )
                                                    }
                                                    if (hasPending) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(4.dp)
                                                                .clip(CircleShape)
                                                                .background(if (isSelected) TealDeep else TealNeon)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Smart Workload & Day Progress Card
            item(key = "workload_card") {
                val parsed = try { dateFormat.parse(selectedDate) } catch (_: Exception) { null }
                val formattedHeader = if (parsed != null) prettyDateFormatter.format(parsed) else selectedDate

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            Brush.horizontalGradient(listOf(TealNeon.copy(alpha = 0.4f), Color.Transparent)),
                            RoundedCornerShape(16.dp)
                        ),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = formattedHeader,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Text(
                                    text = workloadText,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TealNeon,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }

                            if (dayTotal > 0) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = "$dayDone / $dayTotal done",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        if (dayTotal > 0) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                LinearProgressIndicator(
                                    progress = { dayProgressPct },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = TealNeon,
                                    trackColor = Color(0xFF162529)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Quick Add Task for this day button
                        OutlinedButton(
                            onClick = { viewModel.openAddTask() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TealNeon),
                            border = androidx.compose.foundation.BorderStroke(1.dp, TealNeon.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Task for this Day", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Agenda List Header
            item(key = "agenda_title") {
                Text(
                    text = "Agenda for Selected Day",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Agenda Items
            if (dayTasks.isEmpty()) {
                item(key = "agenda_empty") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Event,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No tasks scheduled for this day.",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            } else {
                items(
                    items = dayTasks,
                    key = { it.id }
                ) { task ->
                    TaskCard(
                        task = task,
                        onToggleComplete = { viewModel.toggleTaskCompletion(task) },
                        onEdit = { viewModel.openEditTask(task) },
                        onDelete = { viewModel.deleteTask(task) },
                        onSnooze = { viewModel.openSnoozeDialog(task) },
                        onImageClick = { uri -> viewModel.openImagePreview(uri) }
                    )
                }
            }

            // Option banner ad
            item(key = "calendar_banner_ad") {
                com.example.ads.ScreenOptionBannerAd(modifier = Modifier.padding(top = 10.dp))
            }
        }
    }
}

private data class CalendarDay(
    val dayNumber: Int,
    val dateString: String,
    val isCurrentMonth: Boolean
)
