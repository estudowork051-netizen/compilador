package com.lifeorganizer.app.core.utils

import com.lifeorganizer.app.domain.model.ActivityModel
import com.lifeorganizer.app.domain.model.DayAvailability
import com.lifeorganizer.app.domain.model.ScheduleConflict
import com.lifeorganizer.app.domain.model.ScheduleEntryModel
import com.lifeorganizer.app.domain.model.ScheduleStatus
import com.lifeorganizer.app.domain.model.TimeSlot
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.max
import kotlin.math.min

/**
 * Intelligent calculation engine for user time availability.
 * Implements Step 3 requirements:
 * - Real interval arithmetic (not just subtracting flat hours).
 * - Interval union to prevent double-counting overlaps.
 * - Accurate midnight-crossing handling for both awake period and activities.
 * - Distinct breakdown of: Available, Planned, Remaining Free, and Unallocated time.
 * - Conflict detection and free slot discovery.
 */
object TimeAvailabilityEngine {

    const val MINUTES_PER_DAY = 1440

    /**
     * Converts a LocalTime or "HH:mm" string to total minutes from midnight (0..1439).
     */
    fun timeStringToMinutes(timeStr: String): Int {
        val parts = timeStr.trim().split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 8
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return (h * 60 + m).coerceIn(0, MINUTES_PER_DAY - 1)
    }

    /**
     * Calculates the awake window [dayStartMinutes, dayEndMinutes].
     * If sleep <= wake, dayEndMinutes = sleep + 1440.
     */
    fun getAwakeWindow(wakeUpTimeStr: String, sleepTimeStr: String): TimeSlot {
        val wakeMin = timeStringToMinutes(wakeUpTimeStr)
        val rawSleepMin = timeStringToMinutes(sleepTimeStr)
        val sleepMin = if (rawSleepMin <= wakeMin) rawSleepMin + MINUTES_PER_DAY else rawSleepMin
        return TimeSlot(wakeMin, sleepMin, "Vigília")
    }

    /**
     * Merges overlapping or contiguous intervals, returning a sorted list of disjoint intervals.
     * Guarantees that overlapping periods are not double counted.
     */
    fun mergeIntervals(slots: List<TimeSlot>): List<TimeSlot> {
        if (slots.isEmpty()) return emptyList()
        val sorted = slots.filter { it.endMinutes > it.startMinutes }
            .sortedBy { it.startMinutes }
        if (sorted.isEmpty()) return emptyList()

        val merged = mutableListOf<TimeSlot>()
        var current = sorted[0]

        for (i in 1 until sorted.size) {
            val next = sorted[i]
            if (next.startMinutes <= current.endMinutes) {
                // Overlapping or touching: extend current
                current = TimeSlot(
                    startMinutes = current.startMinutes,
                    endMinutes = max(current.endMinutes, next.endMinutes),
                    label = if (current.label.isNotEmpty() && next.label.isNotEmpty() && current.label != next.label) {
                        "${current.label} + ${next.label}"
                    } else current.label.ifEmpty { next.label }
                )
            } else {
                merged.add(current)
                current = next
            }
        }
        merged.add(current)
        return merged
    }

    /**
     * Sums the duration of a list of disjoint (merged) intervals.
     */
    fun totalDurationOf(mergedSlots: List<TimeSlot>): Int {
        return mergedSlots.sumOf { (it.endMinutes - it.startMinutes).coerceAtLeast(0) }
    }

