package com.lifeorganizer.app.domain.model

data class HabitInsightSummary(
    val topCompletedActivities: List<Pair<String, Int>> = emptyList(),
    val bestTimePreferences: List<TimeSlotPreference> = emptyList(),
    val durationInsights: List<DurationInsight> = emptyList(),
    val frequentlyPostponed: List<PostponementInsight> = emptyList(),
    val categoryDistributionMinutes: Map<ActivityCategory, Int> = emptyMap(),
    val totalCompletedWeek: Int = 0,
    val totalPlannedWeek: Int = 0,
    val hasSufficientData: Boolean = false
) {
    val completionRateWeek: Float
        get() = if (totalPlannedWeek > 0) totalCompletedWeek.toFloat() / totalPlannedWeek else 0f

    val completionPercentageWeek: Int
        get() = (completionRateWeek * 100).toInt()
}
