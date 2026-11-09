package com.lifeorganizer.app.domain.model

data class DurationInsight(
    val activityId: Long,
    val activityTitle: String,
    val plannedDurationMinutes: Int,
    val learnedMedianDurationMinutes: Int,
    val sampleCount: Int,
    val confidence: ConfidenceLevel,
    val suggestionMessage: String
) {
    val differenceMinutes: Int get() = learnedMedianDurationMinutes - plannedDurationMinutes
}
