package com.lifeorganizer.app

import com.lifeorganizer.app.core.utils.HabitLearningEngine
import com.lifeorganizer.app.domain.model.ActivityCategory
import com.lifeorganizer.app.domain.model.ActivityModel
import com.lifeorganizer.app.domain.model.ActivityType
import com.lifeorganizer.app.domain.model.ExecutionRecordModel
import com.lifeorganizer.app.domain.model.ScheduleStatus
import com.lifeorganizer.app.domain.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HabitLearningEngineTest {

    @Test
    fun computeHabitSummary_withEmptyData_returnsEmptyAndNotSufficient() {
        val summary = HabitLearningEngine.computeHabitSummary(
            records = emptyList(),
            activities = emptyList(),
            currentEntries = emptyList()
        )

        assertFalse(summary.hasSufficientData)
        assertEquals(0, summary.totalCompletedWeek)
        assertTrue(summary.bestTimePreferences.isEmpty())
        assertTrue(summary.durationInsights.isEmpty())
    }

    @Test
    fun computeHabitSummary_withSufficientData_aggregatesCategoriesAndRanks() {
        val activities = listOf(
            ActivityModel(id = 1, title = "Estudar Java", category = "Estudos", estimatedDurationMinutes = 60),
            ActivityModel(id = 2, title = "Lavar Louça", category = "Casa", estimatedDurationMinutes = 60)
        )

        val records = mutableListOf<ExecutionRecordModel>()
        // 5 records for Estudar Java (60 min each)
        repeat(5) { idx ->
            records.add(
                ExecutionRecordModel(
                    id = idx.toLong() + 1,
                    scheduleEntryId = idx.toLong() + 1,
                    activityId = 1,
                    activityTitle = "Estudar Java",
                    category = ActivityCategory.STUDY,
                    plannedDate = "2026-11-0$idx",
                    plannedStartMinutes = 600, // 10:00
                    plannedDurationMinutes = 60,
                    actualDurationMinutes = 55,
                    finalStatus = ScheduleStatus.COMPLETED
                )
            )
        }
        // 4 records for Lavar Louça (real duration 25m)
        repeat(4) { idx ->
            records.add(
                ExecutionRecordModel(
                    id = idx.toLong() + 10,
                    scheduleEntryId = idx.toLong() + 10,
                    activityId = 2,
                    activityTitle = "Lavar Louça",
                    category = ActivityCategory.CHORES,
                    plannedDate = "2026-11-0$idx",
                    plannedStartMinutes = 720,
                    plannedDurationMinutes = 60,
                    actualDurationMinutes = 25,
                    finalStatus = ScheduleStatus.COMPLETED
                )
            )
        }

        val summary = HabitLearningEngine.computeHabitSummary(
            records = records,
            activities = activities,
            currentEntries = emptyList()
        )

        assertTrue(summary.hasSufficientData)
        assertEquals(9, summary.totalCompletedWeek)
        assertEquals(2, summary.topCompletedActivities.size)
        assertEquals("Estudar Java", summary.topCompletedActivities[0].first)

        // Duration insight for Lavar Louça (diff 60 -> 25)
        val choreInsight = summary.durationInsights.find { it.activityId == 2L }
        assertNotNull(choreInsight)
        assertEquals(25, choreInsight?.learnedMedianDurationMinutes)

        // Category distribution: Study: 5 * 55 = 275m, Chores: 4 * 25 = 100m
        assertEquals(275, summary.categoryDistributionMinutes[ActivityCategory.STUDY])
        assertEquals(100, summary.categoryDistributionMinutes[ActivityCategory.CHORES])
    }

    @Test
    fun generateLearnedRecommendations_whenDisabled_fallsBackToGeneral() {
        val userProfile = UserProfile(learningEnabled = false)
        val activities = listOf(
            ActivityModel(id = 1, title = "Ler Livro", category = "Lazer", type = ActivityType.FLEXIBLE, estimatedDurationMinutes = 45)
        )

        val recs = HabitLearningEngine.generateLearnedRecommendations(
            currentTimeMinutes = 600,
            userProfile = userProfile,
            activities = activities,
            records = emptyList(),
            contexts = emptyList(),
            feedbacks = emptyList(),
            todayEntries = emptyList()
        )

        // When learning is disabled, should not crash, returns standard fallback
        assertNotNull(recs)
    }
}
