package com.lifeorganizer.app

import com.lifeorganizer.app.core.utils.WellbeingRecommendationEngine
import com.lifeorganizer.app.domain.model.ActivityCategory
import com.lifeorganizer.app.domain.model.AppCategoryClassification
import com.lifeorganizer.app.domain.model.AppUsageStatModel
import com.lifeorganizer.app.domain.model.FocusSessionModel
import com.lifeorganizer.app.domain.model.ScheduleEntryModel
import com.lifeorganizer.app.domain.model.ScheduleStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WellbeingRecommendationEngineTest {

    @Test
    fun testDayBalanceCalculation() {
        val entries = listOf(
            ScheduleEntryModel(id = 1L, activityId = 1L, activityTitle = "Estudo Java", category = ActivityCategory.STUDIES, date = "2026-11-06", targetDurationMinutes = 90, status = ScheduleStatus.COMPLETED),
            ScheduleEntryModel(id = 2L, activityId = 2L, activityTitle = "Reunião de Trabalho", category = ActivityCategory.WORK, date = "2026-11-06", targetDurationMinutes = 60, status = ScheduleStatus.COMPLETED),
            ScheduleEntryModel(id = 3L, activityId = 3L, activityTitle = "Série Netflix", category = ActivityCategory.LEISURE, date = "2026-11-06", targetDurationMinutes = 45, status = ScheduleStatus.PENDING),
            ScheduleEntryModel(id = 4L, activityId = 4L, activityTitle = "Cuidar das Plantas", category = ActivityCategory.HOME, date = "2026-11-06", targetDurationMinutes = 30, status = ScheduleStatus.POSTPONED),
            ScheduleEntryModel(id = 5L, activityId = 5L, activityTitle = "Soneca da Tarde", category = ActivityCategory.REST, date = "2026-11-06", targetDurationMinutes = 30, status = ScheduleStatus.COMPLETED)
        )

        val focusSessions = listOf(
            FocusSessionModel(id = 1L, activityId = 1L, activityTitle = "Estudo Java", category = ActivityCategory.STUDIES, plannedDurationMinutes = 25, startTimeMillis = 0L, elapsedSecondsBeforePause = 25 * 60L, completedAtMillis = 25 * 60 * 1000L)
        )

        val usageLogs = listOf(
            AppUsageStatModel(packageName = "com.instagram.android", appName = "Instagram", totalTimeInForegroundSeconds = 1200L, classification = AppCategoryClassification.DISTRACTION, windowStartMillis = 0L, windowEndMillis = 1000L),
            AppUsageStatModel(packageName = "notion.id", appName = "Notion", totalTimeInForegroundSeconds = 1800L, classification = AppCategoryClassification.WORK, windowStartMillis = 0L, windowEndMillis = 1000L)
        )

        val balance = WellbeingRecommendationEngine.computeDayBalance(
            entries = entries,
            focusSessions = focusSessions,
            usageLogs = usageLogs
        )

        assertEquals(90, balance.studiesMinutes)
        assertEquals(60, balance.workMinutes)
        assertEquals(45, balance.leisureMinutes)
        assertEquals(30, balance.homeMinutes)
        assertEquals(30, balance.restMinutes)
        assertEquals(25, balance.totalFocusMinutes)
        assertEquals(3, balance.completedTasksCount)
        assertEquals(1, balance.postponedTasksCount)
        assertEquals(1, balance.pendingTasksCount)
        assertEquals(20, balance.distractionUsageMinutes) // 1200s / 60
    }

    @Test
    fun testOverloadInsight() {
        val entries = listOf(
            ScheduleEntryModel(id = 1L, activityId = 10L, activityTitle = "Trabalho 1", category = ActivityCategory.WORK, date = "2026-11-06", targetDurationMinutes = 300, status = ScheduleStatus.PENDING),
            ScheduleEntryModel(id = 2L, activityId = 11L, activityTitle = "Estudos 2", category = ActivityCategory.STUDIES, date = "2026-11-06", targetDurationMinutes = 200, status = ScheduleStatus.PENDING)
        ) // 500 productive minutes > 480 (8h)

        val balance = WellbeingRecommendationEngine.computeDayBalance(entries)
        val insights = WellbeingRecommendationEngine.generateInsights(balance)

        assertTrue(insights.any { it.isWarning && it.title.contains("Ritmo Intenso") })
    }

    @Test
    fun testRestReminderInsightWhenMissing() {
        val entries = listOf(
            ScheduleEntryModel(id = 1L, activityId = 20L, activityTitle = "Trabalho 1", category = ActivityCategory.WORK, date = "2026-11-06", targetDurationMinutes = 180, status = ScheduleStatus.PENDING)
        )

        val balance = WellbeingRecommendationEngine.computeDayBalance(entries)
        val insights = WellbeingRecommendationEngine.generateInsights(balance)

        assertTrue(insights.any { it.title.contains("descanso") })
    }
}
