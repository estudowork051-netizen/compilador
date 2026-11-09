package com.lifeorganizer.app.feature.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lifeorganizer.app.core.utils.HabitLearningEngine
import com.lifeorganizer.app.core.utils.WellbeingInsight
import com.lifeorganizer.app.core.utils.WellbeingRecommendationEngine
import com.lifeorganizer.app.data.repository.ActivityRepository
import com.lifeorganizer.app.data.repository.DeviceUsageRepository
import com.lifeorganizer.app.data.repository.FocusRepository
import com.lifeorganizer.app.data.repository.LearningRepository
import com.lifeorganizer.app.data.repository.ScheduleRepository
import com.lifeorganizer.app.domain.model.ActivityModel
import com.lifeorganizer.app.domain.model.AppUsageStatModel
import com.lifeorganizer.app.domain.model.DayBalanceSummary
import com.lifeorganizer.app.domain.model.ExecutionRecordModel
import com.lifeorganizer.app.domain.model.FocusSessionModel
import com.lifeorganizer.app.domain.model.HabitInsightSummary
import com.lifeorganizer.app.domain.model.ScheduleEntryModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate

data class HabitsUiState(
    val summary: HabitInsightSummary = HabitInsightSummary(),
    val activities: List<ActivityModel> = emptyList(),
    val dayBalance: DayBalanceSummary = DayBalanceSummary(),
    val wellbeingInsights: List<WellbeingInsight> = emptyList(),
    val isLoading: Boolean = false,
    val feedbackMessage: String? = null
)

class HabitsViewModel(
    private val scheduleRepository: ScheduleRepository,
    private val activityRepository: ActivityRepository,
    private val learningRepository: LearningRepository,
    private val focusRepository: FocusRepository,
    private val deviceUsageRepository: DeviceUsageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HabitsUiState(isLoading = true))
    val uiState: StateFlow<HabitsUiState> = _uiState.asStateFlow()

    init {
        val todayStr = LocalDate.now().toString()
        val startOfDayMillis = System.currentTimeMillis() - 24 * 3600 * 1000L

        viewModelScope.launch {
            combine(
                scheduleRepository.getExecutionHistory(),
                activityRepository.allActivities,
                scheduleRepository.getEntriesForDate(todayStr),
                focusRepository.getAllSessionsFlow(),
                deviceUsageRepository.getLogsSinceFlow(startOfDayMillis)
            ) { records: List<ExecutionRecordModel>,
                activities: List<ActivityModel>,
                todayEntries: List<ScheduleEntryModel>,
                focusSessions: List<FocusSessionModel>,
                usageLogs: List<AppUsageStatModel> ->

                val habitSummary = HabitLearningEngine.computeHabitSummary(
                    records = records,
                    activities = activities,
                    currentEntries = todayEntries
                )

                val dayBalance = WellbeingRecommendationEngine.computeDayBalance(
                    entries = todayEntries,
                    focusSessions = focusSessions,
                    usageLogs = usageLogs
                )

                val insights = WellbeingRecommendationEngine.generateInsights(dayBalance)

                HabitsUiState(
                    summary = habitSummary,
                    activities = activities,
                    dayBalance = dayBalance,
                    wellbeingInsights = insights,
                    isLoading = false
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun applyLearnedDuration(activityId: Long, newDurationMinutes: Int) {
        viewModelScope.launch {
            learningRepository.applyLearnedDuration(activityId, newDurationMinutes)
            _uiState.value = _uiState.value.copy(
                feedbackMessage = "Duração atualizada para ${newDurationMinutes} minutos com sucesso!"
            )
        }
    }

    fun clearFeedbackMessage() {
        _uiState.value = _uiState.value.copy(feedbackMessage = null)
    }

    companion object {
        fun provideFactory(
            scheduleRepository: ScheduleRepository,
            activityRepository: ActivityRepository,
            learningRepository: LearningRepository,
            focusRepository: FocusRepository,
            deviceUsageRepository: DeviceUsageRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HabitsViewModel(
                    scheduleRepository,
                    activityRepository,
                    learningRepository,
                    focusRepository,
                    deviceUsageRepository
                ) as T
            }
        }
    }
}
