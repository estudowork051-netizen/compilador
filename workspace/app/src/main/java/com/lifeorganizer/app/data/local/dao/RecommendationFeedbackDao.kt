package com.lifeorganizer.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lifeorganizer.app.data.local.entity.RecommendationFeedbackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecommendationFeedbackDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedback(feedback: RecommendationFeedbackEntity): Long

    @Query("SELECT * FROM recommendation_feedbacks ORDER BY timestamp DESC")
    fun getAllFeedback(): Flow<List<RecommendationFeedbackEntity>>

    @Query("SELECT * FROM recommendation_feedbacks ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentFeedback(limit: Int = 50): List<RecommendationFeedbackEntity>

    @Query("SELECT * FROM recommendation_feedbacks WHERE activityId = :activityId ORDER BY timestamp DESC")
    suspend fun getFeedbackForActivity(activityId: Long): List<RecommendationFeedbackEntity>

    @Query("SELECT COUNT(*) FROM recommendation_feedbacks WHERE activityId = :activityId AND action IN ('DISMISSED', 'NOT_INTERESTED')")
    suspend fun countNegativeFeedback(activityId: Long): Int

    @Query("DELETE FROM recommendation_feedbacks")
    suspend fun clearAllFeedback()
}
