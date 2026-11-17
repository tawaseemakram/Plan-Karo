package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.NoteItem
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllNotes(): Flow<List<NoteItem>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Long): NoteItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteItem): Long

    @Update
    suspend fun updateNote(note: NoteItem)

    @Delete
    suspend fun deleteNote(note: NoteItem)

    @Query("UPDATE notes SET isPinned = NOT isPinned, updatedAt = :updatedAt WHERE id = :id")
    suspend fun togglePin(id: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM notes WHERE imageUri = :uri")
    suspend fun countImageUriReferences(uri: String): Int

    @Query("SELECT COUNT(*) FROM notes WHERE audioUri = :uri")
    suspend fun countAudioUriReferences(uri: String): Int

    @Query("SELECT DISTINCT imageUri FROM notes WHERE imageUri IS NOT NULL AND imageUri != ''")
    suspend fun getAllImageUris(): List<String>

    @Query("SELECT DISTINCT audioUri FROM notes WHERE audioUri IS NOT NULL AND audioUri != ''")
    suspend fun getAllAudioUris(): List<String>
}
