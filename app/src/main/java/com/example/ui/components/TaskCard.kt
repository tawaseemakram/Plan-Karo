package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Snooze
import com.example.data.model.RepeatMode
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Priority
import com.example.data.model.TaskCategory
import com.example.data.model.TaskItem
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.PriorityHigh
import com.example.ui.theme.TealNeon
import com.example.ui.theme.TealNeonDark
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskCard(
    task: TaskItem,
    onToggleComplete: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSnooze: () -> Unit,
    onImageClick: (String) -> Unit,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var expandedNotes by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    val isOverdue = task.reminderTimeMillis != null &&
            !task.isCompleted &&
            !task.isReminderDismissed &&
            task.reminderTimeMillis <= System.currentTimeMillis()

    val cardBorderColor by animateColorAsState(
        targetValue = when {
            isOverdue -> PriorityHigh
            task.isCompleted -> Color.Transparent
            task.priority == Priority.HIGH -> PriorityHigh.copy(alpha = 0.5f)
            else -> TealNeon.copy(alpha = 0.15f)
        },
        animationSpec = tween(300),
        label = "cardBorderColor"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("task_item_${task.id}")
            .border(
                width = if (isOverdue) 1.5.dp else 1.dp,
                color = cardBorderColor,
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted) DarkSurfaceCard.copy(alpha = 0.6f) else DarkSurfaceCard
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (task.isCompleted) 1.dp else 3.dp)
    ) {
        if (task.isCompleted) {
            // Sleek, compact fitted layout for completed tasks
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Completed Checkbox
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(TealNeon)
                        .clickable { onToggleComplete() }
                        .testTag("task_checkbox_${task.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Completed",
                        tint = Color(0xFF00382E),
                        modifier = Modifier.size(15.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Title + small completed indicator
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onToggleComplete() }
                ) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                            textDecoration = TextDecoration.LineThrough
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    FlowRow(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 3.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = TealNeon.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "Done ✓",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TealNeon,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }

                        if (task.repeatMode != RepeatMode.NONE) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF6C63FF).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "🔁 ${task.repeatMode.label}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFF9D95FF),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }

                        if (task.category.isNotBlank() && task.category != "Personal") {
                            Text(
                                text = "• ${task.category}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    fontSize = 10.sp
                                ),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (!task.audioUri.isNullOrBlank()) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice note",
                                tint = TealNeon.copy(alpha = 0.6f),
                                modifier = Modifier.size(11.dp)
                            )
                        }

                        if (!task.imageUri.isNullOrBlank()) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = "Image",
                                tint = TealNeon.copy(alpha = 0.6f),
                                modifier = Modifier.size(11.dp)
                            )
                        }

                        if (task.notes.isNotBlank()) {
                            Icon(
                                imageVector = Icons.Default.Notes,
                                contentDescription = "Notes",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                }

                // Delete quick action
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete task",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        } else {
            // Clean, sleek, spacious interface for active tasks
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEdit() }
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                // Main Header Row: Checkbox, Title, Reorder & Action Menu
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Modern Rounded Checkbox with clear touch target
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .border(
                                width = 1.8.dp,
                                color = TealNeon.copy(alpha = 0.85f),
                                shape = CircleShape
                            )
                            .clickable { onToggleComplete() }
                            .testTag("task_checkbox_${task.id}"),
                        contentAlignment = Alignment.Center
                    ) {
                        // Empty for active task
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Title
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp,
                            lineHeight = 20.sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Reorder arrows if available
                    if (onMoveUp != null || onMoveDown != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (onMoveUp != null) {
                                IconButton(
                                    onClick = onMoveUp,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowUp,
                                        contentDescription = "Move Up",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            if (onMoveDown != null) {
                                IconButton(
                                    onClick = onMoveDown,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Move Down",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 3-dots Menu Button
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Task options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit Task") },
                                onClick = {
                                    showMenu = false
                                    onEdit()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Edit, contentDescription = null)
                                }
                            )

                            if (task.reminderTimeMillis != null) {
                                DropdownMenuItem(
                                    text = { Text("Snooze Reminder") },
                                    onClick = {
                                        showMenu = false
                                        onSnooze()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Snooze, contentDescription = null)
                                    }
                                )
                            }

                            DropdownMenuItem(
                                text = { Text("Delete Task", color = PriorityHigh) },
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = null,
                                        tint = PriorityHigh
                                    )
                                }
                            )
                        }
                    }
                }

                // 2. UPER: Notes and Additional Details (as requested by user)
                // Notes Preview Section
                if (task.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(7.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .clickable { expandedNotes = !expandedNotes }
                            .padding(horizontal = 9.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notes,
                            contentDescription = null,
                            tint = TealNeon.copy(alpha = 0.85f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = task.notes,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            ),
                            maxLines = if (expandedNotes) Int.MAX_VALUE else 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (task.notes.length > 60 || task.notes.contains("\n")) {
                            Icon(
                                imageVector = if (expandedNotes) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = TealNeon.copy(alpha = 0.85f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Attached Audio Voice Note Player (Additional Detail)
                if (!task.audioUri.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(7.dp))
                    Box(modifier = Modifier.padding(start = 36.dp)) {
                        TaskCardAudioPlayer(
                            audioUri = task.audioUri,
                            durationSeconds = task.audioDurationSeconds,
                            taskId = task.id
                        )
                    }
                }

                // Attached Image Thumbnail (Additional Detail)
                if (!task.imageUri.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(7.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 36.dp)
                            .height(100.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .clickable { onImageClick(task.imageUri) }
                    ) {
                        AsyncImage(
                            model = task.imageUri,
                            contentDescription = "Task image attachment",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().height(100.dp)
                        )
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(6.dp),
                            shape = RoundedCornerShape(4.dp),
                            color = Color.Black.copy(alpha = 0.65f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = "View Photo",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }
                }

                // 3. NICHE: Category (Personal, Education, Work...), Priority, Time, and Repeat
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Category Badge (Personal, Education, Work, Health, etc.)
                    CategoryBadge(categoryName = task.category)

                    // Priority Badge
                    PriorityBadge(priority = task.priority)

                    // Scheduled Reminder Time Badge (⏰ Time)
                    if (task.reminderTimeMillis != null) {
                        ReminderBadge(
                            reminderTimeMillis = task.reminderTimeMillis,
                            isOverdue = isOverdue,
                            onSnooze = onSnooze
                        )
                    }

                    // Repeat Badge (Daily, Weekly, Monthly)
                    if (task.repeatMode != RepeatMode.NONE) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF6C63FF).copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6C63FF).copy(alpha = 0.45f)),
                            modifier = Modifier.testTag("task_repeat_badge")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Repeat,
                                    contentDescription = "Repeats ${task.repeatMode.label}",
                                    tint = Color(0xFF9D95FF),
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = task.repeatMode.label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color(0xFF9D95FF),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.5.sp
                                    ),
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryBadge(
    categoryName: String,
    modifier: Modifier = Modifier
) {
    val category = TaskCategory.findCategory(TaskCategory.DEFAULT_CATEGORIES, categoryName)
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = category.containerColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, category.color.copy(alpha = 0.45f)),
        modifier = modifier.testTag("task_category_badge")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(category.color)
            )
            Text(
                text = category.name,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = category.color,
                    fontSize = 10.5.sp
                ),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PriorityBadge(priority: Priority) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = priority.containerColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, priority.color.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(priority.color)
            )
            Text(
                text = priority.label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = priority.color,
                    fontSize = 10.5.sp
                ),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ReminderBadge(
    reminderTimeMillis: Long,
    isOverdue: Boolean,
    onSnooze: () -> Unit
) {
    val timeFormatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val formattedTime = timeFormatter.format(Date(reminderTimeMillis))

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (isOverdue) PriorityHigh.copy(alpha = 0.15f) else TealNeonDark.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isOverdue) PriorityHigh.copy(alpha = 0.8f) else TealNeon.copy(alpha = 0.4f)
        ),
        modifier = Modifier.clickable { onSnooze() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (isOverdue) Icons.Default.NotificationsActive else Icons.Default.Alarm,
                contentDescription = null,
                tint = if (isOverdue) PriorityHigh else TealNeon,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = if (isOverdue) "Due: $formattedTime (Snooze)" else formattedTime,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = if (isOverdue) PriorityHigh else TealNeon,
                    fontSize = 10.5.sp
                ),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
