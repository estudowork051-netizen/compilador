package com.lifeorganizer.app.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lifeorganizer.app.core.utils.DateTimeUtils
import com.lifeorganizer.app.core.utils.TimeCalculationUtils
import com.lifeorganizer.app.data.repository.UserPreferencesRepository
import com.lifeorganizer.app.domain.model.OrganizationMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val currentStep: Int = 1,
    val name: String = "",
    val wakeUpTime: String = "08:00",
    val sleepTime: String = "00:00",
    val selectedCommitments: Set<String> = emptySet(),
    val organizationMode: OrganizationMode = OrganizationMode.MANUAL,
    val isCompleted: Boolean = false,
    val isLoading: Boolean = false
) {
    val awakeDurationMinutes: Long
        get() {
            val wake = DateTimeUtils.parseTime(wakeUpTime)
            val sleep = DateTimeUtils.parseTime(sleepTime)
            return TimeCalculationUtils.calculateAwakeDurationMinutes(wake, sleep)
        }

    val awakeDurationFormatted: String
        get() = TimeCalculationUtils.formatMinutesToHoursAndMinutes(awakeDurationMinutes)
}

class OnboardingViewModel(
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        loadSavedProgress()
    }

    private fun loadSavedProgress() {
        viewModelScope.launch {
            val profile = preferencesRepository.userProfileFlow.first()
            _uiState.update { current ->
                current.copy(
                    currentStep = if (profile.onboardingStep in 1..6) profile.onboardingStep else 1,
                    name = profile.name,
                    wakeUpTime = profile.wakeUpTime.ifEmpty { "08:00" },
                    sleepTime = profile.sleepTime.ifEmpty { "00:00" },
                    selectedCommitments = profile.fixedCommitments,
                    organizationMode = profile.organizationMode,
                    isCompleted = profile.isOnboardingCompleted
                )
            }
        }
    }

    fun setName(name: String) {
        _uiState.update { it.copy(name = name) }
        viewModelScope.launch {
            preferencesRepository.updateName(name)
        }
    }

    fun setWakeUpTime(time: String) {
        _uiState.update { it.copy(wakeUpTime = time) }
        viewModelScope.launch {
            preferencesRepository.updateWakeUpTime(time)
        }
    }

    fun setSleepTime(time: String) {
        _uiState.update { it.copy(sleepTime = time) }
        viewModelScope.launch {
            preferencesRepository.updateSleepTime(time)
        }
    }

    fun toggleCommitment(commitment: String) {
        _uiState.update { state ->
            val updated = if (commitment == "Configurar depois") {
                setOf("Configurar depois")
            } else {
                val current = state.selectedCommitments.toMutableSet()
                current.remove("Configurar depois")
                if (current.contains(commitment)) {
                    current.remove(commitment)
                } else {
                    current.add(commitment)
                }
                current
            }
            state.copy(selectedCommitments = updated)
        }
        viewModelScope.launch {
            preferencesRepository.updateCommitments(_uiState.value.selectedCommitments)
        }
    }

    fun setOrganizationMode(mode: OrganizationMode) {
        _uiState.update { it.copy(organizationMode = mode) }
        viewModelScope.launch {
            preferencesRepository.updateOrganizationMode(mode)
        }
    }

    fun nextStep() {
        if (_uiState.value.currentStep < 6) {
            val newStep = _uiState.value.currentStep + 1
            _uiState.update { it.copy(currentStep = newStep) }
            viewModelScope.launch {
                preferencesRepository.setOnboardingStep(newStep)
            }
        }
    }

    fun previousStep() {
        if (_uiState.value.currentStep > 1) {
            val newStep = _uiState.value.currentStep - 1
            _uiState.update { it.copy(currentStep = newStep) }
            viewModelScope.launch {
                preferencesRepository.setOnboardingStep(newStep)
            }
        }
    }

    fun completeOnboarding(onSuccess: () -> Unit) {
        val state = _uiState.value
        viewModelScope.launch {
            preferencesRepository.saveOnboardingData(
                name = state.name,
                wakeUpTime = state.wakeUpTime,
                sleepTime = state.sleepTime,
                commitments = state.selectedCommitments,
                orgMode = state.organizationMode
            )
            _uiState.update { it.copy(isCompleted = true) }
            onSuccess()
        }
    }

    companion object {
        fun provideFactory(
            repository: UserPreferencesRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return OnboardingViewModel(repository) as T
            }
        }
    }
}
