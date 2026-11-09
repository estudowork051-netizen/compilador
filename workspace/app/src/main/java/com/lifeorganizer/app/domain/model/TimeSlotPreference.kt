package com.lifeorganizer.app.domain.model

data class TimeSlotPreference(
    val activityId: Long,
    val activityTitle: String,
    val timeSlotName: String, // e.g. "Manhã (09h-12h)"
    val startHour: Int,
    val endHour: Int,
    val completedCount: Int,
    val totalAttempts: Int,
    val completionRate: Float,
    val confidence: ConfidenceLevel,
    val reason: String
) {
    val completionPercentage: Int get() = (completionRate * 100).toInt()
    val preferredSlot: TimeSlotPreference get() = this
    val displayName: String get() = timeSlotName
    val confidenceRate: Float get() = completionRate
}
