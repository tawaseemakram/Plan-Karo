package com.example.ui.notes

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.ads.ScreenOptionBannerAd
import com.example.data.model.NoteItem
import com.example.ui.TaskViewModel
import com.example.ui.components.TaskCardAudioPlayer
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.TealDeep
import com.example.ui.theme.TealNeon
import com.example.ui.theme.TealNeonDark
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotesScreen(
    viewModel: TaskViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val notes by viewModel.uiNotes.collectAsStateWithLifecycle()
    val allNotes by viewModel.allNotes.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchNotesQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedNoteCategory.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("notes_screen"),
        containerColor = DarkBg,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openAddNote() },
                containerColor = TealNeon,
                contentColor = DarkBg,
                shape = CircleShape,
                modifier = Modifier
                    .size(58.dp)
                    .testTag("add_note_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create New Note",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // Header
            item {
                NotesHeader(
                    totalNotes = allNotes.size,
                    pinnedNotes = allNotes.count { it.isPinned },
                    onQuickAdd = { viewModel.openAddNote() }
                )
            }

            // Search Bar
            item {
                NotesSearchBar(
                    query = searchQuery,
                    onQueryChange = { viewModel.onSearchNotesChanged(it) }
                )
            }

            // Category Filter Chips
            item {
                NotesCategoryFilterRow(
                    selectedCategory = selectedCategory,
                    onSelectCategory = { viewModel.selectNoteCategory(it) }
                )
            }

            // Empty State
            if (notes.isEmpty()) {
                item {
                    NotesEmptyState(
                        isSearching = searchQuery.isNotBlank() || selectedCategory != null,
                        onCreateNote = { viewModel.openAddNote() }
                    )
                }
            } else {
                items(notes, key = { it.id }) { note ->
                    NoteCard(
                        note = note,
                        onClick = { viewModel.openEditNote(note) },
                        onTogglePin = { viewModel.toggleNotePin(note) },
                        onDelete = {
                            viewModel.deleteNote(note)
                            Toast.makeText(context, "Note deleted", Toast.LENGTH_SHORT).show()
                        },
                        onToggleChecklistItem = { index ->
                            viewModel.toggleChecklistItem(note, index)
                        },
                        onConvertToTask = {
                            viewModel.convertNoteToTask(note)
                            Toast.makeText(
                                context,
                                "⚡ Converted \"${note.title}\" to Daily Task!",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        onImageClick = {
                            note.imageUri?.let { viewModel.openImagePreview(it) }
                        }
                    )
                }
            }

            // Screen Banner Ad
            item {
                ScreenOptionBannerAd(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun NotesHeader(
    totalNotes: Int,
    pinnedNotes: Int,
    onQuickAdd: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
        color = DarkSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, TealDeep.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Plan Karo Notes",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "📝", fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$totalNotes notes • $pinnedNotes pinned • 1-tap task conversion",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White.copy(alpha = 0.6f)
                    )
                )
            }

            Button(
                onClick = onQuickAdd,
                colors = ButtonDefaults.buttonColors(
                    containerColor = TealNeonDark,
                    contentColor = TealNeon
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, TealNeon.copy(alpha = 0.4f)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.testTag("quick_new_note_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "New",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun NotesSearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = {
            Text(
                "Search notes, checklists, voice memos...",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 14.sp
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search Notes",
                tint = TealNeon
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear search",
                        tint = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = DarkSurface,
            unfocusedContainerColor = DarkSurface,
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedBorderColor = TealNeon,
            unfocusedBorderColor = TealDeep.copy(alpha = 0.35f)
        ),
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("notes_search_field")
    )
}

@Composable
private fun NotesCategoryFilterRow(
    selectedCategory: String?,
    onSelectCategory: (String?) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onSelectCategory(null) },
                label = { Text("All Notes") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = TealNeon,
                    selectedLabelColor = DarkBg,
                    containerColor = DarkSurface,
                    labelColor = Color.White.copy(alpha = 0.7f)
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selectedCategory == null,
                    borderColor = TealDeep.copy(alpha = 0.4f),
                    selectedBorderColor = TealNeon
                ),
                shape = RoundedCornerShape(20.dp)
            )
        }

        items(NOTE_CATEGORIES) { cat ->
            val isSelected = selectedCategory.equals(cat.name, ignoreCase = true)
            FilterChip(
                selected = isSelected,
                onClick = {
                    onSelectCategory(if (isSelected) null else cat.name)
                },
                label = { Text("${cat.icon} ${cat.name}") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = parseColorSafely(cat.defaultColor),
                    selectedLabelColor = DarkBg,
                    containerColor = DarkSurface,
                    labelColor = Color.White.copy(alpha = 0.75f)
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = TealDeep.copy(alpha = 0.4f),
                    selectedBorderColor = parseColorSafely(cat.defaultColor)
                ),
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

@Composable
private fun NoteCard(
    note: NoteItem,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit,
    onToggleChecklistItem: (Int) -> Unit,
    onConvertToTask: () -> Unit,
    onImageClick: () -> Unit
) {
    val accentColor = remember(note.colorHex) { parseColorSafely(note.colorHex) }
    val formattedDate = remember(note.updatedAt) {
        val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
        sdf.format(Date(note.updatedAt))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() }
            .testTag("note_card_${note.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
        border = BorderStroke(
            1.dp,
            if (note.isPinned) accentColor else accentColor.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Top Row: Category tag, Pinned icon, Action menu buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = accentColor.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = getCategoryWithIcon(note.category),
                            color = accentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    if (note.isPinned) {
                        Surface(
                            color = Color(0xFFFFB800).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, Color(0xFFFFB800).copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PushPin,
                                    contentDescription = "Pinned",
                                    tint = Color(0xFFFFB800),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "PINNED",
                                    color = Color(0xFFFFB800),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onTogglePin,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("pin_note_${note.id}")
                    ) {
                        Icon(
                            imageVector = if (note.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (note.isPinned) "Unpin Note" else "Pin Note",
                            tint = if (note.isPinned) Color(0xFFFFB800) else Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("delete_note_${note.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete Note",
                            tint = Color.White.copy(alpha = 0.45f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title
            Text(
                text = note.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Content or Interactive Checklist
            if (note.isChecklist && note.checklistItems.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                ChecklistSection(
                    checklistText = note.checklistItems,
                    accentColor = accentColor,
                    onToggleIndex = onToggleChecklistItem
                )
            } else if (note.content.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White.copy(alpha = 0.8f),
                        lineHeight = 20.sp
                    ),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Embedded Audio Player if voice note attached
            if (!note.audioUri.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                TaskCardAudioPlayer(
                    audioUri = note.audioUri,
                    durationSeconds = note.audioDurationSeconds,
                    taskId = note.id
                )
            }

            // Attached Photo Preview
            if (!note.imageUri.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onImageClick() }
                ) {
                    AsyncImage(
                        model = note.imageUri,
                        contentDescription = "Attached Photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Bar: Date & 1-Tap Convert to Task
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.White.copy(alpha = 0.45f),
                        fontSize = 11.sp
                    )
                )

                // 1-Tap Convert to Task
                OutlinedButton(
                    onClick = onConvertToTask,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TealNeon
                    ),
                    border = BorderStroke(1.dp, TealNeon.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(34.dp)
                        .testTag("convert_note_to_task_${note.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.FlashOn,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "To Task",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ChecklistSection(
    checklistText: String,
    accentColor: Color,
    onToggleIndex: (Int) -> Unit
) {
    val items = remember(checklistText) {
        checklistText.lines().filter { it.isNotBlank() }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = DarkSurface.copy(alpha = 0.6f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            items.take(6).forEachIndexed { index, rawLine ->
                val parts = rawLine.split("|", limit = 2)
                val isDone = parts.size == 2 && parts[0] == "Done"
                val text = if (parts.size == 2) parts[1] else rawLine

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleIndex(index) }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isDone,
                        onCheckedChange = { onToggleIndex(index) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = accentColor,
                            uncheckedColor = Color.White.copy(alpha = 0.4f),
                            checkmarkColor = DarkBg
                        ),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = if (isDone) Color.White.copy(alpha = 0.45f) else Color.White,
                            textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (items.size > 6) {
                Text(
                    text = "+ ${items.size - 6} more checklist items...",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = accentColor.copy(alpha = 0.8f),
                        fontSize = 11.sp
                    ),
                    modifier = Modifier.padding(start = 32.dp, top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun NotesEmptyState(
    isSearching: Boolean,
    onCreateNote: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        color = DarkSurface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, TealDeep.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        brush = Brush.radialGradient(
                            listOf(TealNeon.copy(alpha = 0.25f), Color.Transparent)
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isSearching) "🔍" else "💡",
                    fontSize = 32.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = if (isSearching) "No matching notes" else "Capture Thoughts in Plan Karo",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (isSearching) {
                    "Try searching with different keywords or clearing your category filter."
                } else {
                    "Write quick ideas, record voice memos, make checklists, and convert them to daily tasks with 1 tap."
                },
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color.White.copy(alpha = 0.65f),
                    lineHeight = 18.sp
                ),
                modifier = Modifier.padding(horizontal = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onCreateNote,
                colors = ButtonDefaults.buttonColors(
                    containerColor = TealNeon,
                    contentColor = DarkBg
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                modifier = Modifier.testTag("create_first_note_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Create a Note",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

private fun getCategoryWithIcon(category: String): String {
    val matched = NOTE_CATEGORIES.firstOrNull { it.name.equals(category, ignoreCase = true) }
    return if (matched != null) "${matched.icon} ${matched.name}" else "💡 $category"
}

private fun parseColorSafely(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        TealNeon
    }
}
