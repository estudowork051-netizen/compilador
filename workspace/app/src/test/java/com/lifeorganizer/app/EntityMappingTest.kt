package com.lifeorganizer.app

import com.lifeorganizer.app.data.local.entity.ActivityEntity
import com.lifeorganizer.app.data.local.entity.toDomain
import com.lifeorganizer.app.data.local.entity.toEntity
import com.lifeorganizer.app.domain.model.ActivityModel
import com.lifeorganizer.app.domain.model.ActivityPriority
import com.lifeorganizer.app.domain.model.ActivityType
import com.lifeorganizer.app.domain.model.RecurrenceType
import org.junit.Assert.assertEquals
import org.junit.Test

class EntityMappingTest {

    @Test
    fun testDomainToEntityAndBack() {
        val domain = ActivityModel(
            id = 42,
            title = "Trabalho",
            category = "Trabalho",
            estimatedDurationMinutes = 360,
            type = ActivityType.FIXED,
            defaultCount = 1,
            priority = ActivityPriority.HIGH,
            colorHex = "#FFA726",
            startHour = 16,
            startMinute = 0,
            endHour = 22,
            endMinute = 0,
            recurrenceType = RecurrenceType.WEEKDAYS,
            recurrenceDays = listOf(1, 2, 3, 4, 5),
            weeklyTargetCount = null
        )

        val entity = domain.toEntity()
        assertEquals(42L, entity.id)
        assertEquals("Trabalho", entity.title)
        assertEquals("FIXED", entity.type)
        assertEquals("HIGH", entity.priority)
        assertEquals("1,2,3,4,5", entity.recurrenceDays)

        val reconstructed = entity.toDomain()
        assertEquals(domain.id, reconstructed.id)
        assertEquals(domain.title, reconstructed.title)
        assertEquals(domain.type, reconstructed.type)
        assertEquals(domain.priority, reconstructed.priority)
        assertEquals(domain.startHour, reconstructed.startHour)
        assertEquals(domain.recurrenceDays, reconstructed.recurrenceDays)
    }

    @Test
    fun testDefaultMigrationValues() {
        // Simulates an entity that was created in Room v1 and read in v2 with defaults
        val v1Entity = ActivityEntity(
            id = 1,
            title = "Estudar",
            category = "Estudos",
            estimatedDurationMinutes = 60
        )
        val domain = v1Entity.toDomain()
        assertEquals(ActivityType.FLEXIBLE, domain.type)
        assertEquals(ActivityPriority.MEDIUM, domain.priority)
        assertEquals(1, domain.defaultCount)
        assertEquals("#8B9CFF", domain.colorHex)
    }

    @Test
    fun testExecutionRecordDomainToEntityAndBack() {
        val record = com.lifeorganizer.app.domain.model.ExecutionRecordModel(
            id = 55L,
            scheduleEntryId = 12L,
            activityId = 7L,
            activityTitle = "Estudos de Rust",
            category = com.lifeorganizer.app.domain.model.ActivityCategory.ESTUDOS,
            plannedDate = "2026-11-06",
            plannedStartMinutes = 9 * 60,
            plannedDurationMinutes = 60,
            actualCompletedAt = 1730890000000L,
            actualDurationMinutes = 55,
            finalStatus = com.lifeorganizer.app.domain.model.ScheduleStatus.COMPLETED,
            timestamp = 1730890000000L,
            notes = "Foco total"
        )

        val entity = com.lifeorganizer.app.data.local.entity.ExecutionRecordEntity.fromDomain(record)
        assertEquals(55L, entity.id)
        assertEquals(12L, entity.scheduleEntryId)
        assertEquals(7L, entity.activityId)
        assertEquals("Estudos de Rust", entity.activityTitle)
        assertEquals("COMPLETED", entity.finalStatus)
        assertEquals(55, entity.actualDurationMinutes)

        val domainBack = entity.toDomain()
        assertEquals(record.id, domainBack.id)
        assertEquals(record.activityTitle, domainBack.activityTitle)
        assertEquals(record.category, domainBack.category)
        assertEquals(record.plannedDate, domainBack.plannedDate)
        assertEquals(record.actualDurationMinutes, domainBack.actualDurationMinutes)
        assertEquals(com.lifeorganizer.app.domain.model.ScheduleStatus.COMPLETED, domainBack.finalStatus)
    }
}
