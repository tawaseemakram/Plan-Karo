package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.HabitItem
import com.example.data.model.NoteItem
import com.example.data.model.RoutineItem
import com.example.data.model.TaskItem

/**
 * Room Database for Plan Karo.
 *
 * Schema Migration Strategy (Zero Data Loss):
 * 1. `exportSchema = true` writes versioned JSON snapshots under `app/schemas/` so every schema
 *    change is tracked and testable across versions, and enables Room `AutoMigration` for future
 *    additive updates.
 * 2. `.fallbackToDestructiveMigration()` is intentionally OMITTED so Room can never silently wipe
 *    user tasks, notes, habits, or routines on an app update.
 * 3. Every upgrade path from v1..v7 -> v8 is explicitly covered in [ALL_MIGRATIONS] using
 *    idempotent `CREATE TABLE IF NOT EXISTS`, `CREATE INDEX IF NOT EXISTS`, and [addColumnIfMissing]
 *    checks (`PRAGMA table_info`).
 * 4. For future releases:
 *    - Additive change (new nullable/defaulted column, new table, or new index): increment `version`
 *      and add `AutoMigration(from = X, to = X + 1)` to `@Database(autoMigrations = [...])`, OR add
 *      an explicit `Migration(X, X + 1)` using [addColumnIfMissing].
 *    - Breaking change (rename/delete/type change): increment `version` and add an explicit
 *      `Migration(X, X + 1)` in [ALL_MIGRATIONS] using SQLite's create-copy-drop-rename pattern.
 */
