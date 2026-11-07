package com.lifeorganizer.app.data.repository

import com.lifeorganizer.app.data.local.dao.FocusSessionDao
import com.lifeorganizer.app.data.local.entity.FocusSessionEntity
import com.lifeorganizer.app.domain.model.FocusSessionModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FocusRepository(
    private val focusSessionDao: FocusSessionDao
) {

    fun getActiveSessionFlow(): Flow<FocusSessionModel?> {
        return focusSessionDao.getActiveSessionFlow().map { it?.toDomain() }
    }

    suspend fun getActiveSessionOnce(): FocusSessionModel? {
        return focusSessionDao.getActiveSessionOnce()?.toDomain()
    }

    fun getAllSessionsFlow(): Flow<List<FocusSessionModel>> {
        return focusSessionDao.getAllSessionsFlow().map { list ->
            list.map { it.toDomain() }
        }
    }

    fun getCompletedSessionsFlow(): Flow<List<FocusSessionModel>> {
        return focusSessionDao.getCompletedSessionsFlow().map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun startSession(session: FocusSessionModel): Long {
        val entity = FocusSessionEntity.fromDomain(session.copy(isActive = true))
        return focusSessionDao.insertSession(entity)
    }

    suspend fun updateSession(session: FocusSessionModel) {
        val entity = FocusSessionEntity.fromDomain(session)
        focusSessionDao.updateSession(entity)
    }

    suspend fun deactivateSession(sessionId: Long) {
        focusSessionDao.deactivateSession(sessionId)
    }

    suspend fun clearAll() {
        focusSessionDao.clearAll()
    }
}
