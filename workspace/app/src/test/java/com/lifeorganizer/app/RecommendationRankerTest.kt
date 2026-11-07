package com.lifeorganizer.app

import com.lifeorganizer.app.core.utils.RecommendationRanker
import com.lifeorganizer.app.data.local.entity.RecommendationFeedbackEntity
import com.lifeorganizer.app.domain.model.ActivityCategory
import com.lifeorganizer.app.domain.model.ActivityModel
import com.lifeorganizer.app.domain.model.ActivityType
import com.lifeorganizer.app.domain.model.FeedbackAction
import com.lifeorganizer.app.domain.model.RecommendationModel
import com.lifeorganizer.app.domain.model.RecommendationType
import com.lifeorganizer.app.domain.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommendationRankerTest {

    @Test
    fun scenarioE_explicitPreference_blocksDisallowedNightCategory() {
        val userProfile = UserProfile(
            disallowedNightCategories = setOf("Estudos")
        )
        val studyActivity = ActivityModel(
            id = 12,
            title = "Banco de Dados",
            category = "Estudos",
            type = ActivityType.FLEXIBLE,
            estimatedDurationMinutes = 60
        )
        val rec = RecommendationModel(
            id = "rec_study",
            type = RecommendationType.BEST_TIME_SLOT,
            title = "Estudar Banco de Dados",
            message = "Que tal estudar agora?",
            activity = studyActivity,
            suggestedStartHour = 22,
            suggestedStartMinute = 30,
            priorityScore = 100
        )

        // At 22:30 (1350 minutes)
        val ranked = RecommendationRanker.rankRecommendations(
            recommendations = listOf(rec),
            feedbacks = emptyList(),
            userProfile = userProfile,
            currentTimeMinutes = 1350
        )

        // Must be filtered out because Studies are disallowed at night!
        assertTrue(ranked.isEmpty())
    }

    @Test
    fun repeatedNotInterested_suppressesRecommendation() {
        val userProfile = UserProfile(personalizedRecommendationsEnabled = true)
        val activity = ActivityModel(id = 8, title = "Yoga", category = "Saúde", estimatedDurationMinutes = 30)
        val rec = RecommendationModel(
            id = "rec_yoga",
            type = RecommendationType.FREE_TIME_FIT,
            title = "Praticar Yoga",
            message = "Tempo livre para Yoga",
            activity = activity,
            priorityScore = 80
        )

        val feedbacks = listOf(
            RecommendationFeedbackEntity(
                recommendationType = "FREE_TIME_FIT",
                activityId = 8,
                action = FeedbackAction.NOT_INTERESTED.name
            ),
            RecommendationFeedbackEntity(
                recommendationType = "FREE_TIME_FIT",
                activityId = 8,
                action = FeedbackAction.NOT_INTERESTED.name
            )
        )

        val ranked = RecommendationRanker.rankRecommendations(
            recommendations = listOf(rec),
            feedbacks = feedbacks,
            userProfile = userProfile,
            currentTimeMinutes = 600
        )

        assertTrue(ranked.isEmpty())
    }

    @Test
    fun acceptedFeedback_boostsPriorityScore() {
        val userProfile = UserProfile(personalizedRecommendationsEnabled = true)
        val activity = ActivityModel(id = 2, title = "Leitura Matinal", category = "Estudos", estimatedDurationMinutes = 30)
        val rec = RecommendationModel(
            id = "rec_reading",
            type = RecommendationType.CONTINUITY,
            title = "Continuar Livro",
            message = "Capítulo 5",
            activity = activity,
            priorityScore = 100
        )

        val feedbacks = listOf(
            RecommendationFeedbackEntity(
                recommendationType = "CONTINUITY",
                activityId = 2,
                action = FeedbackAction.ACCEPTED.name
            )
        )

        val ranked = RecommendationRanker.rankRecommendations(
            recommendations = listOf(rec),
            feedbacks = feedbacks,
            userProfile = userProfile,
            currentTimeMinutes = 540
        )

        assertEquals(1, ranked.size)
        assertEquals(125, ranked[0].priorityScore) // 100 + 25
    }
}
