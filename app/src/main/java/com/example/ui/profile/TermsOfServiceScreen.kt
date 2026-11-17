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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdsClick
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.VerifiedUser
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

private data class TermsSection(
    val id: String,
    val title: String,
    val body: String,
    val icon: ImageVector
)

private val TERMS_OF_SERVICE_SECTIONS = listOf(
    TermsSection(
        id = "acceptance",
        title = "1. Acceptance of Terms",
        body = "By accessing or using Plan Karo, you agree to be bound by these Terms of Service and our Privacy Policy. If you do not agree with any part of these terms, please discontinue use of the application.",
        icon = Icons.Default.VerifiedUser
    ),
    TermsSection(
        id = "data_usage",
        title = "2. Transparent User Data Usage",
        body = "All tasks, notes, checklists, voice memos, habits, and routines you author are stored locally on your device. Plan Karo accesses Microphone, Notifications, Exact Alarms, and selected Photos strictly when you initiate those features and never sells your personal records.",
        icon = Icons.Default.Description
    ),
    TermsSection(
        id = "alarms_reliability",
        title = "3. Reminders & Scheduled Alarms",
        body = "Plan Karo uses Android AlarmManager and notification APIs to fire task, habit, and routine reminders. Timely delivery depends on granting OS permissions (Notifications, Alarms & Reminders).",
        icon = Icons.Default.NotificationsActive
    ),
    TermsSection(
        id = "ads_service",
        title = "4. Advertisements & Third-Party SDKs",
        body = "Plan Karo displays banner and interstitial ads through Google AdMob to keep the app accessible. Third-party SDKs operate in accordance with Google Play Developer Program policies.",
        icon = Icons.Default.AdsClick
    ),
    TermsSection(
        id = "updates_contact",
        title = "5. Updates & Contact",
        body = "We may refine these Terms as Plan Karo evolves. Continued use after updates signifies acceptance. For questions or feedback, contact tawaseemakram@gmail.com.",
        icon = Icons.Default.Info
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsOfServiceScreen(
    onAccept: () -> Unit,
    onDismiss: () -> Unit = onAccept,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onDismiss)

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("terms_of_service_screen"),
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
                                imageVector = Icons.Default.Gavel,
                                contentDescription = null,
                                tint = TealNeon,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(id = R.string.terms_of_service_title),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            )
                            Text(
                                text = stringResource(id = R.string.terms_of_service_subtitle),
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
                        onClick = onDismiss,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("terms_top_close_button")
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
                        onClick = onAccept,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TealNeon,
                            contentColor = TealDeep
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 600.dp)
                            .heightIn(min = 50.dp)
                            .testTag("terms_accept_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier
                                .size(18.dp)
                                .padding(end = 2.dp)
                        )
                        Text(
                            text = " ${stringResource(id = R.string.accept_button)}",
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
                    .testTag("terms_of_service_list"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "terms_hero") {
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
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = TealNeon,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "User Data Usage & Transparency",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TealNeon
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Please review these Terms of Service outlining how Plan Karo handles local storage, device permissions, and AdMob advertising before tapping Accept.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 19.sp
                                )
                            )
                        }
                    }
                }

                items(TERMS_OF_SERVICE_SECTIONS, key = { it.id }) { section ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = DarkSurfaceCard,
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("terms_section_${section.id}")
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
