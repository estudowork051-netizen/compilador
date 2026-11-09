package com.lifeorganizer.app.data.repository

import com.lifeorganizer.app.core.utils.StringUtils
import com.lifeorganizer.app.data.local.dao.ActivityDao
import com.lifeorganizer.app.data.local.entity.toDomain
import com.lifeorganizer.app.data.local.entity.toEntity
import com.lifeorganizer.app.domain.model.ActivityModel
import com.lifeorganizer.app.domain.model.ActivityType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class ActivityRepository(
    private val activityDao: ActivityDao
) {

    val allActivities: Flow<List<ActivityModel>> = activityDao.getAllActivities()
        .map { list -> list.map { it.toDomain() } }

    fun getActivitiesByType(type: ActivityType): Flow<List<ActivityModel>> =
        activityDao.getActivitiesByType(type.name)
            .map { list -> list.map { it.toDomain() } }

    fun getActivitiesByCategory(category: String): Flow<List<ActivityModel>> =
        activityDao.getActivitiesByCategory(category)
            .map { list -> list.map { it.toDomain() } }

    fun searchActivities(query: String): Flow<List<ActivityModel>> =
        activityDao.getAllActivities().map { list ->
            if (query.isBlank()) {
                list.map { it.toDomain() }
            } else {
                list.map { it.toDomain() }.filter {
                    StringUtils.fuzzyContains(it.title, query) ||
                            StringUtils.fuzzyContains(it.category, query) ||
                            StringUtils.fuzzyContains(it.description, query)
                }
            }
        }

    fun getRecentActivities(limit: Int = 5): Flow<List<ActivityModel>> =
        activityDao.getRecentActivities(limit)
            .map { list -> list.map { it.toDomain() } }

    suspend fun getActivityById(id: Long): ActivityModel? =
        activityDao.getActivityById(id)?.toDomain()

    suspend fun saveActivity(activity: ActivityModel): Long {
        val entity = activity.toEntity()
        return if (activity.id > 0L) {
            val existing = activityDao.getActivityById(activity.id)
            if (existing != null) {
                activityDao.updateActivity(entity)
                activity.id
            } else {
                activityDao.insertActivity(entity.copy(id = 0L))
            }
        } else {
            activityDao.insertActivity(entity.copy(id = 0L))
        }
    }

    suspend fun updateActivity(activity: ActivityModel) {
        val entity = activity.toEntity()
        activityDao.updateActivity(entity)
    }

    suspend fun updateDuration(activityId: Long, newDurationMinutes: Int) {
        val existing = activityDao.getActivityById(activityId) ?: return
        val updated = existing.copy(
            estimatedDurationMinutes = newDurationMinutes,
            updatedAt = System.currentTimeMillis()
        )
        activityDao.updateActivity(updated)
    }

    suspend fun deleteActivity(activity: ActivityModel) {
        activityDao.deleteActivityById(activity.id)
    }

    suspend fun deleteActivityById(id: Long) {
        activityDao.deleteActivityById(id)
    }

    suspend fun duplicateActivity(activity: ActivityModel): Long {
        val duplicated = activity.copy(
            id = 0,
            title = "${activity.title} (Cópia)",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        return saveActivity(duplicated)
    }
}
