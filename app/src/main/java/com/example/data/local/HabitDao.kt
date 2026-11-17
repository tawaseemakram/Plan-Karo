package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.HabitItem
import com.example.data.model.RoutineTime
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY id ASC")
    fun getAllHabits(): Flow<List<HabitItem>>

    @Query("SELECT * FROM habits WHERE routineTime = :routineTime ORDER BY id ASC")
    fun getHabitsByRoutineTime(routineTime: RoutineTime): Flow<List<HabitItem>>

    @Query("SELECT * FROM habits WHERE category = :category ORDER BY id ASC")
    fun getHabitsByCategory(category: String): Flow<List<HabitItem>>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getHabitById(id: Long): HabitItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllHabits(habits: List<HabitItem>): List<Long>

    @Update
    suspend fun updateHabit(habit: HabitItem)

    @Delete
    suspend fun deleteHabit(habit: HabitItem)

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun deleteHabitById(id: Long)

    @Query("SELECT COUNT(*) FROM habits")
    fun getTotalHabitsCount(): Flow<Int>
}
