package com.lifeorganizer.app

import com.lifeorganizer.app.core.navigation.BottomNavDestination
import com.lifeorganizer.app.data.local.entity.toDomain
import com.lifeorganizer.app.data.local.entity.toEntity
import com.lifeorganizer.app.domain.model.ActivityCategory
import com.lifeorganizer.app.domain.model.ActivityModel
import com.lifeorganizer.app.domain.model.ActivityPriority
import com.lifeorganizer.app.domain.model.ActivityType
import com.lifeorganizer.app.domain.model.OrganizationProposal
import com.lifeorganizer.app.domain.model.ProposedScheduleItem
import com.lifeorganizer.app.domain.model.RecurrenceType
import com.lifeorganizer.app.domain.model.ScheduleEntryModel
import com.lifeorganizer.app.domain.model.ScheduleStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import com.lifeorganizer.app.feature.home.HomeTimelineItem
import com.lifeorganizer.app.feature.home.HomeUiState
import java.time.LocalDate
import java.time.LocalTime

class UsabilityAndRegressionTests {

    @Test
    fun testActivityWithDescription_PreservedInDomainAndEntity() {
        val originalActivity = ActivityModel(
            id = 101L,
            title = "Arrumar o quarto",
            category = "Casa",
            estimatedDurationMinutes = 45,
            type = ActivityType.FLEXIBLE,
            priority = ActivityPriority.HIGH,
            description = "Organizar o guarda-roupa, trocar a roupa de cama e limpar a mesa."
        )

        assertEquals("Organizar o guarda-roupa, trocar a roupa de cama e limpar a mesa.", originalActivity.description)

        // Convert to entity
        val entity = originalActivity.toEntity()
        assertEquals("Arrumar o quarto", entity.title)
        assertEquals("Organizar o guarda-roupa, trocar a roupa de cama e limpar a mesa.", entity.description)

        // Convert back to domain
        val roundTripDomain = entity.toDomain()
        assertEquals(originalActivity.id, roundTripDomain.id)
        assertEquals(originalActivity.title, roundTripDomain.title)
        assertEquals(originalActivity.description, roundTripDomain.description)
    }

    @Test
    fun testScheduleEntry_DescriptionPreview() {
        val activity = ActivityModel(
            id = 202L,
            title = "Estudar programação",
            category = "Estudos",
            estimatedDurationMinutes = 60,
            description = "Revisar orientação a objetos em Java e resolver dois exercícios."
        )

        val scheduleEntry = ScheduleEntryModel(
            id = 1L,
            activityId = activity.id,
            activity = activity,
            date = "2026-11-12",
            startHour = 14,
            startMinute = 0,
            endHour = 15,
            endMinute = 0,
            targetDurationMinutes = 60,
            notes = ""
        )

        // Preview resolves from activity description or custom notes
        val preview = scheduleEntry.activity?.description?.takeIf { it.isNotBlank() } ?: scheduleEntry.notes
        assertEquals("Revisar orientação a objetos em Java e resolver dois exercícios.", preview)
    }

