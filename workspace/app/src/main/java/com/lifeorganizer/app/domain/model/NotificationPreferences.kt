package com.lifeorganizer.app.domain.model

enum class ReminderFrequency(val displayName: String, val description: String) {
    GENTLE("Lembretes suaves", "Apenas avisos essenciais e horários de início"),
    MODERATE("Lembretes moderados", "Avisos de início e confirmação de tarefas pendentes"),
    COMPLETE("Lembretes completos", "Avisos de início, término e confirmação de tarefas pendentes");

    companion object {
        fun fromString(value: String?): ReminderFrequency {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: GENTLE
        }
    }
}

data class NotificationPreferences(
    val remindersEnabled: Boolean = true,
    val notifyBeforeStart: Boolean = true,
    val notifyBeforeMinutes: Int = 15,
    val promptCompletionOnEnd: Boolean = true,
    val remindPendingTasks: Boolean = true,
    val reorganizeSuggestionsEnabled: Boolean = true,
    val quietHoursEnabled: Boolean = true,
    val quietHoursStart: String = "22:00",
    val quietHoursEnd: String = "08:00",
    val reminderFrequency: ReminderFrequency = ReminderFrequency.GENTLE,
    val usageMonitoringConsentGranted: Boolean = false
) {
    /**
     * Checks whether a specific time (hour and minute) falls inside the defined quiet hours window.
     */
    fun isQuietHour(hour: Int, minute: Int): Boolean {
        if (!quietHoursEnabled) return false
        val currentMinutes = hour * 60 + minute
        val startParts = quietHoursStart.split(":").mapNotNull { it.toIntOrNull() }
        val endParts = quietHoursEnd.split(":").mapNotNull { it.toIntOrNull() }
        if (startParts.size < 2 || endParts.size < 2) return false

        val startMin = startParts[0] * 60 + startParts[1]
        val endMin = endParts[0] * 60 + endParts[1]

        return if (startMin <= endMin) {
            currentMinutes in startMin..endMin
        } else {
            // Crosses midnight, e.g. 22:00 to 08:00
            currentMinutes >= startMin || currentMinutes < endMin
        }
    }
}
