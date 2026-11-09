package com.lifeorganizer.app.domain.model

import com.lifeorganizer.app.core.utils.TimeCalculationUtils

/**
 * Domain model representing a gentle focus session ("Meu momento de foco").
 * Powered by timestamps (not in-memory counters) so that closing/reopening the app
 * never loses accurate progress.
 */
data class FocusSessionModel(
    val id: Long = 0L,
    val scheduleEntryId: Long? = null,
    val activityId: Long,
    val activityTitle: String,
    val category: ActivityCategory = ActivityCategory.OUTROS,
    val plannedDurationMinutes: Int = 30,
    val startTimeMillis: Long = System.currentTimeMillis(),
    val elapsedSecondsBeforePause: Long = 0L,
    val pausedAtMillis: Long? = null,
    val isPaused: Boolean = false,
    val isActive: Boolean = true,
    val completedAtMillis: Long? = null,
    val notes: String = ""
) {
    /**
     * Calculates the real total elapsed seconds up to now or pausedAt.
     */
    fun computeTotalElapsedSeconds(nowMillis: Long = System.currentTimeMillis()): Long {
        if (completedAtMillis != null) {
            val additional = if (startTimeMillis > 0L && completedAtMillis >= startTimeMillis) {
                (completedAtMillis - startTimeMillis) / 1000L
            } else 0L
            return elapsedSecondsBeforePause + additional
        }
        if (!isActive) {
            return elapsedSecondsBeforePause
        }
        return if (isPaused) {
            elapsedSecondsBeforePause
        } else {
            val additionalSeconds = ((nowMillis - startTimeMillis).coerceAtLeast(0L)) / 1000L
            elapsedSecondsBeforePause + additionalSeconds
        }
    }

    /**
     * Remaining seconds based on planned duration.
     */
    fun computeRemainingSeconds(nowMillis: Long = System.currentTimeMillis()): Long {
        val totalPlannedSeconds = plannedDurationMinutes * 60L
        val elapsed = computeTotalElapsedSeconds(nowMillis)
        return (totalPlannedSeconds - elapsed).coerceAtLeast(0L)
    }

    fun isCompletedOrOverdue(nowMillis: Long = System.currentTimeMillis()): Boolean {
        return computeRemainingSeconds(nowMillis) <= 0L
    }

    fun calculateCurrentElapsedSeconds(nowMillis: Long = System.currentTimeMillis()): Long =
        computeTotalElapsedSeconds(nowMillis)

    fun calculateRemainingSeconds(nowMillis: Long = System.currentTimeMillis()): Long =
        computeRemainingSeconds(nowMillis)

    val formattedRemainingTime: String
        get() {
            val remSec = calculateRemainingSeconds()
            val m = remSec / 60
            val s = remSec % 60
            return "%02d:%02d".format(m, s)
        }

    val formattedElapsedTime: String
        get() {
            val elSec = calculateCurrentElapsedSeconds()
            val m = elSec / 60
            val s = elSec % 60
            return "%02d:%02d".format(m, s)
        }

    val progressFraction: Float
        get() {
            val totalSec = (plannedDurationMinutes * 60L).coerceAtLeast(1L)
            val elapsed = calculateCurrentElapsedSeconds()
            return (elapsed.toFloat() / totalSec.toFloat()).coerceIn(0f, 1f)
        }
}
