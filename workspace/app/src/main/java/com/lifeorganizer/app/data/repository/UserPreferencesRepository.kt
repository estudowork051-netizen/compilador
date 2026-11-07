package com.lifeorganizer.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.lifeorganizer.app.data.preferences.UserPreferenceKeys
import com.lifeorganizer.app.domain.model.BalancePreference
import com.lifeorganizer.app.domain.model.OrganizationMode
import com.lifeorganizer.app.domain.model.ReminderFrequency
import com.lifeorganizer.app.domain.model.ThemeMode
import com.lifeorganizer.app.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

class UserPreferencesRepository(
    private val dataStore: DataStore<Preferences>
) {

    val userProfileFlow: Flow<UserProfile> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val name = preferences[UserPreferenceKeys.USER_NAME] ?: ""
            val wakeUpTime = preferences[UserPreferenceKeys.WAKE_UP_TIME] ?: "08:00"
            val sleepTime = preferences[UserPreferenceKeys.SLEEP_TIME] ?: "00:00"
            val commitments = preferences[UserPreferenceKeys.FIXED_COMMITMENTS] ?: emptySet()
            val themeString = preferences[UserPreferenceKeys.THEME_MODE]
            val orgString = preferences[UserPreferenceKeys.ORGANIZATION_MODE]
            val balanceString = preferences[UserPreferenceKeys.BALANCE_PREFERENCE]
            val completed = preferences[UserPreferenceKeys.ONBOARDING_COMPLETED] ?: false
            val step = preferences[UserPreferenceKeys.ONBOARDING_STEP] ?: 1

            val learningEnabled = preferences[UserPreferenceKeys.LEARNING_ENABLED] ?: true
            val personalizedRecommendations = preferences[UserPreferenceKeys.PERSONALIZED_RECOMMENDATIONS_ENABLED] ?: true
            val showExplanations = preferences[UserPreferenceKeys.SHOW_EXPLANATIONS_ENABLED] ?: true
            val disallowedNight = preferences[UserPreferenceKeys.DISALLOWED_NIGHT_CATEGORIES] ?: emptySet()

            val remindersEnabled = preferences[UserPreferenceKeys.REMINDERS_ENABLED] ?: true
            val notifyBeforeStart = preferences[UserPreferenceKeys.NOTIFY_BEFORE_START] ?: true
            val notifyBeforeMinutes = preferences[UserPreferenceKeys.NOTIFY_BEFORE_MINUTES] ?: 15
            val promptCompletionOnEnd = preferences[UserPreferenceKeys.PROMPT_COMPLETION_ON_END] ?: true
            val remindPendingTasks = preferences[UserPreferenceKeys.REMIND_PENDING_TASKS] ?: true
            val reorganizeSuggestions = preferences[UserPreferenceKeys.REORGANIZE_SUGGESTIONS_ENABLED] ?: true
            val quietHoursEnabled = preferences[UserPreferenceKeys.QUIET_HOURS_ENABLED] ?: true
            val quietHoursStart = preferences[UserPreferenceKeys.QUIET_HOURS_START] ?: "22:00"
            val quietHoursEnd = preferences[UserPreferenceKeys.QUIET_HOURS_END] ?: "08:00"
            val reminderFreqString = preferences[UserPreferenceKeys.REMINDER_FREQUENCY]
            val usageMonitoringConsent = preferences[UserPreferenceKeys.USAGE_MONITORING_CONSENT_GRANTED] ?: false

            UserProfile(
                name = name,
                wakeUpTime = wakeUpTime,
                sleepTime = sleepTime,
                fixedCommitments = commitments,
                themeMode = ThemeMode.fromString(themeString),
                organizationMode = OrganizationMode.fromString(orgString),
                balancePreference = BalancePreference.fromString(balanceString),
                isOnboardingCompleted = completed,
                onboardingStep = step,
                learningEnabled = learningEnabled,
                personalizedRecommendationsEnabled = personalizedRecommendations,
                showExplanationsEnabled = showExplanations,
                disallowedNightCategories = disallowedNight,
                remindersEnabled = remindersEnabled,
                notifyBeforeStart = notifyBeforeStart,
                notifyBeforeMinutes = notifyBeforeMinutes,
                promptCompletionOnEnd = promptCompletionOnEnd,
                remindPendingTasks = remindPendingTasks,
                reorganizeSuggestionsEnabled = reorganizeSuggestions,
                quietHoursEnabled = quietHoursEnabled,
                quietHoursStart = quietHoursStart,
                quietHoursEnd = quietHoursEnd,
                reminderFrequency = ReminderFrequency.fromString(reminderFreqString),
                usageMonitoringConsentGranted = usageMonitoringConsent
            )
        }

    val isOnboardingCompletedFlow: Flow<Boolean> = userProfileFlow.map { it.isOnboardingCompleted }

    val themeModeFlow: Flow<ThemeMode> = userProfileFlow.map { it.themeMode }

    suspend fun updateName(name: String) {
        dataStore.edit { preferences ->
            preferences[UserPreferenceKeys.USER_NAME] = name.trim()
        }
    }

    suspend fun updateWakeUpTime(time: String) {
        dataStore.edit { preferences ->
            preferences[UserPreferenceKeys.WAKE_UP_TIME] = time.trim()
        }
    }

    suspend fun updateSleepTime(time: String) {
        dataStore.edit { preferences ->
            preferences[UserPreferenceKeys.SLEEP_TIME] = time.trim()
        }
    }

    suspend fun updateThemeMode(theme: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[UserPreferenceKeys.THEME_MODE] = theme.name
        }
    }

    suspend fun updateOrganizationMode(mode: OrganizationMode) {
        dataStore.edit { preferences ->
            preferences[UserPreferenceKeys.ORGANIZATION_MODE] = mode.name
        }
    }

    suspend fun updateBalancePreference(preference: BalancePreference) {
        dataStore.edit { preferences ->
            preferences[UserPreferenceKeys.BALANCE_PREFERENCE] = preference.name
        }
    }

    suspend fun updateLearningEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[UserPreferenceKeys.LEARNING_ENABLED] = enabled
        }
    }

    suspend fun updatePersonalizedRecommendations(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[UserPreferenceKeys.PERSONALIZED_RECOMMENDATIONS_ENABLED] = enabled
        }
    }

    suspend fun updateShowExplanations(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[UserPreferenceKeys.SHOW_EXPLANATIONS_ENABLED] = enabled
        }
    }

    suspend fun updateDisallowedNightCategories(categories: Set<String>) {
        dataStore.edit { preferences ->
            preferences[UserPreferenceKeys.DISALLOWED_NIGHT_CATEGORIES] = categories
        }
    }

    suspend fun updateCommitments(commitments: Set<String>) {
        dataStore.edit { preferences ->
            preferences[UserPreferenceKeys.FIXED_COMMITMENTS] = commitments
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[UserPreferenceKeys.ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun setOnboardingStep(step: Int) {
        dataStore.edit { preferences ->
            preferences[UserPreferenceKeys.ONBOARDING_STEP] = step
        }
    }

    suspend fun saveOnboardingData(
        name: String,
        wakeUpTime: String,
        sleepTime: String,
        commitments: Set<String>,
        orgMode: OrganizationMode
    ) {
        dataStore.edit { preferences ->
            preferences[UserPreferenceKeys.USER_NAME] = name.trim()
            preferences[UserPreferenceKeys.WAKE_UP_TIME] = wakeUpTime.trim()
            preferences[UserPreferenceKeys.SLEEP_TIME] = sleepTime.trim()
            preferences[UserPreferenceKeys.FIXED_COMMITMENTS] = commitments
            preferences[UserPreferenceKeys.ORGANIZATION_MODE] = orgMode.name
            preferences[UserPreferenceKeys.ONBOARDING_COMPLETED] = true
        }
    }

    suspend fun updateRemindersEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[UserPreferenceKeys.REMINDERS_ENABLED] = enabled
        }
    }

    suspend fun updateNotifyBeforeStart(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[UserPreferenceKeys.NOTIFY_BEFORE_START] = enabled
        }
    }

    suspend fun updateNotifyBeforeMinutes(minutes: Int) {
        dataStore.edit { preferences ->
            preferences[UserPreferenceKeys.NOTIFY_BEFORE_MINUTES] = minutes
        }
    }

    suspend fun updatePromptCompletionOnEnd(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[UserPreferenceKeys.PROMPT_COMPLETION_ON_END] = enabled
        }
    }

    suspend fun updateRemindPendingTasks(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[UserPreferenceKeys.REMIND_PENDING_TASKS] = enabled
        }
    }

    suspend fun updateReorganizeSuggestionsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[UserPreferenceKeys.REORGANIZE_SUGGESTIONS_ENABLED] = enabled
        }
    }

    suspend fun updateQuietHoursEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[UserPreferenceKeys.QUIET_HOURS_ENABLED] = enabled
        }
    }

    suspend fun updateQuietHours(start: String, end: String) {
        dataStore.edit { preferences ->
            preferences[UserPreferenceKeys.QUIET_HOURS_START] = start.trim()
            preferences[UserPreferenceKeys.QUIET_HOURS_END] = end.trim()
        }
    }

    suspend fun updateReminderFrequency(frequency: ReminderFrequency) {
        dataStore.edit { preferences ->
            preferences[UserPreferenceKeys.REMINDER_FREQUENCY] = frequency.name
        }
    }

    suspend fun updateUsageMonitoringConsent(granted: Boolean) {
        dataStore.edit { preferences ->
            preferences[UserPreferenceKeys.USAGE_MONITORING_CONSENT_GRANTED] = granted
        }
    }
}
