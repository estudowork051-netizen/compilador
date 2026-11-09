package com.lifeorganizer.app.data.repository

import com.lifeorganizer.app.data.local.dao.ActivityContextDao
import com.lifeorganizer.app.data.local.entity.ActivityContextEntity
import com.lifeorganizer.app.domain.model.ActivityContextModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ContextRepository(
    private val activityContextDao: ActivityContextDao
) {

    fun getContextForActivity(activityId: Long): Flow<ActivityContextModel?> {
        return activityContextDao.getContextForActivity(activityId).map { it?.toDomain() }
    }

    suspend fun getContextForActivityOnce(activityId: Long): ActivityContextModel? {
        return activityContextDao.getContextForActivityOnce(activityId)?.toDomain()
    }

    fun getAllActiveContexts(): Flow<List<ActivityContextModel>> {
        return activityContextDao.getAllActiveContexts().map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun saveContext(context: ActivityContextModel): Long {
        return activityContextDao.insertOrUpdate(ActivityContextEntity.fromDomain(context))
    }

    suspend fun updateProgress(activityId: Long, progress: String, unitVal: Int?) {
        activityContextDao.updateProgress(activityId, progress, unitVal)
    }

    suspend fun advanceProgress(activityId: Long) {
        val existing = activityContextDao.getContextForActivityOnce(activityId)?.toDomain()
        if (existing != null) {
            val updated = existing.nextUnitProgress()
            activityContextDao.insertOrUpdate(ActivityContextEntity.fromDomain(updated))
        }
    }
}
