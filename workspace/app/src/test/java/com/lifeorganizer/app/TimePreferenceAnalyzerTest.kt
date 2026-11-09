package com.lifeorganizer.app

import com.lifeorganizer.app.core.utils.TimePreferenceAnalyzer
import com.lifeorganizer.app.domain.model.ActivityCategory
import com.lifeorganizer.app.domain.model.ConfidenceLevel
import com.lifeorganizer.app.domain.model.ExecutionRecordModel
import com.lifeorganizer.app.domain.model.ScheduleStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimePreferenceAnalyzerTest {

    @Test
    fun scenarioA_insufficientHistory_doesNotClaimTrend() {
        // User attempted study only twice at 10h (600 min)
        val records = listOf(
            ExecutionRecordModel(
                id = 1,
                scheduleEntryId = 1,
                activityId = 10,
                activityTitle = "Estudar Kotlin",
                category = ActivityCategory.STUDY,
                plannedDate = "2026-11-01",
                plannedStartMinutes = 600, // 10:00
                plannedDurationMinutes = 60,
                actualDurationMinutes = 60,
                finalStatus = ScheduleStatus.COMPLETED
            ),
            ExecutionRecordModel(
                id = 2,
                scheduleEntryId = 2,
                activityId = 10,
                activityTitle = "Estudar Kotlin",
                category = ActivityCategory.STUDY,
                plannedDate = "2026-11-02",
                plannedStartMinutes = 600, // 10:00
                plannedDurationMinutes = 60,
                actualDurationMinutes = 60,
                finalStatus = ScheduleStatus.COMPLETED
            )
        )

        val slots = TimePreferenceAnalyzer.analyzeActivityTimeSlots(10, "Estudar Kotlin", records)
        assertEquals(1, slots.size)
        assertEquals(ConfidenceLevel.INSUFFICIENT_DATA, slots[0].confidence)
        assertEquals(2, slots[0].totalAttempts)

        // Best slot should be null because confidence is insufficient (<3 records)
        val bestSlot = TimePreferenceAnalyzer.findBestTimeSlot(10, "Estudar Kotlin", records)
        assertNull(bestSlot)
    }

    @Test
    fun scenarioB_sufficientHistory_identifiesBestTimeSlotWithRate() {
        val records = mutableListOf<ExecutionRecordModel>()

        // 08h (480 min): 2 completed, 6 not done (rate 25%)
        repeat(2) {
            records.add(createRecord(10, 480, ScheduleStatus.COMPLETED))
        }
        repeat(6) {
            records.add(createRecord(10, 480, ScheduleStatus.NOT_DONE))
        }

        // 10h (600 min): 7 completed, 1 postponed (rate 87.5%)
        repeat(7) {
            records.add(createRecord(10, 600, ScheduleStatus.COMPLETED))
        }
        records.add(createRecord(10, 600, ScheduleStatus.POSTPONED))

        // 14h (840 min): 5 completed, 2 not done (rate 71%)
        repeat(5) {
            records.add(createRecord(10, 840, ScheduleStatus.COMPLETED))
        }
        repeat(2) {
            records.add(createRecord(10, 840, ScheduleStatus.NOT_DONE))
        }

        val bestSlot = TimePreferenceAnalyzer.findBestTimeSlot(10, "Estudar", records)
        assertNotNull(bestSlot)
        assertEquals("Manhã (09h-12h)", bestSlot?.timeSlotName)
        assertEquals(9, bestSlot?.startHour)
        assertEquals(12, bestSlot?.endHour)
        assertEquals(8, bestSlot?.totalAttempts)
        assertEquals(7, bestSlot?.completedCount)
        assertTrue(bestSlot!!.completionRate > 0.85f)
        assertEquals(ConfidenceLevel.MODERATE, bestSlot.confidence)
    }

    private fun createRecord(activityId: Long, startMinutes: Int, status: ScheduleStatus): ExecutionRecordModel {
        return ExecutionRecordModel(
            id = System.nanoTime(),
            scheduleEntryId = 1,
            activityId = activityId,
            activityTitle = "Estudar",
            category = ActivityCategory.STUDY,
            plannedDate = "2026-11-01",
            plannedStartMinutes = startMinutes,
            plannedDurationMinutes = 60,
            actualDurationMinutes = 60,
            finalStatus = status
        )
    }
}
