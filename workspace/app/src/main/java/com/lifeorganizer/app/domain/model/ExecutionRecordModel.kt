package com.lifeorganizer.app.domain.model

import com.lifeorganizer.app.core.utils.TimeCalculationUtils

/**
 * Historical record of activity execution for learning and analytics (Etapa 3/5/6).
 */
data class ExecutionRecordModel(
    val id: Long = 0,
    val scheduleEntryId: Long,
    val activityId: Long,
    val activityTitle: String,
    val category: ActivityCategory,
    val plannedDate: String, // "YYYY-MM-DD"
    val plannedStartMinutes: Int,
    val plannedDurationMinutes: Int,
    val actualCompletedAt: Long? = null,
    val actualDurationMinutes: Int? = null,
    val finalStatus: ScheduleStatus,
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
) {
    constructor(
        id: Long = 0,
        scheduleEntryId: Long,
        activityId: Long,
        activityTitle: String,
        category: String,
        plannedDate: String,
        plannedStartMinutes: Int,
        plannedDurationMinutes: Int,
        actualCompletedAt: Long? = null,
        actualDurationMinutes: Int? = null,
        finalStatus: ScheduleStatus,
        timestamp: Long = System.currentTimeMillis(),
        notes: String = ""
    ) : this(
        id = id,
        scheduleEntryId = scheduleEntryId,
        activityId = activityId,
        activityTitle = activityTitle,
        category = ActivityCategory.fromDisplayName(category),
        plannedDate = plannedDate,
        plannedStartMinutes = plannedStartMinutes,
        plannedDurationMinutes = plannedDurationMinutes,
        actualCompletedAt = actualCompletedAt,
        actualDurationMinutes = actualDurationMinutes,
        finalStatus = finalStatus,
        timestamp = timestamp,
        notes = notes
    )

    constructor(
        id: Long = 0,
        scheduleEntryId: Long,
        activityId: Long,
        activityTitle: String,
        category: ActivityCategory,
        plannedDate: String,
        plannedStartMinutes: Int,
        plannedDurationMinutes: Int,
        actualCompletedAt: Long? = null,
        actualDurationMinutes: Int? = null,
        finalStatus: String,
        timestamp: Long = System.currentTimeMillis(),
        notes: String = ""
    ) : this(
        id = id,
        scheduleEntryId = scheduleEntryId,
        activityId = activityId,
        activityTitle = activityTitle,
        category = category,
        plannedDate = plannedDate,
        plannedStartMinutes = plannedStartMinutes,
        plannedDurationMinutes = plannedDurationMinutes,
        actualCompletedAt = actualCompletedAt,
        actualDurationMinutes = actualDurationMinutes,
        finalStatus = ScheduleStatus.fromString(finalStatus),
        timestamp = timestamp,
        notes = notes
    )

    constructor(
        id: Long = 0,
        scheduleEntryId: Long,
        activityId: Long,
        activityTitle: String,
        category: String,
        plannedDate: String,
        plannedStartMinutes: Int,
        plannedDurationMinutes: Int,
        actualCompletedAt: Long? = null,
        actualDurationMinutes: Int? = null,
        finalStatus: String,
        timestamp: Long = System.currentTimeMillis(),
        notes: String = ""
    ) : this(
        id = id,
        scheduleEntryId = scheduleEntryId,
        activityId = activityId,
        activityTitle = activityTitle,
        category = ActivityCategory.fromDisplayName(category),
        plannedDate = plannedDate,
        plannedStartMinutes = plannedStartMinutes,
        plannedDurationMinutes = plannedDurationMinutes,
        actualCompletedAt = actualCompletedAt,
        actualDurationMinutes = actualDurationMinutes,
        finalStatus = ScheduleStatus.fromString(finalStatus),
        timestamp = timestamp,
        notes = notes
    )

    val plannedTimeFormatted: String
        get() {
            val h = (plannedStartMinutes / 60) % 24
            val m = plannedStartMinutes % 60
            return "%02d:%02d".format(h, m)
        }

    val plannedDurationFormatted: String
        get() = TimeCalculationUtils.formatMinutesToHoursAndMinutes(plannedDurationMinutes.toLong())

    val actualDurationFormatted: String?
        get() = actualDurationMinutes?.let { TimeCalculationUtils.formatMinutesToHoursAndMinutes(it.toLong()) }
}
