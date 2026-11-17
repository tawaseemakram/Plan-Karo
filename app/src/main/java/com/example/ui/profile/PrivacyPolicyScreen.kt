package com.example.ui.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdsClick
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.TealDeep
import com.example.ui.theme.TealNeon

private data class PolicySection(
    val id: String,
    val title: String,
    val body: String,
    val icon: ImageVector
)

private val PRIVACY_POLICY_SECTIONS = listOf(
    PolicySection(
        id = "local_storage",
        title = "1. Local-First Data Storage",
        body = "Plan Karo stores your tasks, notes, checklists, daily habits, and routines locally on your device using an encrypted-by-OS SQLite database (Room). We do not sell, rent, or trade your personal productivity records.",
        icon = Icons.Default.Storage
    ),
    PolicySection(
        id = "permissions",
        title = "2. App Permissions & Device Access",
        body = "• Notifications & Alarms: Used solely to deliver your scheduled task reminders, daily routine alarms, and optional 3x daily check-in nudges.\n• Foreground Service (Alarm Audio Playback): Used only when a user-enabled Task Alarm rings to keep alarm audio playing reliably with an active notification and Dismiss/Snooze controls.\n• Microphone: Accessed only when you tap Record to save an Audio Voice Note; audio files stay stored locally inside app storage.\n• Photos: Uses the zero-permission Android Photo Picker so Plan Karo only reads the specific image you select.",
        icon = Icons.Default.Mic
    ),
    PolicySection(
        id = "advertising",
        title = "3. Advertising & Consent (Google AdMob)",
        body = "Plan Karo displays banner and interstitial ads through Google AdMob to keep core features accessible. AdMob may collect device advertising identifiers, coarse IP-based location, and ad interaction diagnostics in accordance with Google's Privacy Policy. Where required by law (such as EEA/UK/Switzerland), a Google consent prompt lets you manage ad personalization choices.",
        icon = Icons.Default.AdsClick
    ),
    PolicySection(
        id = "user_control",
        title = "4. Your Data Controls & Deletion",
        body = "You can edit or delete any task, voice note, routine, habit, or note directly inside Plan Karo at any time. Uninstalling the app, clearing app storage, or using Profile > Clear All Local Data permanently removes all locally stored records.",
        icon = Icons.Default.DeleteForever
    ),
    PolicySection(
        id = "contact",
        title = "5. Contact & Policy Updates",
        body = "This policy may be updated ahead of new feature releases. For privacy inquiries, deletion help, or Play Store support, contact the developer at tawaseemakram@gmail.com.",
        icon = Icons.Default.Mail
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onClose)

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("privacy_policy_screen"),
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
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(TealNeon.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = TealNeon,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(id = R.string.privacy_policy_title),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            )
                            Text(
                                text = stringResource(id = R.string.privacy_policy_subtitle),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TealNeon,
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("privacy_policy_top_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(id = R.string.close_button),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        },
        bottomBar = {
            Surface(
                color = DarkSurface,
                tonalElevation = 4.dp,
                border = BorderStroke(1.dp, TealNeon.copy(alpha = 0.2f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = onClose,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TealNeon,
                            contentColor = TealDeep
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 600.dp)
                            .heightIn(min = 50.dp)
                            .testTag("privacy_policy_close_button")
                    ) {
                        Text(
                            text = stringResource(id = R.string.close_button),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 600.dp)
                    .testTag("privacy_policy_list"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "policy_hero") {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = DarkSurfaceCard,
                        border = BorderStroke(1.dp, TealNeon.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            TealNeon.copy(alpha = 0.14f),
                                            DarkSurfaceCard
                                        )
                                    )
                                )
                                .padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = TealNeon,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Your Privacy Matters",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TealNeon
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "This Privacy Policy explains how Plan Karo collects, uses, and protects information when you organize tasks, habits, routines, and voice notes.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 19.sp
                                )
                            )
                        }
                    }
                }

                items(PRIVACY_POLICY_SECTIONS, key = { it.id }) { section ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = DarkSurfaceCard,
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("privacy_section_${section.id}")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = section.icon,
                                    contentDescription = null,
                                    tint = TealNeon,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = section.title,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 10.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
                            )

                            Text(
                                text = section.body,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 19.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
