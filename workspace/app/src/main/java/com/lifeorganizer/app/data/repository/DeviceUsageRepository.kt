package com.lifeorganizer.app.data.repository

import com.lifeorganizer.app.data.local.dao.AppClassificationDao
import com.lifeorganizer.app.data.local.dao.AppUsageLogDao
import com.lifeorganizer.app.data.local.entity.AppClassificationEntity
import com.lifeorganizer.app.data.local.entity.AppUsageLogEntity
import com.lifeorganizer.app.domain.model.AppCategoryClassification
import com.lifeorganizer.app.domain.model.AppClassificationModel
import com.lifeorganizer.app.domain.model.AppUsageStatModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DeviceUsageRepository(
    private val appClassificationDao: AppClassificationDao,
    private val appUsageLogDao: AppUsageLogDao
) {

    fun getAllClassificationsFlow(): Flow<List<AppClassificationModel>> {
        return appClassificationDao.getAllClassificationsFlow().map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun getClassificationForPackage(packageName: String): AppClassificationModel? {
        return appClassificationDao.getClassificationForPackage(packageName)?.toDomain()
    }

    suspend fun setClassification(packageName: String, appName: String, classification: AppCategoryClassification) {
        appClassificationDao.insertClassification(
            AppClassificationEntity(
                packageName = packageName,
                appName = appName,
                classification = classification.name,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun setClassifications(classifications: List<AppClassificationModel>) {
        appClassificationDao.insertClassifications(
            classifications.map { AppClassificationEntity.fromDomain(it) }
        )
    }

    fun getLogsSinceFlow(sinceMillis: Long): Flow<List<AppUsageStatModel>> {
        return appUsageLogDao.getLogsSinceFlow(sinceMillis).map { list ->
            list.map { it.toDomain() }
        }
    }

    fun getTotalMonitoredMinutesSinceFlow(sinceMillis: Long): Flow<Int> {
        return appUsageLogDao.getTotalMonitoredSecondsSinceFlow(sinceMillis).map { totalSecs ->
            (totalSecs / 60L).toInt()
        }
    }

    suspend fun recordUsageLogs(logs: List<AppUsageStatModel>, activityId: Long? = null) {
        val entities = logs.map { log ->
            AppUsageLogEntity(
                activityId = activityId,
                packageName = log.packageName,
                appName = log.appName,
                durationSeconds = log.totalTimeInForegroundSeconds,
                windowStartMillis = log.windowStartMillis,
                windowEndMillis = log.windowEndMillis,
                classification = log.classification.name
            )
        }
        appUsageLogDao.insertLogs(entities)
    }

    suspend fun clearAll() {
        appClassificationDao.clearAll()
        appUsageLogDao.clearAll()
    }
}
