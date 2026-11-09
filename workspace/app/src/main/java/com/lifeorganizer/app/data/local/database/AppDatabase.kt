package com.lifeorganizer.app.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.lifeorganizer.app.data.local.dao.ActivityContextDao
import com.lifeorganizer.app.data.local.dao.ActivityDao
import com.lifeorganizer.app.data.local.dao.AppClassificationDao
import com.lifeorganizer.app.data.local.dao.AppUsageLogDao
import com.lifeorganizer.app.data.local.dao.ExecutionRecordDao
import com.lifeorganizer.app.data.local.dao.FocusSessionDao
import com.lifeorganizer.app.data.local.dao.HabitDao
import com.lifeorganizer.app.data.local.dao.RecommendationFeedbackDao
import com.lifeorganizer.app.data.local.dao.ScheduleEntryDao
import com.lifeorganizer.app.data.local.entity.ActivityContextEntity
import com.lifeorganizer.app.data.local.entity.ActivityEntity
import com.lifeorganizer.app.data.local.entity.AppClassificationEntity
import com.lifeorganizer.app.data.local.entity.AppUsageLogEntity
import com.lifeorganizer.app.data.local.entity.ExecutionRecordEntity
import com.lifeorganizer.app.data.local.entity.FocusSessionEntity
import com.lifeorganizer.app.data.local.entity.HabitEntity
import com.lifeorganizer.app.data.local.entity.RecommendationFeedbackEntity
import com.lifeorganizer.app.data.local.entity.ScheduleEntryEntity

