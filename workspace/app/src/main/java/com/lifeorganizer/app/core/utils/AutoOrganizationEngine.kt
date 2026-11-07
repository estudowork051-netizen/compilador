package com.lifeorganizer.app.core.utils

import com.lifeorganizer.app.domain.model.ActivityCategory
import com.lifeorganizer.app.domain.model.ActivityContextModel
import com.lifeorganizer.app.domain.model.ActivityModel
import com.lifeorganizer.app.domain.model.ActivityPriority
import com.lifeorganizer.app.domain.model.ActivityType
import com.lifeorganizer.app.domain.model.BalancePreference
import com.lifeorganizer.app.domain.model.OrganizationProposal
import com.lifeorganizer.app.domain.model.ProposedScheduleItem
import com.lifeorganizer.app.domain.model.ScheduleEntryModel
import com.lifeorganizer.app.domain.model.PredefinedActivity
import com.lifeorganizer.app.domain.model.ScheduleStatus
import com.lifeorganizer.app.domain.model.TimeSlot
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.max

/**
 * Deterministic, offline and testable automatic routine organization engine.
 * Integrates behavioral habits and learned durations from HabitLearningEngine.
 */
object AutoOrganizationEngine {

    const val DEFAULT_BUFFER_MINUTES = 10

    /**
     * Generates a routine proposal for a date without mutating the database.
     * Respects user profile preferences, availability gaps, and situations A through E.
     */
    fun generateProposal(
        date: LocalDate,
        wakeUpTimeStr: String,
        sleepTimeStr: String,
        allActivities: List<ActivityModel>,
        existingEntries: List<ScheduleEntryModel>,
        contexts: List<ActivityContextModel> = emptyList(),
        learnedDurations: Map<Long, Int> = emptyMap(),
        preferredTimeSlots: Map<Long, IntRange> = emptyMap(),
        minBufferMinutes: Int = DEFAULT_BUFFER_MINUTES,
        balancePreference: BalancePreference = BalancePreference.BALANCED
    ): OrganizationProposal {
        val awakeWindow = TimeAvailabilityEngine.getAwakeWindow(wakeUpTimeStr, sleepTimeStr)

        // 1. Fixed commitments active for this date (strictly unmovable)
        val activeFixed = allActivities.filter { it.type == ActivityType.FIXED && it.isScheduledForDate(date) }
        val fixedSlots = activeFixed.mapNotNull {
            it.toTimeSlot?.let { slot -> TimeAvailabilityEngine.clampToAwakeWindow(slot, awakeWindow) }
        }

        // 2. Already completed or locked entries on this date (preserve them!)
        val lockedEntries = existingEntries.filter { it.status == ScheduleStatus.COMPLETED }
        val lockedSlots = lockedEntries.mapNotNull {
            it.effectiveOccupiedSlot(date.toString())?.let { slot ->
                TimeAvailabilityEngine.clampToAwakeWindow(slot, awakeWindow)
            }
        }

        val allOccupied = TimeAvailabilityEngine.mergeIntervals(fixedSlots + lockedSlots)

        // 3. Find free slots in awake window (never suggest past times for today)
        val rawFreeSlots = TimeAvailabilityEngine.findFreeSlots(awakeWindow, allOccupied, minSlotDurationMinutes = 15)
        val isToday = date == java.time.LocalDate.now()
        val nowMin = java.time.LocalTime.now().let { it.hour * 60 + it.minute }
        val minAllowedStart = if (isToday) ((nowMin + 4) / 5) * 5 else 0

        val freeSlots = if (isToday) {
            rawFreeSlots.mapNotNull { slot ->
                val s = kotlin.math.max(slot.startMinutes, minAllowedStart)
                if (slot.endMinutes - s >= 15) {
                    TimeSlot(s, slot.endMinutes, slot.label)
                } else null
            }
        } else rawFreeSlots

        val totalFreeMinutes = freeSlots.sumOf { it.durationMinutes }

        // Situação D — Dia completamente ocupado
        if (totalFreeMinutes < 15) {
            return OrganizationProposal(
                date = date,
                items = emptyList(),
                totalProposedMinutes = 0,
                remainingFreeMinutes = 0,
                unallocatedCount = 0,
                explanation = "Seu dia já está completamente preenchido com compromissos e tarefas. Quer revisar a agenda ou planejar o dia de amanhã?"
            )
        }

        // 4. Exclude activities already scheduled/completed on this date to prevent duplicate scheduling
        val scheduledActivityIds = existingEntries.filter { it.status != ScheduleStatus.CANCELLED }.map { it.activityId }.toSet()
        val candidateActivities = allActivities.filter {
            it.type == ActivityType.FLEXIBLE && !scheduledActivityIds.contains(it.id)
        }

        val contextMap = contexts.associateBy { it.activityId }

        // Situação B — Não existem atividades flexíveis cadastradas (Fallback inteligente)
        if (candidateActivities.isEmpty()) {
            val suggestions = when (balancePreference) {
                BalancePreference.STUDY_FOCUS -> listOf(
                    PredefinedActivity("Leitura / Estudo focado", "Estudos", 45),
                    PredefinedActivity("Revisão de Conteúdo", "Estudos", 30)
                )
                BalancePreference.REST_FOCUS -> listOf(
                    PredefinedActivity("Momento de Descanso", "Lazer", 30),
                    PredefinedActivity("Alongamento e Respiração", "Saúde", 15)
                )
                else -> listOf(
                    PredefinedActivity("Caminhada ao ar livre", "Saúde", 30),
                    PredefinedActivity("Leitura tranquila", "Estudos", 30),
                    PredefinedActivity("Organizar ambiente", "Casa", 20)
                )
            }.map { p ->
                ActivityModel(
                    id = 0L,
                    title = p.title,
                    category = p.category,
                    type = ActivityType.FLEXIBLE,
                    estimatedDurationMinutes = p.suggestedDurationMinutes
                )
            }

            val fallbackItems = mutableListOf<ProposedScheduleItem>()
            var fallbackAllocated = 0
            val currentGaps = freeSlots.toMutableList()

            for (suggested in suggestions) {
                val needed = suggested.estimatedDurationMinutes
                val gapIndex = currentGaps.indexOfFirst { it.durationMinutes >= needed }
                if (gapIndex >= 0) {
                    val gap = currentGaps[gapIndex]
                    val startMin = gap.startMinutes
                    val sH = (startMin / 60) % 24
                    val sM = startMin % 60

                    fallbackItems.add(
                        ProposedScheduleItem(
                            activity = suggested,
                            startHour = sH,
                            startMinute = sM,
                            durationMinutes = needed,
                            reason = "Sugestão inteligente para seu tempo livre",
                            contextInfo = "Perfil: ${balancePreference.displayName}"
                        )
                    )
                    fallbackAllocated += needed

                    val newStart = startMin + needed + minBufferMinutes
                    if (gap.endMinutes > newStart) {
                        currentGaps[gapIndex] = TimeSlot(newStart, gap.endMinutes, "Tempo livre")
                    } else {
                        currentGaps.removeAt(gapIndex)
                    }
                }
            }

            val remainingFree = currentGaps.sumOf { it.durationMinutes }
            return OrganizationProposal(
                date = date,
                items = fallbackItems,
                totalProposedMinutes = fallbackAllocated,
                remainingFreeMinutes = remainingFree,
                unallocatedCount = 0,
                explanation = "Você não possui atividades cadastradas pendentes. Preparamos sugestões equilibradas para seu tempo livre com base no seu perfil (${balancePreference.displayName})."
            )
        }

        // Situação A & E — Existem atividades flexíveis cadastradas
        val sortedCandidates = candidateActivities.sortedWith(
            compareByDescending<ActivityModel> { act ->
                var score = when (act.priority) {
                    ActivityPriority.HIGH -> 300
                    ActivityPriority.MEDIUM -> 200
                    ActivityPriority.LOW -> 100
                }
                when (balancePreference) {
                    BalancePreference.STUDY_FOCUS -> {
                        if (act.category.equals("Estudos", ignoreCase = true) || act.category.equals("Trabalho", ignoreCase = true)) {
                            score += 250
                        }
                    }
                    BalancePreference.REST_FOCUS -> {
                        if (act.category.equals("Lazer", ignoreCase = true) || act.category.equals("Saúde", ignoreCase = true)) {
                            score += 150
                        }
                    }
                    BalancePreference.BALANCED -> {
                        if (act.category.equals("Lazer", ignoreCase = true)) score += 50
                    }
                    else -> {}
                }
                if (preferredTimeSlots.containsKey(act.id)) score += 80
                if (contextMap.containsKey(act.id)) score += 50
                score
            }.thenBy {
                learnedDurations[it.id] ?: it.totalEstimatedMinutes
            }
        )

        val proposedItems = mutableListOf<ProposedScheduleItem>()
        var totalAllocatedMinutes = 0
        val currentGaps = freeSlots.toMutableList()

        for (activity in sortedCandidates) {
            val neededDuration = learnedDurations[activity.id] ?: activity.totalEstimatedMinutes
            if (neededDuration <= 0) continue

            val preferredRange = preferredTimeSlots[activity.id]
            var gapIndex = -1

            if (preferredRange != null) {
                gapIndex = currentGaps.indexOfFirst { gap ->
                    gap.durationMinutes >= neededDuration &&
                            gap.startMinutes < preferredRange.last &&
                            gap.endMinutes > preferredRange.first
                }
            }

            if (gapIndex < 0) {
                gapIndex = currentGaps.indexOfFirst { it.durationMinutes >= neededDuration }
            }

            if (gapIndex >= 0) {
                val gap = currentGaps[gapIndex]
                val startMin = gap.startMinutes
                val endMin = startMin + neededDuration

                val sH = (startMin / 60) % 24
                val sM = startMin % 60

                val context = contextMap[activity.id]
                val contextNote = context?.progressLabel ?: ""
                val hasLearnedSlot = preferredRange != null && startMin in preferredRange
                val hasLearnedDuration = learnedDurations.containsKey(activity.id)

                val reason = when {
                    hasLearnedSlot -> "Melhor horário habitual (${String.format("%02d:00", preferredRange?.first?.div(60) ?: 0)})"
                    activity.priority == ActivityPriority.HIGH -> "Prioridade alta"
                    hasLearnedDuration -> "Duração real aprendida (${neededDuration}m)"
                    contextNote.isNotEmpty() -> "Continuidade: $contextNote"
                    else -> "Perfil: ${balancePreference.displayName}"
                }

                proposedItems.add(
                    ProposedScheduleItem(
                        activity = activity,
                        startHour = sH,
                        startMinute = sM,
                        durationMinutes = neededDuration,
                        reason = reason,
                        contextInfo = contextNote
                    )
                )

                totalAllocatedMinutes += neededDuration

                val newStart = endMin + minBufferMinutes
                if (gap.endMinutes > newStart) {
                    currentGaps[gapIndex] = TimeSlot(newStart, gap.endMinutes, "Tempo livre")
                } else {
                    currentGaps.removeAt(gapIndex)
                }
            }
        }

        val totalFreeMinutesRemaining = currentGaps.sumOf { it.durationMinutes }
        val unallocatedCount = sortedCandidates.size - proposedItems.size

        // Situação C — Existem atividades, mas nenhuma cabe nos horários disponíveis
        val explanation = when {
            proposedItems.isEmpty() -> {
                val minNeeded = candidateActivities.minOf { learnedDurations[it.id] ?: it.totalEstimatedMinutes }
                "Suas atividades cadastradas precisam de mais tempo (${minNeeded}m) do que os intervalos disponíveis livres. Que tal ajustar as durações ou escolher outro dia?"
            }
            totalFreeMinutesRemaining >= 180 -> {
                // Situação E — Dia com muito tempo livre
                "Organizamos ${proposedItems.size} atividade(s) respeitando seu perfil (${balancePreference.displayName}) e preservamos ${TimeCalculationUtils.formatMinutesToHoursAndMinutes(totalFreeMinutesRemaining.toLong())} livres para seu descanso e bem-estar."
            }
            unallocatedCount > 0 -> {
                "Distribuímos ${proposedItems.size} atividades de acordo com seu perfil (${balancePreference.displayName}). Restaram $unallocatedCount tarefas sem horário livre."
            }
            else -> {
                "Todas as suas atividades prioritárias foram distribuídas em horários ideais com intervalos confortáveis (${balancePreference.displayName})."
            }
        }

        return OrganizationProposal(
            date = date,
            items = proposedItems,
            totalProposedMinutes = totalAllocatedMinutes,
            remainingFreeMinutes = totalFreeMinutesRemaining,
            unallocatedCount = unallocatedCount,
            explanation = explanation
        )
    }

