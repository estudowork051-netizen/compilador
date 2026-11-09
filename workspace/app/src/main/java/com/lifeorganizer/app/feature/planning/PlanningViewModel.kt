package com.lifeorganizer.app.feature.planning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lifeorganizer.app.core.utils.AutoOrganizationEngine
import com.lifeorganizer.app.core.utils.DateTimeUtils
import com.lifeorganizer.app.core.utils.ScheduleReorderEngine
import com.lifeorganizer.app.core.utils.TimeAvailabilityEngine
import com.lifeorganizer.app.data.repository.ActivityRepository
import com.lifeorganizer.app.data.repository.ContextRepository
import com.lifeorganizer.app.data.repository.LearningRepository
import com.lifeorganizer.app.data.repository.ScheduleRepository
import com.lifeorganizer.app.data.repository.UserPreferencesRepository
import com.lifeorganizer.app.domain.model.ActivityContextModel
import com.lifeorganizer.app.domain.model.ActivityModel
import com.lifeorganizer.app.domain.model.ActivityType
import com.lifeorganizer.app.domain.model.BalancePreference
import com.lifeorganizer.app.domain.model.DayAvailability
import com.lifeorganizer.app.domain.model.ExecutionRecordModel
import com.lifeorganizer.app.domain.model.OrganizationProposal
import com.lifeorganizer.app.domain.model.ProposedScheduleItem
import com.lifeorganizer.app.domain.model.ScheduleEntryModel
import com.lifeorganizer.app.domain.model.ScheduleStatus
import com.lifeorganizer.app.domain.model.TimeSlot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

data class PlanningUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val displayDateFormatted: String = "",
    val wakeUpTime: String = "08:00",
    val sleepTime: String = "00:00",
    val balancePreference: BalancePreference = BalancePreference.BALANCED,
    val allActivities: List<ActivityModel> = emptyList(),
    val fixedActivitiesForDay: List<ActivityModel> = emptyList(),
    val scheduledEntriesForDay: List<ScheduleEntryModel> = emptyList(),
    val availability: DayAvailability = DayAvailability(
        date = "",
        wakeUpTime = "08:00",
        sleepTime = "00:00",
        awakeDurationMinutes = 960,
        fixedCommitmentsMinutes = 0,
        availableTimeMinutes = 960,
        plannedActivitiesMinutes = 0,
        remainingFreeMinutes = 960
    ),
    val daysWithTasks: Set<LocalDate> = emptySet(),
    val showScheduleSheet: Boolean = false,
    val showCreateActivitySheet: Boolean = false,
    val selectedSlotForSchedule: TimeSlot? = null,
    val selectedActivityForSchedule: ActivityModel? = null,
    val executionRecords: List<ExecutionRecordModel> = emptyList(),
    val activeCompletionPromptEntry: ScheduleEntryModel? = null,
    val activeCompletionPromptContext: ActivityContextModel? = null,
    val activePostponeEntry: ScheduleEntryModel? = null,
    val activeProposal: OrganizationProposal? = null,
    val showProposalSheet: Boolean = false,
    val feedbackMessage: String? = null
)

