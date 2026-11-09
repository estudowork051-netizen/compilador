package com.lifeorganizer.app.core.utils

import com.lifeorganizer.app.domain.model.ExecutionRecordModel
import com.lifeorganizer.app.domain.model.NotificationPreferences
import com.lifeorganizer.app.domain.model.ScheduleEntryModel
import com.lifeorganizer.app.domain.model.ScheduleStatus
import java.time.LocalTime

data class ScheduledReminder(
    val entryId: Long,
    val activityTitle: String,
    val reminderType: ReminderType,
    val triggerTimeMinutes: Int,
    val title: String,
    val message: String,
    val isAdaptiveSuggestion: Boolean = false
)

enum class ReminderType {
    PRE_START,
    COMPLETION_PROMPT,
    POSTPONED_ADAPTIVE_CHECK,
    WELLBEING_PAUSE
}

object AdaptiveReminderEngine {

    fun isInQuietHours(
        hour: Int,
        minute: Int,
        quietHoursEnabled: Boolean,
        quietHoursStart: String,
        quietHoursEnd: String
    ): Boolean {
        if (!quietHoursEnabled) return false

        val currentMinutes = hour * 60 + minute
        val startParts = quietHoursStart.split(":").mapNotNull { it.toIntOrNull() }
        val endParts = quietHoursEnd.split(":").mapNotNull { it.toIntOrNull() }

        val startMinutes = if (startParts.size >= 2) startParts[0] * 60 + startParts[1] else 22 * 60
        val endMinutes = if (endParts.size >= 2) endParts[0] * 60 + endParts[1] else 7 * 60

        return if (startMinutes <= endMinutes) {
            currentMinutes in startMinutes until endMinutes
        } else {
            // Wraps midnight (e.g. 22:00 to 07:00)
            currentMinutes >= startMinutes || currentMinutes < endMinutes
        }
    }

    fun computeRemindersForDay(
        entries: List<ScheduleEntryModel>,
        preferences: NotificationPreferences,
        executionHistory: List<ExecutionRecordModel> = emptyList(),
        nowHour: Int = LocalTime.now().hour,
        nowMinute: Int = LocalTime.now().minute
    ): List<ScheduledReminder> {
        if (!preferences.remindersEnabled) return emptyList()

        val reminders = mutableListOf<ScheduledReminder>()
        val postponedCounts = executionHistory
            .filter { it.finalStatus == ScheduleStatus.POSTPONED }
            .groupingBy { it.activityId }
            .eachCount()

        for (entry in entries) {
            if (entry.status == ScheduleStatus.COMPLETED || entry.status == ScheduleStatus.CANCELLED) {
                continue
            }

            val startMinutes = entry.startHour * 60 + entry.startMinute
            val endMinutes = entry.endHour * 60 + entry.endMinute

            // 1. Pre-start reminder
            if (preferences.notifyBeforeStart) {
                val triggerMin = startMinutes - preferences.notifyBeforeMinutes
                val triggerHour = (triggerMin / 60).coerceIn(0, 23)
                val triggerMinutePart = (triggerMin % 60).coerceIn(0, 59)

                if (!isInQuietHours(
                        triggerHour,
                        triggerMinutePart,
                        preferences.quietHoursEnabled,
                        preferences.quietHoursStart,
                        preferences.quietHoursEnd
                    )
                ) {
                    val message = "Sua atividade \"${entry.activityTitle}\" começa em ${preferences.notifyBeforeMinutes} minutos."
                    reminders.add(
                        ScheduledReminder(
                            entryId = entry.id,
                            activityTitle = entry.activityTitle,
                            reminderType = ReminderType.PRE_START,
                            triggerTimeMinutes = triggerMin,
                            title = "Próxima atividade",
                            message = message
                        )
                    )
                }
            }

            // 2. Completion prompt at end time
            if (preferences.promptCompletionOnEnd) {
                val triggerHour = (endMinutes / 60).coerceIn(0, 23)
                val triggerMinutePart = (endMinutes % 60).coerceIn(0, 59)

                if (!isInQuietHours(
                        triggerHour,
                        triggerMinutePart,
                        preferences.quietHoursEnabled,
                        preferences.quietHoursStart,
                        preferences.quietHoursEnd
                    )
                ) {
                    val message = "Conseguiu terminar \"${entry.activityTitle}\"? Toque para confirmar ou reorganizar."
                    reminders.add(
                        ScheduledReminder(
                            entryId = entry.id,
                            activityTitle = entry.activityTitle,
                            reminderType = ReminderType.COMPLETION_PROMPT,
                            triggerTimeMinutes = endMinutes,
                            title = "Como foi sua atividade?",
                            message = message
                        )
                    )
                }
            }

            // 3. Adaptive check if repeatedly postponed
            val postponeCount = postponedCounts[entry.activityId] ?: 0
            if (preferences.reorganizeSuggestionsEnabled && postponeCount >= 2 && entry.status == ScheduleStatus.POSTPONED) {
                val adaptiveTime = (nowHour * 60 + nowMinute).coerceAtLeast(startMinutes)
                reminders.add(
                    ScheduledReminder(
                        entryId = entry.id,
                        activityTitle = entry.activityTitle,
                        reminderType = ReminderType.POSTPONED_ADAPTIVE_CHECK,
                        triggerTimeMinutes = adaptiveTime,
                        title = "Dica gentil de rotina",
                        message = "Percebemos que \"${entry.activityTitle}\" foi adiada algumas vezes. Que tal dividi-la em etapas menores?",
                        isAdaptiveSuggestion = true
                    )
                )
            }
        }

        return reminders.sortedBy { it.triggerTimeMinutes }
    }
}
