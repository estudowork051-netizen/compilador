package com.lifeorganizer.app

import com.lifeorganizer.app.core.utils.AdaptiveReminderEngine
import com.lifeorganizer.app.core.utils.ReminderType
import com.lifeorganizer.app.domain.model.ActivityCategory
import com.lifeorganizer.app.domain.model.ExecutionRecordModel
import com.lifeorganizer.app.domain.model.NotificationPreferences
import com.lifeorganizer.app.domain.model.ScheduleEntryModel
import com.lifeorganizer.app.domain.model.ScheduleStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AdaptiveReminderEngineTest {

    @Test
    fun testQuietHoursCalculation() {
        // Daytime quiet hours: 14:00 to 16:00
        assertTrue(AdaptiveReminderEngine.isInQuietHours(15, 0, true, "14:00", "16:00"))
        assertFalse(AdaptiveReminderEngine.isInQuietHours(17, 0, true, "14:00", "16:00"))

        // Overnight quiet hours: 22:00 to 07:00
        assertTrue(AdaptiveReminderEngine.isInQuietHours(23, 30, true, "22:00", "07:00"))
        assertTrue(AdaptiveReminderEngine.isInQuietHours(6, 45, true, "22:00", "07:00"))
        assertFalse(AdaptiveReminderEngine.isInQuietHours(10, 0, true, "22:00", "07:00"))

        // Disabled quiet hours always return false
        assertFalse(AdaptiveReminderEngine.isInQuietHours(23, 30, false, "22:00", "07:00"))
    }

    @Test
    fun testComputeRemindersPreStartAndCompletionPrompt() {
        val entry = ScheduleEntryModel(
            id = 101L,
            activityId = 1L,
            activityTitle = "Estudar Algoritmos",
            category = ActivityCategory.STUDIES,
            date = "2026-11-06",
            startHour = 10,
            startMinute = 0,
            endHour = 11,
            endMinute = 0,
            targetDurationMinutes = 60,
            status = ScheduleStatus.PENDING
        )

        val prefs = NotificationPreferences(
            remindersEnabled = true,
            notifyBeforeStart = true,
            notifyBeforeMinutes = 15,
            promptCompletionOnEnd = true,
            quietHoursEnabled = true,
            quietHoursStart = "22:00",
            quietHoursEnd = "07:00"
        )

        val reminders = AdaptiveReminderEngine.computeRemindersForDay(
            entries = listOf(entry),
            preferences = prefs
        )

        assertEquals(2, reminders.size)

        // Pre-start trigger: 10:00 (600m) - 15m = 585m (09:45)
        val preStart = reminders[0]
        assertEquals(ReminderType.PRE_START, preStart.reminderType)
        assertEquals(585, preStart.triggerTimeMinutes)
        assertTrue(preStart.message.contains("15 minutos"))

        // Completion prompt trigger: 11:00 (660m)
        val completion = reminders[1]
        assertEquals(ReminderType.COMPLETION_PROMPT, completion.reminderType)
        assertEquals(660, completion.triggerTimeMinutes)
    }

    @Test
    fun testAdaptiveHintForRepeatedlyPostponedTask() {
        val entry = ScheduleEntryModel(
            id = 202L,
            activityId = 2L,
            activityTitle = "Limpeza do Quarto",
            category = ActivityCategory.HOME,
            date = "2026-11-06",
            startHour = 14,
            startMinute = 0,
            endHour = 15,
            endMinute = 0,
            targetDurationMinutes = 60,
            status = ScheduleStatus.POSTPONED
        )

        val executionHistory = listOf(
            ExecutionRecordModel(id = 1L, scheduleEntryId = 1L, activityId = 2L, activityTitle = "Limpeza do Quarto", category = ActivityCategory.CASA, plannedDate = "2026-11-04", plannedStartMinutes = 800, plannedDurationMinutes = 60, finalStatus = ScheduleStatus.POSTPONED, timestamp = 1L),
            ExecutionRecordModel(id = 2L, scheduleEntryId = 2L, activityId = 2L, activityTitle = "Limpeza do Quarto", category = ActivityCategory.CASA, plannedDate = "2026-11-05", plannedStartMinutes = 800, plannedDurationMinutes = 60, finalStatus = ScheduleStatus.POSTPONED, timestamp = 2L)
        )

        val prefs = NotificationPreferences(
            remindersEnabled = true,
            notifyBeforeStart = false,
            promptCompletionOnEnd = false,
            reorganizeSuggestionsEnabled = true
        )

        val reminders = AdaptiveReminderEngine.computeRemindersForDay(
            entries = listOf(entry),
            preferences = prefs,
            executionHistory = executionHistory,
            nowHour = 14,
            nowMinute = 10
        )

        assertEquals(1, reminders.size)
        val hint = reminders[0]
        assertEquals(ReminderType.POSTPONED_ADAPTIVE_CHECK, hint.reminderType)
        assertTrue(hint.isAdaptiveSuggestion)
        assertTrue(hint.message.contains("foi adiada algumas vezes"))
    }
}
