package com.lifeorganizer.app.core.utils

import com.lifeorganizer.app.data.local.entity.RecommendationFeedbackEntity
import com.lifeorganizer.app.domain.model.ActivityCategory
import com.lifeorganizer.app.domain.model.ActivityContextModel
import com.lifeorganizer.app.domain.model.ActivityModel
import com.lifeorganizer.app.domain.model.ConfidenceLevel
import com.lifeorganizer.app.domain.model.ExecutionRecordModel
import com.lifeorganizer.app.domain.model.HabitInsightSummary
import com.lifeorganizer.app.domain.model.RecommendationModel
import com.lifeorganizer.app.domain.model.RecommendationType
import com.lifeorganizer.app.domain.model.ScheduleEntryModel
import com.lifeorganizer.app.domain.model.ScheduleStatus
import com.lifeorganizer.app.domain.model.UserProfile
import java.time.LocalDate

object HabitLearningEngine {

    const val MIN_TOTAL_RECORDS_FOR_INSIGHTS = 3

    /**
     * Computes the complete statistical habit summary for the Habits dashboard.
     */
    fun computeHabitSummary(
        records: List<ExecutionRecordModel>,
        activities: List<ActivityModel>,
        currentEntries: List<ScheduleEntryModel>
    ): HabitInsightSummary {
        val completedRecords = records.filter { it.finalStatus == ScheduleStatus.COMPLETED }
        val hasSufficientData = completedRecords.size >= MIN_TOTAL_RECORDS_FOR_INSIGHTS

        // 1. Top completed activities
        val topCompleted = completedRecords
            .groupBy { it.activityTitle }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
            .take(5)

        // 2. Best time preferences per activity
        val bestTimes = activities.mapNotNull { activity ->
            TimePreferenceAnalyzer.findBestTimeSlot(activity.id, activity.title, records)
        }.sortedByDescending { it.completionRate }

        // 3. Duration insights
        val durationInsights = activities.mapNotNull { activity ->
            DurationEstimator.estimateActivityDuration(
                activity.id,
                activity.title,
                activity.durationMinutes,
                records
            )
        }.filter { it.differenceMinutes != 0 }

        // 4. Frequently postponed
        val postponedInsights = PostponementDetector.detectPostponedActivities(records)

        // 5. Category distribution minutes
        val categoryMinutes = mutableMapOf<ActivityCategory, Int>()
        for (rec in completedRecords) {
            val duration = rec.actualDurationMinutes ?: rec.plannedDurationMinutes
            val current = categoryMinutes[rec.category] ?: 0
            categoryMinutes[rec.category] = current + duration
        }

        // 6. Weekly completion counts
        val totalCompletedWeek = completedRecords.size
        val totalPlannedWeek = records.size

        return HabitInsightSummary(
            topCompletedActivities = topCompleted,
            bestTimePreferences = bestTimes,
            durationInsights = durationInsights,
            frequentlyPostponed = postponedInsights,
            categoryDistributionMinutes = categoryMinutes,
            totalCompletedWeek = totalCompletedWeek,
            totalPlannedWeek = totalPlannedWeek,
            hasSufficientData = hasSufficientData
        )
    }

