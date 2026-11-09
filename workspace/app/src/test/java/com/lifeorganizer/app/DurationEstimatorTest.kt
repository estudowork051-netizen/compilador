package com.lifeorganizer.app

import com.lifeorganizer.app.core.utils.DurationEstimator
import com.lifeorganizer.app.domain.model.ActivityCategory
import com.lifeorganizer.app.domain.model.ConfidenceLevel
import com.lifeorganizer.app.domain.model.ExecutionRecordModel
import com.lifeorganizer.app.domain.model.ScheduleStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DurationEstimatorTest {

    @Test
    fun scenarioC_learnedDuration_suggestsAdjustmentFromPlanned() {
        // Activity originally planned for 60 min, real executions: 25, 30, 20, 25, 30 min
        val records = listOf(25, 30, 20, 25, 30).mapIndexed { index, duration ->
            ExecutionRecordModel(
                id = index.toLong() + 1,
                scheduleEntryId = index.toLong() + 1,
                activityId = 5,
                activityTitle = "Lavar louça",
                category = ActivityCategory.CHORES,
                plannedDate = "2026-11-0$index",
                plannedStartMinutes = 720,
                plannedDurationMinutes = 60,
                actualDurationMinutes = duration,
                finalStatus = ScheduleStatus.COMPLETED
            )
        }

        val insight = DurationEstimator.estimateActivityDuration(
            activityId = 5,
            activityTitle = "Lavar louça",
            plannedDurationMinutes = 60,
            records = records
        )

        assertNotNull(insight)
        assertEquals(25, insight?.learnedMedianDurationMinutes)
        assertEquals(60, insight?.plannedDurationMinutes)
        assertEquals(-35, insight?.differenceMinutes)
        assertEquals(5, insight?.sampleCount)
        assertTrue(insight!!.suggestionMessage.contains("25 minutos"))
    }

    @Test
    fun insufficientSamples_returnsNull() {
        val records = listOf(
            ExecutionRecordModel(
                id = 1,
                scheduleEntryId = 1,
                activityId = 5,
                activityTitle = "Lavar louça",
                category = ActivityCategory.CHORES,
                plannedDate = "2026-11-01",
                plannedStartMinutes = 720,
                plannedDurationMinutes = 60,
                actualDurationMinutes = 25,
                finalStatus = ScheduleStatus.COMPLETED
            )
        )

        val insight = DurationEstimator.estimateActivityDuration(
            activityId = 5,
            activityTitle = "Lavar louça",
            plannedDurationMinutes = 60,
            records = records
        )

        assertNull(insight)
    }

    @Test
    fun outlierFiltering_discardsExtremeValues() {
        // Normal 20, 25, 25, 30 plus an extreme outlier of 300 minutes (e.g. phone forgot to pause)
        val records = listOf(20, 25, 25, 30, 300).mapIndexed { index, duration ->
            ExecutionRecordModel(
                id = index.toLong() + 1,
                scheduleEntryId = index.toLong() + 1,
                activityId = 5,
                activityTitle = "Lavar louça",
                category = ActivityCategory.CHORES,
                plannedDate = "2026-11-0$index",
                plannedStartMinutes = 720,
                plannedDurationMinutes = 60,
                actualDurationMinutes = duration,
                finalStatus = ScheduleStatus.COMPLETED
            )
        }

        val insight = DurationEstimator.estimateActivityDuration(
            activityId = 5,
            activityTitle = "Lavar louça",
            plannedDurationMinutes = 60,
            records = records
        )

        assertNotNull(insight)
        assertEquals(25, insight?.learnedMedianDurationMinutes)
    }
}
