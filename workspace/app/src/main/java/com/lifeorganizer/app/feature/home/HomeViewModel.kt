package com.lifeorganizer.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lifeorganizer.app.core.focus.FocusSessionManager
import com.lifeorganizer.app.core.utils.DateTimeUtils
import com.lifeorganizer.app.core.utils.HabitLearningEngine
import com.lifeorganizer.app.core.utils.ScheduleReorderEngine
import com.lifeorganizer.app.core.utils.TimeAvailabilityEngine
import com.lifeorganizer.app.core.utils.TimeCalculationUtils
import com.lifeorganizer.app.data.local.entity.RecommendationFeedbackEntity
import com.lifeorganizer.app.data.repository.ActivityRepository
import com.lifeorganizer.app.data.repository.ContextRepository
import com.lifeorganizer.app.data.repository.LearningRepository
import com.lifeorganizer.app.data.repository.ScheduleRepository
import com.lifeorganizer.app.data.repository.UserPreferencesRepository
import com.lifeorganizer.app.domain.model.ActivityCategory
import com.lifeorganizer.app.domain.model.ActivityContextModel
import com.lifeorganizer.app.domain.model.ActivityModel
import com.lifeorganizer.app.domain.model.ActivityType
import com.lifeorganizer.app.domain.model.DayAvailability
import com.lifeorganizer.app.domain.model.ExecutionRecordModel
import com.lifeorganizer.app.domain.model.FeedbackAction
import com.lifeorganizer.app.core.components.EnvironmentPeriod
import com.lifeorganizer.app.core.components.MoodType
import com.lifeorganizer.app.domain.model.FocusSessionModel
import com.lifeorganizer.app.domain.model.RecommendationModel
import com.lifeorganizer.app.domain.model.RecommendationType
import com.lifeorganizer.app.domain.model.ScheduleEntryModel
import com.lifeorganizer.app.domain.model.ScheduleStatus
import com.lifeorganizer.app.domain.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import com.lifeorganizer.app.core.analytics.UsageMonitoringService

sealed interface HomeTimelineItem {
    val startMinutes: Int
    val endMinutes: Int
    val title: String

    data class Scheduled(val entry: ScheduleEntryModel) : HomeTimelineItem {
        override val startMinutes: Int get() = entry.startTotalMinutes
        override val endMinutes: Int get() = entry.endTotalMinutes
        override val title: String get() = entry.activityTitle
    }

    data class Fixed(val activity: ActivityModel) : HomeTimelineItem {
        override val startMinutes: Int get() = (activity.effectiveStartHour ?: 0) * 60 + (activity.effectiveStartMinute ?: 0)
        override val endMinutes: Int get() = startMinutes + activity.fixedDurationMinutes
        override val title: String get() = activity.title
    }
}

