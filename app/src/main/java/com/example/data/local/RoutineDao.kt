package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.RoutineItem
import com.example.data.model.RoutineTime
import kotlinx.coroutines.flow.Flow

@Dao
interface RoutineDao {
    @Query("SELECT * FROM routines ORDER BY orderIndex ASC, id ASC")
    fun getAllRoutines(): Flow<List<RoutineItem>>

    @Query("SELECT * FROM routines WHERE routineTime = :routineTime ORDER BY orderIndex ASC, id ASC")
    fun getRoutinesByTime(routineTime: RoutineTime): Flow<List<RoutineItem>>

    @Query("SELECT * FROM routines WHERE isEnabled = 1 ORDER BY orderIndex ASC, id ASC")
    fun getEnabledRoutines(): Flow<List<RoutineItem>>

    @Query("SELECT * FROM routines WHERE id = :id")
    suspend fun getRoutineById(id: Long): RoutineItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutine(routine: RoutineItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllRoutines(routines: List<RoutineItem>): List<Long>

    @Update
    suspend fun updateRoutine(routine: RoutineItem)

    @Update
    suspend fun updateRoutines(routines: List<RoutineItem>)

    @Delete
    suspend fun deleteRoutine(routine: RoutineItem)

    @Query("DELETE FROM routines WHERE id = :id")
    suspend fun deleteRoutineById(id: Long)

    @Query("UPDATE routines SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun setRoutineEnabled(id: Long, isEnabled: Boolean)

    @Query("SELECT COUNT(*) FROM routines")
    fun getTotalRoutinesCount(): Flow<Int>
}
