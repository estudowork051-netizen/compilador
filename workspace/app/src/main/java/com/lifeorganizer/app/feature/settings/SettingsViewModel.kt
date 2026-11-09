package com.lifeorganizer.app.feature.settings

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lifeorganizer.app.core.analytics.UsageMonitoringService
import com.lifeorganizer.app.data.repository.ActivityRepository
import com.lifeorganizer.app.data.repository.DeviceUsageRepository
import com.lifeorganizer.app.data.repository.LearningRepository
import com.lifeorganizer.app.data.repository.UserPreferencesRepository
import com.lifeorganizer.app.domain.model.BalancePreference
import com.lifeorganizer.app.domain.model.OrganizationMode
import com.lifeorganizer.app.domain.model.ReminderFrequency
import com.lifeorganizer.app.domain.model.ThemeMode
import com.lifeorganizer.app.domain.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class SettingsUiState(
    val name: String = "",
    val wakeUpTime: String = "08:00",
    val sleepTime: String = "00:00",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val organizationMode: OrganizationMode = OrganizationMode.MANUAL,
    val balancePreference: BalancePreference = BalancePreference.BALANCED,
    val learningEnabled: Boolean = true,
    val personalizedRecommendationsEnabled: Boolean = true,
    val showExplanationsEnabled: Boolean = true,
    val remindersEnabled: Boolean = true,
    val notifyBeforeStart: Boolean = true,
    val notifyBeforeMinutes: Int = 15,
    val promptCompletionOnEnd: Boolean = true,
    val remindPendingTasks: Boolean = true,
    val reorganizeSuggestionsEnabled: Boolean = true,
    val quietHoursEnabled: Boolean = true,
    val quietHoursStart: String = "22:00",
    val quietHoursEnd: String = "07:00",
    val reminderFrequency: ReminderFrequency = ReminderFrequency.MODERATE,
    val usageMonitoringConsentGranted: Boolean = false,
    val isUsagePermissionGranted: Boolean = false,
    val showClearLearningDialog: Boolean = false,
    val showRestoreDurationsDialog: Boolean = false,
    val feedbackMessage: String? = null,
    val appVersion: String = "1.0.0 (Viora)",
    val appName: String = "Viora — Minha Rotina"
)

