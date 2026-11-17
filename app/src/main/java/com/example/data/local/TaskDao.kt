package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Priority
import com.example.data.model.TaskItem
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY orderIndex ASC, id DESC")
    fun getAllTasks(): Flow<List<TaskItem>>

    @Query("SELECT * FROM tasks WHERE taskDate = :date ORDER BY orderIndex ASC, id DESC")
    fun getTasksForDate(date: String): Flow<List<TaskItem>>

    @Query("SELECT * FROM tasks WHERE taskDate BETWEEN :startDate AND :endDate ORDER BY taskDate ASC, orderIndex ASC")
    fun getTasksBetweenDates(startDate: String, endDate: String): Flow<List<TaskItem>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY taskDate ASC, orderIndex ASC")
    fun getPendingTasks(): Flow<List<TaskItem>>

    @Query("SELECT * FROM tasks WHERE priority = :priority ORDER BY taskDate ASC, orderIndex ASC")
    fun getTasksByPriority(priority: Priority): Flow<List<TaskItem>>

    @Query("SELECT * FROM tasks WHERE title LIKE '%' || :query || '%' OR notes LIKE '%' || :query || '%' ORDER BY taskDate DESC")
    fun searchTasks(query: String): Flow<List<TaskItem>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): TaskItem?

    @Query("SELECT * FROM tasks WHERE taskDate = :date AND title = :title LIMIT 1")
    suspend fun getTaskByDateAndTitle(date: String, title: String): TaskItem?

    @Query("SELECT MAX(orderIndex) FROM tasks WHERE taskDate = :date")
    suspend fun getMaxOrderIndex(date: String): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllTasks(tasks: List<TaskItem>): List<Long>

    @Update
    suspend fun updateTask(task: TaskItem)

    @Update
    suspend fun updateTasks(tasks: List<TaskItem>)

    @Delete
    suspend fun deleteTask(task: TaskItem)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)

    @Query("DELETE FROM tasks WHERE isCompleted = 1")
    suspend fun deleteCompletedTasks(): Int

    @Query("SELECT * FROM tasks WHERE reminderTimeMillis IS NOT NULL AND isReminderDismissed = 0 AND isCompleted = 0")
    suspend fun getActiveReminders(): List<TaskItem>

    @Query("SELECT COUNT(*) FROM tasks WHERE taskDate = :date AND isCompleted = 0")
    fun getPendingCountForDate(date: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM tasks WHERE taskDate = :date AND isCompleted = 1")
    fun getCompletedCountForDate(date: String): Flow<Int>

    @Query("SELECT * FROM tasks WHERE repeatMode != 'NONE' ORDER BY taskDate DESC")
    suspend fun getRepeatingTasks(): List<TaskItem>

    @Query("SELECT COUNT(*) FROM tasks WHERE imageUri = :uri")
    suspend fun countImageUriReferences(uri: String): Int

    @Query("SELECT COUNT(*) FROM tasks WHERE audioUri = :uri")
    suspend fun countAudioUriReferences(uri: String): Int

    @Query("SELECT DISTINCT imageUri FROM tasks WHERE imageUri IS NOT NULL AND imageUri != ''")
    suspend fun getAllImageUris(): List<String>

    @Query("SELECT DISTINCT audioUri FROM tasks WHERE audioUri IS NOT NULL AND audioUri != ''")
    suspend fun getAllAudioUris(): List<String>
}
