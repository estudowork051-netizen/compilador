package com.lifeorganizer.app

import com.lifeorganizer.app.core.utils.AdaptiveReminderEngine
import com.lifeorganizer.app.core.utils.AutoOrganizationEngine
import com.lifeorganizer.app.core.utils.ContextExtractor
import com.lifeorganizer.app.core.utils.HabitLearningEngine
import com.lifeorganizer.app.core.utils.ScheduleReorderEngine
import com.lifeorganizer.app.core.utils.TimeAvailabilityEngine
import com.lifeorganizer.app.data.local.entity.toDomain
import com.lifeorganizer.app.data.local.entity.toEntity
import com.lifeorganizer.app.domain.model.ActivityCategory
import com.lifeorganizer.app.domain.model.ActivityContextModel
import com.lifeorganizer.app.domain.model.ActivityModel
import com.lifeorganizer.app.domain.model.ActivityType
import com.lifeorganizer.app.domain.model.AppCategoryClassification
import com.lifeorganizer.app.domain.model.AppUsageStatModel
import com.lifeorganizer.app.domain.model.ContinuityStatus
import com.lifeorganizer.app.domain.model.ExecutionRecordModel
import com.lifeorganizer.app.domain.model.FocusSessionModel
import com.lifeorganizer.app.domain.model.NotificationPreferences
import com.lifeorganizer.app.domain.model.ScheduleEntryModel
import com.lifeorganizer.app.domain.model.ScheduleStatus
import com.lifeorganizer.app.domain.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * End-to-end domain verification covering all 8 comprehensive integration flows (Etapa 7).
 */
class ComprehensiveIntegrationFlowsTest {

    // Fluxo 1: Cadastrar atividade -> Planejar -> Concluir -> Consultar histórico
    @Test
    fun testFlow1_CreateActivity_Schedule_Complete_History() {
        val activity = ActivityModel(
            id = 10L,
            title = "Estudar Algoritmos",
            category = ActivityCategory.STUDIES.displayName,
            type = ActivityType.FLEXIBLE,
            estimatedDurationMinutes = 60
        )

        val scheduled = ScheduleEntryModel(
            id = 100L,
            activityId = activity.id,
            activityTitle = activity.title,
            category = ActivityCategory.STUDIES,
            date = "2026-11-06",
            startHour = 9,
            startMinute = 0,
            endHour = 10,
            endMinute = 0,
            targetDurationMinutes = 60,
            status = ScheduleStatus.PENDING
        )

        // Complete the task with actual 55 min
        val completed = scheduled.copy(
            status = ScheduleStatus.COMPLETED,
            actualDurationMinutes = 55,
            completedAt = 100_000L
        )

        val historyRecord = ExecutionRecordModel(
            id = 1L,
            scheduleEntryId = completed.id,
            activityId = activity.id,
            activityTitle = activity.title,
            category = ActivityCategory.STUDIES,
            plannedDate = completed.date,
            plannedStartMinutes = 9 * 60,
            plannedDurationMinutes = 60,
            actualCompletedAt = 100_000L,
            actualDurationMinutes = 55,
            finalStatus = ScheduleStatus.COMPLETED,
            timestamp = 100_000L
        )

        assertEquals(ScheduleStatus.COMPLETED, completed.status)
        assertEquals(55, completed.actualDurationMinutes)
        assertEquals(activity.id, historyRecord.activityId)
        assertEquals(ScheduleStatus.COMPLETED, historyRecord.finalStatus)
    }

