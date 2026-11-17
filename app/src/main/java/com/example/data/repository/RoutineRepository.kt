package com.example.data.repository

import android.content.Context
import com.example.data.local.RoutineDao
import com.example.data.model.RoutineItem
import com.example.data.model.RoutineTime
import com.example.reminder.RoutineReminderManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class RoutineRepository(
    private val routineDao: RoutineDao,
    private val context: Context? = null
) {
    val allRoutines: Flow<List<RoutineItem>> = routineDao.getAllRoutines()
    val enabledRoutines: Flow<List<RoutineItem>> = routineDao.getEnabledRoutines()

    fun getRoutinesByTime(routineTime: RoutineTime): Flow<List<RoutineItem>> {
        return routineDao.getRoutinesByTime(routineTime)
    }

    suspend fun insertRoutine(routine: RoutineItem): Long = withContext(Dispatchers.IO) {
        val newId = routineDao.insertRoutine(routine)
        context?.let { ctx ->
            val saved = routine.copy(id = newId)
            RoutineReminderManager.scheduleRoutine(ctx, saved)
        }
        newId
    }

    suspend fun insertAllRoutines(routines: List<RoutineItem>): List<Long> = withContext(Dispatchers.IO) {
        val ids = routineDao.insertAllRoutines(routines)
        context?.let { ctx ->
            routines.forEachIndexed { idx, routine ->
                val newId = ids.getOrNull(idx) ?: routine.id
                RoutineReminderManager.scheduleRoutine(ctx, routine.copy(id = newId))
            }
        }
        ids
    }

    suspend fun updateRoutine(routine: RoutineItem) = withContext(Dispatchers.IO) {
        routineDao.updateRoutine(routine)
        context?.let { ctx ->
            RoutineReminderManager.scheduleRoutine(ctx, routine)
        }
    }

    suspend fun toggleRoutineEnabled(routine: RoutineItem) = withContext(Dispatchers.IO) {
        val newEnabled = !routine.isEnabled
        routineDao.setRoutineEnabled(routine.id, newEnabled)
        context?.let { ctx ->
            RoutineReminderManager.scheduleRoutine(ctx, routine.copy(isEnabled = newEnabled))
        }
    }

    suspend fun deleteRoutine(routine: RoutineItem) = withContext(Dispatchers.IO) {
        routineDao.deleteRoutine(routine)
        context?.let { ctx ->
            RoutineReminderManager.cancelRoutine(ctx, routine.id)
        }
    }

    suspend fun deleteRoutineById(id: Long) = withContext(Dispatchers.IO) {
        routineDao.deleteRoutineById(id)
        context?.let { ctx ->
            RoutineReminderManager.cancelRoutine(ctx, id)
        }
    }

    suspend fun reorderRoutines(orderedList: List<RoutineItem>) = withContext(Dispatchers.IO) {
        val updated = orderedList.mapIndexed { index, item -> item.copy(orderIndex = index) }
        routineDao.updateRoutines(updated)
    }
}
