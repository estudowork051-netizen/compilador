package com.lifeorganizer.app

import com.lifeorganizer.app.core.utils.RecommendationEngine
import com.lifeorganizer.app.domain.model.ActivityCategory
import com.lifeorganizer.app.domain.model.ActivityContextModel
import com.lifeorganizer.app.domain.model.ActivityModel
import com.lifeorganizer.app.domain.model.ActivityPriority
import com.lifeorganizer.app.domain.model.ActivityType
import com.lifeorganizer.app.domain.model.BalancePreference
import com.lifeorganizer.app.domain.model.ContentType
import com.lifeorganizer.app.domain.model.ContinuityStatus
import com.lifeorganizer.app.domain.model.DayAvailability
import com.lifeorganizer.app.domain.model.RecommendationType
import com.lifeorganizer.app.domain.model.ScheduleEntryModel
import com.lifeorganizer.app.domain.model.ScheduleStatus
import com.lifeorganizer.app.domain.model.TimeSlot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class RecommendationEngineTest {

    @Test
    fun testContinuityRecommendation_suggestsActiveSeries() {
        val seriesTask = ActivityModel(
            id = 10L,
            title = "Assistir Severance",
            category = "Lazer",
            estimatedDurationMinutes = 50,
            type = ActivityType.FLEXIBLE
        )
        val context = ActivityContextModel(
            activityId = 10L,
            contentType = ContentType.SERIES,
            contentName = "Severance",
            progressLabel = "Episódio 7",
            currentUnit = 7,
            continuityStatus = ContinuityStatus.IN_PROGRESS
        )

        val availability = DayAvailability(
            date = "2026-11-10",
            wakeUpTime = "08:00",
            sleepTime = "23:00",
            awakeDurationMinutes = 900,
            fixedCommitmentsMinutes = 0,
            availableTimeMinutes = 900,
            plannedActivitiesMinutes = 0,
            remainingFreeMinutes = 900,
            freeSlots = listOf(
                TimeSlot("15:00", "17:00", 120)
            )
        )

        val rec = RecommendationEngine.generateCurrentRecommendation(
            currentTime = LocalTime.of(15, 10),
            availability = availability,
            activities = listOf(seriesTask),
            scheduledEntries = emptyList(),
            contexts = listOf(context),
            balancePreference = BalancePreference.BALANCED
        )

        assertNotNull(rec)
        assertEquals(RecommendationType.RESUME_CONTENT, rec!!.type)
        assertTrue(rec.title.contains("Severance") || rec.subtitle.contains("Episódio 8") || rec.subtitle.contains("Severance"))
    }

    @Test
    fun testRestRecommendation_suggestedAfterIntenseWork() {
        val entry1 = ScheduleEntryModel(
            id = 1L,
            activityId = 100L,
            activityTitle = "Trabalho Deep Work 1",
            category = ActivityCategory.TRABALHO,
            date = "2026-11-10",
            startHour = 9,
            startMinute = 0,
            targetDurationMinutes = 120,
            status = ScheduleStatus.COMPLETED
        )
        val entry2 = ScheduleEntryModel(
            id = 2L,
            activityId = 101L,
            activityTitle = "Trabalho Deep Work 2",
            category = ActivityCategory.TRABALHO,
            date = "2026-11-10",
            startHour = 11,
            startMinute = 0,
            targetDurationMinutes = 90,
            status = ScheduleStatus.COMPLETED
        )

        val availability = DayAvailability(
            date = "2026-11-10",
            wakeUpTime = "08:00",
            sleepTime = "23:00",
            awakeDurationMinutes = 900,
            fixedCommitmentsMinutes = 0,
            availableTimeMinutes = 690,
            plannedActivitiesMinutes = 210,
            remainingFreeMinutes = 690,
            freeSlots = listOf(
                TimeSlot("12:30", "14:00", 90)
            )
        )

        val rec = RecommendationEngine.generateCurrentRecommendation(
            currentTime = LocalTime.of(12, 35),
            availability = availability,
            activities = emptyList(),
            scheduledEntries = listOf(entry1, entry2),
            contexts = emptyList(),
            balancePreference = BalancePreference.BALANCED
        )

        assertNotNull(rec)
        assertEquals(RecommendationType.REST_BREAK, rec!!.type)
        assertTrue(rec.title.contains("pausa") || rec.title.contains("Pausa"))
    }

    @Test
    fun testFillGapRecommendation_matchesSlotDuration() {
        val quickTask = ActivityModel(
            id = 5L,
            title = "Alongamento Rápido",
            category = "Saúde",
            estimatedDurationMinutes = 20,
            type = ActivityType.FLEXIBLE
        )
        val longTask = ActivityModel(
            id = 6L,
            title = "Limpar a casa inteira",
            category = "Casa",
            estimatedDurationMinutes = 120,
            type = ActivityType.FLEXIBLE
        )

        // Slot of 30 minutes right now
        val availability = DayAvailability(
            date = "2026-11-10",
            wakeUpTime = "08:00",
            sleepTime = "23:00",
            awakeDurationMinutes = 900,
            fixedCommitmentsMinutes = 0,
            availableTimeMinutes = 870,
            plannedActivitiesMinutes = 30,
            remainingFreeMinutes = 870,
            freeSlots = listOf(
                TimeSlot("10:00", "10:30", 30)
            )
        )

        val rec = RecommendationEngine.generateCurrentRecommendation(
            currentTime = LocalTime.of(10, 5),
            availability = availability,
            activities = listOf(quickTask, longTask),
            scheduledEntries = emptyList<ScheduleEntryModel>(),
            contexts = emptyList<ActivityContextModel>(),
            balancePreference = BalancePreference.NO_PREFERENCE
        )

        assertNotNull(rec)
        // Should recommend the 20min task, not the 120min task
        assertEquals("Alongamento Rápido", rec!!.suggestedActivity?.title)
    }
}
