package com.lifeorganizer.app

import com.lifeorganizer.app.core.utils.TimeAvailabilityEngine
import com.lifeorganizer.app.domain.model.ActivityModel
import com.lifeorganizer.app.domain.model.ActivityType
import com.lifeorganizer.app.domain.model.RecurrenceType
import com.lifeorganizer.app.domain.model.ScheduleEntryModel
import com.lifeorganizer.app.domain.model.ScheduleStatus
import com.lifeorganizer.app.domain.model.TimeSlot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimeAvailabilityEngineTest {

    @Test
    fun testDayWithZeroActivitiesIsCompletelyAvailable() {
        val calc = TimeAvailabilityEngine.calculateDayAvailability(
            date = "2026-11-05",
            wakeUpTimeStr = "08:00",
            sleepTimeStr = "00:00",
            fixedActivities = emptyList(),
            scheduledEntries = emptyList()
        )

        assertEquals(960, calc.awakeDurationMinutes) // 16 hours
        assertEquals(0, calc.fixedCommitmentsMinutes)
        assertEquals(960, calc.availableTimeMinutes)
        assertEquals(0, calc.plannedActivitiesMinutes)
        assertEquals(960, calc.remainingFreeMinutes)
        assertFalse(calc.isOverbooked)
        assertEquals(1, calc.freeSlots.size)
        assertEquals(480, calc.freeSlots[0].startMinutes)
        assertEquals(1440, calc.freeSlots[0].endMinutes)
    }

    @Test
    fun testSubtractionOfFixedCommitmentsAndPlannedActivities() {
        // Wake 08:00, Sleep 00:00 (16h = 960 min)
        // Fixed: Work from 16:00 to 22:00 (6h = 360 min)
        val work = ActivityModel(
            id = 1,
            title = "Trabalho",
            category = "Trabalho",
            estimatedDurationMinutes = 360,
            type = ActivityType.FIXED,
            startHour = 16,
            startMinute = 0,
            endHour = 22,
            endMinute = 0,
            recurrenceType = RecurrenceType.DAILY
        )

        // Planned: Study 09:00 to 10:30 (90 min), Gym 11:00 to 12:30 (90 min) = 180 min (3h)
        val study = ScheduleEntryModel(
            id = 1,
            activityId = 2,
            activityTitle = "Estudar",
            date = "2026-11-05",
            startHour = 9,
            startMinute = 0,
            endHour = 10,
            endMinute = 30,
            targetDurationMinutes = 90
        )
        val gym = ScheduleEntryModel(
            id = 2,
            activityId = 3,
            activityTitle = "Academia",
            date = "2026-11-05",
            startHour = 11,
            startMinute = 0,
            endHour = 12,
            endMinute = 30,
            targetDurationMinutes = 90
        )

        val calc = TimeAvailabilityEngine.calculateDayAvailability(
            date = "2026-11-05",
            wakeUpTimeStr = "08:00",
            sleepTimeStr = "00:00",
            fixedActivities = listOf(work),
            scheduledEntries = listOf(study, gym)
        )

        assertEquals(960, calc.awakeDurationMinutes) // 16h
        assertEquals(360, calc.fixedCommitmentsMinutes) // 6h
        assertEquals(600, calc.availableTimeMinutes) // 10h available without fixed
        assertEquals(180, calc.plannedActivitiesMinutes) // 3h planned
        assertEquals(420, calc.remainingFreeMinutes) // 7h remaining
        assertFalse(calc.isOverbooked)
    }

    @Test
    fun testIntervalOverlapUnionDoesNotDoubleCount() {
        // Two overlapping slots: [14:00 to 16:00] (120m) and [15:00 to 17:00] (120m)
        // Union should be [14:00 to 17:00] = 180m, NOT 240m!
        val slotA = TimeSlot(14 * 60, 16 * 60, "A")
        val slotB = TimeSlot(15 * 60, 17 * 60, "B")

        val merged = TimeAvailabilityEngine.mergeIntervals(listOf(slotA, slotB))
        assertEquals(1, merged.size)
        assertEquals(14 * 60, merged[0].startMinutes)
        assertEquals(17 * 60, merged[0].endMinutes)
        assertEquals(180, TimeAvailabilityEngine.totalDurationOf(merged))
    }

    @Test
    fun testMidnightCrossingAwakePeriod() {
        // Wake 08:00, Sleep 01:00 (next day) = 17 hours = 1020 minutes
        val awake = TimeAvailabilityEngine.getAwakeWindow("08:00", "01:00")
        assertEquals(480, awake.startMinutes)
        assertEquals(1500, awake.endMinutes)
        assertEquals(1020, awake.durationMinutes)
    }

    @Test
    fun testMidnightCrossingFixedCommitment() {
        // Fixed commitment: 22:00 to 06:00 (8h = 480 min)
        val nightShift = ActivityModel(
            id = 10,
            title = "Plantão Noturno",
            category = "Trabalho",
            estimatedDurationMinutes = 480,
            type = ActivityType.FIXED,
            startHour = 22,
            startMinute = 0,
            endHour = 6,
            endMinute = 0,
            recurrenceType = RecurrenceType.DAILY
        )

        assertTrue(nightShift.isCrossingMidnight)
        assertEquals(480, nightShift.fixedDurationMinutes)
        assertTrue(nightShift.fixedTimeRangeFormatted.contains("(+1 dia)"))
    }

    @Test
    fun testOverbookedDayDetection() {
        // Wake 08:00, Sleep 14:00 = 6 hours (360 min) available
        // User plans 7 hours (420 min) of activities
        val task = ScheduleEntryModel(
            id = 1,
            activityId = 5,
            activityTitle = "Maratona de Estudos",
            date = "2026-11-05",
            startHour = 8,
            startMinute = 0,
            endHour = 15,
            endMinute = 0,
            targetDurationMinutes = 420
        )

        val calc = TimeAvailabilityEngine.calculateDayAvailability(
            date = "2026-11-05",
            wakeUpTimeStr = "08:00",
            sleepTimeStr = "14:00",
            fixedActivities = emptyList(),
            scheduledEntries = listOf(task)
        )

        assertTrue(calc.isOverbooked)
        assertEquals(60, calc.overbookedMinutes) // 420 - 360 = 60 min excess
        assertEquals(0, calc.remainingFreeMinutes)
    }

    @Test
    fun testFreeSlotsDiscoveryAndFirstAvailableSlot() {
        // Awake 08:00 (480) to 12:00 (720)
        // Occupied: 09:00 (540) to 10:00 (600)
        // Free gaps should be: 08:00..09:00 (60m) and 10:00..12:00 (120m)
        val awake = TimeSlot(480, 720, "Vigília")
        val occupied = listOf(TimeSlot(540, 600, "Reunião"))

        val freeSlots = TimeAvailabilityEngine.findFreeSlots(awake, occupied, minSlotDurationMinutes = 15)
        assertEquals(2, freeSlots.size)
        assertEquals(480, freeSlots[0].startMinutes)
        assertEquals(540, freeSlots[0].endMinutes)
        assertEquals(600, freeSlots[1].startMinutes)
        assertEquals(720, freeSlots[1].endMinutes)

        // Find first slot for 90-minute task: should pick 10:00 to 11:30
        val suggested = TimeAvailabilityEngine.findFirstAvailableSlot(awake, occupied, durationMinutes = 90)
        assertNotNull(suggested)
        assertEquals(600, suggested!!.startMinutes)
        assertEquals(690, suggested.endMinutes)
    }
}
