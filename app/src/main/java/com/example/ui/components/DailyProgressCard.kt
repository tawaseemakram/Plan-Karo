package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.DailyProgress
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.PriorityHigh
import com.example.ui.theme.TealNeon
import com.example.ui.theme.TealNeonDark

@Composable
fun DailyProgressCard(
    progress: DailyProgress,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.percentage,
        animationSpec = tween(durationMillis = 600),
        label = "progressAnimation"
    )

    val percentInt = (progress.percentage * 100).toInt()

    val motivationText = when {
        progress.totalCount == 0 -> "Add tasks to start tracking your day!"
        progress.percentage >= 1.0f -> "🎉 All done! Amazing productivity today!"
        progress.percentage >= 0.75f -> "Almost there! Keep up the momentum!"
        progress.percentage >= 0.5f -> "Halfway through! You're making great progress."
        progress.percentage > 0f -> "Great start! One task at a time."
        else -> "Ready to crush today's goals?"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_progress_card")
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(TealNeon.copy(alpha = 0.5f), Color.Transparent)
                ),
                shape = RoundedCornerShape(20.dp)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurfaceCard
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Daily Completion",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = motivationText,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                // Circular percent badge
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(TealNeon, TealNeonDark)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$percentInt%",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF00382E),
                            fontSize = 15.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Visual Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .testTag("progress_bar"),
                    color = TealNeon,
                    trackColor = Color(0xFF162529)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stat pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatBadge(
                    label = "Done",
                    value = "${progress.completedCount}",
                    color = TealNeon,
                    icon = Icons.Default.CheckCircle
                )

                StatBadge(
                    label = "Pending",
                    value = "${progress.pendingCount}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    icon = null
                )

                StatBadge(
                    label = "Total",
                    value = "${progress.totalCount}",
                    color = MaterialTheme.colorScheme.onSurface,
                    icon = null
                )

                if (progress.highPriorityPending > 0) {
                    StatBadge(
                        label = "Urgent",
                        value = "${progress.highPriorityPending}",
                        color = PriorityHigh,
                        icon = Icons.Default.Warning
                    )
                }
            }
        }
    }
}

@Composable
private fun StatBadge(
    label: String,
    value: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector?
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = color
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        )
    }
}
