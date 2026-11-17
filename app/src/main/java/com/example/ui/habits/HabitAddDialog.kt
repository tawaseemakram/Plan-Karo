package com.example.ui.habits

import android.app.Activity
import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ads.AdManager
import com.example.data.model.HabitItem
import com.example.data.model.RoutineTime
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.PriorityHigh
import com.example.ui.theme.TealDeep
import com.example.ui.theme.TealNeon
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitAddDialog(
    initialHabit: HabitItem?,
    onDismiss: () -> Unit,
    onSave: (id: Long, title: String, category: String, routineTime: RoutineTime, targetDays: Int, reminderTime: String?) -> Unit,
    onHabitCreatedQuote: () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val isCreatingNew = initialHabit == null

    var title by remember { mutableStateOf(initialHabit?.title ?: "") }
    var category by remember { mutableStateOf(initialHabit?.category ?: "Health") }
    var routineTime by remember { mutableStateOf(initialHabit?.routineTime ?: RoutineTime.MORNING) }
    var targetDays by remember { mutableIntStateOf(initialHabit?.targetDaysPerWeek ?: 7) }
    var reminderTime by remember { mutableStateOf(initialHabit?.reminderTime) }
    var titleError by remember { mutableStateOf(false) }

    val presetHabits = listOf(
        "Drink 2.5L Water 💧",
        "Morning Meditation 🧘",
        "Read 20 Mins 📖",
        "10k Daily Steps 🚶",
        "Stretch & Yoga 🤸",
        "No Sugar Today 🥗",
        "Journal & Gratitude ✍️",
        "Plan Tomorrow 🎯"
    )

    val categories = listOf("Health", "Productivity", "Mindfulness", "Fitness", "Learning")
    val targetDayOptions = listOf(3, 4, 5, 6, 7)
    val quickTimes = listOf("07:00 AM", "08:00 AM", "01:00 PM", "06:00 PM", "09:00 PM")

    fun openTimePicker() {
        val cal = Calendar.getInstance()
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                val amPm = if (hourOfDay < 12) "AM" else "PM"
                val hour12 = when {
                    hourOfDay == 0 -> 12
                    hourOfDay > 12 -> hourOfDay - 12
                    else -> hourOfDay
                }
                reminderTime = String.format(Locale.ENGLISH, "%02d:%02d %s", hour12, minute, amPm)
            },
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            false
        ).show()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header (Notes-style)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = TealNeon,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = if (isCreatingNew) "New Daily Habit" else "Edit Daily Habit",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_habit_sheet_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Habit Name Field (Notes-style)
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    if (titleError && it.isNotBlank()) titleError = false
                },
                placeholder = { Text("Habit Name (e.g., Morning 15 min workout)…") },
                isError = titleError,
                supportingText = if (titleError) {
                    { Text("Habit name is required", color = PriorityHigh) }
                } else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("habit_title_input"),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TealNeon,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Category Chips (Notes-style)
            Text(
                text = "Category",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    val isSelected = category == cat
                    FilterChip(
                        selected = isSelected,
                        onClick = { category = cat },
                        label = {
                            Text(
                                text = cat,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TealNeon,
                            selectedLabelColor = TealDeep
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Routine Time Segment Chips
            Text(
                text = "Routine Segment",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(RoutineTime.values()) { rTime ->
                    val isSelected = routineTime == rTime
                    FilterChip(
                        selected = isSelected,
                        onClick = { routineTime = rTime },
                        label = {
                            Text(
                                text = "${rTime.icon} ${rTime.title}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TealNeon,
                            selectedLabelColor = TealDeep
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Weekly Target (targetDaysPerWeek)
            Text(
                text = "Weekly Goal (Days / Week)",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(targetDayOptions) { days ->
                    val isSelected = targetDays == days
                    FilterChip(
                        selected = isSelected,
                        onClick = { targetDays = days },
                        label = {
                            Text(
                                text = if (days == 7) "Daily (7/7)" else "$days days/wk",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TealNeon,
                            selectedLabelColor = TealDeep
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("habit_target_days_$days")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Optional Habit Reminder Time
            Text(
                text = "Daily Reminder Time (Optional)",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (reminderTime != null) TealNeon.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { openTimePicker() }
                    .testTag("habit_reminder_picker")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = if (reminderTime != null) TealNeon else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = if (reminderTime != null) "Reminder Scheduled" else "No Reminder",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            )
                            Text(
                                text = reminderTime ?: "Tap to set daily time",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (reminderTime != null) TealNeon else MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }
                    }

                    if (reminderTime != null) {
                        IconButton(
                            onClick = { reminderTime = null },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear Reminder",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = TealNeon.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Set Time",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TealNeon,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(quickTimes) { t ->
                    val isSelected = reminderTime == t
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) TealNeon.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) TealNeon else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.clickable {
                            reminderTime = if (isSelected) null else t
                        }
                    ) {
                        Text(
                            text = t,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = if (isSelected) TealNeon else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.sp
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Suggestions (1-tap fill)
            Text(
                text = "Quick Habit Suggestions",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(presetHabits) { preset ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.clickable {
                            title = preset.replace(Regex("[^\\p{L}\\p{Nd}\\s.]"), "").trim()
                            titleError = false
                        }
                    ) {
                        Text(
                            text = preset,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp
                            ),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Primary "Create Daily Habit" / "Save Changes" Button (Notes-style)
            Button(
                onClick = {
                    if (title.isBlank()) {
                        titleError = true
                    } else {
                        val trimmedTitle = title.trim()
                        val habitId = initialHabit?.id ?: 0L

                        if (isCreatingNew) {
                            // Save habit immediately first, then show Interstitial Ad (subject to cooldown) & Quote
                            onSave(habitId, trimmedTitle, category, routineTime, targetDays, reminderTime)
                            (context as? Activity)?.let { act ->
                                AdManager.showInterstitialAfterTaskEntry(act) {
                                    onHabitCreatedQuote()
                                }
                            } ?: run {
                                onHabitCreatedQuote()
                            }
                        } else {
                            // Editing existing habit: save directly without showing annoying interstitial ad
                            onSave(habitId, trimmedTitle, category, routineTime, targetDays, reminderTime)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_habit_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TealNeon,
                    contentColor = TealDeep
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = if (isCreatingNew) "Create Daily Habit" else "Save Changes",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
