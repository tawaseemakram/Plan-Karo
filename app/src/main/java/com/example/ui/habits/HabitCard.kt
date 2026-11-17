package com.example.ui.habits

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.HabitItem
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.PriorityHigh
import com.example.ui.theme.PriorityMedium
import com.example.ui.theme.TealDeep
import com.example.ui.theme.TealNeon
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HabitCard(
    habit: HabitItem,
    targetDate: String,
    onToggleCompletion: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTogglePastDate: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isCompletedToday = habit.isCompletedOn(targetDate)
    var showMenu by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val dayOfWeekFormat = remember { SimpleDateFormat("E", Locale.getDefault()) }

    // Last 7 days completion history
    val last7Days = remember(habit.completedDates) {
        val list = mutableListOf<Pair<String, String>>() // Pair of dayName to dateString
        val cal = Calendar.getInstance()
        for (i in 6 downTo 0) {
            val c = Calendar.getInstance()
            c.add(Calendar.DAY_OF_YEAR, -i)
            val dStr = dateFormat.format(c.time)
            val dayName = dayOfWeekFormat.format(c.time).take(1) // M, T, W...
            list.add(Pair(dayName, dStr))
        }
        list
    }

    val cardBorderColor by animateColorAsState(
        targetValue = if (isCompletedToday) TealNeon.copy(alpha = 0.5f) else Color(0xFF223A40),
        label = "habitBorderColor"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("habit_card_${habit.id}")
            .border(1.dp, cardBorderColor, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurfaceCard
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top Row: Checkbox, Title & Routine, Streak badge & Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Round Checkbox
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            if (isCompletedToday) TealNeon else Color.Transparent
                        )
                        .border(
                            width = 2.dp,
                            color = if (isCompletedToday) TealNeon else TealNeon.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                        .clickable { onToggleCompletion() }
                        .testTag("habit_checkbox_${habit.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompletedToday) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Completed",
                            tint = TealDeep,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onToggleCompletion() }
                ) {
                    Text(
                        text = habit.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Streak Badge 🔥
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PriorityMedium.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PriorityMedium.copy(alpha = 0.4f)),
                    modifier = Modifier.padding(end = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Streak",
                            tint = PriorityMedium,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "${habit.streakCount}d",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = PriorityMedium,
                                fontSize = 12.sp
                            ),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // Options Menu
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Habit") },
                            onClick = {
                                showMenu = false
                                onEdit()
                            },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                        )

                        DropdownMenuItem(
                            text = { Text("Delete Habit", color = PriorityHigh) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = PriorityHigh)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Full-width FlowRow for Routine, Weekly Goal, Reminder & Category badges
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Routine badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "${habit.routineTime.icon} ${habit.routineTime.title}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }

                // Category badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                ) {
                    Text(
                        text = habit.category,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }

                // Weekly Goal badge
                if (habit.targetDaysPerWeek < 7) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = TealNeon.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "${habit.targetDaysPerWeek}d/wk",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TealNeon,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }

                // Optional Reminder Time badge
                if (!habit.reminderTime.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = TealNeon.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "⏰ ${habit.reminderTime}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TealNeon,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 7-Day Completion Mini Tracker Dots
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                last7Days.forEach { (dayLetter, dateStr) ->
                    val isDone = habit.isCompletedOn(dateStr)
                    val isDayTarget = dateStr == targetDate

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable {
                            onTogglePastDate?.invoke(dateStr)
                        }
                    ) {
                        Text(
                            text = dayLetter,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isDayTarget) TealNeon else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isDayTarget) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 10.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isDone) TealNeon else Color.Transparent
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isDone) TealNeon else Color(0xFF3B565D),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isDone) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = TealDeep,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