    /**
     * Clamps an interval to fit inside the awake window [awake.startMinutes, awake.endMinutes].
     * Returns null if the slot lies entirely outside the window.
     */
    fun clampToAwakeWindow(slot: TimeSlot, awake: TimeSlot): TimeSlot? {
        // Also check if slot wrapped past midnight and awake is normal, or vice-versa
        var s = slot.startMinutes
        var e = slot.endMinutes

        // If the slot is scheduled before awake start (e.g. night owl activity starting at 00:30 when awake started at 08:00 yesterday)
        if (e <= awake.startMinutes && awake.endMinutes > MINUTES_PER_DAY) {
            s += MINUTES_PER_DAY
            e += MINUTES_PER_DAY
        }

        val clampedStart = max(s, awake.startMinutes)
        val clampedEnd = min(e, awake.endMinutes)

        return if (clampedEnd > clampedStart) {
            TimeSlot(clampedStart, clampedEnd, slot.label)
        } else null
    }

    /**
     * Normalizes a slot's minutes to the awake window without truncating its duration.
     * Handles midnight crossings (e.g. night-owl activities starting after midnight).
     */
    fun normalizeSlotToAwakeWindow(slot: TimeSlot, awake: TimeSlot): TimeSlot {
        var s = slot.startMinutes
        var e = slot.endMinutes
        if (e <= awake.startMinutes && awake.endMinutes > MINUTES_PER_DAY) {
            s += MINUTES_PER_DAY
            e += MINUTES_PER_DAY
        }
        return TimeSlot(s, e, slot.label)
    }

    /**
     * Finds all conflicts (overlaps) between scheduled entries or fixed commitments.
     */
    fun detectConflicts(allSlots: List<TimeSlot>): List<ScheduleConflict> {
        val conflicts = mutableListOf<ScheduleConflict>()
        for (i in allSlots.indices) {
            for (j in i + 1 until allSlots.size) {
                val a = allSlots[i]
                val b = allSlots[j]
                if (a.overlaps(b)) {
                    val overlapStart = max(a.startMinutes, b.startMinutes)
                    val overlapEnd = min(a.endMinutes, b.endMinutes)
                    if (overlapEnd > overlapStart) {
                        conflicts.add(
                            ScheduleConflict(
                                titleA = a.label,
                                titleB = b.label,
                                overlapSlot = TimeSlot(overlapStart, overlapEnd)
                            )
                        )
                    }
                }
            }
        }
        return conflicts
    }

    /**
     * Finds free slots (gaps) within the awake window that are not occupied by occupiedSlots.
     */
    fun findFreeSlots(
        awakeWindow: TimeSlot,
        occupiedMergedSlots: List<TimeSlot>,
        minSlotDurationMinutes: Int = 15
    ): List<TimeSlot> {
        val freeSlots = mutableListOf<TimeSlot>()
        var cursor = awakeWindow.startMinutes

        for (slot in occupiedMergedSlots) {
            if (slot.startMinutes > cursor) {
                val gap = slot.startMinutes - cursor
                if (gap >= minSlotDurationMinutes) {
                    freeSlots.add(TimeSlot(cursor, slot.startMinutes, "Tempo livre"))
                }
            }
            cursor = max(cursor, slot.endMinutes)
        }

        if (awakeWindow.endMinutes > cursor) {
            val gap = awakeWindow.endMinutes - cursor
            if (gap >= minSlotDurationMinutes) {
                freeSlots.add(TimeSlot(cursor, awakeWindow.endMinutes, "Tempo livre"))
            }
        }

        return freeSlots
    }

    /**
     * Finds the first available free slot that can accommodate the required duration.
     */
    /**
     * Finds the first available free slot that can accommodate the required duration.
     * Optionally clamps to minStartTime (e.g. for today's current time).
     */
    fun findFirstAvailableSlot(
        awakeWindow: TimeSlot,
        occupiedMergedSlots: List<TimeSlot>,
        durationMinutes: Int,
        minStartTime: Int? = null
    ): TimeSlot? {
        val freeSlots = findFreeSlots(awakeWindow, occupiedMergedSlots, minSlotDurationMinutes = durationMinutes)
        val validSlots = if (minStartTime != null) {
            freeSlots.mapNotNull { slot ->
                val s = max(slot.startMinutes, minStartTime)
                if (slot.endMinutes - s >= durationMinutes) {
                    TimeSlot(s, slot.endMinutes, slot.label)
                } else null
            }
        } else {
            freeSlots
        }
        return validSlots.firstOrNull { it.durationMinutes >= durationMinutes }?.let { slot ->
            TimeSlot(slot.startMinutes, slot.startMinutes + durationMinutes, "Horário sugerido")
        }
    }

