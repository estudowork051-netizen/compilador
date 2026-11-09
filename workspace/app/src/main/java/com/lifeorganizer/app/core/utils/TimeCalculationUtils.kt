package com.lifeorganizer.app.core.utils

import java.time.LocalTime

object TimeCalculationUtils {

    const val MINUTES_PER_DAY = 24 * 60

    /**
     * Calculates the awake duration in minutes between wakeUpTime and sleepTime,
     * correctly handling intervals that cross midnight (e.g. 08:00 to 01:00 = 17h, 08:00 to 00:00 = 16h).
     */
    fun calculateAwakeDurationMinutes(wakeUpTime: LocalTime, sleepTime: LocalTime): Long {
        val wakeMinutes = wakeUpTime.hour * 60 + wakeUpTime.minute
        val sleepMinutes = sleepTime.hour * 60 + sleepTime.minute

        val duration = if (sleepMinutes <= wakeMinutes) {
            // Crosses midnight (e.g., 08:00 to 00:00 or 08:00 to 01:30)
            (MINUTES_PER_DAY - wakeMinutes) + sleepMinutes
        } else {
            // Same day (e.g., 08:00 to 23:00)
            sleepMinutes - wakeMinutes
        }

        return duration.toLong()
    }

    /**
     * Formats minutes into a friendly string, e.g. "16h" or "16h 30min".
     */
    fun formatMinutesToHoursAndMinutes(totalMinutes: Long): String {
        val hours = totalMinutes / 60
        val remainingMinutes = totalMinutes % 60
        return when {
            hours > 0 && remainingMinutes > 0 -> "${hours}h ${remainingMinutes}min"
            hours > 0 -> "${hours}h"
            else -> "${remainingMinutes}min"
        }
    }

    /**
     * Formats integer minutes into a friendly string.
     */
    fun formatMinutesToHoursAndMinutes(totalMinutes: Int): String {
        return formatMinutesToHoursAndMinutes(totalMinutes.toLong())
    }

    /**
     * Formats minutes into a friendly string.
     */
    fun formatMinutes(totalMinutes: Int): String = formatMinutesToHoursAndMinutes(totalMinutes)

    /**
     * Formats long minutes into a friendly string.
     */
    fun formatMinutes(totalMinutes: Long): String = formatMinutesToHoursAndMinutes(totalMinutes)

    /**
     * Validates if a string is a valid time in HH:mm format (00:00 to 23:59).
     */
    fun isValidTimeFormat(timeStr: String): Boolean {
        return timeRegex.matches(timeStr.trim())
    }

    private val timeRegex = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")
}
