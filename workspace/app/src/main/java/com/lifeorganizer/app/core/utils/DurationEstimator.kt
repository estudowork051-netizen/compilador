package com.lifeorganizer.app.core.utils

import com.lifeorganizer.app.domain.model.ConfidenceLevel
import com.lifeorganizer.app.domain.model.DurationInsight
import com.lifeorganizer.app.domain.model.ExecutionRecordModel
import com.lifeorganizer.app.domain.model.ScheduleStatus
import kotlin.math.abs

object DurationEstimator {

    const val MIN_SAMPLES_FOR_ESTIMATE = 3
    const val MIN_DIFFERENCE_MINUTES = 10

    /**
     * Estimates real duration for an activity using robust median calculation and outlier filtering.
     */
    fun estimateActivityDuration(
        activityId: Long,
        activityTitle: String,
        plannedDurationMinutes: Int,
        records: List<ExecutionRecordModel>
    ): DurationInsight? {
        val validDurations = records
            .filter { it.activityId == activityId && it.finalStatus == ScheduleStatus.COMPLETED }
            .mapNotNull { it.actualDurationMinutes }
            .filter { it in 5..480 } // Ignore invalid/corrupted durations (<5m or >8h)

        if (validDurations.size < MIN_SAMPLES_FOR_ESTIMATE) {
            return null
        }

        // Outlier filtering using robust bounds around median
        val sorted = validDurations.sorted()
        val rawMedian = calculateMedian(sorted)

        // Filter out extreme values that are > 3x median or < 0.25x median if sample is large enough
        val filtered = if (sorted.size >= 5) {
            sorted.filter { it <= rawMedian * 3 && it >= rawMedian * 0.25 }
        } else {
            sorted
        }

        val learnedMedian = calculateMedian(filtered)
        val sampleCount = filtered.size

        val confidence = when {
            sampleCount < MIN_SAMPLES_FOR_ESTIMATE -> ConfidenceLevel.INSUFFICIENT_DATA
            sampleCount in 3..5 -> ConfidenceLevel.LOW
            sampleCount in 6..9 -> ConfidenceLevel.MODERATE
            else -> ConfidenceLevel.HIGH
        }

        val diff = abs(learnedMedian - plannedDurationMinutes)
        val isSignificant = diff >= MIN_DIFFERENCE_MINUTES || (diff.toFloat() / plannedDurationMinutes >= 0.25f)

        val suggestion = if (isSignificant) {
            "Você costuma terminar essa atividade em cerca de $learnedMedian minutos (planejado: ${plannedDurationMinutes}m). Quer atualizar a duração sugerida?"
        } else {
            "Sua duração real de $learnedMedian min está muito próxima do planejado (${plannedDurationMinutes}m)."
        }

        return DurationInsight(
            activityId = activityId,
            activityTitle = activityTitle,
            plannedDurationMinutes = plannedDurationMinutes,
            learnedMedianDurationMinutes = learnedMedian,
            sampleCount = sampleCount,
            confidence = confidence,
            suggestionMessage = suggestion
        )
    }

    fun calculateMedian(values: List<Int>): Int {
        if (values.isEmpty()) return 0
        val sorted = values.sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 1) {
            sorted[middle]
        } else {
            (sorted[middle - 1] + sorted[middle]) / 2
        }
    }
}