class SettingsViewModel(
    private val preferencesRepository: UserPreferencesRepository,
    private val learningRepository: LearningRepository,
    private val activityRepository: ActivityRepository,
    private val deviceUsageRepository: DeviceUsageRepository? = null,
    private val usageMonitoringService: UsageMonitoringService? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        preferencesRepository.userProfileFlow
            .onEach { profile: UserProfile ->
                val permGranted = usageMonitoringService?.isUsageAccessGranted() ?: false
                _uiState.value = _uiState.value.copy(
                    name = profile.name,
                    wakeUpTime = profile.wakeUpTime,
                    sleepTime = profile.sleepTime,
                    themeMode = profile.themeMode,
                    organizationMode = profile.organizationMode,
                    balancePreference = profile.balancePreference,
                    learningEnabled = profile.learningEnabled,
                    personalizedRecommendationsEnabled = profile.personalizedRecommendationsEnabled,
                    showExplanationsEnabled = profile.showExplanationsEnabled,
                    remindersEnabled = profile.remindersEnabled,
                    notifyBeforeStart = profile.notifyBeforeStart,
                    notifyBeforeMinutes = profile.notifyBeforeMinutes,
                    promptCompletionOnEnd = profile.promptCompletionOnEnd,
                    remindPendingTasks = profile.remindPendingTasks,
                    reorganizeSuggestionsEnabled = profile.reorganizeSuggestionsEnabled,
                    quietHoursEnabled = profile.quietHoursEnabled,
                    quietHoursStart = profile.quietHoursStart,
                    quietHoursEnd = profile.quietHoursEnd,
                    reminderFrequency = profile.reminderFrequency,
                    usageMonitoringConsentGranted = profile.usageMonitoringConsentGranted,
                    isUsagePermissionGranted = permGranted
                )
            }
            .launchIn(viewModelScope)
    }

    fun checkUsagePermission() {
        val granted = usageMonitoringService?.isUsageAccessGranted() ?: false
        _uiState.value = _uiState.value.copy(isUsagePermissionGranted = granted)
    }

    fun getUsageAccessIntent(): Intent? {
        return usageMonitoringService?.createUsageAccessIntent()
    }

    // --- Lembretes e Tranquilidade (Etapa 6) ---

    fun updateRemindersEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(remindersEnabled = enabled)
        viewModelScope.launch { preferencesRepository.updateRemindersEnabled(enabled) }
    }

    fun updateNotifyBeforeStart(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(notifyBeforeStart = enabled)
        viewModelScope.launch { preferencesRepository.updateNotifyBeforeStart(enabled) }
    }

    fun updateNotifyBeforeMinutes(minutes: Int) {
        _uiState.value = _uiState.value.copy(notifyBeforeMinutes = minutes)
        viewModelScope.launch { preferencesRepository.updateNotifyBeforeMinutes(minutes) }
    }

    fun updatePromptCompletionOnEnd(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(promptCompletionOnEnd = enabled)
        viewModelScope.launch { preferencesRepository.updatePromptCompletionOnEnd(enabled) }
    }

    fun updateRemindPendingTasks(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(remindPendingTasks = enabled)
        viewModelScope.launch { preferencesRepository.updateRemindPendingTasks(enabled) }
    }

    fun updateReorganizeSuggestionsEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(reorganizeSuggestionsEnabled = enabled)
        viewModelScope.launch { preferencesRepository.updateReorganizeSuggestionsEnabled(enabled) }
    }

    fun updateQuietHoursEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(quietHoursEnabled = enabled)
        viewModelScope.launch { preferencesRepository.updateQuietHoursEnabled(enabled) }
    }

    fun updateQuietHours(start: String, end: String) {
        _uiState.value = _uiState.value.copy(quietHoursStart = start, quietHoursEnd = end)
        viewModelScope.launch { preferencesRepository.updateQuietHours(start, end) }
    }

    fun updateReminderFrequency(frequency: ReminderFrequency) {
        _uiState.value = _uiState.value.copy(reminderFrequency = frequency)
        viewModelScope.launch { preferencesRepository.updateReminderFrequency(frequency) }
    }

    fun updateUsageMonitoringConsent(granted: Boolean) {
        _uiState.value = _uiState.value.copy(usageMonitoringConsentGranted = granted)
        viewModelScope.launch { preferencesRepository.updateUsageMonitoringConsent(granted) }
    }

    // --- Outras Configurações ---

    fun updateBalancePreference(pref: BalancePreference) {
        _uiState.value = _uiState.value.copy(balancePreference = pref)
        viewModelScope.launch { preferencesRepository.updateBalancePreference(pref) }
    }

    fun updateLearningEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(learningEnabled = enabled)
        viewModelScope.launch { preferencesRepository.updateLearningEnabled(enabled) }
    }

    fun updatePersonalizedRecommendations(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(personalizedRecommendationsEnabled = enabled)
        viewModelScope.launch { preferencesRepository.updatePersonalizedRecommendations(enabled) }
    }

    fun updateShowExplanations(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(showExplanationsEnabled = enabled)
        viewModelScope.launch { preferencesRepository.updateShowExplanations(enabled) }
    }

    fun openClearLearningDialog() {
        _uiState.value = _uiState.value.copy(showClearLearningDialog = true)
    }

    fun closeClearLearningDialog() {
        _uiState.value = _uiState.value.copy(showClearLearningDialog = false)
    }

    fun clearLearningData() {
        viewModelScope.launch {
            learningRepository.clearLearningData()
            _uiState.value = _uiState.value.copy(
                showClearLearningDialog = false,
                feedbackMessage = "Dados de histórico e aprendizado foram redefinidos com sucesso. Suas tarefas continuam salvas."
            )
        }
    }

    fun openRestoreDurationsDialog() {
        _uiState.value = _uiState.value.copy(showRestoreDurationsDialog = true)
    }

    fun closeRestoreDurationsDialog() {
        _uiState.value = _uiState.value.copy(showRestoreDurationsDialog = false)
    }

    fun restoreDefaultSuggestedDurations() {
        viewModelScope.launch {
            val activities = activityRepository.allActivities.first()
            learningRepository.restoreDefaultSuggestedDurations(activities)
            _uiState.value = _uiState.value.copy(
                showRestoreDurationsDialog = false,
                feedbackMessage = "Durações restauradas para os valores padrão."
            )
        }
    }

    fun clearFeedbackMessage() {
        _uiState.value = _uiState.value.copy(feedbackMessage = null)
    }

    fun updateName(name: String) {
        _uiState.value = _uiState.value.copy(name = name)
        viewModelScope.launch { preferencesRepository.updateName(name) }
    }

    fun updateWakeUpTime(time: String) {
        _uiState.value = _uiState.value.copy(wakeUpTime = time)
        viewModelScope.launch { preferencesRepository.updateWakeUpTime(time) }
    }

    fun updateSleepTime(time: String) {
        _uiState.value = _uiState.value.copy(sleepTime = time)
        viewModelScope.launch { preferencesRepository.updateSleepTime(time) }
    }

    fun updateThemeMode(theme: ThemeMode) {
        _uiState.value = _uiState.value.copy(themeMode = theme)
        viewModelScope.launch { preferencesRepository.updateThemeMode(theme) }
    }

    fun updateOrganizationMode(mode: OrganizationMode) {
        _uiState.value = _uiState.value.copy(organizationMode = mode)
        viewModelScope.launch { preferencesRepository.updateOrganizationMode(mode) }
    }

    companion object {
        fun provideFactory(
            repository: UserPreferencesRepository,
            learningRepository: LearningRepository,
            activityRepository: ActivityRepository,
            deviceUsageRepository: DeviceUsageRepository? = null,
            usageMonitoringService: UsageMonitoringService? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SettingsViewModel(
                    repository,
                    learningRepository,
                    activityRepository,
                    deviceUsageRepository,
                    usageMonitoringService
                ) as T
            }
        }
    }
}