class PlanningViewModel(
    private val preferencesRepository: UserPreferencesRepository,
    private val activityRepository: ActivityRepository,
    private val scheduleRepository: ScheduleRepository,
    private val contextRepository: ContextRepository,
    private val learningRepository: LearningRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlanningUiState())
    val uiState: StateFlow<PlanningUiState> = _uiState.asStateFlow()

    init {
        updateDateDisplay()
        loadUserRoutine()
        observeData()

        val historyFlow: Flow<List<ExecutionRecordModel>> =
            learningRepository?.allExecutionRecords ?: scheduleRepository.getExecutionHistory()
        historyFlow
            .onEach { records: List<ExecutionRecordModel> ->
                _uiState.update { it.copy(executionRecords = records) }
            }
            .launchIn(viewModelScope)
    }

    private fun loadUserRoutine() {
        viewModelScope.launch {
            val profile = preferencesRepository.userProfileFlow.first()
            _uiState.update { current ->
                current.copy(
                    wakeUpTime = profile.wakeUpTime,
                    sleepTime = profile.sleepTime,
                    balancePreference = profile.balancePreference
                )
            }
            recalculateAvailability()
        }
    }

    private fun observeData() {
        combine(
            activityRepository.allActivities,
            scheduleRepository.getEntriesForDate(_uiState.value.selectedDate.toString())
        ) { allActs, entries ->
            Pair(allActs, entries)
        }.onEach { (allActs, entries) ->
            val date = _uiState.value.selectedDate
            val fixedForDay = allActs.filter { it.type == ActivityType.FIXED && it.isScheduledForDate(date) }
            _uiState.update { current ->
                current.copy(
                    allActivities = allActs,
                    fixedActivitiesForDay = fixedForDay,
                    scheduledEntriesForDay = entries
                )
            }
            recalculateAvailability()
        }.launchIn(viewModelScope)

        // Observe dates with active tasks in the current range
        viewModelScope.launch {
            scheduleRepository.getUncompletedEntries().collect { uncompleted ->
                val dateSet = uncompleted.mapNotNull {
                    try {
                        LocalDate.parse(it.date)
                    } catch (e: Exception) {
                        null
                    }
                }.toSet()
                _uiState.update { it.copy(daysWithTasks = dateSet) }
            }
        }
    }

    private fun reloadDateEntries() {
        viewModelScope.launch {
            val dateStr = _uiState.value.selectedDate.toString()
            val date = _uiState.value.selectedDate
            scheduleRepository.getEntriesForDate(dateStr).first().let { entries ->
                val fixedForDay = _uiState.value.allActivities.filter {
                    it.type == ActivityType.FIXED && it.isScheduledForDate(date)
                }
                _uiState.update { current ->
                    current.copy(
                        fixedActivitiesForDay = fixedForDay,
                        scheduledEntriesForDay = entries
                    )
                }
                recalculateAvailability()
            }
        }
    }

    private fun recalculateAvailability() {
        val state = _uiState.value
        val dateStr = state.selectedDate.toString()
        val unallocated = state.allActivities.filter { it.type == ActivityType.FLEXIBLE }

        val calc = TimeAvailabilityEngine.calculateDayAvailability(
            date = dateStr,
            wakeUpTimeStr = state.wakeUpTime,
            sleepTimeStr = state.sleepTime,
            fixedActivities = state.fixedActivitiesForDay,
            scheduledEntries = state.scheduledEntriesForDay,
            unallocatedActivities = unallocated
        )

        _uiState.update { it.copy(availability = calc) }
    }

    fun selectDate(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date) }
        updateDateDisplay()
        reloadDateEntries()
    }

    fun nextWeek() {
        _uiState.update { it.copy(selectedDate = it.selectedDate.plusWeeks(1)) }
        updateDateDisplay()
        reloadDateEntries()
    }

    fun previousWeek() {
        _uiState.update { it.copy(selectedDate = it.selectedDate.minusWeeks(1)) }
        updateDateDisplay()
        reloadDateEntries()
    }

    fun resetToToday() {
        selectDate(LocalDate.now())
    }

    fun openScheduleSheet(slot: TimeSlot? = null) {
        _uiState.update {
            it.copy(
                showScheduleSheet = true,
                selectedSlotForSchedule = slot
            )
        }
    }

    fun closeScheduleSheet() {
        _uiState.update {
            it.copy(
                showScheduleSheet = false,
                selectedSlotForSchedule = null
            )
        }
    }

    fun openCreateActivitySheet() {
        _uiState.update {
            it.copy(
                showScheduleSheet = false,
                showCreateActivitySheet = true
            )
        }
    }

    fun closeCreateActivitySheet() {
        _uiState.update { it.copy(showCreateActivitySheet = false, showScheduleSheet = true) }
    }

    fun saveNewActivity(activity: ActivityModel, contextModel: ActivityContextModel? = null) {
        viewModelScope.launch {
            val newId = activityRepository.saveActivity(activity)
            val savedActivity = activity.copy(id = newId)
            if (contextModel != null) {
                contextRepository.saveContext(contextModel.copy(activityId = newId))
            }
            val allActs: List<ActivityModel> = activityRepository.allActivities.first()
            _uiState.update { current ->
                current.copy(
                    allActivities = allActs,
                    showCreateActivitySheet = false,
                    showScheduleSheet = true,
                    selectedActivityForSchedule = savedActivity
                )
            }
            recalculateAvailability()
        }
    }

    fun confirmSchedule(activity: ActivityModel, startHour: Int, startMinute: Int, durationMinutes: Int) {
        viewModelScope.launch {
            val targetDate = _uiState.value.selectedDate
            val isToday = targetDate == LocalDate.now()
            val nowMin = LocalTime.now().hour * 60 + LocalTime.now().minute
            val proposedStart = startHour * 60 + startMinute
            if (isToday && proposedStart < nowMin) {
                _uiState.update {
                    it.copy(feedbackMessage = "Esse horário já passou. Por favor, escolha um horário a partir de agora.")
                }
                return@launch
            }

            val targetActivity = if (activity.id <= 0L || activityRepository.getActivityById(activity.id) == null) {
                val newId = activityRepository.saveActivity(activity.copy(id = 0L))
                activity.copy(id = newId)
            } else {
                activity
            }
            scheduleRepository.scheduleActivity(
                activity = targetActivity,
                date = targetDate.toString(),
                startHour = startHour,
                startMinute = startMinute,
                durationMinutes = durationMinutes
            )
            closeScheduleSheet()
            reloadDateEntries()
        }
    }

    fun reorderScheduledEntries(fromIndex: Int, toIndex: Int) {
        val entries = _uiState.value.scheduledEntriesForDay
        val fixed = _uiState.value.fixedActivitiesForDay
        val wake = DateTimeUtils.parseTime(_uiState.value.wakeUpTime)
        val sleep = DateTimeUtils.parseTime(_uiState.value.sleepTime)
        val wakeMin = wake.hour * 60 + wake.minute
        val sleepMin = if (sleep.hour == 0 && sleep.minute == 0) 24 * 60 else sleep.hour * 60 + sleep.minute

        val result = ScheduleReorderEngine.reorderAndRecalculate(
            entries = entries,
            fromIndex = fromIndex,
            toIndex = toIndex,
            fixedCommitments = fixed,
            wakeUpTimeMinutes = wakeMin,
            sleepTimeMinutes = sleepMin
        )

        viewModelScope.launch {
            scheduleRepository.updateEntries(result.updatedEntries)
            reloadDateEntries()
        }
    }

    // Auto-Organization Engine Integration (Etapa 4)
    fun generateAutoRoutine() {
        viewModelScope.launch {
            val state = _uiState.value
            val contexts = contextRepository.getAllActiveContexts().first()
            val contextMap = contexts.associateBy { it.activityId }

            val unallocated = state.allActivities.filter { it.type == ActivityType.FLEXIBLE }
            val proposal = AutoOrganizationEngine.generateProposal(
                targetDate = state.selectedDate,
                wakeUpTimeStr = state.wakeUpTime,
                sleepTimeStr = state.sleepTime,
                fixedActivities = state.fixedActivitiesForDay,
                existingEntries = state.scheduledEntriesForDay,
                unallocatedActivities = unallocated,
                contextMap = contextMap,
                balancePreference = state.balancePreference
            )

            _uiState.update {
                it.copy(
                    activeProposal = proposal,
                    showProposalSheet = true
                )
            }
        }
    }

    fun reorganizeToday() {
        viewModelScope.launch {
            val state = _uiState.value
            val contexts = contextRepository.getAllActiveContexts().first()
            val contextMap = contexts.associateBy { it.activityId }

            val unallocated = state.allActivities.filter { it.type == ActivityType.FLEXIBLE }
            val proposal = AutoOrganizationEngine.reorganizeRemainingDay(
                currentTime = LocalTime.now(),
                targetDate = state.selectedDate,
                wakeUpTimeStr = state.wakeUpTime,
                sleepTimeStr = state.sleepTime,
                fixedActivities = state.fixedActivitiesForDay,
                existingEntries = state.scheduledEntriesForDay,
                unallocatedActivities = unallocated,
                contextMap = contextMap,
                balancePreference = state.balancePreference
            )

            _uiState.update {
                it.copy(
                    activeProposal = proposal,
                    showProposalSheet = true
                )
            }
        }
    }

    fun dismissProposalSheet() {
        _uiState.update {
            it.copy(
                showProposalSheet = false,
                activeProposal = null
            )
        }
    }

    private var isApprovingProposal = false

    fun approveProposal(proposal: OrganizationProposal) {
        if (isApprovingProposal) return
        isApprovingProposal = true
        viewModelScope.launch {
            try {
                val state = _uiState.value
                val dateStr = proposal.date.toString()
                val existingEntries = scheduleRepository.getEntriesForDate(dateStr).first()
                val activityMap = state.allActivities.associateBy { it.id }.toMutableMap()

                proposal.items.forEach { proposedItem ->
                    val timeParts = proposedItem.startTime.split(":")
                    val startHour = timeParts.getOrNull(0)?.toIntOrNull() ?: proposedItem.startHour
                    val startMinute = timeParts.getOrNull(1)?.toIntOrNull() ?: proposedItem.startMinute

                    // Check if already scheduled at this time or for this activity on this date to ensure idempotency
                    val alreadyScheduled = existingEntries.any { entry ->
                        (entry.activityId == proposedItem.activityId && entry.activityId > 0 && entry.date == dateStr) ||
                        (entry.date == dateStr && entry.startHour == startHour && entry.startMinute == startMinute)
                    }

                    if (!alreadyScheduled) {
                        val existing = activityMap[proposedItem.activityId]
                        val activity = if (existing != null) {
                            existing
                        } else {
                            val savedId = activityRepository.saveActivity(proposedItem.activity)
                            val saved = proposedItem.activity.copy(id = savedId)
                            activityMap[savedId] = saved
                            saved
                        }

                        scheduleRepository.scheduleActivity(
                            activity = activity,
                            date = dateStr,
                            startHour = startHour,
                            startMinute = startMinute,
                            durationMinutes = proposedItem.durationMinutes
                        )
                    }
                }

                dismissProposalSheet()
                reloadDateEntries()
            } finally {
                isApprovingProposal = false
            }
        }
    }

    fun onTaskCheckboxClicked(entry: ScheduleEntryModel) {
        if (entry.status == ScheduleStatus.COMPLETED) {
            // Reopen task
            viewModelScope.launch {
                scheduleRepository.updateStatus(entry.id, ScheduleStatus.PENDING)
                reloadDateEntries()
            }
        } else {
            // Load contextual information for completion prompt
            viewModelScope.launch {
                val ctx = entry.activityId?.let { contextRepository.getContextForActivityOnce(it) }
                _uiState.update {
                    it.copy(
                        activeCompletionPromptEntry = entry,
                        activeCompletionPromptContext = ctx
                    )
                }
            }
        }
    }

    fun confirmCompletion(entry: ScheduleEntryModel, actualDurationMinutes: Int?, advanceContext: Boolean) {
        viewModelScope.launch {
            val now = LocalTime.now()
            val nowMin = now.hour * 60 + now.minute
            val isToday = entry.date == LocalDate.now().toString()

            val finalActualMinutes: Int? = if (actualDurationMinutes != null) {
                actualDurationMinutes
            } else when {
                !isToday -> entry.computedDurationMinutes
                nowMin <= entry.startTotalMinutes -> null
                nowMin in (entry.startTotalMinutes + 1) until entry.endTotalMinutes -> nowMin - entry.startTotalMinutes
                else -> entry.computedDurationMinutes
            }

            scheduleRepository.updateStatus(
                id = entry.id,
                newStatus = ScheduleStatus.COMPLETED,
                actualDurationMinutes = finalActualMinutes
            )
            if (advanceContext && entry.activityId > 0) {
                contextRepository.advanceProgress(entry.activityId)
            }
            _uiState.update {
                it.copy(
                    activeCompletionPromptEntry = null,
                    activeCompletionPromptContext = null
                )
            }
            reloadDateEntries()
        }
    }

    fun markStillDoing(entry: ScheduleEntryModel) {
        viewModelScope.launch {
            scheduleRepository.updateStatus(entry.id, ScheduleStatus.IN_PROGRESS)
            _uiState.update {
                it.copy(
                    activeCompletionPromptEntry = null,
                    activeCompletionPromptContext = null
                )
            }
            reloadDateEntries()
        }
    }

    fun markNotDone(entry: ScheduleEntryModel) {
        viewModelScope.launch {
            scheduleRepository.updateStatus(entry.id, ScheduleStatus.NOT_DONE)
            _uiState.update {
                it.copy(
                    activeCompletionPromptEntry = null,
                    activeCompletionPromptContext = null
                )
            }
            reloadDateEntries()
        }
    }

    fun openPostponeDialog(entry: ScheduleEntryModel) {
        _uiState.update {
            it.copy(
                activeCompletionPromptEntry = null,
                activeCompletionPromptContext = null,
                activePostponeEntry = entry
            )
        }
    }

    fun closePostponeDialog() {
        _uiState.update { it.copy(activePostponeEntry = null) }
    }

    fun postponeToTomorrow(entry: ScheduleEntryModel) {
        viewModelScope.launch {
            val tomorrow = _uiState.value.selectedDate.plusDays(1).toString()
            scheduleRepository.postponeEntry(entry.id, tomorrow)
            closePostponeDialog()
            reloadDateEntries()
        }
    }

    fun removeEntryFromDay(entry: ScheduleEntryModel) {
        viewModelScope.launch {
            scheduleRepository.deleteEntry(entry.id)
            reloadDateEntries()
        }
    }

    private fun updateDateDisplay() {
        val formatted = DateTimeUtils.formatDisplayDate(_uiState.value.selectedDate)
        _uiState.update { it.copy(displayDateFormatted = formatted) }
    }

    fun dismissFeedback() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }

    companion object {
        fun provideFactory(
            preferencesRepository: UserPreferencesRepository,
            activityRepository: ActivityRepository,
            scheduleRepository: ScheduleRepository,
            contextRepository: ContextRepository,
            learningRepository: LearningRepository? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PlanningViewModel(preferencesRepository, activityRepository, scheduleRepository, contextRepository, learningRepository) as T
            }
        }
    }
}