@Database(
    entities = [
        ActivityEntity::class,
        ScheduleEntryEntity::class,
        HabitEntity::class,
        ExecutionRecordEntity::class,
        ActivityContextEntity::class,
        RecommendationFeedbackEntity::class,
        FocusSessionEntity::class,
        AppClassificationEntity::class,
        AppUsageLogEntity::class
    ],
    version = 6,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun activityDao(): ActivityDao
    abstract fun scheduleEntryDao(): ScheduleEntryDao
    abstract fun habitDao(): HabitDao
    abstract fun executionRecordDao(): ExecutionRecordDao
    abstract fun activityContextDao(): ActivityContextDao
    abstract fun recommendationFeedbackDao(): RecommendationFeedbackDao
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun appClassificationDao(): AppClassificationDao
    abstract fun appUsageLogDao(): AppUsageLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE activities ADD COLUMN type TEXT NOT NULL DEFAULT 'FLEXIBLE'")
                db.execSQL("ALTER TABLE activities ADD COLUMN defaultCount INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE activities ADD COLUMN priority TEXT NOT NULL DEFAULT 'MEDIUM'")
                db.execSQL("ALTER TABLE activities ADD COLUMN colorHex TEXT NOT NULL DEFAULT '#8B9CFF'")
                db.execSQL("ALTER TABLE activities ADD COLUMN iconName TEXT NOT NULL DEFAULT 'default'")
                db.execSQL("ALTER TABLE activities ADD COLUMN description TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE activities ADD COLUMN startHour INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE activities ADD COLUMN startMinute INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE activities ADD COLUMN endHour INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE activities ADD COLUMN endMinute INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE activities ADD COLUMN recurrenceType TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE activities ADD COLUMN recurrenceDays TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE activities ADD COLUMN weeklyTargetCount INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE activities ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE activities ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add Step 3 fields to schedule_entries
                db.execSQL("ALTER TABLE schedule_entries ADD COLUMN date TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE schedule_entries ADD COLUMN startHour INTEGER NOT NULL DEFAULT 9")
                db.execSQL("ALTER TABLE schedule_entries ADD COLUMN startMinute INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE schedule_entries ADD COLUMN endHour INTEGER NOT NULL DEFAULT 10")
                db.execSQL("ALTER TABLE schedule_entries ADD COLUMN endMinute INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE schedule_entries ADD COLUMN targetDurationMinutes INTEGER NOT NULL DEFAULT 60")
                db.execSQL("ALTER TABLE schedule_entries ADD COLUMN actualDurationMinutes INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE schedule_entries ADD COLUMN completedAt INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE schedule_entries ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE schedule_entries ADD COLUMN postponedToDate TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE schedule_entries ADD COLUMN startAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE schedule_entries ADD COLUMN endAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_schedule_entries_activityId ON schedule_entries(activityId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_schedule_entries_date ON schedule_entries(date)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_schedule_entries_startAt ON schedule_entries(startAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_schedule_entries_status ON schedule_entries(status)")

                // Create execution_records table for persistent execution history & behavioral learning
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS execution_records (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        scheduleEntryId INTEGER NOT NULL,
                        activityId INTEGER NOT NULL,
                        activityTitle TEXT NOT NULL,
                        category TEXT NOT NULL,
                        plannedDate TEXT NOT NULL,
                        plannedStartMinutes INTEGER NOT NULL,
                        plannedDurationMinutes INTEGER NOT NULL,
                        actualCompletedAt INTEGER DEFAULT NULL,
                        actualDurationMinutes INTEGER DEFAULT NULL,
                        finalStatus TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        notes TEXT NOT NULL DEFAULT '',
                        FOREIGN KEY(activityId) REFERENCES activities(id) ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_execution_records_activityId ON execution_records(activityId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_execution_records_plannedDate ON execution_records(plannedDate)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_execution_records_finalStatus ON execution_records(finalStatus)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create activity_contexts table for LifeFlow Context memory
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS activity_contexts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        activityId INTEGER NOT NULL,
                        contentType TEXT NOT NULL DEFAULT 'GENERAL',
                        contentName TEXT NOT NULL,
                        currentProgress TEXT NOT NULL DEFAULT '',
                        unitType TEXT NOT NULL DEFAULT '',
                        currentUnitValue INTEGER DEFAULT NULL,
                        continuityStatus TEXT NOT NULL DEFAULT 'IN_PROGRESS',
                        lastSessionAt INTEGER NOT NULL,
                        notes TEXT NOT NULL DEFAULT '',
                        FOREIGN KEY(activityId) REFERENCES activities(id) ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_activity_contexts_activityId ON activity_contexts(activityId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_activity_contexts_continuityStatus ON activity_contexts(continuityStatus)")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create recommendation_feedbacks table for habit learning & user preference tuning
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS recommendation_feedbacks (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        recommendationType TEXT NOT NULL,
                        activityId INTEGER DEFAULT NULL,
                        activityTitle TEXT NOT NULL DEFAULT '',
                        action TEXT NOT NULL,
                        reason TEXT NOT NULL DEFAULT '',
                        timestamp INTEGER NOT NULL
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_recommendation_feedbacks_activityId ON recommendation_feedbacks(activityId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_recommendation_feedbacks_recommendationType ON recommendation_feedbacks(recommendationType)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_recommendation_feedbacks_action ON recommendation_feedbacks(action)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_recommendation_feedbacks_timestamp ON recommendation_feedbacks(timestamp)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Focus sessions table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS focus_sessions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        scheduleEntryId INTEGER DEFAULT NULL,
                        activityId INTEGER NOT NULL,
                        activityTitle TEXT NOT NULL,
                        category TEXT NOT NULL,
                        plannedDurationMinutes INTEGER NOT NULL,
                        startTimeMillis INTEGER NOT NULL,
                        elapsedSecondsBeforePause INTEGER NOT NULL DEFAULT 0,
                        pausedAtMillis INTEGER DEFAULT NULL,
                        isPaused INTEGER NOT NULL DEFAULT 0,
                        isActive INTEGER NOT NULL DEFAULT 1,
                        completedAtMillis INTEGER DEFAULT NULL,
                        notes TEXT NOT NULL DEFAULT '',
                        FOREIGN KEY(activityId) REFERENCES activities(id) ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_focus_sessions_activityId ON focus_sessions(activityId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_focus_sessions_scheduleEntryId ON focus_sessions(scheduleEntryId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_focus_sessions_isActive ON focus_sessions(isActive)")

                // App classifications table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS app_classifications (
                        packageName TEXT PRIMARY KEY NOT NULL,
                        appName TEXT NOT NULL,
                        classification TEXT NOT NULL DEFAULT 'UNCLASSIFIED',
                        updatedAt INTEGER NOT NULL
                    )
                """)

                // App usage logs table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS app_usage_logs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        activityId INTEGER DEFAULT NULL,
                        packageName TEXT NOT NULL,
                        appName TEXT NOT NULL,
                        durationSeconds INTEGER NOT NULL,
                        windowStartMillis INTEGER NOT NULL,
                        windowEndMillis INTEGER NOT NULL,
                        classification TEXT NOT NULL DEFAULT 'UNCLASSIFIED'
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_app_usage_logs_activityId ON app_usage_logs(activityId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_app_usage_logs_windowStartMillis ON app_usage_logs(windowStartMillis)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_app_usage_logs_packageName ON app_usage_logs(packageName)")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lifeflow_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .fallbackToDestructiveMigration()
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