data class AssistantChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val isFromUser: Boolean,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class HomeUiState(
    val isLoading: Boolean = true,
    val userName: String = "",
    val greeting: String = "",
    val currentDateFormatted: String = "",
    val dayOfWeekFormatted: String = "",
    val wakeUpTime: String = "08:00",
    val sleepTime: String = "00:00",
    val awakeDurationFormatted: String = "",
    val flexibleActivities: List<ActivityModel> = emptyList(),
    val fixedCommitments: List<ActivityModel> = emptyList(),
    val todayScheduledEntries: List<ScheduleEntryModel> = emptyList(),
    val todayAvailability: DayAvailability = DayAvailability(
        date = "",
        wakeUpTime = "08:00",
        sleepTime = "00:00",
        awakeDurationMinutes = 960,
        fixedCommitmentsMinutes = 0,
        availableTimeMinutes = 960,
        plannedActivitiesMinutes = 0,
        remainingFreeMinutes = 960
    ),
    val currentRecommendation: RecommendationModel? = null,
    val showExplanations: Boolean = true,
    val isRecommendationDismissed: Boolean = false,
    val showAddSheet: Boolean = false,
    val selectedTypeForAdd: ActivityType = ActivityType.FLEXIBLE,
    val editingActivity: ActivityModel? = null,
    val activeCompletionEntry: ScheduleEntryModel? = null,
    val activeCompletionContext: ActivityContextModel? = null,
    val activePostponeEntry: ScheduleEntryModel? = null,
    val timeExtensionTargetEntry: ScheduleEntryModel? = null,
    val activeFocusSession: FocusSessionModel? = null,
    val feedbackMessage: String? = null,
    val currentMood: MoodType? = null,
    val hydrationMl: Int = 500,
    val waterRecordsCount: Int = 2,
    val canUndoWater: Boolean = false,
    val lastWaterAmountMl: Int = 0,
    val screenTimeMinutesToday: Int = 0,
    val isUsagePermissionGranted: Boolean = false,
    val showAssistantDialog: Boolean = false,
    val assistantMessages: List<AssistantChatMessage> = emptyList(),
    val showScheduleSheet: Boolean = false,
    val showBreatheModal: Boolean = false,
    val selectedEnvironment: EnvironmentPeriod = when (LocalTime.now().hour) {
        in 5..11 -> EnvironmentPeriod.MORNING
        in 12..17 -> EnvironmentPeriod.AFTERNOON
        else -> EnvironmentPeriod.NIGHT
    }
) {
    val timelineItems: List<HomeTimelineItem>
        get() {
            val scheduled = todayScheduledEntries.map { HomeTimelineItem.Scheduled(it) }
            val fixed = fixedCommitments.map { HomeTimelineItem.Fixed(it) }
            return (scheduled + fixed).sortedBy { it.startMinutes }
        }

    val completedCountToday: Int
        get() = todayScheduledEntries.count { it.status == ScheduleStatus.COMPLETED }

    val pendingCountToday: Int
        get() = todayScheduledEntries.count { it.status == ScheduleStatus.PENDING || it.status == ScheduleStatus.IN_PROGRESS }

    val activeNowEntry: ScheduleEntryModel?
        get() {
            val inProgress = todayScheduledEntries.firstOrNull { it.status == ScheduleStatus.IN_PROGRESS }
            if (inProgress != null) return inProgress

            val nowMin = LocalTime.now().hour * 60 + LocalTime.now().minute
            return todayScheduledEntries.firstOrNull { entry ->
                entry.status == ScheduleStatus.PENDING &&
                nowMin >= entry.startTotalMinutes &&
                nowMin < entry.endTotalMinutes
            }
        }

    val nextUpcomingEntry: ScheduleEntryModel?
        get() {
            val nowMin = LocalTime.now().hour * 60 + LocalTime.now().minute
            val active = activeNowEntry
            return todayScheduledEntries
                .filter { it.status == ScheduleStatus.PENDING && it.id != active?.id && it.startTotalMinutes >= nowMin }
                .minByOrNull { it.startTotalMinutes }
                ?: todayScheduledEntries.firstOrNull { it.status == ScheduleStatus.PENDING && it.id != active?.id }
        }
}

