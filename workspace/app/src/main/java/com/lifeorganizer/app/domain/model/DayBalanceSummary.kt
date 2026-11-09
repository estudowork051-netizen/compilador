package com.lifeorganizer.app.domain.model

data class AdaptiveReminderInsight(
    val activityId: Long,
    val activityTitle: String,
    val category: ActivityCategory,
    val patternType: AdaptivePatternType,
    val suggestedAction: String,
    val reason: String,
    val confidence: ConfidenceLevel = ConfidenceLevel.MODERATE
)

enum class AdaptivePatternType {
    FREQUENT_POSTPONE_MORNING,
    FREQUENT_POSTPONE_NIGHT,
    FREQUENTLY_UNFINISHED,
    LONGER_THAN_EXPECTED,
    HIGH_SUCCESS_PERIOD
}

data class DayBalanceSummary(
    val studiesMinutes: Int = 0,
    val workMinutes: Int = 0,
    val leisureMinutes: Int = 0,
    val homeMinutes: Int = 0,
    val restMinutes: Int = 0,
    val otherMinutes: Int = 0,
    val totalFocusMinutes: Int = 0,
    val totalMonitoredPhoneMinutes: Int = 0,
    val distractionUsageMinutes: Int = 0,
    val completedTasksCount: Int = 0,
    val postponedTasksCount: Int = 0,
    val pendingTasksCount: Int = 0,
    val totalCompletedTasks: Int = completedTasksCount,
    val totalPendingTasks: Int = pendingTasksCount,
    val totalPostponedTasks: Int = postponedTasksCount,
    val totalPlannedTasks: Int = completedTasksCount + postponedTasksCount + pendingTasksCount,
    val totalFreeTimeMinutes: Int = 0,
    val nextCommitmentText: String = "",
    val categoryDistributionRecordedMinutes: Map<ActivityCategory, Int> = emptyMap(),
    val appUsageMonitoredMinutes: Long = totalMonitoredPhoneMinutes.toLong(),
    val isScheduleOverloaded: Boolean = false,
    val suggestedPauseMinutes: Int? = null,
    val balanceNarrative: String = "Seu dia está tomando forma."
)