    /**
     * Generates learned, personalized recommendations combining habits, context, and feedback.
     */
    fun generateLearnedRecommendations(
        currentTimeMinutes: Int,
        userProfile: UserProfile,
        activities: List<ActivityModel>,
        records: List<ExecutionRecordModel>,
        contexts: List<ActivityContextModel>,
        feedbacks: List<RecommendationFeedbackEntity>,
        todayEntries: List<ScheduleEntryModel>
    ): List<RecommendationModel> {
        if (!userProfile.learningEnabled) {
            // If learning is disabled by user, fallback to general rule-based recommendations
            return RecommendationEngine.generateRecommendations(
                currentTimeMinutes = currentTimeMinutes,
                userProfile = userProfile,
                allActivities = activities,
                todayEntries = todayEntries,
                contexts = contexts
            )
        }

        val recommendations = mutableListOf<RecommendationModel>()
        val scheduledTodayIds = todayEntries.filter { it.status != ScheduleStatus.CANCELLED }.map { it.activityId }.toSet()

        // 1. Learned Best Time Slots
        for (activity in activities) {
            if (scheduledTodayIds.contains(activity.id)) continue
            val bestSlot = TimePreferenceAnalyzer.findBestTimeSlot(activity.id, activity.title, records)
            if (bestSlot != null && bestSlot.confidence != ConfidenceLevel.INSUFFICIENT_DATA) {
                // If current time is close to or inside the best slot
                val currentHour = (currentTimeMinutes / 60) % 24
                if (currentHour in (bestSlot.startHour - 1)..bestSlot.endHour) {
                    recommendations.add(
                        RecommendationModel(
                            id = "rec_best_time_${activity.id}",
                            type = RecommendationType.BEST_TIME_SLOT,
                            title = "Melhor horário para ${activity.title}",
                            message = "Você costuma concluir ${activity.title} com mais frequência por volta das ${String.format("%02d:00", bestSlot.startHour)}. Quer reservar esse horário?",
                            activity = activity,
                            suggestedStartHour = bestSlot.startHour,
                            suggestedStartMinute = 0,
                            suggestedDurationMinutes = activity.durationMinutes,
                            reason = bestSlot.reason,
                            confidence = bestSlot.confidence,
                            sampleCount = bestSlot.totalAttempts,
                            priorityScore = 150
                        )
                    )
                }
            }
        }

        // 2. Learned Duration Adjustments
        for (activity in activities) {
            val durationInsight = DurationEstimator.estimateActivityDuration(
                activity.id,
                activity.title,
                activity.durationMinutes,
                records
            )
            if (durationInsight != null && durationInsight.confidence != ConfidenceLevel.INSUFFICIENT_DATA && durationInsight.differenceMinutes != 0) {
                recommendations.add(
                    RecommendationModel(
                        id = "rec_duration_${activity.id}",
                        type = RecommendationType.LEARNED_DURATION,
                        title = "Ajuste de duração: ${activity.title}",
                        message = durationInsight.suggestionMessage,
                        activity = activity,
                        suggestedDurationMinutes = durationInsight.learnedMedianDurationMinutes,
                        reason = "Baseado em ${durationInsight.sampleCount} execuções reais com duração mediana de ${durationInsight.learnedMedianDurationMinutes} min.",
                        confidence = durationInsight.confidence,
                        sampleCount = durationInsight.sampleCount,
                        priorityScore = 120
                    )
                )
            }
        }

        // 3. Frequently Postponed Task Adjustment
        val postponedList = PostponementDetector.detectPostponedActivities(records)
        for (postponed in postponedList.take(2)) {
            val activity = activities.find { it.id == postponed.activityId }
            if (activity != null) {
                recommendations.add(
                    RecommendationModel(
                        id = "rec_postponed_${activity.id}",
                        type = RecommendationType.POSTPONED_ADJUSTMENT,
                        title = "Novo horário para ${activity.title}",
                        message = postponed.suggestionMessage,
                        activity = activity,
                        suggestedDurationMinutes = activity.durationMinutes,
                        reason = "Identificamos ${postponed.postponedCount} reagendamentos anteriores.",
                        confidence = ConfidenceLevel.MODERATE,
                        sampleCount = postponed.totalScheduledCount,
                        priorityScore = 110
                    )
                )
            }
        }

        // 4. Integrate continuity and contextual suggestions from Step 4
        val step4Recs = RecommendationEngine.generateRecommendations(
            currentTimeMinutes = currentTimeMinutes,
            userProfile = userProfile,
            allActivities = activities,
            todayEntries = todayEntries,
            contexts = contexts
        )
        recommendations.addAll(step4Recs)

        // 5. Rank and filter via RecommendationRanker
        return RecommendationRanker.rankRecommendations(
            recommendations = recommendations,
            feedbacks = feedbacks,
            userProfile = userProfile,
            currentTimeMinutes = currentTimeMinutes
        )
    }
}
