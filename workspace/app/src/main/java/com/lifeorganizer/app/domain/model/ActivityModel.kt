package com.lifeorganizer.app.domain.model

import com.lifeorganizer.app.core.utils.TimeCalculationUtils

data class ActivityModel(
    val id: Long = 0,
    val title: String,
    val category: String,
    val estimatedDurationMinutes: Int,
    val type: ActivityType = ActivityType.FLEXIBLE,
    val defaultCount: Int = 1,
    val priority: ActivityPriority = ActivityPriority.MEDIUM,
    val colorHex: String = "#8B9CFF",
    val iconName: String = "default",
    val description: String = "",
    // Fixed commitment fields
    val startHour: Int? = null,
    val startMinute: Int? = null,
    val endHour: Int? = null,
    val endMinute: Int? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val recurrenceType: RecurrenceType? = null,
    val recurrenceDays: List<Int> = emptyList(), // 1 = Monday, 7 = Sunday
    // Weekly goal fields
    val weeklyTargetCount: Int? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val effectiveStartHour: Int?
        get() = startHour ?: startTime?.split(":")?.firstOrNull()?.toIntOrNull()

    val effectiveStartMinute: Int?
        get() = startMinute ?: startTime?.split(":")?.getOrNull(1)?.toIntOrNull()

    val effectiveEndHour: Int?
        get() = endHour ?: endTime?.split(":")?.firstOrNull()?.toIntOrNull()

    val effectiveEndMinute: Int?
        get() = endMinute ?: endTime?.split(":")?.getOrNull(1)?.toIntOrNull()
    val durationMinutes: Int
        get() = estimatedDurationMinutes

    val totalEstimatedMinutes: Int
        get() = estimatedDurationMinutes * defaultCount.coerceAtLeast(1)

    val durationFormatted: String
        get() = TimeCalculationUtils.formatMinutesToHoursAndMinutes(estimatedDurationMinutes.toLong())

    val totalDurationFormatted: String
        get() = TimeCalculationUtils.formatMinutesToHoursAndMinutes(totalEstimatedMinutes.toLong())

    val isCrossingMidnight: Boolean
        get() {
            if (startHour == null || endHour == null) return false
            val startMin = startHour * 60 + (startMinute ?: 0)
            val endMin = endHour * 60 + (endMinute ?: 0)
            return endMin <= startMin
        }

    val fixedDurationMinutes: Int
        get() {
            if (startHour == null || endHour == null) return estimatedDurationMinutes
            val startMin = startHour * 60 + (startMinute ?: 0)
            val endMin = endHour * 60 + (endMinute ?: 0)
            return if (endMin <= startMin) {
                (24 * 60 - startMin) + endMin
            } else {
                endMin - startMin
            }
        }

    val fixedTimeRangeFormatted: String
        get() {
            if (startHour == null || endHour == null) return ""
            val startStr = String.format("%02d:%02d", startHour, startMinute ?: 0)
            val endStr = String.format("%02d:%02d", endHour, endMinute ?: 0)
            return if (isCrossingMidnight) "$startStr às $endStr (+1 dia)" else "$startStr às $endStr"
        }

    fun isScheduledForDate(date: java.time.LocalDate): Boolean {
        if (type != ActivityType.FIXED) return true
        val dow = date.dayOfWeek.value // 1 (Monday) to 7 (Sunday)
        return when (recurrenceType) {
            RecurrenceType.DAILY -> true
            RecurrenceType.WEEKDAYS -> dow in 1..5
            RecurrenceType.WEEKENDS -> dow in 6..7 || recurrenceDays.contains(dow)
            RecurrenceType.SPECIFIC_DAYS -> recurrenceDays.isEmpty() || recurrenceDays.contains(dow)
            RecurrenceType.ONCE -> true
            null -> true
        }
    }

    val toTimeSlot: TimeSlot?
        get() {
            val sH = effectiveStartHour ?: return null
            val sM = effectiveStartMinute ?: 0
            val eH = effectiveEndHour ?: return null
            val eM = effectiveEndMinute ?: 0
            val startMin = sH * 60 + sM
            val endMin = if (eH * 60 + eM <= startMin) {
                (eH * 60 + eM) + 1440
            } else {
                eH * 60 + eM
            }
            return TimeSlot(startMin, endMin, label = title)
        }
}