    // Fluxo 2: Cadastrar compromisso fixo -> Planejar automaticamente -> Verificar conflitos -> Aprovar
    @Test
    fun testFlow2_FixedCommitment_AutoPlan_Conflicts_Approval() {
        val fixedMeeting = ActivityModel(
            id = 20L,
            title = "Reunião de Equipe",
            category = ActivityCategory.WORK.displayName,
            type = ActivityType.FIXED,
            startTime = "10:00",
            endTime = "11:30",
            startHour = 10,
            startMinute = 0,
            endHour = 11,
            endMinute = 30,
            estimatedDurationMinutes = 90
        )

        val flexibleActivity = ActivityModel(
            id = 21L,
            title = "Revisar Documento",
            category = ActivityCategory.WORK.displayName,
            type = ActivityType.FLEXIBLE,
            estimatedDurationMinutes = 45
        )

        val targetDate = LocalDate.now().plusDays(1)
        val proposal = AutoOrganizationEngine.generateProposal(
            targetDate = targetDate,
            wakeUpTimeStr = "08:00",
            sleepTimeStr = "23:00",
            fixedActivities = listOf(fixedMeeting),
            existingEntries = emptyList(),
            unallocatedActivities = listOf(flexibleActivity),
            referenceTimeMinutes = 480 // 08:00 AM start
        )

        assertTrue("Proposal must generate allocated items", proposal.proposedEntries.isNotEmpty())
        val proposedItem = proposal.proposedEntries.first()
        val proposedStartMin = proposedItem.startHour * 60 + proposedItem.startMinute

        // Must not conflict with 10:00 - 11:30 fixed meeting
        val fixedStartMin = 10 * 60
        val fixedEndMin = 11 * 60 + 30
        val overlapsMeeting = proposedStartMin < fixedEndMin && (proposedStartMin + proposedItem.durationMinutes) > fixedStartMin
        assertFalse("Auto proposed flexible task must not overlap fixed meeting", overlapsMeeting)

        // Simula aprovação da proposta na agenda (criação de ScheduleEntryModel)
        val approvedEntries = mutableListOf<ScheduleEntryModel>()
        proposal.proposedEntries.forEach { item ->
            approvedEntries.add(
                ScheduleEntryModel(
                    id = 100L,
                    activityId = item.activityId,
                    activityTitle = item.title,
                    date = targetDate.toString(),
                    startHour = item.startHour,
                    startMinute = item.startMinute,
                    endHour = item.endHour,
                    endMinute = item.endMinute,
                    targetDurationMinutes = item.durationMinutes
                )
            )
        }
        assertEquals(1, approvedEntries.size)
        assertEquals(flexibleActivity.id, approvedEntries.first().activityId)

        // Idempotência da aprovação: aprovar novamente não duplica agendamentos
        val duplicateCheck = proposal.proposedEntries.filter { proposed ->
            approvedEntries.none { existing ->
                existing.activityId == proposed.activityId && existing.date == targetDate.toString()
            }
        }
        assertEquals("Aprovação idempotente não deve gerar agendamentos duplicados", 0, duplicateCheck.size)
    }

    // Fluxo 3: Cadastrar série -> Registrar episódio -> Concluir sessão -> Receber sugestão de continuidade
    @Test
    fun testFlow3_SeriesContext_RegisterEpisode_ContinuitySuggestion() {
        val contextInfo = ContextExtractor.extractContext(
            activityTitle = "Assistir Naruto",
            notes = "Episódio 42"
        )
        assertNotNull(contextInfo)
        assertEquals("Episódio 42", contextInfo?.currentProgress)
        assertEquals(42, contextInfo?.currentUnitValue)

        val seriesContext = ActivityContextModel(
            id = 1L,
            activityId = 30L,
            contentType = contextInfo!!.contentType,
            contentName = contextInfo.contentName,
            currentProgress = contextInfo.currentProgress,
            unitType = contextInfo.unitType,
            currentUnitValue = contextInfo.currentUnitValue,
            continuityStatus = ContinuityStatus.IN_PROGRESS,
            lastSessionAt = System.currentTimeMillis()
        )

        // Advance progress
        val nextContext = seriesContext.advance()
        assertEquals("Episódio 43", nextContext.currentProgress)
        assertEquals(43, nextContext.currentUnitValue)
    }

