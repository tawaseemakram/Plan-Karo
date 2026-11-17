package com.example.data.repository

import com.example.data.local.NoteDao
import com.example.data.model.NoteItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class NoteRepository(private val noteDao: NoteDao) {

    val allNotes: Flow<List<NoteItem>> = noteDao.getAllNotes()

    suspend fun insertNote(note: NoteItem): Long = withContext(Dispatchers.IO) {
        noteDao.insertNote(note)
    }

    suspend fun updateNote(note: NoteItem) = withContext(Dispatchers.IO) {
        noteDao.updateNote(note.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteNote(note: NoteItem) = withContext(Dispatchers.IO) {
        noteDao.deleteNote(note)
    }

    suspend fun togglePin(id: Long) = withContext(Dispatchers.IO) {
        noteDao.togglePin(id)
    }
}
