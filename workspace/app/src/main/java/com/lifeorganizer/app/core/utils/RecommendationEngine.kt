package com.lifeorganizer.app.core.utils

import com.lifeorganizer.app.domain.model.ActivityCategory
import com.lifeorganizer.app.domain.model.ActivityContextModel
import com.lifeorganizer.app.domain.model.ActivityModel
import com.lifeorganizer.app.domain.model.ActivityType
import com.lifeorganizer.app.domain.model.BalancePreference
import com.lifeorganizer.app.domain.model.ContentType
import com.lifeorganizer.app.domain.model.ContinuityStatus
import com.lifeorganizer.app.domain.model.DayAvailability
import com.lifeorganizer.app.domain.model.RecommendationModel
import com.lifeorganizer.app.domain.model.RecommendationType
import com.lifeorganizer.app.domain.model.ScheduleEntryModel
import com.lifeorganizer.app.domain.model.ScheduleStatus
import com.lifeorganizer.app.domain.model.TimeSlot
import com.lifeorganizer.app.domain.model.UserProfile
import java.time.LocalTime
import kotlin.math.min

/**
 * Intelligent recommendation engine for free time suggestions and category balance.
 * Implements Step 4 Sections 8, 9, 10 and 11.
 */
object RecommendationEngine {

    fun generateCurrentRecommendation(
        currentTime: LocalTime = LocalTime.now(),
        availability: DayAvailability,
        activities: List<ActivityModel>,
        scheduledEntries: List<ScheduleEntryModel> = emptyList(),
        contexts: List<ActivityContextModel> = emptyList(),
        balancePreference: BalancePreference = BalancePreference.BALANCED
    ): RecommendationModel? {
        val currentMinutes = currentTime.hour * 60 + currentTime.minute

        // 1. Check for rest break after intense work (>= 180 min work today)
        val completedEntries = scheduledEntries.filter { it.status == ScheduleStatus.COMPLETED }
        val workMinutesCompleted = completedEntries
            .filter { it.category == ActivityCategory.TRABALHO || it.category.name.equals("Trabalho", ignoreCase = true) }
            .sumOf { it.computedDurationMinutes }

        if (workMinutesCompleted >= 180) {
            val freeSlot = availability.freeSlots.firstOrNull { it.startMinutes <= currentMinutes && currentMinutes < it.endMinutes }
                ?: availability.freeSlots.firstOrNull { it.startMinutes >= currentMinutes }
                ?: TimeSlot(currentMinutes, currentMinutes + 30)
            return RecommendationModel(
                id = "rec_rest_intense_work",
                type = RecommendationType.REST_BREAK,
                title = "Momento de pausa recomendado",
                message = "Você já realizou ${TimeCalculationUtils.formatMinutesToHoursAndMinutes(workMinutesCompleted.toLong())} de trabalho intenso. Que tal uma pausa?",
                activity = null,
                suggestedStartHour = (freeSlot.startMinutes / 60) % 24,
                suggestedStartMinute = freeSlot.startMinutes % 60,
                suggestedDurationMinutes = 30.coerceAtMost(freeSlot.durationMinutes.takeIf { it > 0 } ?: 30),
                reason = "Recuperação após trabalho intenso",
                priorityScore = 95
            )
        }

        // 2. Check for contextual continuity (e.g. series, book in progress)
        val candidateContext = contexts.firstOrNull { ctx ->
            ctx.continuityStatus == ContinuityStatus.IN_PROGRESS &&
            activities.any { it.id == ctx.activityId }
        }
        if (candidateContext != null) {
            val targetAct = activities.firstOrNull { it.id == candidateContext.activityId } ?: activities.first()
            val nextUnit = (candidateContext.currentUnit ?: candidateContext.currentUnitValue ?: 0) + 1
            val nextLabel = if (candidateContext.contentType == ContentType.SERIES) {
                if (candidateContext.currentUnit != null || candidateContext.currentUnitValue != null) {
                    "Episódio $nextUnit"
                } else if (candidateContext.progressLabel.isNotBlank()) {
                    candidateContext.progressLabel
                } else {
                    "próximo episódio"
                }
            } else if (candidateContext.progressLabel.isNotBlank()) {
                candidateContext.progressLabel
            } else {
                "onde você parou"
            }
            return RecommendationModel(
                id = "rec_cont_${candidateContext.id}",
                type = RecommendationType.RESUME_CONTENT,
                title = "Continuar ${candidateContext.contentName}",
                message = "Quer continuar ${candidateContext.contentName} do $nextLabel?",
                activity = targetAct,
                suggestedDurationMinutes = targetAct.estimatedDurationMinutes,
                reason = "Continuidade de conteúdo em andamento",
                priorityScore = 95
            )
        }

        // 3. Slot fitting / gap filling (Fill gap)
        val activeFreeSlot = availability.freeSlots.firstOrNull { slot ->
            currentMinutes >= slot.startMinutes && currentMinutes < slot.endMinutes
        } ?: availability.freeSlots.firstOrNull { it.startMinutes >= currentMinutes }
          ?: availability.freeSlots.firstOrNull()

        val availableMinutes = if (activeFreeSlot != null) {
            if (currentMinutes in activeFreeSlot.startMinutes until activeFreeSlot.endMinutes) {
                activeFreeSlot.endMinutes - currentMinutes
            } else {
                activeFreeSlot.durationMinutes
            }
        } else {
            30
        }

        val fittingActivity = activities
            .filter { it.type == ActivityType.FLEXIBLE && it.estimatedDurationMinutes <= availableMinutes }
            .maxByOrNull { it.estimatedDurationMinutes }
            ?: activities.firstOrNull { it.estimatedDurationMinutes <= availableMinutes }

        if (fittingActivity != null) {
            return RecommendationModel(
                id = "rec_fill_gap_${fittingActivity.id}",
                type = RecommendationType.FILL_GAP,
                title = "Aproveitar tempo livre",
                message = "Você tem ${TimeCalculationUtils.formatMinutesToHoursAndMinutes(availableMinutes.toLong())} livres. Que tal ${fittingActivity.title}?",
                activity = fittingActivity,
                suggestedDurationMinutes = fittingActivity.estimatedDurationMinutes,
                reason = "Encaixe no intervalo disponível",
                priorityScore = 80
            )
        }

        return null
    }

