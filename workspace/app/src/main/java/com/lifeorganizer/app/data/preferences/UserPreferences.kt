package com.lifeorganizer.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

object UserPreferenceKeys {
    val USER_NAME = stringPreferencesKey("user_name")
    val WAKE_UP_TIME = stringPreferencesKey("wake_up_time")
    val SLEEP_TIME = stringPreferencesKey("sleep_time")
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val ORGANIZATION_MODE = stringPreferencesKey("organization_mode")
    val FIXED_COMMITMENTS = stringSetPreferencesKey("fixed_commitments")
    val BALANCE_PREFERENCE = stringPreferencesKey("balance_preference")
    val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    val ONBOARDING_STEP = intPreferencesKey("onboarding_step")

    // Step 5: Intelligence and Habit Learning Controls
    val LEARNING_ENABLED = booleanPreferencesKey("learning_enabled")
    val PERSONALIZED_RECOMMENDATIONS_ENABLED = booleanPreferencesKey("personalized_recommendations_enabled")
    val SHOW_EXPLANATIONS_ENABLED = booleanPreferencesKey("show_explanations_enabled")
    val DISALLOWED_NIGHT_CATEGORIES = stringSetPreferencesKey("disallowed_night_categories")

    // Step 6: Reminders and Tranquility Preferences
    val REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
    val NOTIFY_BEFORE_START = booleanPreferencesKey("notify_before_start")
    val NOTIFY_BEFORE_MINUTES = intPreferencesKey("notify_before_minutes")
    val PROMPT_COMPLETION_ON_END = booleanPreferencesKey("prompt_completion_on_end")
    val REMIND_PENDING_TASKS = booleanPreferencesKey("remind_pending_tasks")
    val REORGANIZE_SUGGESTIONS_ENABLED = booleanPreferencesKey("reorganize_suggestions_enabled")
    val QUIET_HOURS_ENABLED = booleanPreferencesKey("quiet_hours_enabled")
    val QUIET_HOURS_START = stringPreferencesKey("quiet_hours_start")
    val QUIET_HOURS_END = stringPreferencesKey("quiet_hours_end")
    val REMINDER_FREQUENCY = stringPreferencesKey("reminder_frequency")
    val USAGE_MONITORING_CONSENT_GRANTED = booleanPreferencesKey("usage_monitoring_consent_granted")
}
