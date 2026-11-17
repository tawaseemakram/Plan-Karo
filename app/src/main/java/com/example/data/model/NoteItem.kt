package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notes",
    indices = [
        Index(value = ["isPinned"]),
        Index(value = ["category"]),
        Index(value = ["updatedAt"])
    ]
)
data class NoteItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val content: String = "",
    val category: String = "Idea", // Idea 💡, Work 💼, Personal 🧘, Study 📚, Meeting 👥, Checklist ✅
    val colorHex: String = "#00E5BE", // Accent neon color
    val isPinned: Boolean = false,
    val audioUri: String? = null,
    val audioDurationSeconds: Int = 0,
    val imageUri: String? = null,
    val isChecklist: Boolean = false,
    val checklistItems: String = "", // Delimited checklist entries: "Done|Buy milk\nTodo|Call mechanic"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