    fun generateRecommendations(
        currentTimeMinutes: Int,
        userProfile: UserProfile,
        allActivities: List<ActivityModel>,
        todayEntries: List<ScheduleEntryModel>,
        contexts: List<ActivityContextModel> = emptyList()
    ): List<RecommendationModel> {
        val freeSlot = TimeSlot(
            startMinutes = currentTimeMinutes,
            endMinutes = (currentTimeMinutes + 60).coerceAtMost(1440),
            label = "Horário livre"
        )
        return generateRecommendations(
            freeSlot = freeSlot,
            allActivities = allActivities,
            todayEntries = todayEntries,
            contexts = contexts,
            balancePreference = userProfile.balancePreference
        )
    }

    fun generateRecommendations(
        freeSlot: TimeSlot?,
        allActivities: List<ActivityModel>,
        todayEntries: List<ScheduleEntryModel>,
        contexts: List<ActivityContextModel> = emptyList(),
        balancePreference: BalancePreference = BalancePreference.BALANCED,
        categoryMinutesToday: Map<String, Int> = emptyMap()
    ): List<RecommendationModel> {
        val freeMinutes = freeSlot?.durationMinutes ?: 60
        val recommendations = mutableListOf<RecommendationModel>()

        val scheduledIds = todayEntries.filter { it.status != ScheduleStatus.CANCELLED }.map { it.activityId }.toSet()
        val candidateActivities = allActivities.filter { !scheduledIds.contains(it.id) }
        val contextMap = contexts.associateBy { it.activityId }

        // 1. Context Continuity Suggestion
        for (context in contexts) {
            val act = allActivities.firstOrNull { it.id == context.activityId }
            if (act != null && !scheduledIds.contains(act.id) && act.estimatedDurationMinutes <= freeMinutes) {
                recommendations.add(
                    RecommendationModel(
                        id = "rec_cont_${context.id}",
                        type = RecommendationType.CONTINUITY,
                        title = "Continuar de onde parou",
                        message = "Você parou no ${context.progressLabel} de \"${context.contentName}\". Quer reservar ${TimeCalculationUtils.formatMinutesToHoursAndMinutes(act.estimatedDurationMinutes.toLong())}?",
                        activity = act,
                        suggestedStartHour = freeSlot?.let { (it.startMinutes / 60) % 24 },
                        suggestedStartMinute = freeSlot?.let { it.startMinutes % 60 },
                        suggestedDurationMinutes = act.estimatedDurationMinutes,
                        reason = "Memória contextual: ${context.contentName}",
                        priorityScore = 95
                    )
                )
                break
            }
        }

        // 2. Responsibility & Leisure Balance Suggestion
        if (balancePreference != BalancePreference.NO_PREFERENCE) {
            val leisureMin = categoryMinutesToday["Lazer"] ?: 0
            val studyMin = categoryMinutesToday["Estudos"] ?: 0

            if (leisureMin >= 90 && studyMin == 0) {
                val studyAct = candidateActivities.firstOrNull { it.category == "Estudos" && it.estimatedDurationMinutes <= freeMinutes }
                if (studyAct != null) {
                    recommendations.add(
                        RecommendationModel(
                            id = "rec_balance_${studyAct.id}",
                            type = RecommendationType.CATEGORY_BALANCE,
                            title = "Equilíbrio da rotina",
                            message = "Hoje você já aproveitou seu lazer (${TimeCalculationUtils.formatMinutesToHoursAndMinutes(leisureMin.toLong())}). Quer dedicar um tempinho a \"${studyAct.title}\"?",
                            activity = studyAct,
                            suggestedStartHour = freeSlot?.let { (it.startMinutes / 60) % 24 },
                            suggestedStartMinute = freeSlot?.let { it.startMinutes % 60 },
                            suggestedDurationMinutes = studyAct.estimatedDurationMinutes,
                            reason = "Equilíbrio entre lazer e responsabilidades",
                            priorityScore = 90
                        )
                    )
                }
            }
        }

        // 3. Free Time Fit Suggestion
        val fittingActivity = candidateActivities.firstOrNull { it.estimatedDurationMinutes <= freeMinutes }
        if (fittingActivity != null) {
            val durText = TimeCalculationUtils.formatMinutesToHoursAndMinutes(freeMinutes.toLong())
            recommendations.add(
                RecommendationModel(
                    id = "rec_fit_${fittingActivity.id}",
                    type = RecommendationType.FREE_TIME_FIT,
                    title = "Uma ideia para agora",
                    message = "Você tem $durText livres. Que tal aproveitar para \"${fittingActivity.title}\"?",
                    activity = fittingActivity,
                    suggestedStartHour = freeSlot?.let { (it.startMinutes / 60) % 24 },
                    suggestedStartMinute = freeSlot?.let { it.startMinutes % 60 },
                    suggestedDurationMinutes = fittingActivity.estimatedDurationMinutes,
                    reason = "Compatível com seu tempo disponível",
                    priorityScore = 80
                )
            )
        }

        // 4. Rest Suggestion if day is heavily occupied
        val totalSpentToday = categoryMinutesToday.values.sum()
        if (totalSpentToday >= 300 && freeMinutes in 15..60) {
            recommendations.add(
                RecommendationModel(
                    id = "rec_rest_today",
                    type = RecommendationType.REST,
                    title = "Pausa recomendada",
                    message = "Seu dia já teve ${TimeCalculationUtils.formatMinutesToHoursAndMinutes(totalSpentToday.toLong())} de atividades. Que tal reservar esse intervalo para descansar e recarregar?",
                    activity = null,
                    suggestedDurationMinutes = freeMinutes,
                    reason = "Recuperação de energia e descanso",
                    priorityScore = 85
                )
            )
        }

        // Fallback generic suggestion if no candidate fits
        if (recommendations.isEmpty()) {
            recommendations.add(
                RecommendationModel(
                    id = "rec_generic_read",
                    type = RecommendationType.FREE_TIME_FIT,
                    title = "Sugestão de leitura ou descanso",
                    message = "Aproveite seu tempo livre para ler algumas páginas ou fazer uma pausa relaxante.",
                    activity = null,
                    suggestedDurationMinutes = min(30, freeMinutes),
                    isGeneric = true,
                    priorityScore = 50
                )
            )
        }

        return recommendations.sortedByDescending { it.priorityScore }
    }
}