    /**
     * Overload for Step 4 PlanningViewModel compatibility.
     */
    fun generateProposal(
        targetDate: LocalDate,
        wakeUpTimeStr: String,
        sleepTimeStr: String,
        fixedActivities: List<ActivityModel>,
        existingEntries: List<ScheduleEntryModel>,
        unallocatedActivities: List<ActivityModel>,
        contextMap: Map<Long, ActivityContextModel> = emptyMap(),
        balancePreference: BalancePreference = BalancePreference.BALANCED,
        learnedDurations: Map<Long, Int> = emptyMap(),
        preferredTimeSlots: Map<Long, IntRange> = emptyMap()
    ): OrganizationProposal {
        val allActivities = fixedActivities + unallocatedActivities
        return generateProposal(
            date = targetDate,
            wakeUpTimeStr = wakeUpTimeStr,
            sleepTimeStr = sleepTimeStr,
            allActivities = allActivities,
            existingEntries = existingEntries,
            contexts = contextMap.values.toList(),
            learnedDurations = learnedDurations,
            preferredTimeSlots = preferredTimeSlots,
            balancePreference = balancePreference
        )
    }

    /**
     * Reorganizes remaining day after an interruption or delay (Step 4 Section 15).
     */
    fun reorganizeRemainingDay(
        date: LocalDate,
        currentTimeMinutes: Int,
        wakeUpTimeStr: String,
        sleepTimeStr: String,
        allActivities: List<ActivityModel>,
        existingEntries: List<ScheduleEntryModel>,
        contexts: List<ActivityContextModel> = emptyList(),
        learnedDurations: Map<Long, Int> = emptyMap()
    ): OrganizationProposal {
        val awakeWindow = TimeAvailabilityEngine.getAwakeWindow(wakeUpTimeStr, sleepTimeStr)
        val effectiveStart = max(currentTimeMinutes, awakeWindow.startMinutes)
        val remainingWindow = TimeSlot(effectiveStart, awakeWindow.endMinutes, "Vigília restante")

        if (remainingWindow.durationMinutes < 15) {
            return OrganizationProposal(
                date = date,
                items = emptyList(),
                totalProposedMinutes = 0,
                remainingFreeMinutes = 0,
                unallocatedCount = 0,
                explanation = "Seu dia já está no final da janela de vigília. Descanse e retome amanhã!"
            )
        }

        // Preserve all completed entries or past entries
        val completedOrPast = existingEntries.filter {
            it.status == ScheduleStatus.COMPLETED || it.startTotalMinutes < currentTimeMinutes
        }
        val lockedSlots = completedOrPast.mapNotNull {
            it.effectiveOccupiedSlot(date.toString())?.let { slot ->
                TimeAvailabilityEngine.clampToAwakeWindow(slot, remainingWindow)
            }
        }

        // Active fixed commitments in remaining window
        val activeFixed = allActivities.filter { it.type == ActivityType.FIXED && it.isScheduledForDate(date) }
        val fixedSlots = activeFixed.mapNotNull {
            it.toTimeSlot?.let { slot -> TimeAvailabilityEngine.clampToAwakeWindow(slot, remainingWindow) }
        }

        val allOccupied = TimeAvailabilityEngine.mergeIntervals(fixedSlots + lockedSlots)
        val freeGaps = TimeAvailabilityEngine.findFreeSlots(remainingWindow, allOccupied, minSlotDurationMinutes = 15).toMutableList()

        // Activities that were scheduled but not yet completed, plus unallocated flexible activities
        val completedIds = completedOrPast.map { it.activityId }.toSet()
        val pendingActivities = allActivities.filter {
            it.type == ActivityType.FLEXIBLE && !completedIds.contains(it.id)
        }

        val contextMap = contexts.associateBy { it.activityId }
        val sortedPending = pendingActivities.sortedByDescending { it.priority == ActivityPriority.HIGH }

        val proposedItems = mutableListOf<ProposedScheduleItem>()
        var totalAllocated = 0

        for (activity in sortedPending) {
            val needed = learnedDurations[activity.id] ?: activity.totalEstimatedMinutes
            val gapIdx = freeGaps.indexOfFirst { it.durationMinutes >= needed }
            if (gapIdx >= 0) {
                val gap = freeGaps[gapIdx]
                val sH = (gap.startMinutes / 60) % 24
                val sM = gap.startMinutes % 60
                val context = contextMap[activity.id]

                proposedItems.add(
                    ProposedScheduleItem(
                        activity = activity,
                        startHour = sH,
                        startMinute = sM,
                        durationMinutes = needed,
                        reason = "Reorganizado para agora",
                        contextInfo = context?.progressLabel ?: ""
                    )
                )
                totalAllocated += needed

                val newStart = gap.startMinutes + needed + DEFAULT_BUFFER_MINUTES
                if (gap.endMinutes > newStart) {
                    freeGaps[gapIdx] = TimeSlot(newStart, gap.endMinutes, "Tempo livre")
                } else {
                    freeGaps.removeAt(gapIdx)
                }
            }
        }

        val remainingFree = freeGaps.sumOf { it.durationMinutes }
        return OrganizationProposal(
            date = date,
            items = proposedItems,
            totalProposedMinutes = totalAllocated,
            remainingFreeMinutes = remainingFree,
            unallocatedCount = sortedPending.size - proposedItems.size,
            explanation = "Reorganizamos as atividades restantes a partir das ${String.format("%02d:%02d", (currentTimeMinutes / 60) % 24, currentTimeMinutes % 60)} respeitando seus compromissos fixos."
        )
    }

    /**
     * Overload for Step 4 PlanningViewModel compatibility.
     */
    fun reorganizeRemainingDay(
        currentTime: LocalTime,
        targetDate: LocalDate,
        wakeUpTimeStr: String,
        sleepTimeStr: String,
        fixedActivities: List<ActivityModel>,
        existingEntries: List<ScheduleEntryModel>,
        unallocatedActivities: List<ActivityModel>,
        contextMap: Map<Long, ActivityContextModel> = emptyMap(),
        balancePreference: BalancePreference = BalancePreference.BALANCED,
        learnedDurations: Map<Long, Int> = emptyMap()
    ): OrganizationProposal {
        val allActivities = fixedActivities + unallocatedActivities
        return reorganizeRemainingDay(
            date = targetDate,
            currentTimeMinutes = currentTime.hour * 60 + currentTime.minute,
            wakeUpTimeStr = wakeUpTimeStr,
            sleepTimeStr = sleepTimeStr,
            allActivities = allActivities,
            existingEntries = existingEntries,
            contexts = contextMap.values.toList(),
            learnedDurations = learnedDurations
        )
    }
}
