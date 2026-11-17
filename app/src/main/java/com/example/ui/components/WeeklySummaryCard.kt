package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.ui.DayProductivityTrend
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.TealDeep
import com.example.ui.theme.TealNeon
import com.example.ui.theme.TealNeonDark

@Composable
fun WeeklySummaryCard(
    trends: List<DayProductivityTrend>,
    modifier: Modifier = Modifier,
    onDaySelected: ((DayProductivityTrend) -> Unit)? = null
) {
    if (trends.isEmpty()) return

    val totalCompleted7Days = trends.sumOf { it.completedTasks }
    val totalTasks7Days = trends.sumOf { it.totalTasks }
    val daysWithTasks = trends.filter { it.totalTasks > 0 }
    val avgCompletionRate = if (daysWithTasks.isNotEmpty()) {
        (daysWithTasks.map { it.completionPercentage }.average()).toInt()
    } else {
        0
    }

    var selectedIndex by remember { mutableStateOf<Int?>(trends.lastIndex) }
    val selectedDay = selectedIndex?.let { trends.getOrNull(it) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("weekly_summary_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, TealNeon.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header Row: Icon, Title & 7-Day Average Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(TealNeon.copy(alpha = 0.15f))
                            .border(1.dp, TealNeon.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "Weekly Summary Chart Icon",
                            tint = TealNeon,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Weekly Summary",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp
                            )
                        )
                        Text(
                            text = "Productivity Trend • Last 7 Days",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = TealNeon.copy(alpha = 0.16f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TealNeon.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = TealNeon,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "$avgCompletionRate% Avg",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TealNeon,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Visual Bar Chart Container
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = DarkSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("weekly_bar_chart_container")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 12.dp)
                ) {
                    // Bar Chart Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        trends.forEachIndexed { index, day ->
                            val isSelected = selectedIndex == index
                            val isToday = index == trends.lastIndex

                            WeeklyBarItem(
                                day = day,
                                isSelected = isSelected,
                                isToday = isToday,
                                onClick = {
                                    selectedIndex = index
                                    onDaySelected?.invoke(day)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .testTag("weekly_summary_bar_$index")
                            )
                        }
                    }
                }
            }

            // Interactive Day Inspector Card
            AnimatedVisibility(
                visible = selectedDay != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                if (selectedDay != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DarkSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, TealNeon.copy(alpha = 0.2f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("weekly_summary_selected_day_card")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "${selectedDay.dayLabel} (${selectedDay.dateString})",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                    if (selectedIndex == trends.lastIndex) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = TealNeon.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "Today",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = TealNeon,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = if (selectedDay.totalTasks > 0) {
                                        "${selectedDay.completedTasks} of ${selectedDay.totalTasks} tasks completed"
                                    } else {
                                        "No tasks scheduled for this day"
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = when {
                                    selectedDay.completionPercentage >= 80f -> TealNeon.copy(alpha = 0.2f)
                                    selectedDay.completionPercentage >= 50f -> Color(0xFFFFB800).copy(alpha = 0.2f)
                                    selectedDay.totalTasks > 0 -> Color(0xFF6C5CE7).copy(alpha = 0.2f)
                                    else -> Color.White.copy(alpha = 0.08f)
                                }
                            ) {
                                Text(
                                    text = when {
                                        selectedDay.completionPercentage >= 80f -> "${selectedDay.completionPercentage.toInt()}% 🚀"
                                        selectedDay.completionPercentage >= 50f -> "${selectedDay.completionPercentage.toInt()}% ⚡"
                                        selectedDay.totalTasks > 0 -> "${selectedDay.completionPercentage.toInt()}% 🌱"
                                        else -> "Rest Day 🧘"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            selectedDay.completionPercentage >= 80f -> TealNeon
                                            selectedDay.completionPercentage >= 50f -> Color(0xFFFFB800)
                                            selectedDay.totalTasks > 0 -> Color(0xFFA29BFE)
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        fontSize = 11.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Footer Summary Metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = TealNeon,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$totalCompleted7Days / $totalTasks7Days tasks done",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    )
                }

                val activeDaysCount = trends.count { it.completedTasks > 0 }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = null,
                        tint = Color(0xFFFFB800),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "$activeDaysCount/7 active days",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TealNeon,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun WeeklyBarItem(
    day: DayProductivityTrend,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pct = (day.completionPercentage / 100f).coerceIn(0f, 1f)

    // Animated bar height fill
    val animatedFill by animateFloatAsState(
        targetValue = pct,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "barFill_${day.dayLabel}"
    )

    Column(
        modifier = modifier
            .padding(horizontal = 2.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        // Percentage text or indicator above bar
        Text(
            text = if (day.totalTasks > 0) "${day.completionPercentage.toInt()}%" else "—",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) TealNeon else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            ),
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Bar Track
        Box(
            modifier = Modifier
                .width(if (isSelected) 22.dp else 18.dp)
                .height(90.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .border(
                    width = if (isSelected) 1.5.dp else 0.5.dp,
                    color = if (isSelected) TealNeon else Color.White.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(6.dp)
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Filled portion of the bar
            val fillHeightFraction = if (day.totalTasks > 0) animatedFill.coerceAtLeast(0.06f) else 0f
            if (fillHeightFraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(fillHeightFraction)
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                        .background(
                            brush = if (isSelected) {
                                Brush.verticalGradient(
                                    colors = listOf(Color(0xFF00FFD5), TealNeon, TealDeep)
                                )
                            } else {
                                Brush.verticalGradient(
                                    colors = listOf(TealNeon.copy(alpha = 0.95f), TealNeonDark.copy(alpha = 0.65f))
                                )
                            }
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Day label underneath
        Text(
            text = day.dayLabel,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                color = when {
                    isSelected -> TealNeon
                    isToday -> Color(0xFF00FFD5)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                fontSize = 10.sp
            ),
            textAlign = TextAlign.Center
        )

        // Today dot
        if (isToday) {
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(TealNeon)
            )
        } else {
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}
