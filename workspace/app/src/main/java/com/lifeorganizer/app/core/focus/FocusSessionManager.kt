package com.lifeorganizer.app.core.focus

import com.lifeorganizer.app.data.repository.FocusRepository
import com.lifeorganizer.app.data.repository.ScheduleRepository
import com.lifeorganizer.app.domain.model.ActivityCategory
import com.lifeorganizer.app.domain.model.FocusSessionModel
import com.lifeorganizer.app.domain.model.ScheduleEntryModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FocusSessionManager(
    private val focusRepository: FocusRepository,
    private val scheduleRepository: ScheduleRepository,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {

    val activeSession: StateFlow<FocusSessionModel?> = focusRepository
        .getActiveSessionFlow()
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    suspend fun startFocus(
        activityId: Long,
        activityTitle: String,
        category: ActivityCategory,
        plannedDurationMinutes: Int,
        scheduleEntryId: Long? = null
    ): Long {
        // Deactivate previous active session if any
        val current = focusRepository.getActiveSessionOnce()
        if (current != null) {
            concludeSession(current.id, "Encerrado para iniciar nova sessão")
        }

        val session = FocusSessionModel(
            scheduleEntryId = scheduleEntryId,
            activityId = activityId,
            activityTitle = activityTitle,
            category = category,
            plannedDurationMinutes = plannedDurationMinutes.coerceAtLeast(5),
            startTimeMillis = System.currentTimeMillis(),
            elapsedSecondsBeforePause = 0L,
            pausedAtMillis = null,
            isPaused = false,
            isActive = true
        )
        return focusRepository.startSession(session)
    }

    suspend fun pauseSession(sessionId: Long) {
        val current = focusRepository.getActiveSessionOnce() ?: return
        if (current.id != sessionId || current.isPaused) return

        val now = System.currentTimeMillis()
        val runningElapsed = (now - current.startTimeMillis) / 1000L
        val totalElapsed = current.elapsedSecondsBeforePause + runningElapsed.coerceAtLeast(0L)

        val updated = current.copy(
            isPaused = true,
            pausedAtMillis = now,
            elapsedSecondsBeforePause = totalElapsed
        )
        focusRepository.updateSession(updated)
    }

    suspend fun resumeSession(sessionId: Long) {
        val current = focusRepository.getActiveSessionOnce() ?: return
        if (current.id != sessionId || !current.isPaused) return

        val now = System.currentTimeMillis()
        val updated = current.copy(
            isPaused = false,
            pausedAtMillis = null,
            startTimeMillis = now
        )
        focusRepository.updateSession(updated)
    }

    suspend fun extendSession(sessionId: Long, additionalMinutes: Int) {
        val current = focusRepository.getActiveSessionOnce() ?: return
        if (current.id != sessionId) return

        val newPlannedDuration = current.plannedDurationMinutes + additionalMinutes.coerceAtLeast(1)
        val updated = current.copy(
            plannedDurationMinutes = newPlannedDuration
        )
        focusRepository.updateSession(updated)
    }

    suspend fun concludeSession(sessionId: Long, notes: String = "") {
        val current = focusRepository.getActiveSessionOnce() ?: return
        if (current.id != sessionId) return

        val now = System.currentTimeMillis()
        val totalElapsedSeconds = current.computeTotalElapsedSeconds(now)
        val actualMinutes = (totalElapsedSeconds / 60L).toInt().coerceAtLeast(1)

        val completed = current.copy(
            isActive = false,
            isPaused = false,
            completedAtMillis = now,
            notes = notes
        )
        focusRepository.updateSession(completed)

        // If connected to a schedule entry, mark it completed and record execution
        current.scheduleEntryId?.let { entryId ->
            try {
                scheduleRepository.markCompleted(
                    entryId = entryId,
                    actualDurationMinutes = actualMinutes,
                    notes = notes
                )
            } catch (e: Exception) {
                // Ignore if entry already resolved
            }
        }
    }

    suspend fun cancelSession(sessionId: Long) {
        focusRepository.deactivateSession(sessionId)
    }
}