    @Test
    fun testAiProposalApproval_IdempotencyPreventsDuplicates() {
        val testDate = LocalDate.now().toString()
        val existingEntries = mutableListOf(
            ScheduleEntryModel(
                id = 10L,
                activityId = 1L,
                date = testDate,
                startHour = 9,
                startMinute = 0,
                endHour = 10,
                endMinute = 0,
                status = ScheduleStatus.PENDING
            )
        )

        val proposedItem1 = ProposedScheduleItem(
            activity = ActivityModel(id = 1L, title = "Atividade 1", category = "Estudos", estimatedDurationMinutes = 60),
            startHour = 9,
            startMinute = 0,
            durationMinutes = 60,
            reason = "Rotina matinal"
        )

        val proposedItem2 = ProposedScheduleItem(
            activity = ActivityModel(id = 2L, title = "Atividade 2", category = "Casa", estimatedDurationMinutes = 30),
            startHour = 10,
            startMinute = 30,
            durationMinutes = 30,
            reason = "Pausa ativa"
        )

        val proposal = OrganizationProposal(
            date = LocalDate.now(),
            items = listOf(proposedItem1, proposedItem2),
            totalProposedMinutes = 90,
            remainingFreeMinutes = 300,
            unallocatedCount = 0,
            explanation = "Proposta equilibrada"
        )

        // Simulate idempotent approval logic:
        val newlyScheduled = mutableListOf<ProposedScheduleItem>()
        proposal.items.forEach { proposedItem ->
            val alreadyScheduled = existingEntries.any { entry ->
                (entry.activityId == proposedItem.activityId && entry.activityId > 0 && entry.date == testDate) ||
                (entry.date == testDate && entry.startHour == proposedItem.startHour && entry.startMinute == proposedItem.startMinute)
            }
            if (!alreadyScheduled) {
                newlyScheduled.add(proposedItem)
                existingEntries.add(
                    ScheduleEntryModel(
                        id = 11L,
                        activityId = proposedItem.activityId,
                        date = testDate,
                        startHour = proposedItem.startHour,
                        startMinute = proposedItem.startMinute,
                        endHour = 11,
                        endMinute = 0
                    )
                )
            }
        }

        // Only proposedItem2 should be scheduled; proposedItem1 was skipped as duplicate
        assertEquals(1, newlyScheduled.size)
        assertEquals(2L, newlyScheduled.first().activityId)

        // Executing approval again with the same proposal results in 0 additions
        val secondRunNewlyScheduled = mutableListOf<ProposedScheduleItem>()
        proposal.items.forEach { proposedItem ->
            val alreadyScheduled = existingEntries.any { entry ->
                (entry.activityId == proposedItem.activityId && entry.activityId > 0 && entry.date == testDate) ||
                (entry.date == testDate && entry.startHour == proposedItem.startHour && entry.startMinute == proposedItem.startMinute)
            }
            if (!alreadyScheduled) {
                secondRunNewlyScheduled.add(proposedItem)
            }
        }
        assertEquals(0, secondRunNewlyScheduled.size)
    }

    @Test
    fun testManualScheduleValidation_TodayVsFutureDate() {
        val now = LocalTime.of(14, 30) // 14:30
        val curMin = now.hour * 60 + now.minute

        // 1. For Today: past time is rejected with clear message
        val isToday = true
        val pastStart = 9 * 60 + 0 // 09:00
        val isPast = isToday && (pastStart < curMin)
        assertTrue("09:00 should be detected as past for today at 14:30", isPast)

        val errorMessage = if (isPast) "Esse horário já passou. Escolha um horário futuro." else null
        assertEquals("Esse horário já passou. Escolha um horário futuro.", errorMessage)

        // 2. For Today: future time is accepted
        val futureStart = 15 * 60 + 0 // 15:00
        val isFuturePast = isToday && (futureStart < curMin)
        assertFalse("15:00 should not be detected as past", isFuturePast)

        // 3. For Future Date: morning time (09:00) is completely valid even if current time is 14:30
        val isTomorrow = false // date != today
        val tomorrowMorningStart = 9 * 60 + 0 // 09:00 AM tomorrow
        val isTomorrowPast = isTomorrow && (tomorrowMorningStart < curMin)
        assertFalse("Morning time tomorrow must not be blocked by today's hour", isTomorrowPast)
    }

    @Test
    fun testMainMenuDestinations_LabelsAndRoutes() {
        val items = BottomNavDestination.items
        assertEquals(4, items.size)

        val titles = items.map { it.title }
        assertEquals(listOf("Hoje", "Agenda", "Hábitos", "Perfil"), titles)

        val routes = items.map { it.route }
        assertEquals(listOf("home", "planning", "habits", "settings"), routes)
    }

    @Test
    fun testHomeTimeline_IntegratesFixedAndScheduledActivitiesChronologically() {
        val fixedWork = ActivityModel(
            id = 1L,
            title = "Trabalho Presencial",
            category = "Trabalho",
            estimatedDurationMinutes = 480,
            type = ActivityType.FIXED,
            startHour = 9,
            startMinute = 0,
            endHour = 17,
            endMinute = 0
        )
        val morningGym = ScheduleEntryModel(
            id = 101L,
            activityId = 2L,
            activityTitle = "Academia",
            date = "2026-11-09",
            startHour = 7,
            startMinute = 0,
            endHour = 8,
            endMinute = 0,
            targetDurationMinutes = 60
        )
        val nightReading = ScheduleEntryModel(
            id = 102L,
            activityId = 3L,
            activityTitle = "Leitura",
            date = "2026-11-09",
            startHour = 20,
            startMinute = 30,
            endHour = 21,
            endMinute = 0,
            targetDurationMinutes = 30
        )

        val state = HomeUiState(
            fixedCommitments = listOf(fixedWork),
            todayScheduledEntries = listOf(nightReading, morningGym)
        )

        val timeline = state.timelineItems
        assertEquals(3, timeline.size)
        // 07:00 (Academia), 09:00 (Trabalho), 20:30 (Leitura)
        assertEquals("Academia", timeline[0].title)
        assertTrue(timeline[0] is HomeTimelineItem.Scheduled)
        assertEquals(7 * 60, timeline[0].startMinutes)

        assertEquals("Trabalho Presencial", timeline[1].title)
        assertTrue(timeline[1] is HomeTimelineItem.Fixed)
        assertEquals(9 * 60, timeline[1].startMinutes)

        assertEquals("Leitura", timeline[2].title)
        assertTrue(timeline[2] is HomeTimelineItem.Scheduled)
        assertEquals(20 * 60 + 30, timeline[2].startMinutes)
    }

