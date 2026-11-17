package com.example.ui.notes

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import android.widget.Toast
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.ImageAttachmentStorage
import com.example.data.model.NoteItem
import com.example.ui.components.TaskEditAudioSection
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.PriorityHigh
import com.example.ui.theme.TealDeep
import com.example.ui.theme.TealNeon
import com.example.ui.theme.TealNeonDark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class NoteCategoryOption(
    val name: String,
    val icon: String,
    val defaultColor: String
)

val NOTE_CATEGORIES = listOf(
    NoteCategoryOption("Idea", "💡", "#00E5BE"),
    NoteCategoryOption("Work", "💼", "#3B82F6"),
    NoteCategoryOption("Personal", "🧘", "#A855F7"),
    NoteCategoryOption("Study", "📚", "#F59E0B"),
    NoteCategoryOption("Meeting", "👥", "#EC4899"),
    NoteCategoryOption("Checklist", "✅", "#10B981")
)

val COLOR_PALETTE = listOf(
    "#00E5BE", // Teal Neon
    "#3B82F6", // Blue
    "#A855F7", // Purple
    "#FFB800", // Amber / Gold
    "#FF6B6B", // Coral
    "#10B981", // Emerald
    "#06B6D4"  // Cyan
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorSheet(
    initialNote: NoteItem?,
    onDismiss: () -> Unit,
    onSave: (
        id: Long,
        title: String,
        content: String,
        category: String,
        colorHex: String,
        isPinned: Boolean,
        audioUri: String?,
        audioDurationSeconds: Int,
        imageUri: String?,
        isChecklist: Boolean,
        checklistItems: String
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var title by remember { mutableStateOf(initialNote?.title ?: "") }
    var content by remember { mutableStateOf(initialNote?.content ?: "") }
    var category by remember { mutableStateOf(initialNote?.category ?: "Idea") }
    var colorHex by remember { mutableStateOf(initialNote?.colorHex ?: "#00E5BE") }
    var isPinned by remember { mutableStateOf(initialNote?.isPinned ?: false) }
    var isChecklist by remember { mutableStateOf(initialNote?.isChecklist ?: (initialNote?.category == "Checklist")) }

    var audioUri by remember { mutableStateOf(initialNote?.audioUri) }
    var audioDurationSeconds by remember { mutableIntStateOf(initialNote?.audioDurationSeconds ?: 0) }
    var imageUri by remember { mutableStateOf(initialNote?.imageUri) }

    // Parse checklist lines: format "Done|Text" or "Todo|Text"
    var checklistList by remember {
        val initialLines = initialNote?.checklistItems?.lines()?.filter { it.isNotBlank() } ?: emptyList()
        val parsed = initialLines.map { line ->
            val parts = line.split("|", limit = 2)
            if (parts.size == 2) Pair(parts[0] == "Done", parts[1]) else Pair(false, line)
        }.toMutableList()
        if (parsed.isEmpty() && isChecklist) {
            parsed.add(Pair(false, ""))
        }
        mutableStateOf(parsed)
    }

    var titleError by remember { mutableStateOf(false) }

    val draftImageUris = remember { mutableSetOf<String>() }
    val draftAudioUris = remember { mutableSetOf<String>() }

    fun cleanupDraftMedia(keepImage: String? = null, keepAudio: String? = null) {
        val imagesToDelete = draftImageUris.filter { it != keepImage }
        val audiosToDelete = draftAudioUris.filter { it != keepAudio }
        if (imagesToDelete.isEmpty() && audiosToDelete.isEmpty()) return
        val appContext = context.applicationContext
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            imagesToDelete.forEach { ImageAttachmentStorage.deleteOwnedImageFile(appContext, it) }
            audiosToDelete.forEach { com.example.audio.AudioUtils.deleteOwnedAudioFile(appContext, it) }
        }
        draftImageUris.removeAll(imagesToDelete.toSet())
        draftAudioUris.removeAll(audiosToDelete.toSet())
    }

    fun handleDismiss() {
        cleanupDraftMedia()
        onDismiss()
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                val persisted = withContext(Dispatchers.IO) {
                    ImageAttachmentStorage.persistPickedImage(context, uri)
                }
                if (persisted != null) {
                    draftImageUris.add(persisted)
                    imageUri = persisted
                } else {
                    Toast.makeText(context, "Unable to attach selected image.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = { handleDismiss() },
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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (initialNote == null) "New Note" else "Edit Note",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Pin Button
                    IconButton(
                        onClick = { isPinned = !isPinned },
                        modifier = Modifier.testTag("pin_note_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Pin Note",
                            tint = if (isPinned) TealNeon else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = { handleDismiss() }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Title Field
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    if (titleError && it.isNotBlank()) titleError = false
                },
                placeholder = { Text("Note Title…") },
                isError = titleError,
                supportingText = if (titleError) {
                    { Text("Title is required", color = PriorityHigh) }
                } else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("note_title_input"),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TealNeon,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Categories Selector
            Text(
                text = "Category Tag",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(NOTE_CATEGORIES) { cat ->
                    val isSelected = category.equals(cat.name, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            category = cat.name
                            if (cat.name == "Checklist") {
                                isChecklist = true
                                if (checklistList.isEmpty()) {
                                    checklistList = mutableListOf(Pair(false, ""))
                                }
                            }
                        },
                        label = {
                            Text("${cat.icon} ${cat.name}")
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TealNeonDark.copy(alpha = 0.3f),
                            selectedLabelColor = TealNeon,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = MaterialTheme.colorScheme.outlineVariant,
                            selectedBorderColor = TealNeon
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Color Accent Selector
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Accent Color:",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(COLOR_PALETTE) { hex ->
                        val parsed = try {
                            Color(android.graphics.Color.parseColor(hex))
                        } catch (_: Exception) { TealNeon }
                        val isSelected = colorHex.equals(hex, ignoreCase = true)

                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(parsed)
                                .border(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    color = if (isSelected) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { colorHex = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mode Selector: Plain Text vs Checklist
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (!isChecklist) TealNeonDark.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (!isChecklist) TealNeon else Color.Transparent
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { isChecklist = false }
                ) {
                    Text(
                        text = "📝 Standard Note",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (!isChecklist) FontWeight.Bold else FontWeight.Normal,
                            color = if (!isChecklist) TealNeon else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.padding(10.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isChecklist) TealNeonDark.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isChecklist) TealNeon else Color.Transparent
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            isChecklist = true
                            if (checklistList.isEmpty()) {
                                checklistList = mutableListOf(Pair(false, ""))
                            }
                        }
                ) {
                    Text(
                        text = "✅ Checklist Mode",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = if (isChecklist) FontWeight.Bold else FontWeight.Normal,
                            color = if (isChecklist) TealNeon else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Body: Checklist or Text Field
            if (isChecklist) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    checklistList.forEachIndexed { index, pair ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = pair.first,
                                onCheckedChange = { checked ->
                                    val updated = checklistList.toMutableList()
                                    updated[index] = Pair(checked, pair.second)
                                    checklistList = updated
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = TealNeon,
                                    checkmarkColor = TealDeep
                                )
                            )

                            OutlinedTextField(
                                value = pair.second,
                                onValueChange = { newText ->
                                    val updated = checklistList.toMutableList()
                                    updated[index] = Pair(pair.first, newText)
                                    checklistList = updated
                                },
                                placeholder = { Text("Checklist item ${index + 1}…") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = TealNeon,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                )
                            )

                            IconButton(
                                onClick = {
                                    val updated = checklistList.toMutableList()
                                    updated.removeAt(index)
                                    checklistList = updated
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove item",
                                    tint = PriorityHigh,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            val updated = checklistList.toMutableList()
                            updated.add(Pair(false, ""))
                            checklistList = updated
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = TealNeon
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Item", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    placeholder = { Text("Write your thoughts, meeting notes, checklist, or ideas…") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .testTag("note_content_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TealNeon,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Audio Voice Note Attachment
            TaskEditAudioSection(
                audioUri = audioUri,
                audioDurationSeconds = audioDurationSeconds,
                onAudioUpdated = { uri, dur ->
                    if (!uri.isNullOrBlank() && uri != initialNote?.audioUri) {
                        draftAudioUris.add(uri)
                    }
                    audioUri = uri
                    audioDurationSeconds = dur
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Attached Photo
            Text(
                text = "Attached Photo",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Spacer(modifier = Modifier.height(6.dp))

            if (!imageUri.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, TealNeon.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                ) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = "Note photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(130.dp)
                    )

                    IconButton(
                        onClick = { imageUri = null },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(30.dp)
                            .background(Color.Black.copy(alpha = 0.7f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove photo",
                            tint = PriorityHigh,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = TealNeon, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add image or screenshot…", style = MaterialTheme.typography.bodySmall.copy(color = TealNeon, fontWeight = FontWeight.SemiBold))
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Save Button
            Button(
                onClick = {
                    if (title.isBlank()) {
                        titleError = true
                    } else {
                        val serializedChecklist = checklistList
                            .filter { it.second.isNotBlank() }
                            .joinToString("\n") { (isDone, itemText) ->
                                "${if (isDone) "Done" else "Todo"}|$itemText"
                            }

                        cleanupDraftMedia(keepImage = imageUri, keepAudio = audioUri)
                        onSave(
                            initialNote?.id ?: 0L,
                            title,
                            content,
                            category,
                            colorHex,
                            isPinned,
                            audioUri,
                            audioDurationSeconds,
                            imageUri,
                            isChecklist,
                            serializedChecklist
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_note_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TealNeon,
                    contentColor = TealDeep
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = if (initialNote == null) "Create Note" else "Save Note Changes",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
