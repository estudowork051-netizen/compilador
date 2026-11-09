package com.lifeorganizer.app.domain.model

import com.lifeorganizer.app.core.utils.TimeCalculationUtils
import java.time.LocalDate

data class ProposedScheduleItem(
    val activity: ActivityModel,
    val startHour: Int,
    val startMinute: Int,
    val durationMinutes: Int,
    val reason: String = "",
    val contextInfo: String = ""
) {
    val activityId: Long get() = activity.id
    val title: String get() = activity.title
    val category: String get() = activity.category
    val startTime: String get() = "%02d:%02d".format(startHour, startMinute)

    val endHour: Int
        get() = ((startHour * 60 + startMinute + durationMinutes) / 60) % 24

    val endMinute: Int
        get() = (startHour * 60 + startMinute + durationMinutes) % 60

    val formattedTimeRange: String
        get() = "%02d:%02d às %02d:%02d".format(startHour, startMinute, endHour, endMinute)

    val timeRangeFormatted: String
        get() = formattedTimeRange

    val formattedDuration: String
        get() = TimeCalculationUtils.formatMinutesToHoursAndMinutes(durationMinutes.toLong())
}

data class OrganizationProposal(
    val date: LocalDate,
    val items: List<ProposedScheduleItem>,
    val totalProposedMinutes: Int,
    val remainingFreeMinutes: Int,
    val unallocatedCount: Int = 0,
    val explanation: String = ""
) {
    val dateString: String get() = date.toString()

    val totalPlannedMinutes: Int get() = totalProposedMinutes

    val totalProposedFormatted: String
        get() = TimeCalculationUtils.formatMinutesToHoursAndMinutes(totalProposedMinutes.toLong())

    val remainingFreeFormatted: String
        get() = TimeCalculationUtils.formatMinutesToHoursAndMinutes(remainingFreeMinutes.toLong())

    val explanationMessage: String get() = explanation

    val proposedEntries: List<ProposedScheduleItem> get() = items
}
