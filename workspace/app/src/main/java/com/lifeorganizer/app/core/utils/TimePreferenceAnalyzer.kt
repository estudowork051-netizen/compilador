package com.lifeorganizer.app.core.utils

import com.lifeorganizer.app.domain.model.ConfidenceLevel
import com.lifeorganizer.app.domain.model.ExecutionRecordModel
import com.lifeorganizer.app.domain.model.ScheduleStatus
import com.lifeorganizer.app.domain.model.TimeSlotPreference

object TimePreferenceAnalyzer {

    data class SlotDefinition(
        val name: String,
        val startHour: Int,
        val endHour: Int
    ) {
        val startMinutes: Int get() = startHour * 60
        val endMinutes: Int get() = if (endHour == 0) 24 * 60 else endHour * 60

        fun contains(minutes: Int): Boolean {
            val normalized = (minutes % 1440 + 1440) % 1440
            return if (startMinutes < endMinutes) {
                normalized in startMinutes until endMinutes
            } else {
                // crosses midnight e.g. 21h to 00h or 00h to 06h
                normalized >= startMinutes || normalized < endMinutes
            }
        }
    }

    val STANDARD_SLOTS = listOf(
        SlotDefinition("Madrugada (00h-06h)", 0, 6),
        SlotDefinition("Manhã cedo (06h-09h)", 6, 9),
        SlotDefinition("Manhã (09h-12h)", 9, 12),
        SlotDefinition("Início da tarde (12h-15h)", 12, 15),
        SlotDefinition("Tarde (15h-18h)", 15, 18),
        SlotDefinition("Noite (18h-21h)", 18, 21),
        SlotDefinition("Fim da noite (21h-00h)", 21, 0)
    )

    const val MIN_OBSERVATIONS_FOR_TREND = 5
    const val MIN_OBSERVATIONS_FOR_INITIAL_HINT = 3

    /**
     * Analyzes execution records and computes completion rate per time slot for an activity.
     */
    fun analyzeActivityTimeSlots(
        activityId: Long,
        activityTitle: String,
        records: List<ExecutionRecordModel>
    ): List<TimeSlotPreference> {
        val relevantRecords = records.filter { it.activityId == activityId }
        if (relevantRecords.isEmpty()) return emptyList()

        val results = mutableListOf<TimeSlotPreference>()

        for (slot in STANDARD_SLOTS) {
            val slotRecords = relevantRecords.filter { slot.contains(it.plannedStartMinutes) }
            val completed = slotRecords.count { it.finalStatus == ScheduleStatus.COMPLETED }
            val notDone = slotRecords.count { it.finalStatus == ScheduleStatus.NOT_DONE || it.finalStatus == ScheduleStatus.CANCELLED }
            val postponed = slotRecords.count { it.finalStatus == ScheduleStatus.POSTPONED }
            val totalAttempts = completed + notDone + postponed

            if (totalAttempts == 0) continue

            val completionRate = completed.toFloat() / totalAttempts

            val confidence = when {
                totalAttempts < MIN_OBSERVATIONS_FOR_INITIAL_HINT -> ConfidenceLevel.INSUFFICIENT_DATA
                totalAttempts < MIN_OBSERVATIONS_FOR_TREND -> ConfidenceLevel.LOW
                totalAttempts < 10 && completionRate >= 0.6f -> ConfidenceLevel.MODERATE
                totalAttempts >= 10 && completionRate >= 0.7f -> ConfidenceLevel.HIGH
                else -> ConfidenceLevel.LOW
            }

            val reason = when (confidence) {
                ConfidenceLevel.HIGH -> "Taxa de conclusão de ${(completionRate * 100).toInt()}% em $totalAttempts registros analisados."
                ConfidenceLevel.MODERATE -> "Maior taxa observada (${(completionRate * 100).toInt()}%) com $totalAttempts execuções."
                ConfidenceLevel.LOW -> "Tendência inicial (${(completionRate * 100).toInt()}%, $totalAttempts tentativas)."
                ConfidenceLevel.INSUFFICIENT_DATA -> "Apenas $totalAttempts registro(s). Histórico insuficiente para conclusões."
            }

            results.add(
                TimeSlotPreference(
                    activityId = activityId,
                    activityTitle = activityTitle,
                    timeSlotName = slot.name,
                    startHour = slot.startHour,
                    endHour = slot.endHour,
                    completedCount = completed,
                    totalAttempts = totalAttempts,
                    completionRate = completionRate,
                    confidence = confidence,
                    reason = reason
                )
            )
        }

        return results.sortedByDescending { it.completionRate }
    }

    /**
     * Finds the single best performing time slot with at least moderate confidence.
     */
    fun findBestTimeSlot(
        activityId: Long,
        activityTitle: String,
        records: List<ExecutionRecordModel>
    ): TimeSlotPreference? {
        val preferences = analyzeActivityTimeSlots(activityId, activityTitle, records)
        // Require at least MIN_OBSERVATIONS_FOR_TREND and completion rate >= 60%
        return preferences.firstOrNull {
            it.totalAttempts >= MIN_OBSERVATIONS_FOR_TREND && it.completionRate >= 0.6f
        } ?: preferences.firstOrNull {
            it.totalAttempts >= MIN_OBSERVATIONS_FOR_INITIAL_HINT && it.completionRate >= 0.66f
        }
    }

    /**
     * Finds the best start/end minutes for an activity based on completed records.
     */
    fun findBestTimeSlotForActivity(
        activityId: Long,
        records: List<ExecutionRecordModel>
    ): Pair<Int, Int>? {
        val completed = records.filter { it.activityId == activityId && it.finalStatus == ScheduleStatus.COMPLETED }
        if (completed.isEmpty()) return null
        val bestRecord = completed.groupBy { it.plannedStartMinutes }
            .maxByOrNull { it.value.size }?.value?.firstOrNull() ?: completed.first()
        val duration = bestRecord.plannedDurationMinutes
        return Pair(bestRecord.plannedStartMinutes, bestRecord.plannedStartMinutes + duration)
    }
}
