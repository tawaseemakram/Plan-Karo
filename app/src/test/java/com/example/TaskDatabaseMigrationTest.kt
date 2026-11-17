package com.example

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.TaskDatabase
import com.example.data.model.Priority
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TaskDatabaseMigrationTest {

    private val testDbNames = mutableListOf<String>()

    @After
    fun tearDown() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        testDbNames.forEach { name ->
            context.deleteDatabase(name)
        }
    }

    private fun createPrePopulatedDb(
        dbName: String,
        version: Int,
        onCreateSql: (SQLiteDatabase) -> Unit
    ) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        testDbNames.add(dbName)
        context.deleteDatabase(dbName)
        val helper = object : SQLiteOpenHelper(context, dbName, null, version) {
            override fun onCreate(db: SQLiteDatabase) {
                onCreateSql(db)
            }

            override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
        }
        helper.writableDatabase.close()
    }

    @Test
    fun migrateV7ToV8_preservesAllUserDataAndPassesRoomValidation() = runBlocking {
        val dbName = "migration_test_v7_to_v8.db"
        createPrePopulatedDb(dbName, version = 7) { db ->
            db.execSQL(
                "CREATE TABLE `tasks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `notes` TEXT NOT NULL, `priority` TEXT NOT NULL, `category` TEXT NOT NULL, `isCompleted` INTEGER NOT NULL, `completedAt` INTEGER, `orderIndex` INTEGER NOT NULL, `reminderTimeMillis` INTEGER, `isReminderDismissed` INTEGER NOT NULL, `imageUri` TEXT, `audioUri` TEXT, `audioDurationSeconds` INTEGER NOT NULL, `taskDate` TEXT NOT NULL, `repeatMode` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)"
            )
            db.execSQL("CREATE INDEX `index_tasks_taskDate` ON `tasks` (`taskDate`)")
            db.execSQL("CREATE INDEX `index_tasks_isCompleted` ON `tasks` (`isCompleted`)")
            db.execSQL("CREATE INDEX `index_tasks_priority` ON `tasks` (`priority`)")
            db.execSQL("CREATE INDEX `index_tasks_category` ON `tasks` (`category`)")
            db.execSQL("CREATE INDEX `index_tasks_taskDate_orderIndex` ON `tasks` (`taskDate`, `orderIndex`)")

            // v7 habits table WITHOUT reminderTime column
            db.execSQL(
                "CREATE TABLE `habits` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `category` TEXT NOT NULL, `routineTime` TEXT NOT NULL, `streakCount` INTEGER NOT NULL, `bestStreak` INTEGER NOT NULL, `completedDates` TEXT NOT NULL, `targetDaysPerWeek` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)"
            )
            db.execSQL("CREATE INDEX `index_habits_routineTime` ON `habits` (`routineTime`)")
            db.execSQL("CREATE INDEX `index_habits_category` ON `habits` (`category`)")

            db.execSQL(
                "CREATE TABLE `routines` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `routineTime` TEXT NOT NULL, `scheduledTime` TEXT NOT NULL, `daysOfWeek` TEXT NOT NULL, `isEnabled` INTEGER NOT NULL, `orderIndex` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)"
            )
            db.execSQL("CREATE INDEX `index_routines_routineTime` ON `routines` (`routineTime`)")
            db.execSQL("CREATE INDEX `index_routines_isEnabled` ON `routines` (`isEnabled`)")

            db.execSQL(
                "CREATE TABLE `notes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `content` TEXT NOT NULL, `category` TEXT NOT NULL, `colorHex` TEXT NOT NULL, `isPinned` INTEGER NOT NULL, `audioUri` TEXT, `audioDurationSeconds` INTEGER NOT NULL, `imageUri` TEXT, `isChecklist` INTEGER NOT NULL, `checklistItems` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)"
            )
            db.execSQL("CREATE INDEX `index_notes_isPinned` ON `notes` (`isPinned`)")
            db.execSQL("CREATE INDEX `index_notes_category` ON `notes` (`category`)")
            db.execSQL("CREATE INDEX `index_notes_updatedAt` ON `notes` (`updatedAt`)")

            // Insert sample user data in v7
            db.execSQL(
                "INSERT INTO `tasks` (`id`, `title`, `notes`, `priority`, `category`, `isCompleted`, `completedAt`, `orderIndex`, `reminderTimeMillis`, `isReminderDismissed`, `imageUri`, `audioUri`, `audioDurationSeconds`, `taskDate`, `repeatMode`, `createdAt`) VALUES (1, 'Pay Rent', 'Transfer before 5 PM', 'HIGH', 'Personal', 0, NULL, 0, NULL, 0, NULL, NULL, 0, '2026-11-13', 'MONTHLY', 1700000000000)"
            )
            db.execSQL(
                "INSERT INTO `habits` (`id`, `title`, `category`, `routineTime`, `streakCount`, `bestStreak`, `completedDates`, `targetDaysPerWeek`, `createdAt`) VALUES (10, 'Morning Walk', 'Fitness', 'MORNING', 4, 7, '2026-11-12', 5, 1700000000000)"
            )
            db.execSQL(
                "INSERT INTO `routines` (`id`, `title`, `description`, `routineTime`, `scheduledTime`, `daysOfWeek`, `isEnabled`, `orderIndex`, `createdAt`) VALUES (20, 'Evening Review', 'Check completed tasks', 'EVENING', '08:00 PM', 'DAILY', 1, 0, 1700000000000)"
            )
            db.execSQL(
                "INSERT INTO `notes` (`id`, `title`, `content`, `category`, `colorHex`, `isPinned`, `audioUri`, `audioDurationSeconds`, `imageUri`, `isChecklist`, `checklistItems`, `createdAt`, `updatedAt`) VALUES (30, 'Book List', 'Atomic Habits', 'Idea', '#00E5BE', 1, NULL, 0, NULL, 0, '', 1700000000000, 1700000000000)"
            )
        }

        val context = ApplicationProvider.getApplicationContext<Context>()
        val migratedDb = Room.databaseBuilder(context, TaskDatabase::class.java, dbName)
            .addMigrations(*TaskDatabase.ALL_MIGRATIONS)
            .allowMainThreadQueries()
            .build()

        try {
            val tasks = migratedDb.taskDao().getAllTasks().first()
            assertEquals(1, tasks.size)
            assertEquals("Pay Rent", tasks[0].title)
            assertEquals(Priority.HIGH, tasks[0].priority)

            val habits = migratedDb.habitDao().getAllHabits().first()
            assertEquals(1, habits.size)
            assertEquals("Morning Walk", habits[0].title)
            assertEquals(5, habits[0].targetDaysPerWeek)
            assertNull(habits[0].reminderTime)

            val routines = migratedDb.routineDao().getAllRoutines().first()
            assertEquals(1, routines.size)
            assertEquals("Evening Review", routines[0].title)

            val notes = migratedDb.noteDao().getAllNotes().first()
            assertEquals(1, notes.size)
            assertEquals("Book List", notes[0].title)
        } finally {
            migratedDb.close()
        }
    }

    @Test
    fun migrateLegacyV1ToV8_preservesLegacyTasksAndInitializesNewTables() = runBlocking {
        val dbName = "migration_test_v1_to_v8.db"
        createPrePopulatedDb(dbName, version = 1) { db ->
            db.execSQL(
                "CREATE TABLE `tasks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `isCompleted` INTEGER NOT NULL, `taskDate` TEXT NOT NULL)"
            )
            db.execSQL(
                "INSERT INTO `tasks` (`id`, `title`, `isCompleted`, `taskDate`) VALUES (1, 'Legacy Task Saved', 1, '2026-10-01')"
            )
        }

        val context = ApplicationProvider.getApplicationContext<Context>()
        val migratedDb = Room.databaseBuilder(context, TaskDatabase::class.java, dbName)
            .addMigrations(*TaskDatabase.ALL_MIGRATIONS)
            .allowMainThreadQueries()
            .build()

        try {
            val tasks = migratedDb.taskDao().getAllTasks().first()
            assertEquals(1, tasks.size)
            assertEquals("Legacy Task Saved", tasks[0].title)
            assertTrue(tasks[0].isCompleted)
            assertEquals("Personal", tasks[0].category)

            // Verify newly reconciled tables work seamlessly
            assertEquals(0, migratedDb.habitDao().getAllHabits().first().size)
            assertEquals(0, migratedDb.routineDao().getAllRoutines().first().size)
            assertEquals(0, migratedDb.noteDao().getAllNotes().first().size)
        } finally {
            migratedDb.close()
        }
    }
}
