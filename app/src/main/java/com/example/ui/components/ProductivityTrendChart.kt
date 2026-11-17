package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
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
fun ProductivityTrendChart(
    trends: List<DayProductivityTrend>,
    modifier: Modifier = Modifier
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

    // Smooth entry animation for the chart bars and curve
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(trends) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("productivity_trend_chart_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, TealNeon.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(TealNeon.copy(alpha = 0.15f))
                            .border(1.dp, TealNeon.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShowChart,
                            contentDescription = "Trend Icon",
                            tint = TealNeon,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "7-Day Task Completion Rate",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp
                            )
                        )
                        Text(
                            text = "Productivity Momentum & Trends",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = TealNeon.copy(alpha = 0.18f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TealNeon.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = TealNeon,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$avgCompletionRate% Avg",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TealNeon,
                                fontSize = 12.sp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Interactive Chart Canvas (Bar + Trend Curve)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkSurface)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .pointerInput(trends) {
                        detectTapGestures { offset ->
                            val slotWidth = size.width / trends.size
                            val tappedIndex = (offset.x / slotWidth).toInt().coerceIn(0, trends.lastIndex)
                            selectedIndex = tappedIndex
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val bottomPadding = 24.dp.toPx()
                    val chartHeight = h - bottomPadding
                    val count = trends.size
                    val slotWidth = w / count
                    val barWidth = (slotWidth * 0.45f).coerceAtMost(28.dp.toPx())

                    // 1. Draw horizontal subtle gridlines (0%, 50%, 100%)
                    val gridLines = listOf(0.0f, 0.5f, 1.0f)
                    val dashedEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)

                    gridLines.forEach { ratio ->
                        val y = chartHeight - (chartHeight * ratio)
                        drawLine(
                            color = Color.White.copy(alpha = 0.08f),
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = dashedEffect
                        )
                    }

                    // Pre-calculate point coordinates for bars and trendline curve
                    val points = mutableListOf<Offset>()
                    val anim = animationProgress.value

                    trends.forEachIndexed { i, day ->
                        val centerX = (i * slotWidth) + (slotWidth / 2f)
                        val pct = (day.completionPercentage / 100f).coerceIn(0f, 1f)
                        val animatedPct = pct * anim
                        val barHeight = (chartHeight * animatedPct).coerceAtLeast(6.dp.toPx() * anim)
                        val barTop = chartHeight - barHeight
                        val isSelected = selectedIndex == i

                        // Background Bar track
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.04f),
                            topLeft = Offset(centerX - barWidth / 2f, 0f),
                            size = Size(barWidth, chartHeight),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )

                        // Colored completion bar with gradient
                        val barBrush = if (isSelected) {
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF00FFD5), TealNeon, TealDeep)
                            )
                        } else {
                            Brush.verticalGradient(
                                colors = listOf(TealNeon.copy(alpha = 0.9f), TealNeonDark.copy(alpha = 0.6f))
                            )
                        }

                        drawRoundRect(
                            brush = barBrush,
                            topLeft = Offset(centerX - barWidth / 2f, barTop),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )

                        // Save point for spline curve
                        points.add(Offset(centerX, barTop))
                    }

                    // 2. Draw smooth trendline curve connecting the bar tops
                    if (points.size >= 2) {
                        val curvePath = Path()
                        curvePath.moveTo(points.first().x, points.first().y)

                        for (i in 0 until points.size - 1) {
                            val p0 = points[i]
                            val p1 = points[i + 1]
                            val controlX = (p0.x + p1.x) / 2f
                            curvePath.cubicTo(
                                controlX, p0.y,
                                controlX, p1.y,
                                p1.x, p1.y
                            )
                        }

                        drawPath(
                            path = curvePath,
                            color = TealNeon.copy(alpha = 0.85f * anim),
                            style = Stroke(
                                width = 2.5.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        )

                        // Draw points on top of curve
                        points.forEachIndexed { idx, pt ->
                            val isSelected = selectedIndex == idx
                            val radius = if (isSelected) 5.dp.toPx() else 3.5.dp.toPx()
                            drawCircle(
                                color = if (isSelected) Color(0xFF00FFD5) else TealNeon,
                                radius = radius,
                                center = pt
                            )
                            drawCircle(
                                color = DarkSurface,
                                radius = radius * 0.45f,
                                center = pt
                            )
                        }
                    }
                }

                // X-Axis Day Labels Row at bottom
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    trends.forEachIndexed { index, day ->
                        val isSelected = selectedIndex == index
                        Text(
                            text = day.dayLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) TealNeon else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            ),
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .clickable { selectedIndex = index }
                                .padding(vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Inspector Detail for Selected Day
            AnimatedVisibility(
                visible = selectedDay != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                if (selectedDay != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = DarkSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, TealNeon.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${selectedDay.dayLabel} (${selectedDay.dateString})",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
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

            Spacer(modifier = Modifier.height(10.dp))

            // Footer Metrics
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
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$totalCompleted7Days / $totalTasks7Days tasks done (7 days)",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    )
                }

                val bestDay = trends.maxByOrNull { it.completionPercentage }
                if (bestDay != null && bestDay.totalTasks > 0) {
                    Text(
                        text = "Peak: ${bestDay.dayLabel} (${bestDay.completionPercentage.toInt()}%)",
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
