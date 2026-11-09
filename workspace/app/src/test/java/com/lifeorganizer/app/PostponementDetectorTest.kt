package com.lifeorganizer.app

import com.lifeorganizer.app.core.utils.PostponementDetector
import com.lifeorganizer.app.domain.model.ActivityCategory
import com.lifeorganizer.app.domain.model.ExecutionRecordModel
import com.lifeorganizer.app.domain.model.ScheduleStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PostponementDetectorTest {

    @Test
    fun detectsFrequentlyPostponedTasksWithGentleMessage() {
        val records = mutableListOf<ExecutionRecordModel>()
        // 5 times scheduled: 4 postponed, 1 completed (80% postponement rate)
        repeat(4) { idx ->
            records.add(
                ExecutionRecordModel(
                    id = idx.toLong() + 1,
                    scheduleEntryId = idx.toLong() + 1,
                    activityId = 7,
                    activityTitle = "Estudar Cálculo",
                    category = ActivityCategory.STUDY,
                    plannedDate = "2026-11-0$idx",
                    plannedStartMinutes = 480, // 08:00
                    plannedDurationMinutes = 60,
                    finalStatus = ScheduleStatus.POSTPONED
                )
            )
        }
        records.add(
            ExecutionRecordModel(
                id = 5,
                scheduleEntryId = 5,
                activityId = 7,
                activityTitle = "Estudar Cálculo",
                category = ActivityCategory.STUDY,
                plannedDate = "2026-11-05",
                plannedStartMinutes = 480,
                plannedDurationMinutes = 60,
                finalStatus = ScheduleStatus.COMPLETED
            )
        )

        val insights = PostponementDetector.detectPostponedActivities(records)
        assertEquals(1, insights.size)
        assertEquals(7L, insights[0].activityId)
        assertEquals(4, insights[0].postponedCount)
        assertEquals(5, insights[0].totalScheduledCount)
        assertTrue(insights[0].postponementRate >= 0.8f)
        assertTrue(insights[0].suggestionMessage.contains("Estudar Cálculo"))
        // Message is respectful, not blaming
        assertTrue(insights[0].suggestionMessage.contains("Que tal tentar um horário ou duração diferente?"))
    }

    @Test
    fun singlePostponement_isNotTreatedAsPattern() {
        val records = listOf(
            ExecutionRecordModel(
                id = 1,
                scheduleEntryId = 1,
                activityId = 7,
                activityTitle = "Treino",
                category = ActivityCategory.HEALTH,
                plannedDate = "2026-11-01",
                plannedStartMinutes = 540,
                plannedDurationMinutes = 45,
                finalStatus = ScheduleStatus.POSTPONED
            ),
            ExecutionRecordModel(
                id = 2,
                scheduleEntryId = 2,
                activityId = 7,
                activityTitle = "Treino",
                category = ActivityCategory.HEALTH,
                plannedDate = "2026-11-02",
                plannedStartMinutes = 540,
                plannedDurationMinutes = 45,
                finalStatus = ScheduleStatus.COMPLETED
            )
        )

        val insights = PostponementDetector.detectPostponedActivities(records)
        assertTrue(insights.isEmpty())
    }
}