class HomeViewModel(
    private val preferencesRepository: UserPreferencesRepository,
    private val activityRepository: ActivityRepository,
    private val scheduleRepository: ScheduleRepository,
    private val contextRepository: ContextRepository,
    private val learningRepository: LearningRepository,
    private val focusSessionManager: FocusSessionManager,
    private val usageMonitoringService: UsageMonitoringService? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        android.util.Log.d("LifeFlowDiagnostic", "HOME_VIEWMODEL_INIT_STARTED")
        try {
            updateDateAndGreeting()
            observeData()
            observeFocusSession()
            refreshScreenTime()
            android.util.Log.d("LifeFlowDiagnostic", "HOME_VIEWMODEL_INIT_COMPLETED")
        } catch (e: Exception) {
            android.util.Log.e("LifeFlowDiagnostic", "HOME_VIEWMODEL_INIT_FAILED: ${e.message}", e)
        }
    }

    fun refreshTime() {
        updateDateAndGreeting()
        refreshScreenTime()
    }

    private fun updateDateAndGreeting() {
        val now = LocalTime.now()
        val today = LocalDate.now()
        val greeting = DateTimeUtils.getGreeting(now.hour)
        val dateFormatted = DateTimeUtils.formatCurrentDate(today)
        val dayOfWeek = DateTimeUtils.formatDayOfWeek(today)

        _uiState.value = _uiState.value.copy(
            greeting = greeting,
            currentDateFormatted = dateFormatted,
            dayOfWeekFormatted = dayOfWeek
        )
    }

    private fun observeFocusSession() {
        viewModelScope.launch {
            try {
                android.util.Log.d("LifeFlowDiagnostic", "FOCUS_SESSION_OBSERVE_STARTED")
                focusSessionManager.activeSession.collect { session ->
                    _uiState.value = _uiState.value.copy(activeFocusSession = session)
                }
            } catch (e: Exception) {
                android.util.Log.e("LifeFlowDiagnostic", "FOCUS_SESSION_STREAM_ERROR: ${e.message}", e)
            }
        }
    }

    private fun observeData() {
        val today = LocalDate.now()
        val todayStr = today.toString()
        com.lifeorganizer.app.core.diagnostic.LifeFlowDiagnosticManager.logEvent("HOME_DATA_LOADING_STARTED", "date=$todayStr")

        val coreFlow = combine(
            preferencesRepository.userProfileFlow.catch { e ->
                com.lifeorganizer.app.core.diagnostic.LifeFlowDiagnosticManager.logError("CORE_FLOW_USER_PROFILE_ERROR", e)
                emit(UserProfile())
            },
            activityRepository.allActivities.catch { e ->
                com.lifeorganizer.app.core.diagnostic.LifeFlowDiagnosticManager.logError("CORE_FLOW_ACTIVITIES_ERROR", e)
                emit(emptyList())
            },
            scheduleRepository.getEntriesForDate(todayStr).catch { e ->
                com.lifeorganizer.app.core.diagnostic.LifeFlowDiagnosticManager.logError("CORE_FLOW_SCHEDULE_ERROR", e)
                emit(emptyList())
            }
        ) { profile: UserProfile, activities: List<ActivityModel>, entries: List<ScheduleEntryModel> ->
            Triple(profile, activities, entries)
        }.catch { e ->
            com.lifeorganizer.app.core.diagnostic.LifeFlowDiagnosticManager.logError("CORE_FLOW_COMBINE_ERROR", e)
            emit(Triple(UserProfile(), emptyList(), emptyList()))
        }

        val learningFlow = combine(
            contextRepository.getAllActiveContexts().catch { e ->
                com.lifeorganizer.app.core.diagnostic.LifeFlowDiagnosticManager.logError("LEARNING_FLOW_CONTEXTS_ERROR", e)
                emit(emptyList())
            },
            scheduleRepository.getExecutionHistory().catch { e ->
                com.lifeorganizer.app.core.diagnostic.LifeFlowDiagnosticManager.logError("LEARNING_FLOW_HISTORY_ERROR", e)
                emit(emptyList())
            },
            learningRepository.allFeedback.catch { e ->
                com.lifeorganizer.app.core.diagnostic.LifeFlowDiagnosticManager.logError("LEARNING_FLOW_FEEDBACK_ERROR", e)
                emit(emptyList())
            }
        ) { contexts: List<ActivityContextModel>, records: List<ExecutionRecordModel>, feedbacks: List<RecommendationFeedbackEntity> ->
            Triple(contexts, records, feedbacks)
        }.catch { e ->
            com.lifeorganizer.app.core.diagnostic.LifeFlowDiagnosticManager.logError("LEARNING_FLOW_COMBINE_ERROR", e)
            emit(Triple(emptyList(), emptyList(), emptyList()))
        }

        combine(coreFlow, learningFlow) { (profile, activities, entries), (contexts, records, feedbacks) ->
            try {
                com.lifeorganizer.app.core.diagnostic.LifeFlowDiagnosticManager.logEvent(
                    "HOME_DATA_LOADING_SUCCESS",
                    "activities=${activities.size}, entries=${entries.size}"
                )
                val wake = DateTimeUtils.parseTime(profile.wakeUpTime)
                val sleep = DateTimeUtils.parseTime(profile.sleepTime)
                val durationMin = TimeCalculationUtils.calculateAwakeDurationMinutes(wake, sleep)
                val durationText = TimeCalculationUtils.formatMinutesToHoursAndMinutes(durationMin)

                val flexible = activities.filter { it.type == ActivityType.FLEXIBLE }
                val fixed = activities.filter { it.type == ActivityType.FIXED }
                val fixedToday = fixed.filter { it.isScheduledForDate(today) }

                val availability = TimeAvailabilityEngine.calculateDayAvailability(
                    date = todayStr,
                    wakeUpTimeStr = profile.wakeUpTime,
                    sleepTimeStr = profile.sleepTime,
                    fixedActivities = fixedToday,
                    scheduledEntries = entries,
                    unallocatedActivities = flexible
                )

                val nowMin = LocalTime.now().hour * 60 + LocalTime.now().minute
                val learnedRecs = HabitLearningEngine.generateLearnedRecommendations(
                    currentTimeMinutes = nowMin,
                    userProfile = profile,
                    activities = activities,
                    records = records,
                    contexts = contexts,
                    feedbacks = feedbacks,
                    todayEntries = entries
                )

                val rec = if (!_uiState.value.isRecommendationDismissed) {
                    learnedRecs.firstOrNull()
                } else {
                    null
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    userName = profile.name,
                    wakeUpTime = profile.wakeUpTime,
                    sleepTime = profile.sleepTime,
                    awakeDurationFormatted = durationText,
                    flexibleActivities = flexible,
                    fixedCommitments = fixedToday,
                    todayScheduledEntries = entries,
                    todayAvailability = availability,
                    currentRecommendation = rec,
                    showExplanations = profile.showExplanationsEnabled
                )
            } catch (e: Exception) {
                com.lifeorganizer.app.core.diagnostic.LifeFlowDiagnosticManager.logError("HOME_DATA_PROCESSING_ERROR", e)
            }
        }.catch { e ->
            com.lifeorganizer.app.core.diagnostic.LifeFlowDiagnosticManager.logError("OBSERVE_DATA_STREAM_ERROR", e)
        }.launchIn(viewModelScope)
    }

    // --- Focus Actions ---

    fun startFocusForEntry(entry: ScheduleEntryModel) {
        viewModelScope.launch {
            focusSessionManager.startFocus(
                activityId = entry.activityId,
                activityTitle = entry.activityTitle,
                category = entry.category,
                plannedDurationMinutes = entry.targetDurationMinutes,
                scheduleEntryId = entry.id
            )
            scheduleRepository.updateStatus(entry.id, ScheduleStatus.IN_PROGRESS)
            _uiState.value = _uiState.value.copy(feedbackMessage = "Momento de foco iniciado para '${entry.activityTitle}'!")
        }
    }

    fun pauseFocus() {
        val session = _uiState.value.activeFocusSession ?: return
        viewModelScope.launch {
            focusSessionManager.pauseSession(session.id)
        }
    }

    fun resumeFocus() {
        val session = _uiState.value.activeFocusSession ?: return
        viewModelScope.launch {
            focusSessionManager.resumeSession(session.id)
        }
    }

    fun extendFocus(minutes: Int) {
        val session = _uiState.value.activeFocusSession ?: return
        viewModelScope.launch {
            focusSessionManager.extendSession(session.id, minutes)
            _uiState.value = _uiState.value.copy(feedbackMessage = "Foco estendido em +$minutes min!")
        }
    }

    fun concludeFocus(notes: String = "") {
        val session = _uiState.value.activeFocusSession ?: return
        viewModelScope.launch {
            focusSessionManager.concludeSession(session.id, notes)
            _uiState.value = _uiState.value.copy(feedbackMessage = "Parabéns pelo foco concluído!")
        }
    }

    // --- Time Extension Dialog ---

    fun openTimeExtension(entry: ScheduleEntryModel) {
        _uiState.value = _uiState.value.copy(
            timeExtensionTargetEntry = entry,
            activeCompletionEntry = null
        )
    }

    fun closeTimeExtension() {
        _uiState.value = _uiState.value.copy(timeExtensionTargetEntry = null)
    }

    fun confirmTimeExtension(entry: ScheduleEntryModel, additionalMinutes: Int) {
        viewModelScope.launch {
            val newDuration = entry.targetDurationMinutes + additionalMinutes
            val currentStartMinutes = entry.startHour * 60 + entry.startMinute
            val newEndMinutes = currentStartMinutes + newDuration
            val newEndHour = (newEndMinutes / 60) % 24
            val newEndMinute = newEndMinutes % 60

            val updatedEntry = entry.copy(
                targetDurationMinutes = newDuration,
                endHour = newEndHour,
                endMinute = newEndMinute,
                status = ScheduleStatus.IN_PROGRESS
            )

            scheduleRepository.updateEntries(listOf(updatedEntry))
            _uiState.value = _uiState.value.copy(
                timeExtensionTargetEntry = null,
                feedbackMessage = "Tempo de '${entry.activityTitle}' estendido em +$additionalMinutes min!"
            )
        }
    }

    // --- Standard Actions ---

    fun acceptRecommendation(recommendation: RecommendationModel) {
        viewModelScope.launch {
            val activity = recommendation.activity
            if (recommendation.type == RecommendationType.LEARNED_DURATION && activity != null) {
                learningRepository.applyLearnedDuration(activity.id, recommendation.suggestedDurationMinutes)
                learningRepository.recordFeedback(
                    type = recommendation.type,
                    activityId = activity.id,
                    activityTitle = activity.title,
                    action = FeedbackAction.ACCEPTED,
                    reason = "Duração de ${recommendation.suggestedDurationMinutes}m aceita"
                )
                _uiState.value = _uiState.value.copy(
                    isRecommendationDismissed = true,
                    currentRecommendation = null,
                    feedbackMessage = "Duração de '${activity.title}' atualizada para ${recommendation.suggestedDurationMinutes} min!"
                )
            } else if (activity != null) {
                val now = LocalTime.now()
                val startHour = recommendation.suggestedStartHour ?: now.hour
                val startMinute = recommendation.suggestedStartMinute ?: now.minute

                scheduleRepository.scheduleActivity(
                    activity = activity,
                    date = LocalDate.now().toString(),
                    startHour = startHour,
                    startMinute = startMinute,
                    durationMinutes = recommendation.suggestedDurationMinutes
                )
                learningRepository.recordFeedback(
                    type = recommendation.type,
                    activityId = activity.id,
                    activityTitle = activity.title,
                    action = FeedbackAction.ACCEPTED,
                    reason = "Agendado para ${startHour}:${startMinute}"
                )
                _uiState.value = _uiState.value.copy(
                    isRecommendationDismissed = true,
                    currentRecommendation = null,
                    feedbackMessage = "'${activity.title}' adicionada à rotina de hoje!"
                )
            } else {
                dismissRecommendation(recommendation)
            }
        }
    }

    fun dismissRecommendation(recommendation: RecommendationModel?) {
        viewModelScope.launch {
            if (recommendation != null) {
                learningRepository.recordFeedback(
                    type = recommendation.type,
                    activityId = recommendation.activity?.id,
                    activityTitle = recommendation.activity?.title ?: "",
                    action = FeedbackAction.DISMISSED
                )
            }
            _uiState.value = _uiState.value.copy(
                isRecommendationDismissed = true,
                currentRecommendation = null
            )
        }
    }

    fun notInterestedRecommendation(recommendation: RecommendationModel) {
        viewModelScope.launch {
            learningRepository.recordFeedback(
                type = recommendation.type,
                activityId = recommendation.activity?.id,
                activityTitle = recommendation.activity?.title ?: "",
                action = FeedbackAction.NOT_INTERESTED
            )
            _uiState.value = _uiState.value.copy(
                isRecommendationDismissed = true,
                currentRecommendation = null,
                feedbackMessage = "Anotado! Reduziremos esse tipo de sugestão."
            )
        }
    }

    fun clearFeedbackMessage() {
        _uiState.value = _uiState.value.copy(feedbackMessage = null)
    }

    fun openAddActivity(type: ActivityType = ActivityType.FLEXIBLE) {
        _uiState.value = _uiState.value.copy(
            showAddSheet = true,
            selectedTypeForAdd = type,
            editingActivity = null
        )
    }

    fun openEditActivity(activity: ActivityModel) {
        _uiState.value = _uiState.value.copy(
            showAddSheet = true,
            selectedTypeForAdd = activity.type,
            editingActivity = activity
        )
    }

    fun closeAddActivity() {
        _uiState.value = _uiState.value.copy(
            showAddSheet = false,
            editingActivity = null
        )
    }

    fun saveActivity(activity: ActivityModel) {
        viewModelScope.launch {
            activityRepository.saveActivity(activity)
            closeAddActivity()
        }
    }

    fun saveActivityWithContext(activity: ActivityModel, contextModel: ActivityContextModel?) {
        viewModelScope.launch {
            val activityId = activityRepository.saveActivity(activity)
            if (contextModel != null) {
                val targetId = if (activity.id != 0L) activity.id else activityId
                contextRepository.saveContext(contextModel.copy(activityId = targetId))
            }
            closeAddActivity()
        }
    }

    fun setEnvironmentPeriod(period: EnvironmentPeriod) {
        _uiState.value = _uiState.value.copy(selectedEnvironment = period)
    }

    fun setMood(mood: MoodType) {
        _uiState.value = _uiState.value.copy(currentMood = mood)
    }

    fun addWater(amountMl: Int = 250) {
        recordWaterIntake(amountMl)
    }

    fun recordWaterIntake(amountMl: Int = 250) {
        val current = _uiState.value
        _uiState.value = current.copy(
            hydrationMl = current.hydrationMl + amountMl,
            waterRecordsCount = current.waterRecordsCount + 1,
            canUndoWater = true,
            lastWaterAmountMl = amountMl,
            feedbackMessage = "💧 Registro de água adicionado (+${amountMl}ml)!"
        )
    }

    fun undoWater() {
        val current = _uiState.value
        if (!current.canUndoWater || current.waterRecordsCount <= 0) return
        _uiState.value = current.copy(
            hydrationMl = (current.hydrationMl - current.lastWaterAmountMl).coerceAtLeast(0),
            waterRecordsCount = (current.waterRecordsCount - 1).coerceAtLeast(0),
            canUndoWater = false,
            lastWaterAmountMl = 0,
            feedbackMessage = "Registro de água desfeito."
        )
    }

    fun refreshScreenTime() {
        if (usageMonitoringService == null) return
        val isGranted = usageMonitoringService.isUsageAccessGranted()
        _uiState.value = _uiState.value.copy(isUsagePermissionGranted = isGranted)
        if (isGranted) {
            viewModelScope.launch {
                try {
                    val startOfDay = LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                    val endOfDay = System.currentTimeMillis()
                    val usageLogs = usageMonitoringService.collectUsageInWindow(startOfDay, endOfDay)
                    val totalSecs = usageLogs.sumOf { it.totalTimeInForegroundSeconds }
                    val totalMinutes = (totalSecs / 60L).toInt()
                    _uiState.value = _uiState.value.copy(screenTimeMinutesToday = totalMinutes)
                } catch (e: Exception) {
                    android.util.Log.e("VioraDiagnostic", "Error collecting screen time: ${e.message}")
                }
            }
        }
    }

    fun openAssistant() {
        val initialMessages = if (_uiState.value.assistantMessages.isEmpty()) {
            listOf(
                AssistantChatMessage(
                    isFromUser = false,
                    message = "Olá, ${_uiState.value.userName.ifBlank { "" }}! Eu sou a Viora, sua assistente de rotina inteligente. Como posso ajudar seu dia a fluir melhor hoje?"
                )
            )
        } else {
            _uiState.value.assistantMessages
        }
        _uiState.value = _uiState.value.copy(
            showAssistantDialog = true,
            assistantMessages = initialMessages
        )
    }

    fun closeAssistant() {
        _uiState.value = _uiState.value.copy(showAssistantDialog = false)
    }

    fun sendAssistantMessage(userPrompt: String) {
        if (userPrompt.isBlank()) return
        val userMsg = AssistantChatMessage(isFromUser = true, message = userPrompt)
        val state = _uiState.value
        val promptLower = userPrompt.trim().lowercase()

        val responseText = when {
            promptLower.contains("organizar") || promptLower.contains("agenda") || promptLower.contains("rotina") -> {
                val pendingCount = state.flexibleActivities.size
                val freeMin = state.todayAvailability.remainingFreeMinutes
                val freeFormatted = TimeCalculationUtils.formatMinutes(freeMin)
                if (pendingCount > 0) {
                    "Você tem $pendingCount atividades flexíveis cadastradas e cerca de $freeFormatted livres hoje. Você pode tocar na aba 'Agenda' para gerar uma proposta inteligente e equilibrada!"
                } else {
                    "Você tem $freeFormatted livres hoje. Que tal cadastrar novas atividades para preencher seu dia com foco e leveza?"
                }
            }
            promptLower.contains("livre") || promptLower.contains("tempo") || promptLower.contains("horas") -> {
                val freeMin = state.todayAvailability.remainingFreeMinutes
                val freeFormatted = TimeCalculationUtils.formatMinutes(freeMin)
                val wake = state.wakeUpTime
                val sleep = state.sleepTime
                "Você tem $freeFormatted livres hoje entre $wake e $sleep. Esse tempo já desconta seus compromissos fixos e atividades planejadas."
            }
            promptLower.contains("água") || promptLower.contains("beber") || promptLower.contains("hidrat") -> {
                recordWaterIntake(250)
                "Ótimo hábito! Registrei +250ml de água para você. Agora seu total hoje é de ${_uiState.value.hydrationMl}ml (${_uiState.value.waterRecordsCount} registros)."
            }
            promptLower.contains("estudar") || promptLower.contains("trabalhar") || promptLower.contains("foco") -> {
                val next = state.nextUpcomingEntry ?: state.activeNowEntry
                if (next != null) {
                    "Sua próxima atividade prioritária é '${next.activityTitle}' (${next.formattedDuration}). Recomendo um bloco de foco contínuo com pausas suaves de 5 minutos."
                } else {
                    "Pela sua rotina matinal e vespertina, os melhores horários para estudo ou trabalho profundo são pela manhã entre 09:00 e 11:30, antes da pausa para o almoço."
                }
            }
            promptLower.contains("dormir") || promptLower.contains("acordar") || promptLower.contains("horário") -> {
                "Seu horário configurado de despertar é às ${state.wakeUpTime} e dormir às ${state.sleepTime}. Respeitar esses horários mantém seu ritmo circadiano equilibrado."
            }
            else -> {
                "Estou aqui para ajudar você a cuidar do seu ritmo com serenidade. Você tem ${TimeCalculationUtils.formatMinutes(state.todayAvailability.remainingFreeMinutes)} livres hoje. O que você gostaria de priorizar agora?"
            }
        }

        val assistantMsg = AssistantChatMessage(isFromUser = false, message = responseText)
        _uiState.value = _uiState.value.copy(
            assistantMessages = state.assistantMessages + userMsg + assistantMsg
        )
    }

    fun openScheduleSheet() {
        _uiState.value = _uiState.value.copy(showScheduleSheet = true)
    }

    fun closeScheduleSheet() {
        _uiState.value = _uiState.value.copy(showScheduleSheet = false)
    }

    fun openBreatheModal() {
        _uiState.value = _uiState.value.copy(showBreatheModal = true)
    }

    fun closeBreatheModal() {
        _uiState.value = _uiState.value.copy(showBreatheModal = false)
    }

    fun notifyFixedMoveAttempt() {
        _uiState.value = _uiState.value.copy(feedbackMessage = "Esse horário é fixo. Quer escolher outro?")
    }

    fun scheduleActivityForToday(activity: ActivityModel, startHour: Int, startMinute: Int, durationMinutes: Int) {
        viewModelScope.launch {
            val now = LocalTime.now()
            val nowMin = now.hour * 60 + now.minute
            val proposedStart = startHour * 60 + startMinute
            if (proposedStart < nowMin) {
                _uiState.value = _uiState.value.copy(
                    feedbackMessage = "Esse horário já passou. Escolha um horário a partir de agora."
                )
                return@launch
            }
            val resolvedActivity = if (activity.id <= 0L || activityRepository.getActivityById(activity.id) == null) {
                val newId = activityRepository.saveActivity(activity.copy(id = 0L))
                activity.copy(id = newId)
            } else {
                activity
            }
            scheduleRepository.scheduleActivity(
                activity = resolvedActivity,
                date = LocalDate.now().toString(),
                startHour = startHour,
                startMinute = startMinute,
                durationMinutes = durationMinutes
            )
            _uiState.value = _uiState.value.copy(
                showScheduleSheet = false,
                feedbackMessage = "'${resolvedActivity.title}' agendada com sucesso para hoje!"
            )
        }
    }

    fun moveEntryUp(entry: ScheduleEntryModel) {
        val list = _uiState.value.todayScheduledEntries.toMutableList()
        val index = list.indexOfFirst { it.id == entry.id }
        if (index > 0) {
            val prev = list[index - 1]
            if (prev.activity?.type == ActivityType.FIXED || entry.activity?.type == ActivityType.FIXED) {
                _uiState.value = _uiState.value.copy(feedbackMessage = "Esse horário é fixo. Quer escolher outro?")
                return
            }
            val updatedCurrent = entry.copy(
                startHour = prev.startHour,
                startMinute = prev.startMinute,
                endHour = (prev.startHour * 60 + prev.startMinute + entry.targetDurationMinutes) / 60 % 24,
                endMinute = (prev.startHour * 60 + prev.startMinute + entry.targetDurationMinutes) % 60
            )
            val updatedPrev = prev.copy(
                startHour = updatedCurrent.endHour,
                startMinute = updatedCurrent.endMinute,
                endHour = (updatedCurrent.endHour * 60 + updatedCurrent.endMinute + prev.targetDurationMinutes) / 60 % 24,
                endMinute = (updatedCurrent.endHour * 60 + updatedCurrent.endMinute + prev.targetDurationMinutes) % 60
            )
            viewModelScope.launch {
                scheduleRepository.updateEntries(listOf(updatedCurrent, updatedPrev))
            }
        }
    }

    fun moveEntryDown(entry: ScheduleEntryModel) {
        val list = _uiState.value.todayScheduledEntries.toMutableList()
        val index = list.indexOfFirst { it.id == entry.id }
        if (index >= 0 && index < list.size - 1) {
            val next = list[index + 1]
            if (next.activity?.type == ActivityType.FIXED || entry.activity?.type == ActivityType.FIXED) {
                _uiState.value = _uiState.value.copy(feedbackMessage = "Esse horário é fixo. Quer escolher outro?")
                return
            }
            val updatedNext = next.copy(
                startHour = entry.startHour,
                startMinute = entry.startMinute,
                endHour = (entry.startHour * 60 + entry.startMinute + next.targetDurationMinutes) / 60 % 24,
                endMinute = (entry.startHour * 60 + entry.startMinute + next.targetDurationMinutes) % 60
            )
            val updatedCurrent = entry.copy(
                startHour = updatedNext.endHour,
                startMinute = updatedNext.endMinute,
                endHour = (updatedNext.endHour * 60 + updatedNext.endMinute + entry.targetDurationMinutes) / 60 % 24,
                endMinute = (updatedNext.endHour * 60 + updatedNext.endMinute + entry.targetDurationMinutes) % 60
            )
            viewModelScope.launch {
                scheduleRepository.updateEntries(listOf(updatedCurrent, updatedNext))
            }
        }
    }

    fun onTaskCheckboxClicked(entry: ScheduleEntryModel) {
        if (entry.status == ScheduleStatus.COMPLETED) {
            viewModelScope.launch {
                scheduleRepository.updateStatus(entry.id, ScheduleStatus.PENDING)
            }
        } else {
            viewModelScope.launch {
                val ctx = entry.activityId?.let { contextRepository.getContextForActivityOnce(it) }
                _uiState.value = _uiState.value.copy(
                    activeCompletionEntry = entry,
                    activeCompletionContext = ctx
                )
            }
        }
    }

    fun closeCompletionDialog() {
        _uiState.value = _uiState.value.copy(
            activeCompletionEntry = null,
            activeCompletionContext = null
        )
    }

    fun completeTaskDirectly(entry: ScheduleEntryModel) {
        viewModelScope.launch {
            val now = LocalTime.now()
            val nowMin = now.hour * 60 + now.minute
            val todayStr = LocalDate.now().toString()
            val isToday = entry.date == todayStr

            val actualMinutes: Int? = when {
                !isToday -> entry.computedDurationMinutes
                nowMin <= entry.startTotalMinutes -> {
                    // Concluída antes de começar: não inventar duração fictícia
                    null
                }
                nowMin in (entry.startTotalMinutes + 1) until entry.endTotalMinutes -> {
                    // Conclusão antecipada! Libera o intervalo restante [nowMin..endTotalMinutes]
                    nowMin - entry.startTotalMinutes
                }
                else -> {
                    // Concluída no horário previsto ou posterior
                    entry.computedDurationMinutes
                }
            }

            scheduleRepository.updateStatus(
                id = entry.id,
                newStatus = ScheduleStatus.COMPLETED,
                actualDurationMinutes = actualMinutes
            )
            if (entry.activityId > 0) {
                contextRepository.advanceProgress(entry.activityId)
            }
            _uiState.value = _uiState.value.copy(
                activeCompletionEntry = null,
                feedbackMessage = "Parabéns! Atividade '${entry.activityTitle}' concluída com sucesso! 🎉"
            )
        }
    }

    fun confirmCompletion(entry: ScheduleEntryModel, actualDuration: Int?, advanceContext: Boolean) {
        viewModelScope.launch {
            val now = LocalTime.now()
            val nowMin = now.hour * 60 + now.minute
            val todayStr = LocalDate.now().toString()
            val isToday = entry.date == todayStr

            val finalActualMinutes: Int? = if (actualDuration != null) {
                actualDuration
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
            closeCompletionDialog()
            _uiState.value = _uiState.value.copy(
                feedbackMessage = "Parabéns! Atividade '${entry.activityTitle}' concluída com sucesso! 🎉"
            )
        }
    }

    fun markStillDoing(entry: ScheduleEntryModel) {
        closeCompletionDialog()
        openTimeExtension(entry)
    }

    fun markNotDone(entry: ScheduleEntryModel) {
        viewModelScope.launch {
            scheduleRepository.updateStatus(entry.id, ScheduleStatus.NOT_DONE)
            closeCompletionDialog()
        }
    }

    fun openPostponeDialog(entry: ScheduleEntryModel) {
        _uiState.value = _uiState.value.copy(
            activeCompletionEntry = null,
            activeCompletionContext = null,
            activePostponeEntry = entry
        )
    }

    fun closePostponeDialog() {
        _uiState.value = _uiState.value.copy(activePostponeEntry = null)
    }

    fun postponeToTomorrow(entry: ScheduleEntryModel) {
        viewModelScope.launch {
            val tomorrow = java.time.LocalDate.now().plusDays(1).toString()
            scheduleRepository.postponeEntry(entry.id, tomorrow)
            _uiState.value = _uiState.value.copy(
                activePostponeEntry = null,
                feedbackMessage = "'${entry.activityTitle}' reagendada para amanhã!"
            )
        }
    }

    fun removeEntryFromDay(entry: ScheduleEntryModel) {
        viewModelScope.launch {
            scheduleRepository.deleteEntry(entry.id)
            _uiState.value = _uiState.value.copy(
                activePostponeEntry = null,
                feedbackMessage = "'${entry.activityTitle}' removida da rotina de hoje."
            )
        }
    }

    fun deleteActivity(activity: ActivityModel) {
        viewModelScope.launch {
            activityRepository.deleteActivity(activity)
        }
    }

    fun duplicateActivity(activity: ActivityModel) {
        viewModelScope.launch {
            activityRepository.duplicateActivity(activity)
        }
    }

    fun reorderTodayEntries(fromIndex: Int, toIndex: Int) {
        val entries = _uiState.value.todayScheduledEntries
        val fixed = _uiState.value.fixedCommitments
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
            if (result.conflictWarning != null) {
                _uiState.value = _uiState.value.copy(feedbackMessage = result.conflictWarning)
            } else {
                _uiState.value = _uiState.value.copy(feedbackMessage = "Ordem das atividades atualizada!")
            }
        }
    }

    companion object {
        fun provideFactory(
            preferencesRepository: UserPreferencesRepository,
            activityRepository: ActivityRepository,
            scheduleRepository: ScheduleRepository,
            contextRepository: ContextRepository,
            learningRepository: LearningRepository,
            focusSessionManager: FocusSessionManager,
            usageMonitoringService: UsageMonitoringService? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(
                    preferencesRepository,
                    activityRepository,
                    scheduleRepository,
                    contextRepository,
                    learningRepository,
                    focusSessionManager,
                    usageMonitoringService
                ) as T
            }
        }
    }
}
