package com.example.ui.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.layout.heightIn
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ads.ScreenOptionBannerAd
import com.example.reminder.PermissionHelper
import com.example.reminder.PermissionPromptType
import com.example.ui.TaskViewModel
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.PriorityHigh
import com.example.ui.theme.TealDeep
import com.example.ui.theme.TealNeon

data class AchievementItem(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val isUnlocked: Boolean,
    val progress: Float, // 0f to 1f
    val progressLabel: String,
    val accentColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: TaskViewModel,
    modifier: Modifier = Modifier
) {
    val allTasks by viewModel.allTasks.collectAsStateWithLifecycle()
    val habits by viewModel.habits.collectAsStateWithLifecycle()
    val routines by viewModel.routines.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var isAchievementsSheetOpen by remember { mutableStateOf(false) }
    var isPermissionsSheetOpen by remember { mutableStateOf(false) }
    var isPrivacyPolicyOpen by remember { mutableStateOf(false) }
    var isTermsOpen by remember { mutableStateOf(false) }
    var isClearDataDialogOpen by remember { mutableStateOf(false) }
    var isClearingLocalData by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    var permissionRefreshTick by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionRefreshTick++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val hasNotifications = remember(permissionRefreshTick) {
        PermissionHelper.hasNotificationPermission(context)
    }
    val hasExactAlarms = remember(permissionRefreshTick) {
        PermissionHelper.canScheduleExactAlarms(context)
    }
    val hasMicrophone = remember(permissionRefreshTick) {
        PermissionHelper.hasRecordAudioPermission(context)
    }
    val hasFullScreenIntent = remember(permissionRefreshTick) {
        PermissionHelper.canUseFullScreenIntent(context)
    }

    val legalPrefs = remember {
        context.getSharedPreferences("plankaro_legal_prefs", android.content.Context.MODE_PRIVATE)
    }
    var hasAcceptedTerms by remember {
        mutableStateOf(legalPrefs.getBoolean("terms_accepted_v1", false))
    }

    val totalTasks = allTasks.size
    val completedTasks = allTasks.count { it.isCompleted }
    val completionRate = if (totalTasks > 0) ((completedTasks.toFloat() / totalTasks) * 100).toInt() else 100
    val bestStreak = habits.maxOfOrNull { it.bestStreak } ?: 0
    val audioNotesCount = allTasks.count { !it.audioUri.isNullOrBlank() }
    val routinesCount = routines.size

    val achievements = remember(totalTasks, completedTasks, bestStreak, audioNotesCount, routinesCount, completionRate) {
        listOf(
            AchievementItem(
                id = "first_task",
                title = "First Step",
                description = "Create your 1st task in Plan Karo",
                icon = Icons.Default.Flag,
                isUnlocked = totalTasks >= 1,
                progress = if (totalTasks >= 1) 1f else 0f,
                progressLabel = if (totalTasks >= 1) "Unlocked 🏆" else "0/1 Task",
                accentColor = TealNeon
            ),
            AchievementItem(
                id = "task_finisher",
                title = "Task Crusher",
                description = "Complete 5 daily tasks",
                icon = Icons.Default.EmojiEvents,
                isUnlocked = completedTasks >= 5,
                progress = (completedTasks / 5f).coerceIn(0f, 1f),
                progressLabel = if (completedTasks >= 5) "Unlocked 🏆" else "$completedTasks/5 done",
                accentColor = Color(0xFFFFB800)
            ),
            AchievementItem(
                id = "streak_champion",
                title = "Streak Champion",
                description = "Reach a 3-day habit streak",
                icon = Icons.Default.LocalFireDepartment,
                isUnlocked = bestStreak >= 3,
                progress = (bestStreak / 3f).coerceIn(0f, 1f),
                progressLabel = if (bestStreak >= 3) "Unlocked 🔥" else "$bestStreak/3 days",
                accentColor = Color(0xFFFF6B6B)
            ),
            AchievementItem(
                id = "voice_pioneer",
                title = "Voice Pioneer",
                description = "Attach an audio voice note to a task",
                icon = Icons.Default.Mic,
                isUnlocked = audioNotesCount >= 1,
                progress = if (audioNotesCount >= 1) 1f else 0f,
                progressLabel = if (audioNotesCount >= 1) "Unlocked 🎙️" else "0/1 Audio",
                accentColor = Color(0xFF00E5BE)
            ),
            AchievementItem(
                id = "routine_builder",
                title = "Routine Builder",
                description = "Set up 2+ daily routines",
                icon = Icons.Default.Repeat,
                isUnlocked = routinesCount >= 2,
                progress = (routinesCount / 2f).coerceIn(0f, 1f),
                progressLabel = if (routinesCount >= 2) "Unlocked ⚡" else "$routinesCount/2 Set",
                accentColor = Color(0xFF38EF7D)
            ),
            AchievementItem(
                id = "consistency_pro",
                title = "Consistency Pro",
                description = "Achieve 80%+ task completion rate",
                icon = Icons.Default.WorkspacePremium,
                isUnlocked = completionRate >= 80 && totalTasks >= 3,
                progress = if (totalTasks >= 3) (completionRate / 100f).coerceIn(0f, 1f) else (totalTasks / 3f) * 0.5f,
                progressLabel = if (completionRate >= 80 && totalTasks >= 3) "Unlocked 🌟" else if (totalTasks < 3) "$totalTasks/3 Tasks" else "$completionRate%",
                accentColor = Color(0xFFA855F7)
            )
        )
    }

    val unlockedCount = achievements.count { it.isUnlocked }

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
                                text = "Profile & Productivity",
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("profile_screen_list"),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Header Profile Card
            item(key = "user_card") {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = DarkSurfaceCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, TealNeon.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(TealNeon.copy(alpha = 0.2f))
                                .border(2.dp, TealNeon, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile Avatar",
                                tint = TealNeon,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = "Plan Karo User",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Discipline • Consistency • Success",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TealNeon,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = DarkSurface
                            ) {
                                Text(
                                    text = "Level: Productivity Master ⚡",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    ),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Achievements Option Card (click to open inside options)
            item(key = "achievements_section") {
                val overallProgress = if (achievements.isNotEmpty()) unlockedCount.toFloat() / achievements.size.toFloat() else 0f
                ProfileAchievementsOptionCard(
                    unlockedCount = unlockedCount,
                    totalCount = achievements.size,
                    overallProgress = overallProgress,
                    onClick = { isAchievementsSheetOpen = true }
                )
            }

            // 4. Productivity Overview Stats Grid
            item(key = "stats_grid") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("productivity_stats_section")
                ) {
                    Text(
                        text = "Productivity Overview",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ProfileStatCard(
                            title = "Completed",
                            value = "$completedTasks",
                            subtitle = "Of $totalTasks tasks",
                            icon = Icons.Default.CheckCircle,
                            iconColor = TealNeon,
                            modifier = Modifier.weight(1f)
                        )

                        ProfileStatCard(
                            title = "Success Rate",
                            value = "$completionRate%",
                            subtitle = "Task completion",
                            icon = Icons.Default.TrendingUp,
                            iconColor = Color(0xFF00E5BE),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ProfileStatCard(
                            title = "Active Habits",
                            value = "${habits.size}",
                            subtitle = "Tracked daily",
                            icon = Icons.Default.Star,
                            iconColor = Color(0xFFFFB800),
                            modifier = Modifier.weight(1f)
                        )

                        ProfileStatCard(
                            title = "Best Streak",
                            value = "$bestStreak Days",
                            subtitle = "Consistent routine",
                            icon = Icons.Default.Shield,
                            iconColor = Color(0xFF6C5CE7),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 5. Android 14+ Permissions & Alarm Access Card (compact, tap to view 3 options)
            item(key = "permissions_section") {
                ProfilePermissionsCard(
                    hasNotifications = hasNotifications,
                    hasExactAlarms = hasExactAlarms,
                    hasMicrophone = hasMicrophone,
                    onClick = { isPermissionsSheetOpen = true }
                )
            }

            // 6. Privacy Policy Option Card (Play Store Compliance)
            item(key = "privacy_policy_section") {
                ProfilePrivacyPolicyCard(
                    onClick = { isPrivacyPolicyOpen = true }
                )
            }

            // 7. Terms of Service Option Card (Play Store Transparency)
            item(key = "terms_of_service_section") {
                ProfileTermsOfServiceCard(
                    hasAccepted = hasAcceptedTerms,
                    onClick = { isTermsOpen = true }
                )
            }

            // 8. Clear All Local Data Option Card (Device Data Control)
            item(key = "clear_local_data_section") {
                ProfileClearLocalDataCard(
                    onClick = { isClearDataDialogOpen = true }
                )
            }

            // 9. Banner Test Ad for Profile option
            item(key = "profile_banner_ad") {
                ScreenOptionBannerAd(modifier = Modifier.fillMaxWidth())
            }
        }
    }

    // Full Achievements Detail BottomSheet launched when tapping Achievements option
    if (isAchievementsSheetOpen) {
        AchievementsDetailSheet(
            achievements = achievements,
            unlockedCount = unlockedCount,
            totalCount = achievements.size,
            onDismiss = { isAchievementsSheetOpen = false }
        )
    }

    // Permissions & Alarm Access Detail BottomSheet launched when tapping Permissions card
    if (isPermissionsSheetOpen) {
        PermissionsDetailSheet(
            hasNotifications = hasNotifications,
            hasExactAlarms = hasExactAlarms,
            hasMicrophone = hasMicrophone,
            hasFullScreenIntent = hasFullScreenIntent,
            onOpenNotifications = {
                PermissionHelper.launchSettingsForType(context, PermissionPromptType.NOTIFICATIONS)
            },
            onOpenExactAlarms = {
                PermissionHelper.launchSettingsForType(context, PermissionPromptType.EXACT_ALARM)
            },
            onOpenMicrophone = {
                PermissionHelper.launchSettingsForType(context, PermissionPromptType.MICROPHONE)
            },
            onOpenFullScreenIntent = {
                PermissionHelper.launchFullScreenIntentSettings(context)
            },
            onDismiss = { isPermissionsSheetOpen = false }
        )
    }

    // Privacy Policy Screen launched from Profile
    if (isPrivacyPolicyOpen) {
        Dialog(
            onDismissRequest = { isPrivacyPolicyOpen = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            PrivacyPolicyScreen(
                onClose = { isPrivacyPolicyOpen = false }
            )
        }
    }

    // Terms of Service Screen launched from Profile
    if (isTermsOpen) {
        Dialog(
            onDismissRequest = { isTermsOpen = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            TermsOfServiceScreen(
                onAccept = {
                    legalPrefs.edit().putBoolean("terms_accepted_v1", true).apply()
                    hasAcceptedTerms = true
                    isTermsOpen = false
                    Toast.makeText(context, "Terms of Service accepted", Toast.LENGTH_SHORT).show()
                },
                onDismiss = { isTermsOpen = false }
            )
        }
    }

    if (isClearDataDialogOpen) {
        val successMsg = stringResource(id = com.example.R.string.clear_local_data_success)
        AlertDialog(
            onDismissRequest = {
                if (!isClearingLocalData) isClearDataDialogOpen = false
            },
            containerColor = DarkSurfaceCard,
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(PriorityHigh.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = null,
                        tint = PriorityHigh,
                        modifier = Modifier.size(26.dp)
                    )
                }
            },
            title = {
                Text(
                    text = stringResource(id = com.example.R.string.clear_local_data_confirm_title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            text = {
                Text(
                    text = "This will permanently erase all tasks, reminders, notes, checklists, recorded audio voice notes, daily habits, routines, custom categories, and saved settings from this device.\n\nThis action cannot be undone.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 19.sp
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!isClearingLocalData) {
                            isClearingLocalData = true
                            viewModel.clearAllLocalData {
                                isClearingLocalData = false
                                hasAcceptedTerms = false
                                isClearDataDialogOpen = false
                                Toast.makeText(context, successMsg, Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = !isClearingLocalData,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PriorityHigh,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("clear_local_data_confirm_button")
                ) {
                    if (isClearingLocalData) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Text(
                            text = stringResource(id = com.example.R.string.clear_local_data_confirm_action),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { isClearDataDialogOpen = false },
                    enabled = !isClearingLocalData,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("clear_local_data_cancel_button")
                ) {
                    Text(
                        text = stringResource(id = com.example.R.string.cancel_recording),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            modifier = Modifier.testTag("clear_local_data_confirm_dialog")
        )
    }
}

/**
 * Clickable Achievements option card on the Profile screen.
 * Tapping this card opens the full inside achievements view (AchievementsDetailSheet).
 */
@Composable
private fun ProfileAchievementsOptionCard(
    unlockedCount: Int,
    totalCount: Int,
    overallProgress: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = DarkSurfaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB800).copy(alpha = 0.45f)),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onClick() }
            .testTag("profile_achievements_option_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFB800).copy(alpha = 0.2f))
                            .border(1.5.dp, Color(0xFFFFB800), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Achievements",
                            tint = Color(0xFFFFB800),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Achievements & Badges",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFFFB800).copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = "🏆",
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Tap to view milestones & unlocked rewards ➔",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFFFFB800),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFB800).copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB800).copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "$unlockedCount / $totalCount",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFB800),
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar showing overall unlock percentage
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LinearProgressIndicator(
                    progress = { overallProgress },
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Color(0xFFFFB800),
                    trackColor = DarkSurface
                )
                Text(
                    text = "${(overallProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFFFFB800),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}

/**
 * Detailed Achievements BottomSheet displaying all milestone badges,
 * progress bars, unlocked rewards, and filters when clicked from Profile.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AchievementsDetailSheet(
    achievements: List<AchievementItem>,
    unlockedCount: Int,
    totalCount: Int,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredAchievements = remember(achievements, selectedFilter) {
        when (selectedFilter) {
            "UNLOCKED" -> achievements.filter { it.isUnlocked }
            "LOCKED" -> achievements.filter { !it.isUnlocked }
            else -> achievements
        }
    }

    val overallProgress = if (totalCount > 0) unlockedCount.toFloat() / totalCount.toFloat() else 0f

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkBg,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .testTag("achievements_detail_sheet")
        ) {
            // Header: Title and Close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFB800).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = Color(0xFFFFB800),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Plan Karo Achievements 🏆",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "Track your productivity milestones & unlock badges",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.65f),
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_achievements_sheet")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Overall Summary Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DarkSurfaceCard,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFB800).copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (unlockedCount == totalCount) "All Badges Unlocked! 🌟" else "$unlockedCount of $totalCount Badges Unlocked",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (unlockedCount == totalCount) "You have mastered productivity in Plan Karo!" else "Keep completing daily tasks and building streaks to unlock them all.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { overallProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = Color(0xFFFFB800),
                            trackColor = DarkSurface
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFB800).copy(alpha = 0.15f))
                            .border(2.dp, Color(0xFFFFB800), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${(overallProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFB800)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Filter Tabs: All, Unlocked, Locked
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AchievementFilterChip(
                    label = "All (${achievements.size})",
                    isSelected = selectedFilter == "ALL",
                    onClick = { selectedFilter = "ALL" },
                    modifier = Modifier.weight(1f)
                )
                AchievementFilterChip(
                    label = "Unlocked ($unlockedCount)",
                    isSelected = selectedFilter == "UNLOCKED",
                    onClick = { selectedFilter = "UNLOCKED" },
                    modifier = Modifier.weight(1f)
                )
                AchievementFilterChip(
                    label = "Locked (${achievements.size - unlockedCount})",
                    isSelected = selectedFilter == "LOCKED",
                    onClick = { selectedFilter = "LOCKED" },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Badges Grid
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (filteredAchievements.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No achievements in this filter",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        )
                    }
                } else {
                    val chunked = filteredAchievements.chunked(2)
                    chunked.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowItems.forEach { item ->
                                AchievementBadgeCard(
                                    item = item,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowItems.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AchievementFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) Color(0xFFFFB800) else DarkSurface,
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isSelected) Color(0xFFFFB800) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        modifier = modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) DarkBg else Color.White.copy(alpha = 0.85f),
                    fontSize = 11.sp
                ),
                maxLines = 1
            )
        }
    }
}

/**
 * Individual achievement badge card.
 */
@Composable
private fun AchievementBadgeCard(
    item: AchievementItem,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.testTag("achievement_badge_${item.id}"),
        shape = RoundedCornerShape(14.dp),
        color = if (item.isUnlocked) {
            DarkSurface.copy(alpha = 0.9f)
        } else {
            DarkSurface.copy(alpha = 0.4f)
        },
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (item.isUnlocked) {
                item.accentColor.copy(alpha = 0.55f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            // Icon + Status Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (item.isUnlocked) {
                                item.accentColor.copy(alpha = 0.2f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            }
                        )
                        .border(
                            width = 1.dp,
                            color = if (item.isUnlocked) item.accentColor.copy(alpha = 0.6f) else Color.Transparent,
                            shape = RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        tint = if (item.isUnlocked) item.accentColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (item.isUnlocked) {
                    Surface(
                        shape = CircleShape,
                        color = item.accentColor.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "✓",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = item.accentColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title
            Text(
                text = item.title,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (item.isUnlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 13.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Description
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    lineHeight = 13.sp
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Mini progress indicator or Unlocked label
            if (item.isUnlocked) {
                Text(
                    text = item.progressLabel,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = item.accentColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    LinearProgressIndicator(
                        progress = { item.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = item.accentColor.copy(alpha = 0.7f),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                    Text(
                        text = item.progressLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontSize = 9.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileStatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = DarkSurfaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            )
        }
    }
}

@Composable
private fun ProfilePermissionsCard(
    hasNotifications: Boolean,
    hasExactAlarms: Boolean,
    hasMicrophone: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val grantedCount = listOf(hasNotifications, hasExactAlarms, hasMicrophone).count { it }
    val allGranted = grantedCount == 3
    val accentColor = if (allGranted) TealNeon else PriorityHigh

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = DarkSurfaceCard,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (allGranted) TealNeon.copy(alpha = 0.35f) else PriorityHigh.copy(alpha = 0.55f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onClick() }
            .testTag("profile_permissions_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Permissions & Alarm Access",
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column {
                    Text(
                        text = "Permissions & Alarm Access",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (allGranted) {
                            "Notifications, exact alarms & mic active"
                        } else {
                            "Tap to enable missing notification, alarm or mic access"
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (allGranted) MaterialTheme.colorScheme.onSurfaceVariant else PriorityHigh,
                            fontSize = 11.sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = accentColor.copy(alpha = 0.16f)
            ) {
                Text(
                    text = if (allGranted) "All Set ✓" else "$grantedCount/3 ➔",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = accentColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    ),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PermissionsDetailSheet(
    hasNotifications: Boolean,
    hasExactAlarms: Boolean,
    hasMicrophone: Boolean,
    hasFullScreenIntent: Boolean,
    onOpenNotifications: () -> Unit,
    onOpenExactAlarms: () -> Unit,
    onOpenMicrophone: () -> Unit,
    onOpenFullScreenIntent: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val allGranted = hasNotifications && hasExactAlarms && hasMicrophone && hasFullScreenIntent

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkBg,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .testTag("permissions_detail_sheet"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background((if (allGranted) TealNeon else PriorityHigh).copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = if (allGranted) TealNeon else PriorityHigh,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Permissions & Alarm Access",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = if (allGranted) {
                                "All alarm & media settings ready"
                            } else {
                                "Tap 'Open Settings' for reliable alarms"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = if (allGranted) TealNeon else PriorityHigh,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            PermissionStatusRow(
                label = "Notifications",
                isGranted = hasNotifications,
                icon = Icons.Default.NotificationsActive,
                onOpenSettings = onOpenNotifications
            )
            PermissionStatusRow(
                label = "Exact Alarms (Task & Routine)",
                isGranted = hasExactAlarms,
                icon = Icons.Default.Alarm,
                onOpenSettings = onOpenExactAlarms
            )
            PermissionStatusRow(
                label = "Microphone (Audio Notes)",
                isGranted = hasMicrophone,
                icon = Icons.Default.Mic,
                onOpenSettings = onOpenMicrophone
            )
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                PermissionStatusRow(
                    label = "Lock-Screen Alarm Display",
                    isGranted = hasFullScreenIntent,
                    icon = Icons.Default.Lock,
                    onOpenSettings = onOpenFullScreenIntent
                )
            }
        }
    }
}

@Composable
private fun PermissionStatusRow(
    label: String,
    isGranted: Boolean,
    icon: ImageVector,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceCard)
            .border(
                width = 1.dp,
                color = if (isGranted) TealNeon.copy(alpha = 0.25f) else PriorityHigh.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onOpenSettings)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isGranted) TealNeon else PriorityHigh,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium
                )
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (isGranted) TealNeon.copy(alpha = 0.16f) else PriorityHigh.copy(alpha = 0.2f),
            modifier = Modifier.clickable(onClick = onOpenSettings)
        ) {
            Text(
                text = if (isGranted) "Allowed ✓" else "Open Settings ➔",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (isGranted) TealNeon else PriorityHigh,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                ),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun ProfilePrivacyPolicyCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = DarkSurfaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, TealNeon.copy(alpha = 0.35f)),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onClick() }
            .testTag("profile_privacy_policy_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(TealNeon.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Policy,
                        contentDescription = "Privacy Policy",
                        tint = TealNeon,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column {
                    Text(
                        text = stringResource(id = com.example.R.string.privacy_policy_title),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = "View data storage, permissions & AdMob disclosures",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = TealNeon.copy(alpha = 0.15f)
            ) {
                Text(
                    text = "View ➔",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TealNeon,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    ),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun ProfileTermsOfServiceCard(
    hasAccepted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = DarkSurfaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, TealNeon.copy(alpha = 0.35f)),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onClick() }
            .testTag("profile_terms_of_service_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(TealNeon.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Gavel,
                        contentDescription = "Terms of Service",
                        tint = TealNeon,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column {
                    Text(
                        text = stringResource(id = com.example.R.string.terms_of_service_title),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = "Review user data usage, alarms & app terms",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = TealNeon.copy(alpha = 0.15f)
            ) {
                Text(
                    text = if (hasAccepted) "Accepted ✓" else "Review ➔",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TealNeon,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    ),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun ProfileClearLocalDataCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = DarkSurfaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, PriorityHigh.copy(alpha = 0.4f)),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onClick() }
            .testTag("profile_clear_local_data_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(PriorityHigh.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = stringResource(id = com.example.R.string.clear_local_data_title),
                        tint = PriorityHigh,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column {
                    Text(
                        text = stringResource(id = com.example.R.string.clear_local_data_title),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = stringResource(id = com.example.R.string.clear_local_data_subtitle),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = PriorityHigh.copy(alpha = 0.15f)
            ) {
                Text(
                    text = "Reset ➔",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = PriorityHigh,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    ),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}
