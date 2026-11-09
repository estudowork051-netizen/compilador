package com.lifeorganizer.app

import android.app.Application
import com.lifeorganizer.app.core.analytics.UsageMonitoringService
import com.lifeorganizer.app.core.focus.FocusSessionManager
import com.lifeorganizer.app.data.local.database.AppDatabase
import com.lifeorganizer.app.data.preferences.dataStore
import com.lifeorganizer.app.data.repository.ActivityRepository
import com.lifeorganizer.app.data.repository.ContextRepository
import com.lifeorganizer.app.data.repository.DeviceUsageRepository
import com.lifeorganizer.app.data.repository.FocusRepository
import com.lifeorganizer.app.data.repository.LearningRepository
import com.lifeorganizer.app.data.repository.ScheduleRepository
import com.lifeorganizer.app.data.repository.UserPreferencesRepository

class LifeFlowApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var preferencesRepository: UserPreferencesRepository
        private set

    lateinit var activityRepository: ActivityRepository
        private set

    lateinit var scheduleRepository: ScheduleRepository
        private set

    lateinit var contextRepository: ContextRepository
        private set

    lateinit var learningRepository: LearningRepository
        private set

    lateinit var focusRepository: FocusRepository
        private set

    lateinit var focusSessionManager: FocusSessionManager
        private set

    lateinit var deviceUsageRepository: DeviceUsageRepository
        private set

    lateinit var usageMonitoringService: UsageMonitoringService
        private set

    override fun onCreate() {
        super.onCreate()
        
        // Initialize central diagnostic manager first to capture everything from process start
        com.lifeorganizer.app.core.diagnostic.LifeFlowDiagnosticManager.initialize(this)
        com.lifeorganizer.app.core.diagnostic.LifeFlowDiagnosticManager.logEvent("APPLICATION_ON_CREATE_STARTED")

        instance = this
        try {
            com.lifeorganizer.app.core.diagnostic.LifeFlowDiagnosticManager.logEvent("ROOM_INITIALIZATION_STARTED")
            database = AppDatabase.getInstance(this)
            com.lifeorganizer.app.core.diagnostic.LifeFlowDiagnosticManager.logEvent("ROOM_INITIALIZATION_SUCCESS")

            com.lifeorganizer.app.core.diagnostic.LifeFlowDiagnosticManager.logEvent("DATASTORE_INITIALIZATION_STARTED")
            preferencesRepository = UserPreferencesRepository(dataStore)
            com.lifeorganizer.app.core.diagnostic.LifeFlowDiagnosticManager.logEvent("DATASTORE_INITIALIZATION_SUCCESS")

            activityRepository = ActivityRepository(database.activityDao())
            scheduleRepository = ScheduleRepository(
                database.scheduleEntryDao(),
                database.executionRecordDao(),
                database.activityDao()
            )
            contextRepository = ContextRepository(
                database.activityContextDao()
            )
            learningRepository = LearningRepository(
                database.recommendationFeedbackDao(),
                database.executionRecordDao(),
                activityRepository
            )
            focusRepository = FocusRepository(database.focusSessionDao())
            focusSessionManager = FocusSessionManager(focusRepository, scheduleRepository)
            deviceUsageRepository = DeviceUsageRepository(
                database.appClassificationDao(),
                database.appUsageLogDao()
            )
            usageMonitoringService = UsageMonitoringService(this, deviceUsageRepository)
            com.lifeorganizer.app.core.diagnostic.LifeFlowDiagnosticManager.logEvent("APPLICATION_ON_CREATE_COMPLETED")
        } catch (e: Exception) {
            com.lifeorganizer.app.core.diagnostic.LifeFlowDiagnosticManager.logError("APPLICATION_ON_CREATE_FAILED", e)
            throw e
        }
    }

    companion object {
        lateinit var instance: LifeFlowApplication
            private set
    }
}
