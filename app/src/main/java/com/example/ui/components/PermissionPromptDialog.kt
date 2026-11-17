package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.reminder.PermissionHelper
import com.example.reminder.PermissionPromptType
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.PriorityHigh
import com.example.ui.theme.TealDeep
import com.example.ui.theme.TealNeon

@Composable
fun PermissionSettingsPromptDialog(
    permissionType: PermissionPromptType,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val icon = when (permissionType) {
        PermissionPromptType.MICROPHONE -> Icons.Default.Mic
        PermissionPromptType.EXACT_ALARM -> Icons.Default.Alarm
        PermissionPromptType.NOTIFICATIONS -> Icons.Default.NotificationsOff
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurfaceCard,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.testTag("permission_settings_dialog"),
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = permissionType.title,
                tint = PriorityHigh,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = permissionType.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        text = {
            Text(
                text = permissionType.message,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    PermissionHelper.launchSettingsForType(context, permissionType)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = TealNeon,
                    contentColor = TealDeep
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("permission_open_settings_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = permissionType.settingsButtonLabel,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("permission_not_now_button")
            ) {
                Text(
                    text = "Not Now",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}
