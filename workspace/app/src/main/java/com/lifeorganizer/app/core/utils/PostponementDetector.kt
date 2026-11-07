package com.lifeorganizer.app.core.utils

import com.lifeorganizer.app.domain.model.ExecutionRecordModel
import com.lifeorganizer.app.domain.model.PostponementInsight
import com.lifeorganizer.app.domain.model.ScheduleStatus

object PostponementDetector {

    const val MIN_POSTPONEMENTS_FOR_PATTERN = 3
    const val MIN_POSTPONEMENT_RATE = 0.35f

    /**
     * Identifies activities frequently postponed without blame or reprimands (Section 7).
     */
    fun detectPostponedActivities(
        records: List<ExecutionRecordModel>
    ): List<PostponementInsight> {
        val groupedByActivity = records.groupBy { it.activityId }
        val insights = mutableListOf<PostponementInsight>()

        for ((activityId, activityRecords) in groupedByActivity) {
            val totalScheduled = activityRecords.size
            if (totalScheduled < MIN_POSTPONEMENTS_FOR_PATTERN) continue

            val postponedCount = activityRecords.count { it.finalStatus == ScheduleStatus.POSTPONED }
            if (postponedCount < MIN_POSTPONEMENTS_FOR_PATTERN) continue

            val postponementRate = postponedCount.toFloat() / totalScheduled
            if (postponementRate >= MIN_POSTPONEMENT_RATE) {
                val title = activityRecords.firstOrNull()?.activityTitle ?: "Esta atividade"
                val percentage = (postponementRate * 100).toInt()

                val message = "Percebi que '$title' foi adiada $postponedCount vezes recentemente ($percentage% dos agendamentos). Que tal tentar um horário ou duração diferente?"

                insights.add(
                    PostponementInsight(
                        activityId = activityId,
                        activityTitle = title,
                        postponedCount = postponedCount,
                        totalScheduledCount = totalScheduled,
                        postponementRate = postponementRate,
                        suggestionMessage = message
                    )
                )
            }
        }

        return insights.sortedByDescending { it.postponementRate }
    }
}