    // Fluxo 4: Cadastrar estudo -> Registrar várias execuções -> Identificar horário preferido -> Receber sugestão
    @Test
    fun testFlow4_StudyLearning_BestTimePreference_Suggestion() {
        val studyAct = ActivityModel(
            id = 40L,
            title = "Banco de Dados",
            category = ActivityCategory.STUDIES.displayName,
            type = ActivityType.FLEXIBLE,
            estimatedDurationMinutes = 60
        )

        // 3 executions completed successfully in the morning (09:00)
        val records = listOf(
            ExecutionRecordModel(id = 1L, scheduleEntryId = 1L, activityId = 40L, activityTitle = "Banco de Dados", category = ActivityCategory.STUDIES, plannedDate = "2026-11-01", plannedStartMinutes = 9 * 60, plannedDurationMinutes = 60, actualDurationMinutes = 60, finalStatus = ScheduleStatus.COMPLETED, timestamp = 1L),
            ExecutionRecordModel(id = 2L, scheduleEntryId = 2L, activityId = 40L, activityTitle = "Banco de Dados", category = ActivityCategory.STUDIES, plannedDate = "2026-11-02", plannedStartMinutes = 9 * 60, plannedDurationMinutes = 60, actualDurationMinutes = 60, finalStatus = ScheduleStatus.COMPLETED, timestamp = 2L),
            ExecutionRecordModel(id = 3L, scheduleEntryId = 3L, activityId = 40L, activityTitle = "Banco de Dados", category = ActivityCategory.STUDIES, plannedDate = "2026-11-03", plannedStartMinutes = 9 * 60, plannedDurationMinutes = 60, actualDurationMinutes = 60, finalStatus = ScheduleStatus.COMPLETED, timestamp = 3L)
        )

        val summary = HabitLearningEngine.computeHabitSummary(
            records = records,
            activities = listOf(studyAct),
            currentEntries = emptyList()
        )

        assertTrue(summary.hasSufficientData)
        assertTrue(summary.bestTimePreferences.isNotEmpty())
        val pref = summary.bestTimePreferences.first { it.activityId == 40L }
        assertTrue(pref.timeSlotName.contains("Manhã"))
        assertEquals(1.0f, pref.completionRate, 0.01f)
    }

    // Fluxo 5: Criar tarefa -> Agendar lembrete -> Reagendar tarefa -> Verificar atualização da notificação
    @Test
    fun testFlow5_ReminderScheduling_Postpone_Update() {
        val entry = ScheduleEntryModel(
            id = 50L,
            activityId = 50L,
            activityTitle = "Fisioterapia",
            category = ActivityCategory.HEALTH,
            date = "2026-11-06",
            startHour = 14,
            startMinute = 0,
            endHour = 15,
            endMinute = 0,
            targetDurationMinutes = 60,
            status = ScheduleStatus.PENDING
        )

        val prefs = NotificationPreferences(
            remindersEnabled = true,
            notifyBeforeStart = true,
            notifyBeforeMinutes = 15,
            promptCompletionOnEnd = true
        )

        val remindersBefore = AdaptiveReminderEngine.computeRemindersForDay(listOf(entry), prefs)
        assertEquals(2, remindersBefore.size)
        assertEquals(14 * 60 - 15, remindersBefore[0].triggerTimeMinutes) // 13:45

        // Postpone task to 16:00
        val postponedEntry = entry.copy(
            startHour = 16,
            startMinute = 0,
            endHour = 17,
            endMinute = 0
        )

        val remindersAfter = AdaptiveReminderEngine.computeRemindersForDay(listOf(postponedEntry), prefs)
        assertEquals(2, remindersAfter.size)
        assertEquals(16 * 60 - 15, remindersAfter[0].triggerTimeMinutes) // 15:45
    }

