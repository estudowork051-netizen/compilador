package com.lifeorganizer.app.core.utils

import com.lifeorganizer.app.domain.model.ActivityModel
import com.lifeorganizer.app.domain.model.ScheduleEntryModel

data class ReorderCalculationResult(
    val updatedEntries: List<ScheduleEntryModel>,
    val conflictWarning: String? = null
)

/**
 * Intelligent engine that recalculates scheduled sequence times when an activity is moved,
 * honoring fixed commitments and waking hours without overlapping.
 */
object ScheduleReorderEngine {

    fun reorderAndRecalculate(
        entries: List<ScheduleEntryModel>,
        fromIndex: Int,
        toIndex: Int,
        fixedCommitments: List<ActivityModel>,
        wakeUpTimeMinutes: Int = 8 * 60,
        sleepTimeMinutes: Int = 24 * 60
    ): ReorderCalculationResult {
        if (fromIndex !in entries.indices || toIndex !in entries.indices || fromIndex == toIndex) {
            return ReorderCalculationResult(entries, null)
        }

        val mutableList = entries.toMutableList()
        val movedItem = mutableList.removeAt(fromIndex)
        mutableList.add(toIndex, movedItem)

        // Parse fixed commitment intervals in minutes for the day
        val fixedIntervals: List<Pair<Int, Int>> = fixedCommitments.mapNotNull { fixed ->
            val slot = fixed.toTimeSlot ?: return@mapNotNull null
            Pair(slot.startMinutes, slot.endMinutes)
        }.sortedBy { it.first }

        // Start time cursor starts at the earliest entry's scheduled time, or wakeUpTime
        val earliestStart = entries.minOfOrNull { it.startTotalMinutes } ?: wakeUpTimeMinutes
        var currentCursor = maxOf(earliestStart, wakeUpTimeMinutes)
        var warning: String? = null

        val recalculatedList = mutableListOf<ScheduleEntryModel>()

        for (entry in mutableList) {
            if (entry.status == com.lifeorganizer.app.domain.model.ScheduleStatus.COMPLETED) {
                recalculatedList.add(entry)
                currentCursor = maxOf(currentCursor, entry.endTotalMinutes + 5)
                continue
            }

            val duration = entry.targetDurationMinutes.coerceAtLeast(10)

            // Adjust cursor past any overlapping fixed commitment
            var slotValid = false
            var attempts = 0
            while (!slotValid && attempts < 20) {
                attempts++
                val slotStart = currentCursor
                val slotEnd = slotStart + duration

                val conflictingFixed = fixedIntervals.firstOrNull { (fStart, fEnd) ->
                    // Overlaps if slotStart < fEnd and slotEnd > fStart
                    slotStart < fEnd && slotEnd > fStart
                }

                if (conflictingFixed != null) {
                    // Push after the conflicting fixed commitment
                    currentCursor = conflictingFixed.second
                } else {
                    slotValid = true
                }
            }

            val finalStartMinutes = currentCursor
            val finalEndMinutes = currentCursor + duration

            val startH = (finalStartMinutes / 60) % 24
            val startM = finalStartMinutes % 60
            val endH = (finalEndMinutes / 60) % 24
            val endM = finalEndMinutes % 60

            val updatedEntry = entry.copy(
                startHour = startH,
                startMinute = startM,
                endHour = endH,
                endMinute = endM
            )
            recalculatedList.add(updatedEntry)

            if (finalEndMinutes > sleepTimeMinutes && warning == null) {
                val actTitle = entry.activity?.title ?: entry.activityTitle.ifBlank { "planejada" }
                warning = "Aviso: a atividade '$actTitle' termina após seu horário de dormir."
            }

            // Natural 5-minute pause before next flexible task
            currentCursor = finalEndMinutes + 5
        }

        return ReorderCalculationResult(recalculatedList, warning)
    }
}
