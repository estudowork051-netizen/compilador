package com.lifeorganizer.app.domain.model

import com.lifeorganizer.app.core.utils.TimeCalculationUtils
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.min

/**
 * Domain representation of a scheduled activity occurrence on a specific date.
 */
data class ScheduleEntryModel(
    val id: Long = 0,
    val activityId: Long,
    val activity: ActivityModel? = null,
    val activityTitle: String = activity?.title ?: "",
    val category: ActivityCategory = activity?.let { ActivityCategory.fromDisplayName(it.category) } ?: ActivityCategory.OUTROS,
    val colorHex: String = activity?.colorHex ?: "#8B9CFF",
    val date: String, // "YYYY-MM-DD"
    val startHour: Int = 9,
    val startMinute: Int = 0,
    val endHour: Int = 10,
    val endMinute: Int = 0,
    val targetDurationMinutes: Int = 60,
    val actualDurationMinutes: Int? = null,
    val status: ScheduleStatus = ScheduleStatus.PENDING,
    val completedAt: Long? = null,
    val notes: String = "",
    val postponedToDate: String? = null,
    val startAt: Long = 0L,
    val endAt: Long = 0L
) {
    val startTotalMinutes: Int
        get() = startHour * 60 + startMinute

    val endTotalMinutes: Int
        get() {
            val rawEnd = endHour * 60 + endMinute
            return if (rawEnd <= startTotalMinutes && targetDurationMinutes > 0) {
                // Crosses midnight or wrapped
                startTotalMinutes + targetDurationMinutes
            } else if (rawEnd <= startTotalMinutes) {
                rawEnd + 1440
            } else {
                rawEnd
            }
        }

    val computedDurationMinutes: Int
        get() = if (targetDurationMinutes > 0) {
            targetDurationMinutes
        } else {
            (endTotalMinutes - startTotalMinutes).coerceAtLeast(0)
        }

    val formattedDuration: String
        get() = TimeCalculationUtils.formatMinutesToHoursAndMinutes(computedDurationMinutes.toLong())

    val durationFormatted: String
        get() = formattedDuration

    val formattedTimeRange: String
        get() {
            val startStr = "%02d:%02d".format(startHour, startMinute)
            val endStr = "%02d:%02d".format(endHour, endMinute)
            val crossesMidnight = (endHour * 60 + endMinute) <= (startHour * 60 + startMinute)
            return if (crossesMidnight) "$startStr às $endStr (+1 dia)" else "$startStr às $endStr"
        }

    val timeFormatted: String
        get() = formattedTimeRange

    val toTimeSlot: TimeSlot
        get() = TimeSlot(startMinutes = startTotalMinutes, endMinutes = endTotalMinutes, label = activityTitle)

    /**
     * Calculates the actual occupied interval for this activity occurrence.
     * If a flexible activity was completed early, only the executed portion is occupied,
     * liberating the remaining window from actual end to endTotalMinutes for new scheduling.
     * Fixed commitments preserve their full reserved window regardless of status.
     */
    fun effectiveOccupiedSlot(dateContext: String? = null, currentTime: LocalTime = LocalTime.now()): TimeSlot? {
        if (status == ScheduleStatus.CANCELLED) return null
        if (activity?.type == ActivityType.FIXED) {
            return TimeSlot(startTotalMinutes, endTotalMinutes, activityTitle)
        }
        if (status == ScheduleStatus.COMPLETED) {
            val effDuration = actualDurationMinutes
            if (effDuration != null) {
                if (effDuration <= 0) return null
                val effEnd = min(endTotalMinutes, startTotalMinutes + effDuration)
                return if (effEnd > startTotalMinutes) {
                    TimeSlot(startTotalMinutes, effEnd, activityTitle)
                } else null
            }
            if (completedAt != null) {
                val instant = Instant.ofEpochMilli(completedAt)
                val zdt = instant.atZone(ZoneId.systemDefault())
                val completedDate = zdt.toLocalDate().toString()
                val targetDate = dateContext ?: date
                if (completedDate == targetDate) {
                    val compMin = zdt.hour * 60 + zdt.minute
                    if (compMin <= startTotalMinutes) {
                        return null
                    } else if (compMin < endTotalMinutes) {
                        return TimeSlot(startTotalMinutes, compMin, activityTitle)
                    } else {
                        return TimeSlot(startTotalMinutes, endTotalMinutes, activityTitle)
                    }
                } else if (completedDate < targetDate) {
                    return null
                }
            }
            return null
        }
        return TimeSlot(startTotalMinutes, endTotalMinutes, activityTitle)
    }
}