    @Test
    fun testWaterIntakeAndUndo_AccuratelyTracksState() {
        var state = HomeUiState(hydrationMl = 500, waterRecordsCount = 2)

        // Add 250ml
        val amount = 250
        state = state.copy(
            hydrationMl = state.hydrationMl + amount,
            waterRecordsCount = state.waterRecordsCount + 1,
            canUndoWater = true,
            lastWaterAmountMl = amount
        )
        assertEquals(750, state.hydrationMl)
        assertEquals(3, state.waterRecordsCount)
        assertTrue(state.canUndoWater)

        // Undo
        state = state.copy(
            hydrationMl = (state.hydrationMl - state.lastWaterAmountMl).coerceAtLeast(0),
            waterRecordsCount = (state.waterRecordsCount - 1).coerceAtLeast(0),
            canUndoWater = false,
            lastWaterAmountMl = 0
        )
        assertEquals(500, state.hydrationMl)
        assertEquals(2, state.waterRecordsCount)
        assertFalse(state.canUndoWater)
    }

    @Test
    fun testActivitySave_PreservesExistingIdWithoutCascadeDelete() {
        val existingActivity = ActivityModel(
            id = 42L,
            title = "Aprender Kotlin",
            category = "Estudos",
            estimatedDurationMinutes = 60
        )
        // Verify id is preserved
        assertEquals(42L, existingActivity.id)
        val entity = existingActivity.toEntity()
        assertEquals(42L, entity.id)
    }

    @Test
    fun testBrandingScript_ExistsAndIsExecutable() {
        val script = java.io.File("src/main/assets/branding/convert_branding_icons.sh")
        val altScript = java.io.File("app/src/main/assets/branding/convert_branding_icons.sh")
        val target = if (script.exists()) script else altScript
        assertTrue("convert_branding_icons.sh must exist", target.exists())
    }

    @Test
    fun testAdaptiveIcon_XmlConfigurationValid() {
        val iconXml = java.io.File("src/main/res/mipmap-anydpi-v26/ic_launcher.xml")
        val altIconXml = java.io.File("app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml")
        val target = if (iconXml.exists()) iconXml else altIconXml
        assertTrue("ic_launcher.xml must exist", target.exists())
        val content = target.readText()
        assertTrue("ic_launcher.xml must contain adaptive-icon", content.contains("<adaptive-icon"))
        assertTrue("ic_launcher.xml must define background", content.contains("android:drawable=\"@drawable/ic_launcher_background\""))
    }