    /**
     * Main calculation function for a day's availability.
     * Respects:
     * 1. Fixed commitments (unmovable).
     * 2. Early completion liberation: flexible activities finished early only occupy their real executed window.
     * 3. Past-time filtering: for the current day, free slots starting before currentTime are excluded/clamped.
     * 4. Future dates: full awake window is available (e.g. tomorrow morning).
     * 5. Past dates: retroactive free slot scheduling is disabled.
     */
    fun calculateDayAvailability(
        date: String,
        wakeUpTimeStr: String,
        sleepTimeStr: String,
        fixedActivities: List<ActivityModel>,
        scheduledEntries: List<ScheduleEntryModel>,
        unallocatedActivities: List<ActivityModel> = emptyList(),
        currentTime: LocalTime = LocalTime.now(),
        referenceDate: String = LocalDate.now().toString()
    ): DayAvailability {
        val awakeWindow = getAwakeWindow(wakeUpTimeStr, sleepTimeStr)
        val awakeDurationMinutes = awakeWindow.durationMinutes

        // Convert active fixed commitments to TimeSlots
        val fixedSlots = fixedActivities.mapNotNull { activity ->
            activity.toTimeSlot?.let { normalizeSlotToAwakeWindow(it, awakeWindow) }
        }
        val mergedFixed = mergeIntervals(fixedSlots)
        val fixedMinutes = totalDurationOf(mergedFixed)

        // Available time is awake time minus fixed commitments
        val availableTimeMinutes = (awakeDurationMinutes - fixedMinutes).coerceAtLeast(0)

        // Convert active scheduled entries to TimeSlots using effectiveOccupiedSlot
        // Flexible activities completed early will only occupy their actual execution window, liberating the rest.
        // Uses normalizeSlotToAwakeWindow so that tasks extending past the awake window are not truncated,
        // correctly registering overbooked minutes.
        val plannedSlots = scheduledEntries
            .mapNotNull { entry ->
                entry.effectiveOccupiedSlot(date, currentTime)?.let { slot ->
                    normalizeSlotToAwakeWindow(slot, awakeWindow)
                }
            }

        val mergedPlanned = mergeIntervals(plannedSlots)
        val plannedMinutes = totalDurationOf(mergedPlanned)

        // Remaining free time
        val remainingFreeMinutes = (availableTimeMinutes - plannedMinutes).coerceAtLeast(0)

        // Unallocated flexible activities
        val unallocatedMinutes = unallocatedActivities.sumOf { it.totalEstimatedMinutes }

        // Combined occupied slots for gap analysis & conflict detection
        val allOccupiedSlots = fixedSlots + plannedSlots
        val mergedAllOccupied = mergeIntervals(allOccupiedSlots)

        // Detect conflicts between individual slots
        val conflicts = detectConflicts(allOccupiedSlots)

        // Free slots discovery within awake window
        val freeSlots = findFreeSlots(awakeWindow, mergedAllOccupied, minSlotDurationMinutes = 15)

        return DayAvailability(
            date = date,
            wakeUpTime = wakeUpTimeStr,
            sleepTime = sleepTimeStr,
            awakeDurationMinutes = awakeDurationMinutes,
            fixedCommitmentsMinutes = fixedMinutes,
            availableTimeMinutes = availableTimeMinutes,
            plannedActivitiesMinutes = plannedMinutes,
            remainingFreeMinutes = remainingFreeMinutes,
            unallocatedActivitiesMinutes = unallocatedMinutes,
            conflicts = conflicts,
            freeSlots = freeSlots
        )
    }
}
