package com.lifeorganizer.app.domain.model

import com.lifeorganizer.app.core.utils.TimeCalculationUtils

/**
 * Represents a discrete interval of time.
 */
data class TimeSlot(
    val startMinutes: Int,
    val endMinutes: Int,
    val label: String = ""
) {
    constructor(startTime: String, endTime: String, durationMinutes: Int = 0) : this(
        startMinutes = parseTimeString(startTime),
        endMinutes = if (endTime.isNotBlank()) parseTimeString(endTime) else parseTimeString(startTime) + durationMinutes,
        label = ""
    )

    val durationMinutes: Int
        get() = (endMinutes - startMinutes).coerceAtLeast(0)

    val formattedRange: String
        get() {
            val sH = (startMinutes / 60) % 24
            val sM = startMinutes % 60
            val eH = (endMinutes / 60) % 24
            val eM = endMinutes % 60
            val isNextDay = endMinutes >= 1440
            val nextDaySuffix = if (isNextDay) " (+1 dia)" else ""
            return "%02d:%02d às %02d:%02d%s".format(sH, sM, eH, eM, nextDaySuffix)
        }

    val formattedDuration: String
        get() = TimeCalculationUtils.formatMinutesToHoursAndMinutes(durationMinutes.toLong())

    fun overlaps(other: TimeSlot): Boolean {
        return startMinutes < other.endMinutes && other.startMinutes < endMinutes
    }
}

/**
 * Represents a scheduling conflict between two activities.
 */
data class ScheduleConflict(
    val titleA: String,
    val titleB: String,
    val overlapSlot: TimeSlot,
    val message: String = "Conflito entre \"$titleA\" e \"$titleB\" das ${overlapSlot.formattedRange}"
)

/**
 * Encapsulates the complete availability metrics for a given day.
 * Conforms to Step 3 Section 4:
 * - Tempo disponível: períodos sem compromissos fixos.
 * - Tempo planejado: períodos ocupados por atividades agendadas.
 * - Tempo livre restante: períodos disponíveis que ainda não foram ocupados.
 * - Tempo não alocado: duração de tarefas pendentes que ainda não receberam horário.
 */
data class DayAvailability(
    val date: String,
    val wakeUpTime: String,
    val sleepTime: String,
    val awakeDurationMinutes: Int,
    val fixedCommitmentsMinutes: Int,
    val availableTimeMinutes: Int,
    val plannedActivitiesMinutes: Int,
    val remainingFreeMinutes: Int,
    val unallocatedActivitiesMinutes: Int = 0,
    val conflicts: List<ScheduleConflict> = emptyList(),
    val freeSlots: List<TimeSlot> = emptyList()
) {
    val isOverbooked: Boolean
        get() = plannedActivitiesMinutes > availableTimeMinutes

    val overbookedMinutes: Int
        get() = if (isOverbooked) plannedActivitiesMinutes - availableTimeMinutes else 0

    val availableTimeFormatted: String
        get() = TimeCalculationUtils.formatMinutesToHoursAndMinutes(availableTimeMinutes.toLong())

    val plannedTimeFormatted: String
        get() = TimeCalculationUtils.formatMinutesToHoursAndMinutes(plannedActivitiesMinutes.toLong())

    val remainingFreeFormatted: String
        get() = TimeCalculationUtils.formatMinutesToHoursAndMinutes(remainingFreeMinutes.toLong())

    val fixedCommitmentsFormatted: String
        get() = TimeCalculationUtils.formatMinutesToHoursAndMinutes(fixedCommitmentsMinutes.toLong())

    val unallocatedFormatted: String
        get() = TimeCalculationUtils.formatMinutesToHoursAndMinutes(unallocatedActivitiesMinutes.toLong())

    // Proportions for visual distribution bar (0f..1f)
    val fixedRatio: Float
        get() = if (awakeDurationMinutes > 0) {
            (fixedCommitmentsMinutes.toFloat() / awakeDurationMinutes).coerceIn(0f, 1f)
        } else 0f

    val plannedRatio: Float
        get() = if (awakeDurationMinutes > 0) {
            (plannedActivitiesMinutes.toFloat() / awakeDurationMinutes).coerceIn(0f, 1f - fixedRatio)
        } else 0f

    val freeRatio: Float
        get() = (1f - fixedRatio - plannedRatio).coerceAtLeast(0f)
}

private fun parseTimeString(timeStr: String): Int {
    val parts = timeStr.trim().split(":")
    if (parts.size != 2) return 0
    val h = parts[0].toIntOrNull() ?: 0
    val m = parts[1].toIntOrNull() ?: 0
    return h * 60 + m
}