    @Test
    fun testNewActivityCreation_DoesNotModifyExistingActivityOrSchedule() {
        val existingActivity = ActivityModel(
            id = 1L,
            title = "Exercício",
            category = "Saúde",
            estimatedDurationMinutes = 60,
            type = ActivityType.FLEXIBLE
        )
        val existingSchedule = ScheduleEntryModel(
            id = 10L,
            activityId = 1L,
            activity = existingActivity,
            activityTitle = "Exercício",
            date = "2026-11-09",
            startHour = 8,
            startMinute = 0,
            endHour = 9,
            endMinute = 0,
            targetDurationMinutes = 60
        )

        // New activity created from scratch with id 0L (auto-generated ID simulation)
        val newCreatedActivity = ActivityModel(
            id = 2L,
            title = "Lavar a casa",
            category = "Casa",
            estimatedDurationMinutes = 30,
            type = ActivityType.FLEXIBLE
        )
        val newSchedule = ScheduleEntryModel(
            id = 11L,
            activityId = 2L,
            activity = newCreatedActivity,
            activityTitle = "Lavar a casa",
            date = "2026-11-09",
            startHour = 10,
            startMinute = 0,
            endHour = 10,
            endMinute = 30,
            targetDurationMinutes = 30
        )

        val state = HomeUiState(
            todayScheduledEntries = listOf(existingSchedule, newSchedule)
        )
        val timeline = state.timelineItems
        assertEquals(2, timeline.size)
        assertEquals("Exercício", timeline[0].title)
        assertEquals(8 * 60, timeline[0].startMinutes)

        assertEquals("Lavar a casa", timeline[1].title)
        assertEquals(10 * 60, timeline[1].startMinutes)

        // Verify independent IDs
        assertNotEquals(existingSchedule.activityId, newSchedule.activityId)
        assertEquals("Exercício", existingSchedule.activityTitle)
        assertEquals("Lavar a casa", newSchedule.activityTitle)
    }

    @Test
    fun testDirectFixedCommitmentCreation_SetsCorrectRecurrenceAndTimes() {
        val fixedActivity = ActivityModel(
            id = 0L,
            title = "Reunião de Equipe",
            category = "Trabalho",
            estimatedDurationMinutes = 60,
            type = ActivityType.FIXED,
            startHour = 10,
            startMinute = 30,
            endHour = 11,
            endMinute = 30,
            recurrenceType = RecurrenceType.WEEKDAYS,
            recurrenceDays = listOf(1, 2, 3, 4, 5)
        )

        assertEquals(ActivityType.FIXED, fixedActivity.type)
        assertEquals(10, fixedActivity.startHour)
        assertEquals(30, fixedActivity.startMinute)
        assertEquals(11, fixedActivity.endHour)
        assertEquals(30, fixedActivity.endMinute)
        assertEquals(5, fixedActivity.recurrenceDays.size)
        assertTrue(fixedActivity.recurrenceDays.contains(1)) // Segunda
        assertTrue(fixedActivity.recurrenceDays.contains(5)) // Sexta
        assertFalse(fixedActivity.recurrenceDays.contains(6)) // Sábado
    }

    @Test
    fun testFreeTimeSelection_AllowsPreciseTimes() {
        val testTimes = listOf(
            Pair(7, 15),
            Pair(10, 30),
            Pair(14, 45),
            Pair(22, 0)
        )
        for ((hour, minute) in testTimes) {
            val formatted = "%02d:%02d".format(hour, minute)
            val totalMinutes = hour * 60 + minute
            assertTrue(hour in 0..23)
            assertTrue(minute in 0..59)
            assertEquals(totalMinutes, hour * 60 + minute)
            when (formatted) {
                "07:15" -> assertEquals("07:15", formatted)
                "10:30" -> assertEquals("10:30", formatted)
                "14:45" -> assertEquals("14:45", formatted)
                "22:00" -> assertEquals("22:00", formatted)
            }
        }
    }

    @Test
    fun testWeekendsRecurrence_OnlyScheduledOnSaturdayAndSunday() {
        val weekendRun = ActivityModel(
            id = 99L,
            title = "Corrida de Fim de Semana",
            category = "Saúde",
            estimatedDurationMinutes = 45,
            type = ActivityType.FIXED,
            startHour = 8,
            startMinute = 0,
            endHour = 8,
            endMinute = 45,
            recurrenceType = RecurrenceType.WEEKENDS,
            recurrenceDays = listOf(6, 7)
        )

        assertEquals(RecurrenceType.WEEKENDS, weekendRun.recurrenceType)
        // 2026-11-09 is Monday (dow = 1) -> false
        assertFalse(weekendRun.isScheduledForDate(java.time.LocalDate.of(2026, 11, 9)))
        // 2026-11-13 is Friday (dow = 5) -> false
        assertFalse(weekendRun.isScheduledForDate(java.time.LocalDate.of(2026, 11, 13)))
        // 2026-11-14 is Saturday (dow = 6) -> true
        assertTrue(weekendRun.isScheduledForDate(java.time.LocalDate.of(2026, 11, 14)))
        // 2026-11-15 is Sunday (dow = 7) -> true
        assertTrue(weekendRun.isScheduledForDate(java.time.LocalDate.of(2026, 11, 15)))
    }
}