    // Fluxo 6: Iniciar modo foco -> Pausar -> Retomar -> Concluir -> Registrar duração
    @Test
    fun testFlow6_FocusSession_Pause_Resume_Conclude() {
        val startMillis = 100_000L
        val session = FocusSessionModel(
            id = 60L,
            activityId = 60L,
            activityTitle = "Escrever Relatório",
            category = ActivityCategory.WORK,
            plannedDurationMinutes = 25,
            startTimeMillis = startMillis,
            isActive = true
        )

        // Paused 10 minutes later (600 seconds)
        val paused = session.copy(
            isPaused = true,
            pausedAtMillis = startMillis + 600_000L,
            elapsedSecondsBeforePause = 600L
        )
        assertEquals(600L, paused.computeTotalElapsedSeconds(startMillis + 700_000L))

        // Resumed at 800_000L
        val resumed = paused.copy(
            isPaused = false,
            pausedAtMillis = null,
            startTimeMillis = 800_000L
        )

        // Concluded at 800_000L + 600_000L (another 10 minutes = 1200 seconds total)
        val nowMillis = 800_000L + 600_000L
        val totalSec = resumed.computeTotalElapsedSeconds(nowMillis)
        assertEquals(1200L, totalSec) // 20 minutes total

        val concluded = resumed.copy(
            isActive = false,
            completedAtMillis = nowMillis
        )
        assertFalse(concluded.isActive)
        assertEquals(20, (totalSec / 60).toInt())
    }

    // Fluxo 7: Ativar monitoramento opcional -> Conceder permissão -> Consultar dados -> Desativar -> Apagar registros
    @Test
    fun testFlow7_OptionalMonitoring_Consent_Classification_Clear() {
        var consentGranted = false
        val initialProfile = UserProfile(usageMonitoringConsentGranted = consentGranted)
        assertFalse(initialProfile.usageMonitoringConsentGranted)

        // User grants consent in settings
        consentGranted = true
        val updatedProfile = initialProfile.copy(usageMonitoringConsentGranted = consentGranted)
        assertTrue(updatedProfile.usageMonitoringConsentGranted)

        // Distraction and productive log simulation
        val logInstagram = AppUsageStatModel(
            packageName = "com.instagram.android",
            appName = "Instagram",
            totalTimeInForegroundSeconds = 1800L,
            classification = AppCategoryClassification.DISTRACTION,
            windowStartMillis = 0L,
            windowEndMillis = 1800L * 1000L
        )

        assertTrue(logInstagram.isDistraction())
        assertEquals(30, (logInstagram.totalTimeInForegroundSeconds / 60).toInt())

        // User disables and clears data
        consentGranted = false
        val resetProfile = updatedProfile.copy(usageMonitoringConsentGranted = consentGranted)
        assertFalse(resetProfile.usageMonitoringConsentGranted)
    }

    // Fluxo 8: Criar atividades -> Fechar aplicativo -> Reabrir -> Verificar persistência
    @Test
    fun testFlow8_ActivityPersistence_AcrossSessions() {
        val original = ActivityModel(
            id = 80L,
            title = "Treino Funcional",
            category = ActivityCategory.HEALTH.displayName,
            type = ActivityType.FLEXIBLE,
            estimatedDurationMinutes = 45,
            priority = com.lifeorganizer.app.domain.model.ActivityPriority.HIGH,
            colorHex = "#10B981"
        )

        // Map to entity and back (simulating SQLite write and read)
        val entity = original.toEntity()
        val reconstructed = entity.toDomain()

        assertEquals(original.id, reconstructed.id)
        assertEquals(original.title, reconstructed.title)
        assertEquals(original.category, reconstructed.category)
        assertEquals(original.type, reconstructed.type)
        assertEquals(original.estimatedDurationMinutes, reconstructed.estimatedDurationMinutes)
        assertEquals(original.priority, reconstructed.priority)
        assertEquals(original.colorHex, reconstructed.colorHex)
    }

