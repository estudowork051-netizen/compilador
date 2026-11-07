package com.lifeorganizer.app.core.utils

import com.lifeorganizer.app.data.local.entity.RecommendationFeedbackEntity
import com.lifeorganizer.app.domain.model.FeedbackAction
import com.lifeorganizer.app.domain.model.RecommendationModel
import com.lifeorganizer.app.domain.model.UserProfile

object RecommendationRanker {

    const val PENALTY_DISMISSED = 20
    const val PENALTY_NOT_INTERESTED = 50
    const val BOOST_ACCEPTED = 25

    /**
     * Filters and ranks recommendations based on feedback history and explicit user preferences.
     */
    fun rankRecommendations(
        recommendations: List<RecommendationModel>,
        feedbacks: List<RecommendationFeedbackEntity>,
        userProfile: UserProfile,
        currentTimeMinutes: Int
    ): List<RecommendationModel> {
        if (!userProfile.personalizedRecommendationsEnabled) {
            // Only keep generic suggestions
            return recommendations.filter { it.isGeneric }
        }

        val feedbackByActivity = feedbacks.groupBy { it.activityId }

        val scored = recommendations.mapNotNull { rec ->
            // 1. Explicit preferences check: block disallowed categories at night
            val isNightTime = currentTimeMinutes >= 22 * 60 || currentTimeMinutes < 6 * 60
            if (isNightTime && rec.activity != null && userProfile.disallowedNightCategories.contains(rec.activity.category)) {
                return@mapNotNull null
            }

            // 2. Feedback adjustment
            val activityFeedbacks = if (rec.activity != null) feedbackByActivity[rec.activity.id] ?: emptyList() else emptyList()
            val notInterestedCount = activityFeedbacks.count { it.action == FeedbackAction.NOT_INTERESTED.name }
            val dismissedCount = activityFeedbacks.count { it.action == FeedbackAction.DISMISSED.name }
            val acceptedCount = activityFeedbacks.count { it.action == FeedbackAction.ACCEPTED.name }

            // If user explicitly stated "Not interested" multiple times, suppress it
            if (notInterestedCount >= 2) {
                return@mapNotNull null
            }

            var adjustedScore = rec.priorityScore
            adjustedScore -= (dismissedCount * PENALTY_DISMISSED)
            adjustedScore -= (notInterestedCount * PENALTY_NOT_INTERESTED)
            adjustedScore += (acceptedCount * BOOST_ACCEPTED)

            rec.copy(priorityScore = adjustedScore)
        }

        return scored.sortedByDescending { it.priorityScore }
    }
}
