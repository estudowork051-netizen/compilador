package com.lifeorganizer.app.domain.model

data class UserProfile(
    val name: String = "",
    val wakeUpTime: String = "08:00",
    val sleepTime: String = "00:00",
    val fixedCommitments: Set<String> = emptySet(),
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val organizationMode: OrganizationMode = OrganizationMode.MANUAL,
    val balancePreference: BalancePreference = BalancePreference.BALANCED,
    val isOnboardingCompleted: Boolean = false,
    val onboardingStep: Int = 1,
    val learningEnabled: Boolean = true,
    val personalizedRecommendationsEnabled: Boolean = true,
    val showExplanationsEnabled: Boolean = true,
    val disallowedNightCategories: Set<String> = emptySet(),
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
    val notificationPreferences: NotificationPreferences
        get() = NotificationPreferences(
            remindersEnabled = remindersEnabled,
            notifyBeforeStart = notifyBeforeStart,
            notifyBeforeMinutes = notifyBeforeMinutes,
            promptCompletionOnEnd = promptCompletionOnEnd,
            remindPendingTasks = remindPendingTasks,
            reorganizeSuggestionsEnabled = reorganizeSuggestionsEnabled,
            quietHoursEnabled = quietHoursEnabled,
            quietHoursStart = quietHoursStart,
            quietHoursEnd = quietHoursEnd,
            reminderFrequency = reminderFrequency,
            usageMonitoringConsentGranted = usageMonitoringConsentGranted
        )
}
