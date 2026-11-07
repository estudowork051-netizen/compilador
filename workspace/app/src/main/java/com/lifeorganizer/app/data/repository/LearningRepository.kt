package com.lifeorganizer.app.data.repository

import com.lifeorganizer.app.data.local.dao.ExecutionRecordDao
import com.lifeorganizer.app.data.local.dao.RecommendationFeedbackDao
import com.lifeorganizer.app.data.local.entity.RecommendationFeedbackEntity
import com.lifeorganizer.app.data.local.entity.ExecutionRecordEntity
import com.lifeorganizer.app.domain.model.ExecutionRecordModel
import com.lifeorganizer.app.domain.model.FeedbackAction
import com.lifeorganizer.app.domain.model.PredefinedActivity
import com.lifeorganizer.app.domain.model.RecommendationType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LearningRepository(
    private val recommendationFeedbackDao: RecommendationFeedbackDao,
    private val executionRecordDao: ExecutionRecordDao,
    private val activityRepository: ActivityRepository
) {

    val allFeedback: Flow<List<RecommendationFeedbackEntity>> =
        recommendationFeedbackDao.getAllFeedback()

    val allExecutionRecords: Flow<List<ExecutionRecordModel>> =
        executionRecordDao.getAllRecords().map { list -> list.map { it.toDomain() } }

    suspend fun recordExecution(record: ExecutionRecordModel): Long {
        val entity = ExecutionRecordEntity.fromDomain(record)
        return executionRecordDao.insertRecord(entity)
    }

    fun getRecordsForActivity(activityId: Long): Flow<List<ExecutionRecordModel>> =
        executionRecordDao.getRecordsForActivity(activityId).map { list -> list.map { it.toDomain() } }

    suspend fun getRecordsForActivityList(activityId: Long): List<ExecutionRecordModel> =
        executionRecordDao.getRecordsForActivityList(activityId).map { it.toDomain() }

    suspend fun getAllExecutionRecordsList(): List<ExecutionRecordModel> =
        executionRecordDao.getAllRecordsList().map { it.toDomain() }

    suspend fun recordFeedback(
        type: RecommendationType,
        activityId: Long?,
        activityTitle: String,
        action: FeedbackAction,
        reason: String = ""
    ): Long {
        val entity = RecommendationFeedbackEntity(
            recommendationType = type.name,
            activityId = activityId,
            activityTitle = activityTitle,
            action = action.name,
            reason = reason,
            timestamp = System.currentTimeMillis()
        )
        return recommendationFeedbackDao.insertFeedback(entity)
    }

    suspend fun getRecentFeedback(limit: Int = 50): List<RecommendationFeedbackEntity> {
        return recommendationFeedbackDao.getRecentFeedback(limit)
    }

    suspend fun getNegativeFeedbackCount(activityId: Long): Int {
        return recommendationFeedbackDao.countNegativeFeedback(activityId)
    }

    /**
     * Clears all behavioral execution history and recommendation feedback.
     * Preserves registered activities and schedule entries intact (Section 14).
     */
    suspend fun clearLearningData() {
        executionRecordDao.clearAllRecords()
        recommendationFeedbackDao.clearAllFeedback()
    }

    /**
     * Applies a learned duration to an activity.
     */
    suspend fun applyLearnedDuration(activityId: Long, newDurationMinutes: Int) {
        activityRepository.updateDuration(activityId, newDurationMinutes)
    }

    /**
     * Restores default suggested durations based on PredefinedActivity catalogue.
     */
    suspend fun restoreDefaultSuggestedDurations(activities: List<com.lifeorganizer.app.domain.model.ActivityModel>) {
        val predefinedMap: Map<String, PredefinedActivity> = PredefinedActivity.getAll().associateBy { it.title.lowercase() }
        for (activity in activities) {
            val defaultPreset = predefinedMap[activity.title.lowercase()]
            if (defaultPreset != null && activity.estimatedDurationMinutes != defaultPreset.suggestedDurationMinutes) {
                activityRepository.updateDuration(activity.id, defaultPreset.suggestedDurationMinutes)
            }
        }
    }
}