@Database(
    entities = [TaskItem::class, HabitItem::class, RoutineItem::class, NoteItem::class],
    version = 8,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class TaskDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun habitDao(): HabitDao
    abstract fun routineDao(): RoutineDao
    abstract fun noteDao(): NoteDao

    companion object {
        const val DATABASE_NAME = "plankaro_database"

        @Volatile
        private var INSTANCE: TaskDatabase? = null

        /**
         * Returns true if [columnName] already exists on [tableName].
         * Prevents duplicate-column crashes when migrating across intermediate builds.
         */
        internal fun hasColumn(
            db: SupportSQLiteDatabase,
            tableName: String,
            columnName: String
        ): Boolean {
            db.query("PRAGMA table_info(`$tableName`)").use { cursor ->
                val nameIndex = cursor.getColumnIndex("name")
                while (cursor.moveToNext()) {
                    if (nameIndex >= 0 && cursor.getString(nameIndex) == columnName) {
                        return true
                    }
                }
            }
            return false
        }

        /**
         * Safely adds a column only if it does not already exist in [tableName].
         */
        internal fun addColumnIfMissing(
            db: SupportSQLiteDatabase,
            tableName: String,
            columnName: String,
            columnDefinition: String
        ) {
            if (!hasColumn(db, tableName, columnName)) {
                db.execSQL("ALTER TABLE `$tableName` ADD COLUMN `$columnName` $columnDefinition")
            }
        }

        /**
         * Non-destructive schema reconciliation up to v8.
         * Ensures all tables, defaulted columns, and Room indices exist while preserving all user rows.
         */
        internal fun reconcileSchemaToV8(db: SupportSQLiteDatabase) {
            // 1. tasks table + v8 columns + indices
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `tasks` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `title` TEXT NOT NULL,
                    `notes` TEXT NOT NULL DEFAULT '',
                    `priority` TEXT NOT NULL DEFAULT 'MEDIUM',
                    `category` TEXT NOT NULL DEFAULT 'Personal',
                    `isCompleted` INTEGER NOT NULL DEFAULT 0,
                    `completedAt` INTEGER,
                    `orderIndex` INTEGER NOT NULL DEFAULT 0,
                    `reminderTimeMillis` INTEGER,
                    `isReminderDismissed` INTEGER NOT NULL DEFAULT 0,
                    `imageUri` TEXT,
                    `audioUri` TEXT,
                    `audioDurationSeconds` INTEGER NOT NULL DEFAULT 0,
                    `taskDate` TEXT NOT NULL DEFAULT '',
                    `repeatMode` TEXT NOT NULL DEFAULT 'NONE',
                    `createdAt` INTEGER NOT NULL DEFAULT 0
                )
                """.trimIndent()
            )
            addColumnIfMissing(db, "tasks", "notes", "TEXT NOT NULL DEFAULT ''")
            addColumnIfMissing(db, "tasks", "priority", "TEXT NOT NULL DEFAULT 'MEDIUM'")
            addColumnIfMissing(db, "tasks", "category", "TEXT NOT NULL DEFAULT 'Personal'")
            addColumnIfMissing(db, "tasks", "isCompleted", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing(db, "tasks", "completedAt", "INTEGER")
            addColumnIfMissing(db, "tasks", "orderIndex", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing(db, "tasks", "reminderTimeMillis", "INTEGER")
            addColumnIfMissing(db, "tasks", "isReminderDismissed", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing(db, "tasks", "imageUri", "TEXT")
            addColumnIfMissing(db, "tasks", "audioUri", "TEXT")
            addColumnIfMissing(db, "tasks", "audioDurationSeconds", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing(db, "tasks", "taskDate", "TEXT NOT NULL DEFAULT ''")
            addColumnIfMissing(db, "tasks", "repeatMode", "TEXT NOT NULL DEFAULT 'NONE'")
            addColumnIfMissing(db, "tasks", "createdAt", "INTEGER NOT NULL DEFAULT 0")

            db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_taskDate` ON `tasks` (`taskDate`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_isCompleted` ON `tasks` (`isCompleted`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_priority` ON `tasks` (`priority`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_category` ON `tasks` (`category`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_tasks_taskDate_orderIndex` ON `tasks` (`taskDate`, `orderIndex`)")

            // 2. habits table + v8 columns + indices
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `habits` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `title` TEXT NOT NULL,
                    `category` TEXT NOT NULL DEFAULT 'Health',
                    `routineTime` TEXT NOT NULL DEFAULT 'MORNING',
                    `streakCount` INTEGER NOT NULL DEFAULT 0,
                    `bestStreak` INTEGER NOT NULL DEFAULT 0,
                    `completedDates` TEXT NOT NULL DEFAULT '',
                    `targetDaysPerWeek` INTEGER NOT NULL DEFAULT 7,
                    `reminderTime` TEXT DEFAULT NULL,
                    `createdAt` INTEGER NOT NULL DEFAULT 0
                )
                """.trimIndent()
            )
            addColumnIfMissing(db, "habits", "category", "TEXT NOT NULL DEFAULT 'Health'")
            addColumnIfMissing(db, "habits", "routineTime", "TEXT NOT NULL DEFAULT 'MORNING'")
            addColumnIfMissing(db, "habits", "streakCount", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing(db, "habits", "bestStreak", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing(db, "habits", "completedDates", "TEXT NOT NULL DEFAULT ''")
            addColumnIfMissing(db, "habits", "targetDaysPerWeek", "INTEGER NOT NULL DEFAULT 7")
            addColumnIfMissing(db, "habits", "reminderTime", "TEXT DEFAULT NULL")
            addColumnIfMissing(db, "habits", "createdAt", "INTEGER NOT NULL DEFAULT 0")

            db.execSQL("CREATE INDEX IF NOT EXISTS `index_habits_routineTime` ON `habits` (`routineTime`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_habits_category` ON `habits` (`category`)")

            // 3. routines table + v8 columns + indices
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `routines` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `title` TEXT NOT NULL,
                    `description` TEXT NOT NULL DEFAULT '',
                    `routineTime` TEXT NOT NULL DEFAULT 'MORNING',
                    `scheduledTime` TEXT NOT NULL DEFAULT '08:00 AM',
                    `daysOfWeek` TEXT NOT NULL DEFAULT 'DAILY',
                    `isEnabled` INTEGER NOT NULL DEFAULT 1,
                    `orderIndex` INTEGER NOT NULL DEFAULT 0,
                    `createdAt` INTEGER NOT NULL DEFAULT 0
                )
                """.trimIndent()
            )
            addColumnIfMissing(db, "routines", "description", "TEXT NOT NULL DEFAULT ''")
            addColumnIfMissing(db, "routines", "routineTime", "TEXT NOT NULL DEFAULT 'MORNING'")
            addColumnIfMissing(db, "routines", "scheduledTime", "TEXT NOT NULL DEFAULT '08:00 AM'")
            addColumnIfMissing(db, "routines", "daysOfWeek", "TEXT NOT NULL DEFAULT 'DAILY'")
            addColumnIfMissing(db, "routines", "isEnabled", "INTEGER NOT NULL DEFAULT 1")
            addColumnIfMissing(db, "routines", "orderIndex", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing(db, "routines", "createdAt", "INTEGER NOT NULL DEFAULT 0")

            db.execSQL("CREATE INDEX IF NOT EXISTS `index_routines_routineTime` ON `routines` (`routineTime`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_routines_isEnabled` ON `routines` (`isEnabled`)")

            // 4. notes table + v8 columns + indices
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `notes` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `title` TEXT NOT NULL,
                    `content` TEXT NOT NULL DEFAULT '',
                    `category` TEXT NOT NULL DEFAULT 'Idea',
                    `colorHex` TEXT NOT NULL DEFAULT '#00E5BE',
                    `isPinned` INTEGER NOT NULL DEFAULT 0,
                    `audioUri` TEXT,
                    `audioDurationSeconds` INTEGER NOT NULL DEFAULT 0,
                    `imageUri` TEXT,
                    `isChecklist` INTEGER NOT NULL DEFAULT 0,
                    `checklistItems` TEXT NOT NULL DEFAULT '',
                    `createdAt` INTEGER NOT NULL DEFAULT 0,
                    `updatedAt` INTEGER NOT NULL DEFAULT 0
                )
                """.trimIndent()
            )
            addColumnIfMissing(db, "notes", "content", "TEXT NOT NULL DEFAULT ''")
            addColumnIfMissing(db, "notes", "category", "TEXT NOT NULL DEFAULT 'Idea'")
            addColumnIfMissing(db, "notes", "colorHex", "TEXT NOT NULL DEFAULT '#00E5BE'")
            addColumnIfMissing(db, "notes", "isPinned", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing(db, "notes", "audioUri", "TEXT")
            addColumnIfMissing(db, "notes", "audioDurationSeconds", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing(db, "notes", "imageUri", "TEXT")
            addColumnIfMissing(db, "notes", "isChecklist", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing(db, "notes", "checklistItems", "TEXT NOT NULL DEFAULT ''")
            addColumnIfMissing(db, "notes", "createdAt", "INTEGER NOT NULL DEFAULT 0")
            addColumnIfMissing(db, "notes", "updatedAt", "INTEGER NOT NULL DEFAULT 0")

            db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_isPinned` ON `notes` (`isPinned`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_category` ON `notes` (`category`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_notes_updatedAt` ON `notes` (`updatedAt`)")
        }

        private fun safeStepMigration(startVersion: Int, endVersion: Int): Migration {
            return object : Migration(startVersion, endVersion) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    reconcileSchemaToV8(db)
                }
            }
        }

        // Legacy sequential catch-up migrations (v1..v7) ensure even very old app installs
        // upgrade smoothly without data loss.
        val MIGRATION_1_2 = safeStepMigration(1, 2)
        val MIGRATION_2_3 = safeStepMigration(2, 3)
        val MIGRATION_3_4 = safeStepMigration(3, 4)
        val MIGRATION_4_5 = safeStepMigration(4, 5)
        val MIGRATION_5_6 = safeStepMigration(5, 6)
        val MIGRATION_6_7 = safeStepMigration(6, 7)

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                addColumnIfMissing(db, "habits", "reminderTime", "TEXT DEFAULT NULL")
                reconcileSchemaToV8(db)
            }
        }

        val ALL_MIGRATIONS: Array<Migration> = arrayOf(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6,
            MIGRATION_6_7,
            MIGRATION_7_8
        )

        fun getDatabase(context: Context): TaskDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TaskDatabase::class.java,
                    DATABASE_NAME
                )
                    // Explicit migrations preserve user data across every app upgrade.
                    // Never enable .fallbackToDestructiveMigration() in production.
                    .addMigrations(*ALL_MIGRATIONS)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
