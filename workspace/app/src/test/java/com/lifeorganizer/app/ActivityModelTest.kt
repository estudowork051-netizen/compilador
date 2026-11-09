package com.lifeorganizer.app

import com.lifeorganizer.app.domain.model.ActivityModel
import com.lifeorganizer.app.domain.model.ActivityPriority
import com.lifeorganizer.app.domain.model.ActivityType
import com.lifeorganizer.app.domain.model.RecurrenceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivityModelTest {

    @Test
    fun testFlexibleActivityTotalDuration() {
        // 3 executions of 20 min = 60 min (1h)
        val activity = ActivityModel(
            title = "Lavar louça",
            category = "Casa",
            estimatedDurationMinutes = 20,
            defaultCount = 3,
            type = ActivityType.FLEXIBLE
        )
        assertEquals(60, activity.totalEstimatedMinutes)
        assertEquals("20min", activity.durationFormatted)
        assertEquals("1h", activity.totalDurationFormatted)
    }

    @Test
    fun testFixedCommitmentSameDay() {
        // Work: 16:00 to 22:00 -> 6 hours = 360 minutes
        val activity = ActivityModel(
            title = "Trabalho",
            category = "Trabalho",
            estimatedDurationMinutes = 360,
            type = ActivityType.FIXED,
            startHour = 16,
            startMinute = 0,
            endHour = 22,
            endMinute = 0,
            recurrenceType = RecurrenceType.WEEKDAYS
        )
        assertFalse(activity.isCrossingMidnight)
        assertEquals(360, activity.fixedDurationMinutes)
        assertEquals("16:00 às 22:00", activity.fixedTimeRangeFormatted)
    }

    @Test
    fun testFixedCommitmentCrossingMidnight() {
        // Night shift: 22:00 to 06:00 next morning -> 8 hours = 480 minutes
        val activity = ActivityModel(
            title = "Plantão Noturno",
            category = "Trabalho",
            estimatedDurationMinutes = 480,
            type = ActivityType.FIXED,
            startHour = 22,
            startMinute = 0,
            endHour = 6,
            endMinute = 0,
            recurrenceType = RecurrenceType.SPECIFIC_DAYS,
            recurrenceDays = listOf(1, 3, 5)
        )
        assertTrue(activity.isCrossingMidnight)
        assertEquals(480, activity.fixedDurationMinutes)
        assertTrue(activity.fixedTimeRangeFormatted.contains("+1 dia"))
        assertEquals("22:00 às 06:00 (+1 dia)", activity.fixedTimeRangeFormatted)
    }

    @Test
    fun testWeeklyGoalConfiguration() {
        // Workout: 5 times per week, 90 minutes each
        val activity = ActivityModel(
            title = "Treinar",
            category = "Saúde",
            estimatedDurationMinutes = 90,
            type = ActivityType.HABIT,
            priority = ActivityPriority.HIGH,
            weeklyTargetCount = 5
        )
        assertEquals(5, activity.weeklyTargetCount)
        assertEquals(ActivityType.HABIT, activity.type)
        assertEquals("1h 30min", activity.durationFormatted)
    }
}
