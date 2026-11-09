package com.lifeorganizer.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.lifeorganizer.app.domain.model.FeedbackAction
import com.lifeorganizer.app.domain.model.RecommendationType

@Entity(
    tableName = "recommendation_feedbacks",
    indices = [
        Index(value = ["activityId"]),
        Index(value = ["recommendationType"]),
        Index(value = ["action"]),
        Index(value = ["timestamp"])
    ]
)
data class RecommendationFeedbackEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val recommendationType: String,
    val activityId: Long? = null,
    val activityTitle: String = "",
    val action: String, // ACCEPTED, DISMISSED, POSTPONED, NOT_INTERESTED
    val reason: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
