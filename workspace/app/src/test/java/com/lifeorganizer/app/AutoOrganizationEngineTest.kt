package com.lifeorganizer.app

import com.lifeorganizer.app.core.utils.AutoOrganizationEngine
import com.lifeorganizer.app.domain.model.ActivityCategory
import com.lifeorganizer.app.domain.model.ActivityContextModel
import com.lifeorganizer.app.domain.model.ActivityModel
import com.lifeorganizer.app.domain.model.ActivityPriority
import com.lifeorganizer.app.domain.model.ActivityType
import com.lifeorganizer.app.domain.model.BalancePreference
import com.lifeorganizer.app.domain.model.ContentType
import com.lifeorganizer.app.domain.model.ContinuityStatus
import com.lifeorganizer.app.domain.model.ScheduleEntryModel
import com.lifeorganizer.app.domain.model.ScheduleStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class AutoOrganizationEngineTest {

    private val targetDate = LocalDate.of(2026, 11, 10)

    @Test
    fun testProposal_respectsFixedCommitmentsAndNeverOverlaps() {
        // Fixed commitment: 09:00 to 12:00
        val fixedWork = ActivityModel(
            id = 1L,
            title = "Trabalho Presencial",
            category = "Trabalho",
            estimatedDurationMinutes = 180,
            type = ActivityType.FIXED,
            startHour = 9,
            startMinute = 0,
            endHour = 12,
            endMinute = 0
        )

        // Flexible tasks
        val task1 = ActivityModel(
            id = 2L,
            title = "Estudar Kotlin",
            category = "Estudos",
            estimatedDurationMinutes = 60,
            type = ActivityType.FLEXIBLE,
            priority = ActivityPriority.HIGH
        )
        val task2 = ActivityModel(
            id = 3L,
            title = "Cozinhar Almoço",
            category = "Casa",
            estimatedDurationMinutes = 45,
            type = ActivityType.FLEXIBLE,
            priority = ActivityPriority.MEDIUM
        )

        val proposal = AutoOrganizationEngine.generateProposal(
            targetDate = targetDate,
            wakeUpTimeStr = "08:00",
            sleepTimeStr = "22:00",
            fixedActivities = listOf(fixedWork),
            existingEntries = emptyList(),
            unallocatedActivities = listOf(task1, task2),
            contextMap = emptyMap(),
            balancePreference = BalancePreference.BALANCED
        )

        assertNotNull(proposal)
        assertTrue(proposal.items.isNotEmpty())

        // Check that no proposed item falls inside 09:00 - 12:00
        proposal.items.forEach { item ->
            val startMin = item.startHour * 60 + item.startMinute
            val endMin = startMin + item.durationMinutes

            val fixedStart = 9 * 60
            val fixedEnd = 12 * 60

            val overlaps = (startMin < fixedEnd && endMin > fixedStart)
            assertFalse("Proposed item ${item.title} ($startMin-$endMin) overlaps fixed commitment (540-720)", overlaps)
        }
    }

    @Test
    fun testProposal_prioritizesContinuityAndHighPriority() {
        val taskLow = ActivityModel(
            id = 10L,
            title = "Organizar Gaveta",
            category = "Casa",
            estimatedDurationMinutes = 30,
            type = ActivityType.FLEXIBLE,
            priority = ActivityPriority.LOW
        )

        val taskSeries = ActivityModel(
            id = 20L,
            title = "Série Dark",
            category = "Lazer",
            estimatedDurationMinutes = 45,
            type = ActivityType.FLEXIBLE,
            priority = ActivityPriority.MEDIUM
        )

        val contextDark = ActivityContextModel(
            activityId = 20L,
            contentType = ContentType.SERIES,
            contentName = "Dark",
            progressLabel = "Episódio 4",
            currentUnit = 4,
            continuityStatus = ContinuityStatus.IN_PROGRESS
        )

        val proposal = AutoOrganizationEngine.generateProposal(
            targetDate = targetDate,
            wakeUpTimeStr = "08:00",
            sleepTimeStr = "12:00", // Limited window: 4 hours
            fixedActivities = emptyList(),
            existingEntries = emptyList(),
            unallocatedActivities = listOf(taskLow, taskSeries),
            contextMap = mapOf(20L to contextDark),
            balancePreference = BalancePreference.BALANCED
        )

        // Dark has active continuity bonus and should be included
        val darkItem = proposal.items.find { it.activityId == 20L }
        assertNotNull("Active context activity must be scheduled", darkItem)
        assertTrue(darkItem!!.reason.contains("Dark") || darkItem.reason.contains("Continuidade"))
    }

    @Test
    fun testReorganizeRemainingDay_preservesCompletedAndFixed() {
        val now = LocalTime.of(14, 0)

        // Fixed past commitment: 10:00 - 12:00
        val pastFixed = ActivityModel(
            id = 1L,
            title = "Reunião de Equipe",
            category = "Trabalho",
            estimatedDurationMinutes = 120,
            type = ActivityType.FIXED,
            startHour = 10,
            startMinute = 0,
            endHour = 12,
            endMinute = 0
        )

        // Completed task at 08:30
        val completedTask = ScheduleEntryModel(
            id = 100L,
            activityId = 2L,
            activityTitle = "Caminhada Matinal",
            category = ActivityCategory.SAUDE,
            date = targetDate.toString(),
            startHour = 8,
            startMinute = 30,
            targetDurationMinutes = 45,
            status = ScheduleStatus.COMPLETED
        )

        // Uncompleted future candidate
        val pendingTask = ActivityModel(
            id = 3L,
            title = "Leitura Livro",
            category = "Lazer",
            estimatedDurationMinutes = 40,
            type = ActivityType.FLEXIBLE,
            priority = ActivityPriority.MEDIUM
        )

        val proposal = AutoOrganizationEngine.reorganizeRemainingDay(
            currentTime = now,
            targetDate = targetDate,
            wakeUpTimeStr = "08:00",
            sleepTimeStr = "23:00",
            fixedActivities = listOf(pastFixed),
            existingEntries = listOf(completedTask),
            unallocatedActivities = listOf(pendingTask),
            contextMap = emptyMap(),
            balancePreference = BalancePreference.BALANCED
        )

        assertNotNull(proposal)
        // All proposed items must be scheduled at or after current time (14:00)
        proposal.items.forEach { item ->
            val startParts = item.startTime.split(":")
            val itemHour = startParts[0].toInt()
            assertTrue("Reorganized items must start at or after current time (14:00)", itemHour >= 14)
        }
    }
}
