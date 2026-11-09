package com.lifeorganizer.app

import com.lifeorganizer.app.domain.model.ActivityCategory
import com.lifeorganizer.app.domain.model.FocusSessionModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusSessionModelTest {

    @Test
    fun testFocusSessionElapsedSecondsRunning() {
        val startMillis = 100_000L
        val currentMillis = 160_000L // 60s later

        val session = FocusSessionModel(
            id = 1L,
            activityId = 10L,
            activityTitle = "Estudo de Kotlin",
            category = ActivityCategory.STUDIES,
            plannedDurationMinutes = 25,
            startTimeMillis = startMillis,
            elapsedSecondsBeforePause = 0L,
            isPaused = false,
            isActive = true
        )

        val elapsed = session.computeTotalElapsedSeconds(currentMillis)
        assertEquals(60L, elapsed)

        val remainingSec = session.computeRemainingSeconds(currentMillis)
        assertEquals(25 * 60L - 60L, remainingSec)
        assertFalse(session.isCompletedOrOverdue(currentMillis))
    }

    @Test
    fun testFocusSessionPausedCalculation() {
        val session = FocusSessionModel(
            id = 2L,
            activityId = 20L,
            activityTitle = "Leitura",
            category = ActivityCategory.LEISURE,
            plannedDurationMinutes = 20,
            startTimeMillis = 200_000L,
            elapsedSecondsBeforePause = 120L, // 2 minutes already stored
            isPaused = true,
            isActive = true
        )

        // Even if nowMillis is way in the future, if paused it returns elapsedSecondsBeforePause
        val elapsed = session.computeTotalElapsedSeconds(999_999L)
        assertEquals(120L, elapsed)
    }

    @Test
    fun testFocusSessionOverdue() {
        val startMillis = 100_000L
        val currentMillis = 100_000L + (30 * 60 * 1000L) // 30 mins later for a 25 min planned session

        val session = FocusSessionModel(
            id = 3L,
            activityId = 30L,
            activityTitle = "Programar LifeFlow",
            category = ActivityCategory.WORK,
            plannedDurationMinutes = 25,
            startTimeMillis = startMillis,
            isActive = true
        )

        assertTrue(session.isCompletedOrOverdue(currentMillis))
        assertEquals(0L, session.computeRemainingSeconds(currentMillis))
    }
}