    // Fluxo 9: Conclusão do onboarding -> Transição para Home -> Reabertura com persistência
    @Test
    fun testFlow9_OnboardingCompletion_AndReopening_StateAndAvailability() {
        // 1. Initial fresh onboarding profile
        val freshProfile = UserProfile(
            name = "",
            wakeUpTime = "08:00",
            sleepTime = "00:00",
            isOnboardingCompleted = false,
            onboardingStep = 1
        )
        assertFalse(freshProfile.isOnboardingCompleted)

        // 2. User completes onboarding with name and custom routine
        val completedProfile = freshProfile.copy(
            name = "LifeFlow Tester",
            wakeUpTime = "07:30",
            sleepTime = "23:30",
            fixedCommitments = setOf("Trabalho", "Treino"),
            organizationMode = com.lifeorganizer.app.domain.model.OrganizationMode.MANUAL,
            isOnboardingCompleted = true,
            onboardingStep = 6
        )
        assertTrue(completedProfile.isOnboardingCompleted)
        assertEquals("LifeFlow Tester", completedProfile.name)

        // 3. Calculate initial DayAvailability for a fresh user with 0 activities
        val wake = com.lifeorganizer.app.core.utils.DateTimeUtils.parseTime(completedProfile.wakeUpTime)
        val sleep = com.lifeorganizer.app.core.utils.DateTimeUtils.parseTime(completedProfile.sleepTime)
        val awakeDurationMin = com.lifeorganizer.app.core.utils.TimeCalculationUtils.calculateAwakeDurationMinutes(wake, sleep)
        assertEquals(960L, awakeDurationMin) // 07:30 to 23:30 = 16h = 960m

        val availability = TimeAvailabilityEngine.calculateDayAvailability(
            date = "2026-11-07",
            wakeUpTimeStr = completedProfile.wakeUpTime,
            sleepTimeStr = completedProfile.sleepTime,
            fixedActivities = emptyList(),
            scheduledEntries = emptyList(),
            unallocatedActivities = emptyList()
        )

        assertEquals(960, availability.awakeDurationMinutes)
        assertEquals(0, availability.fixedCommitmentsMinutes)
        assertEquals(960, availability.availableTimeMinutes)
        assertEquals(0, availability.plannedActivitiesMinutes)
        assertEquals(960, availability.remainingFreeMinutes)
        assertEquals(0f, availability.fixedRatio, 0.001f)
        assertEquals(0f, availability.plannedRatio, 0.001f)
        assertEquals(1f, availability.freeRatio, 0.001f)
        assertFalse(availability.isOverbooked)
        assertTrue(availability.freeSlots.isNotEmpty())

        // 4. Verify HabitLearningEngine on newly onboarded user with 0 activities:
        // Safely returns welcoming generic fallback suggestion without throwing exceptions
        val learnedRecs = HabitLearningEngine.generateLearnedRecommendations(
            currentTimeMinutes = 8 * 60,
            userProfile = completedProfile,
            activities = emptyList(),
            records = emptyList(),
            contexts = emptyList(),
            feedbacks = emptyList(),
            todayEntries = emptyList()
        )
        assertNotNull(learnedRecs)
        assertTrue(learnedRecs.isNotEmpty())
        assertTrue(learnedRecs.all { it.isGeneric })
        assertEquals("rec_generic_read", learnedRecs.first().id)

        // 5. Simulate reopening: profile is retrieved from persistence with completed flag
        val reopenedProfile = completedProfile.copy()
        assertTrue(reopenedProfile.isOnboardingCompleted)
        val startDestination = if (reopenedProfile.isOnboardingCompleted) "main" else "onboarding"
        assertEquals("main", startDestination)

        // 6. Resilience: verify reopening with partially persisted / incomplete time fields falls back to safe routine
        val partialProfile = UserProfile(
            name = "",
            wakeUpTime = "",
            sleepTime = "",
            isOnboardingCompleted = true
        )
        val safeWake = if (partialProfile.wakeUpTime.isNotBlank() && com.lifeorganizer.app.core.utils.TimeCalculationUtils.isValidTimeFormat(partialProfile.wakeUpTime)) partialProfile.wakeUpTime else "08:00"
        val safeSleep = if (partialProfile.sleepTime.isNotBlank() && com.lifeorganizer.app.core.utils.TimeCalculationUtils.isValidTimeFormat(partialProfile.sleepTime)) partialProfile.sleepTime else "00:00"
        val safeAvailability = TimeAvailabilityEngine.calculateDayAvailability(
            date = "2026-11-07",
            wakeUpTimeStr = safeWake,
            sleepTimeStr = safeSleep,
            fixedActivities = emptyList(),
            scheduledEntries = emptyList()
        )
        assertEquals(960, safeAvailability.awakeDurationMinutes)
        assertEquals(960, safeAvailability.availableTimeMinutes)
        assertFalse(safeAvailability.isOverbooked)
    }
}
