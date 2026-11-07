package com.lifeorganizer.app.core.utils

import com.lifeorganizer.app.domain.model.ActivityCategory
import com.lifeorganizer.app.domain.model.AppUsageStatModel
import com.lifeorganizer.app.domain.model.DayBalanceSummary
import com.lifeorganizer.app.domain.model.FocusSessionModel
import com.lifeorganizer.app.domain.model.ScheduleEntryModel
import com.lifeorganizer.app.domain.model.ScheduleStatus

data class WellbeingInsight(
    val title: String,
    val description: String,
    val iconName: String = "favorite",
    val isWarning: Boolean = false
)

object WellbeingRecommendationEngine {

    fun computeDayBalance(
        entries: List<ScheduleEntryModel>,
        focusSessions: List<FocusSessionModel> = emptyList(),
        usageLogs: List<AppUsageStatModel> = emptyList()
    ): DayBalanceSummary {
        var studiesMinutes = 0
        var workMinutes = 0
        var leisureMinutes = 0
        var homeMinutes = 0
        var restMinutes = 0
        var otherMinutes = 0

        var completedCount = 0
        var postponedCount = 0
        var pendingCount = 0

        for (entry in entries) {
            val duration = entry.targetDurationMinutes.coerceAtLeast(0)
            when (entry.category) {
                ActivityCategory.STUDIES -> studiesMinutes += duration
                ActivityCategory.WORK -> workMinutes += duration
                ActivityCategory.LEISURE -> leisureMinutes += duration
                ActivityCategory.HOME -> homeMinutes += duration
                ActivityCategory.REST -> restMinutes += duration
                else -> otherMinutes += duration
            }

            when (entry.status) {
                ScheduleStatus.COMPLETED -> completedCount++
                ScheduleStatus.POSTPONED -> postponedCount++
                else -> pendingCount++
            }
        }

        val totalFocusMinutes = focusSessions
            .filter { it.completedAtMillis != null || it.isActive }
            .sumOf { (it.computeTotalElapsedSeconds() / 60L).toInt() }

        val distractionUsageMinutes = usageLogs
            .filter { it.isDistraction() }
            .sumOf { (it.totalTimeInForegroundSeconds / 60L).toInt() }

        val productiveUsageMinutes = usageLogs
            .filter { it.isProductive() }
            .sumOf { (it.totalTimeInForegroundSeconds / 60L).toInt() }

        val totalMonitoredPhoneMinutes = distractionUsageMinutes + productiveUsageMinutes

        return DayBalanceSummary(
            studiesMinutes = studiesMinutes,
            workMinutes = workMinutes,
            leisureMinutes = leisureMinutes,
            homeMinutes = homeMinutes,
            restMinutes = restMinutes,
            otherMinutes = otherMinutes,
            totalFocusMinutes = totalFocusMinutes,
            totalMonitoredPhoneMinutes = totalMonitoredPhoneMinutes,
            distractionUsageMinutes = distractionUsageMinutes,
            completedTasksCount = completedCount,
            postponedTasksCount = postponedCount,
            pendingTasksCount = pendingCount
        )
    }

    fun generateInsights(balance: DayBalanceSummary): List<WellbeingInsight> {
        val insights = mutableListOf<WellbeingInsight>()
        val productiveMinutes = balance.studiesMinutes + balance.workMinutes

        // Overload check
        if (productiveMinutes >= 480) {
            insights.add(
                WellbeingInsight(
                    title = "Ritmo Intenso",
                    description = "Seu dia tem mais de 8 horas de foco e trabalho planejadas. Reserve intervalos curtos de respiro para manter a clareza mental.",
                    iconName = "warning",
                    isWarning = true
                )
            )
        }

        // Rest / Leisure check
        if (balance.restMinutes == 0 && balance.leisureMinutes == 0 && productiveMinutes > 120) {
            insights.add(
                WellbeingInsight(
                    title = "Cuidado com o descanso",
                    description = "Ainda não identificamos momentos de descanso ou lazer hoje. Uma pausa suave de 15 minutos renova sua energia.",
                    iconName = "spa",
                    isWarning = false
                )
            )
        }

        // Positive completion feedback
        if (balance.completedTasksCount > 0 && balance.postponedTasksCount == 0 && balance.pendingTasksCount <= 1) {
            insights.add(
                WellbeingInsight(
                    title = "Dia produtivo e leve",
                    description = "Você completou a maioria das tarefas sem precisar adiar. Excelente harmonia na rotina!",
                    iconName = "check_circle",
                    isWarning = false
                )
            )
        }

        // Postponement empathy
        if (balance.postponedTasksCount >= 2) {
            insights.add(
                WellbeingInsight(
                    title = "Flexibilidade é natural",
                    description = "Adiar atividades faz parte do dia a dia. Reorganize o restante sem autocobrança excessiva.",
                    iconName = "self_improvement",
                    isWarning = false
                )
            )
        }

        // Focus session accomplishment
        if (balance.totalFocusMinutes >= 30) {
            insights.add(
                WellbeingInsight(
                    title = "Momento de foco aproveitado",
                    description = "Você já dedicou ${balance.totalFocusMinutes} minutos de atenção plena hoje. Parabéns pela dedicação!",
                    iconName = "timer",
                    isWarning = false
                )
            )
        }

        // Fallback welcoming insight if list is empty
        if (insights.isEmpty()) {
            insights.add(
                WellbeingInsight(
                    title = "Dia em equilíbrio",
                    description = "Suas atividades estão bem distribuídas. Mantenha um passo tranquilo e respeite seus limites.",
                    iconName = "favorite",
                    isWarning = false
                )
            )
        }

        return insights
    }
}
