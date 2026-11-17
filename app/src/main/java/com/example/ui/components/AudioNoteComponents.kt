package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.audio.AudioNotePlayer
import com.example.audio.AudioNoteRecorder
import com.example.audio.AudioUtils
import com.example.reminder.PermissionPromptType
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.PriorityHigh
import com.example.ui.theme.TealDeep
import com.example.ui.theme.TealNeon
import com.example.ui.theme.TealNeonDark
import kotlinx.coroutines.delay

/**
 * Compact audio player embedded directly inside TaskCard on the daily task list.
 */
@Composable
fun TaskCardAudioPlayer(
    audioUri: String,
    durationSeconds: Int,
    taskId: Long,
    modifier: Modifier = Modifier
) {
    val player = remember { AudioNotePlayer() }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosMs by remember { mutableIntStateOf(0) }
    val totalDurationMs = remember(durationSeconds) { (durationSeconds * 1000).coerceAtLeast(1000) }

    DisposableEffect(audioUri) {
        onDispose {
            player.release()
            isPlaying = false
        }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            currentPosMs = player.currentPosition
            delay(150L)
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("task_audio_player_$taskId"),
        shape = RoundedCornerShape(12.dp),
        color = TealNeonDark.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(1.dp, TealNeon.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Play / Pause Circle Button
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(TealNeon)
                    .clickable {
                        if (isPlaying) {
                            player.pause()
                            isPlaying = false
                        } else {
                            val started = player.play(audioUri) {
                                isPlaying = false
                                currentPosMs = 0
                            }
                            if (started) {
                                isPlaying = true
                            }
                        }
                    }
                    .testTag("task_audio_play_btn_$taskId"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause audio note" else "Play audio note",
                    tint = TealDeep,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Animated waveform bars when playing, or static bars when idle
            AnimatedWaveform(
                isPlaying = isPlaying,
                modifier = Modifier.weight(1f)
            )

            // Duration display
            Text(
                text = if (isPlaying) {
                    "${AudioUtils.formatDurationMillis(currentPosMs)} / ${AudioUtils.formatDuration(durationSeconds)}"
                } else {
                    AudioUtils.formatDuration(durationSeconds)
                },
                style = MaterialTheme.typography.labelSmall.copy(
                    color = TealNeon,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            )

            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = null,
                tint = TealNeon.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * Animated audio waveform simulation showing audio pulses.
 */
@Composable
fun AnimatedWaveform(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveformTransition")

    val h1 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = if (isPlaying) 1f else 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h1"
    )
    val h2 by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = if (isPlaying) 0.2f else 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(320, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h2"
    )
    val h3 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = if (isPlaying) 0.9f else 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(480, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h3"
    )
    val h4 by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = if (isPlaying) 0.3f else 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(360, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h4"
    )
    val h5 by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = if (isPlaying) 0.95f else 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h5"
    )

    Row(
        modifier = modifier.height(24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        val heights = listOf(h1, h2, h3, h4, h5, h2, h4, h1, h3, h5, h2, h3)
        heights.forEach { frac ->
            val barHeight = (frac * 20.dp.value).coerceIn(4f, 22f).dp
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(barHeight)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(if (isPlaying) TealNeon else TealNeon.copy(alpha = 0.45f))
            )
        }
    }
}

/**
 * Full audio recording and preview section inside TaskEditSheet.
 */
@Composable
fun TaskEditAudioSection(
    audioUri: String?,
    audioDurationSeconds: Int,
    onAudioUpdated: (uri: String?, durationSeconds: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val recorder = remember { AudioNoteRecorder() }
    val previewPlayer = remember { AudioNotePlayer() }

    var isRecording by remember { mutableStateOf(false) }
    var recordingDuration by remember { mutableIntStateOf(0) }

    var isPreviewPlaying by remember { mutableStateOf(false) }
    var previewPosMs by remember { mutableIntStateOf(0) }
    var showMicSettingsPrompt by remember { mutableStateOf(false) }

    // Clean up recorder and player when exiting
    DisposableEffect(Unit) {
        onDispose {
            if (recorder.isRecording) {
                recorder.cancelRecording()
            }
            previewPlayer.release()
        }
    }

    // Permission launcher for microphone access
    val recordPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val file = recorder.startRecording(context)
            if (file != null) {
                isRecording = true
                recordingDuration = 0
            } else {
                Toast.makeText(context, "Could not initialize audio recorder", Toast.LENGTH_SHORT).show()
            }
        } else {
            showMicSettingsPrompt = true
        }
    }

    if (showMicSettingsPrompt) {
        PermissionSettingsPromptDialog(
            permissionType = PermissionPromptType.MICROPHONE,
            onDismiss = { showMicSettingsPrompt = false }
        )
    }

    // Live timer while recording
    LaunchedEffect(isRecording) {
        while (isRecording) {
            delay(1000L)
            recordingDuration += 1
            // Auto stop at 2 minutes (120 seconds) for short audio notes
            if (recordingDuration >= 120) {
                val (file, dur) = recorder.stopRecording()
                isRecording = false
                if (file != null && file.exists()) {
                    onAudioUpdated(file.absolutePath, dur)
                }
            }
        }
    }

    // Live position while playing preview
    LaunchedEffect(isPreviewPlaying) {
        while (isPreviewPlaying) {
            previewPosMs = previewPlayer.currentPosition
            delay(150L)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
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
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    tint = TealNeon,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Audio Voice Note",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            if (audioUri != null && !isRecording) {
                Text(
                    text = AudioUtils.formatDuration(audioDurationSeconds),
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = TealNeon,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        when {
            // Case 1: Currently Recording
            isRecording -> {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = PriorityHigh.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, PriorityHigh),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("audio_recording_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Pulsing red recording dot
                                val infiniteTransition = rememberInfiniteTransition(label = "recDot")
                                val alpha by infiniteTransition.animateFloat(
                                    initialValue = 0.3f,
                                    targetValue = 1f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(500),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "recAlpha"
                                )
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(PriorityHigh.copy(alpha = alpha))
                                )
                                Text(
                                    text = "Recording Voice Note…",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = PriorityHigh,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }

                            // Elapsed time / limit
                            Text(
                                text = "${AudioUtils.formatDuration(recordingDuration)} / 2:00",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Waveform animation
                        AnimatedWaveform(
                            isPlaying = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Action Buttons: Done (Save) & Cancel
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    val (file, dur) = recorder.stopRecording()
                                    isRecording = false
                                    if (file != null && file.exists()) {
                                        onAudioUpdated(file.absolutePath, dur)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = TealNeon,
                                    contentColor = TealDeep
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("audio_stop_record_button")
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Stop & Attach", fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    recorder.cancelRecording()
                                    isRecording = false
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("audio_cancel_record_button")
                            ) {
                                Text("Cancel")
                            }
                        }
                    }
                }
            }

            // Case 2: Audio Note is attached
            audioUri != null -> {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = TealNeonDark.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TealNeon.copy(alpha = 0.45f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("attached_audio_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Play / Pause button
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(TealNeon)
                                        .clickable {
                                            if (isPreviewPlaying) {
                                                previewPlayer.pause()
                                                isPreviewPlaying = false
                                            } else {
                                                val started = previewPlayer.play(audioUri) {
                                                    isPreviewPlaying = false
                                                    previewPosMs = 0
                                                }
                                                if (started) {
                                                    isPreviewPlaying = true
                                                }
                                            }
                                        }
                                        .testTag("preview_audio_play_btn"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isPreviewPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isPreviewPlaying) "Pause" else "Play",
                                        tint = TealDeep,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = "Voice Memo Attached",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    )
                                    Text(
                                        text = if (isPreviewPlaying) {
                                            "${AudioUtils.formatDurationMillis(previewPosMs)} / ${AudioUtils.formatDuration(audioDurationSeconds)}"
                                        } else {
                                            "Duration: ${AudioUtils.formatDuration(audioDurationSeconds)}"
                                        },
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = TealNeon,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Re-record action
                                IconButton(
                                    onClick = {
                                        previewPlayer.stop()
                                        isPreviewPlaying = false
                                        // Request permission or start recording
                                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                            val file = recorder.startRecording(context)
                                            if (file != null) {
                                                isRecording = true
                                                recordingDuration = 0
                                            }
                                        } else {
                                            recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Re-record",
                                        tint = TealNeon,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // Delete attached audio
                                IconButton(
                                    onClick = {
                                        previewPlayer.stop()
                                        isPreviewPlaying = false
                                        onAudioUpdated(null, 0)
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Remove voice note",
                                        tint = PriorityHigh,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // Playback Progress Slider
                        if (audioDurationSeconds > 0) {
                            val totalMs = (audioDurationSeconds * 1000).toFloat().coerceAtLeast(1000f)
                            Slider(
                                value = previewPosMs.toFloat().coerceIn(0f, totalMs),
                                onValueChange = { newVal ->
                                    previewPosMs = newVal.toInt()
                                    previewPlayer.seekTo(newVal.toInt())
                                },
                                valueRange = 0f..totalMs,
                                colors = SliderDefaults.colors(
                                    thumbColor = TealNeon,
                                    activeTrackColor = TealNeon,
                                    inactiveTrackColor = TealNeon.copy(alpha = 0.25f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(28.dp)
                            )
                        }
                    }
                }
            }

            // Case 3: Empty - No Audio Note Attached Yet
            else -> {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                val file = recorder.startRecording(context)
                                if (file != null) {
                                    isRecording = true
                                    recordingDuration = 0
                                } else {
                                    Toast.makeText(context, "Could not start audio recorder", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                        .testTag("attach_audio_button")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(TealNeon.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = TealNeon,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Record audio note…",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = TealNeon,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            Text(
                                text = "Speak reminders, instructions, or thoughts (up to 2 mins)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
