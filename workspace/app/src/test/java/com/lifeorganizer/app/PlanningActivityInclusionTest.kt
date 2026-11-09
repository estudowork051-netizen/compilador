package com.lifeorganizer.app

import com.lifeorganizer.app.core.utils.TimePreferenceAnalyzer
import com.lifeorganizer.app.domain.model.ActivityCategory
import com.lifeorganizer.app.domain.model.ActivityModel
import com.lifeorganizer.app.domain.model.ActivityType
import com.lifeorganizer.app.domain.model.ExecutionRecordModel
import com.lifeorganizer.app.domain.model.PredefinedActivity
import com.lifeorganizer.app.domain.model.ScheduleStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanningActivityInclusionTest {

    @Test
    fun userCreatedActivities_andPredefinedLibrary_areBothAvailable() {
        val userActivities = listOf(
            ActivityModel(
                id = 101L,
                title = "Meu Projeto Android",
                category = "Trabalho",
                type = ActivityType.FLEXIBLE,
                estimatedDurationMinutes = 75
            ),
            ActivityModel(
                id = 102L,
                title = "Aula de Violão",
                category = "Pessoal",
                type = ActivityType.FLEXIBLE,
                estimatedDurationMinutes = 45
            )
        )

        val predefinedList = PredefinedActivity.LIBRARY

        // Verify user activities exist and have real IDs
        assertEquals(2, userActivities.size)
        assertTrue(userActivities.all { it.id > 0L })

        // Verify library contains predefined quick suggestions
        assertTrue(predefinedList.isNotEmpty())
        assertTrue(predefinedList.any { it.title == "Caminhar" })
        assertTrue(predefinedList.any { it.title == "Estudar" })

        // Verify predefined conversion to transient ActivityModel
        val mappedPredefined = predefinedList.map { p ->
            ActivityModel(
                id = 0L,
                title = p.title,
                category = p.category,
                type = p.type,
                estimatedDurationMinutes = p.suggestedDurationMinutes
            )
        }

        assertEquals(predefinedList.size, mappedPredefined.size)
        assertTrue(mappedPredefined.all { it.id == 0L })
    }

    @Test
    fun predefinedSearch_returnsRelevantSuggestions() {
        val searchStudy = PredefinedActivity.search("estud")
        assertFalse(searchStudy.isEmpty())
        assertTrue(searchStudy.any { it.title.contains("Estud", ignoreCase = true) })

        val searchHouse = PredefinedActivity.search("Casa")
        assertFalse(searchHouse.isEmpty())
        assertTrue(searchHouse.all { it.category.equals("Casa", ignoreCase = true) || it.title.contains("Casa", ignoreCase = true) })
    }

    @Test
    fun customActivities_withExecutionHistory_providesIntelligentSuggestions() {
        val customActivity = ActivityModel(
            id = 501L,
            title = "Aprender Rust Avançado",
            category = "Estudos",
            type = ActivityType.FLEXIBLE,
            estimatedDurationMinutes = 50
        )

        val historyRecords = listOf(
            ExecutionRecordModel(
                id = 1L,
                scheduleEntryId = 1L,
                activityId = 501L,
                activityTitle = "Aprender Rust Avançado",
                category = ActivityCategory.ESTUDOS,
                plannedDate = "2026-11-04",
                plannedStartMinutes = 9 * 60,
                plannedDurationMinutes = 50,
                actualDurationMinutes = 50,
                finalStatus = ScheduleStatus.COMPLETED,
                timestamp = 1000L
            ),
            ExecutionRecordModel(
                id = 2L,
                scheduleEntryId = 2L,
                activityId = 501L,
                activityTitle = "Aprender Rust Avançado",
                category = ActivityCategory.ESTUDOS,
                plannedDate = "2026-11-05",
                plannedStartMinutes = 9 * 60,
                plannedDurationMinutes = 50,
                actualDurationMinutes = 45,
                finalStatus = ScheduleStatus.COMPLETED,
                timestamp = 2000L
            )
        )

        val bestSlot = TimePreferenceAnalyzer.findBestTimeSlotForActivity(
            activityId = 501L,
            records = historyRecords
        )

        assertNotNull(bestSlot)
        assertEquals(9 * 60, bestSlot!!.first)
    }
}
