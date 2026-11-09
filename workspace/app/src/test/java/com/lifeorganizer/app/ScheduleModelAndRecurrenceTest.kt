package com.lifeorganizer.app

import com.lifeorganizer.app.domain.model.ActivityCategory
import com.lifeorganizer.app.domain.model.ActivityModel
import com.lifeorganizer.app.domain.model.ActivityType
import com.lifeorganizer.app.domain.model.ExecutionRecordModel
import com.lifeorganizer.app.domain.model.RecurrenceType
import com.lifeorganizer.app.domain.model.ScheduleEntryModel
import com.lifeorganizer.app.domain.model.ScheduleStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ScheduleModelAndRecurrenceTest {

    @Test
    fun testRecurrenceRules() {
        // Monday = 2026-11-02, Saturday = 2026-11-07, Sunday = 2026-11-08
        val monday = LocalDate.of(2026, 11, 2)
        val saturday = LocalDate.of(2026, 11, 7)

        val weekdayCommitment = ActivityModel(
            id = 1,
            title = "Trabalho",
            category = "Trabalho",
            estimatedDurationMinutes = 480,
            type = ActivityType.FIXED,
            recurrenceType = RecurrenceType.WEEKDAYS
        )

        assertTrue(weekdayCommitment.isScheduledForDate(monday))
        assertFalse(weekdayCommitment.isScheduledForDate(saturday))

        val specificDaysCommitment = ActivityModel(
            id = 2,
            title = "Natação",
            category = "Saúde",
            estimatedDurationMinutes = 60,
            type = ActivityType.FIXED,
            recurrenceType = RecurrenceType.SPECIFIC_DAYS,
            recurrenceDays = listOf(6) // Saturday only
        )

        assertFalse(specificDaysCommitment.isScheduledForDate(monday))
        assertTrue(specificDaysCommitment.isScheduledForDate(saturday))

        val weekendCommitment = ActivityModel(
            id = 3,
            title = "Corrida",
            category = "Saúde",
            estimatedDurationMinutes = 60,
            type = ActivityType.FIXED,
            recurrenceType = RecurrenceType.WEEKENDS
        )

        assertFalse(weekendCommitment.isScheduledForDate(monday))
        assertTrue(weekendCommitment.isScheduledForDate(saturday))
        assertTrue(weekendCommitment.isScheduledForDate(LocalDate.of(2026, 11, 8))) // Sunday
    }

    @Test
    fun testScheduleStatusProperties() {
        assertTrue(ScheduleStatus.COMPLETED.isFinished)
        assertTrue(ScheduleStatus.NOT_DONE.isFinished)
        assertTrue(ScheduleStatus.CANCELLED.isFinished)
        assertFalse(ScheduleStatus.PENDING.isFinished)
        assertFalse(ScheduleStatus.IN_PROGRESS.isFinished)
        assertFalse(ScheduleStatus.POSTPONED.isFinished)
    }

    @Test
    fun testScheduleEntryDurationAndRange() {
        val entry = ScheduleEntryModel(
            id = 10,
            activityId = 5,
            activityTitle = "Leitura",
            date = "2026-11-05",
            startHour = 14,
            startMinute = 15,
            endHour = 15,
            endMinute = 0,
            targetDurationMinutes = 45
        )

        assertEquals(45, entry.computedDurationMinutes)
        assertEquals("45min", entry.formattedDuration)
        assertEquals("14:15 às 15:00", entry.formattedTimeRange)
    }

    @Test
    fun testExecutionRecordModelCreation() {
        val record = ExecutionRecordModel(
            id = 1,
            scheduleEntryId = 10,
            activityId = 5,
            activityTitle = "Estudos de Programação",
            category = ActivityCategory.ESTUDOS,
            plannedDate = "2026-11-05",
            plannedStartMinutes = 9 * 60,
            plannedDurationMinutes = 90,
            actualCompletedAt = System.currentTimeMillis(),
            actualDurationMinutes = 80,
            finalStatus = ScheduleStatus.COMPLETED,
            notes = "Foco total"
        )

        assertEquals("09:00", record.plannedTimeFormatted)
        assertEquals("1h 30min", record.plannedDurationFormatted)
        assertEquals("1h 20min", record.actualDurationFormatted)
        assertEquals(ScheduleStatus.COMPLETED, record.finalStatus)
    }
}
