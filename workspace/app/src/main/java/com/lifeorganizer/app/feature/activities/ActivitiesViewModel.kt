package com.lifeorganizer.app.feature.activities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lifeorganizer.app.core.utils.StringUtils
import com.lifeorganizer.app.data.repository.ActivityRepository
import com.lifeorganizer.app.domain.model.ActivityModel
import com.lifeorganizer.app.domain.model.ActivityType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import com.lifeorganizer.app.data.repository.ContextRepository
import com.lifeorganizer.app.domain.model.ActivityContextModel

data class ActivitiesUiState(
    val searchQuery: String = "",
    val selectedCategoryFilter: String? = null,
    val selectedTypeFilter: ActivityType? = null,
    val showAddSheet: Boolean = false,
    val editingActivity: ActivityModel? = null,
    val activityToDelete: ActivityModel? = null
)

class ActivitiesViewModel(
    private val activityRepository: ActivityRepository,
    private val contextRepository: ContextRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActivitiesUiState())
    val uiState: StateFlow<ActivitiesUiState> = _uiState.asStateFlow()

    val activities: StateFlow<List<ActivityModel>> = combine(
        activityRepository.allActivities,
        _uiState
    ) { all, state ->
        all.filter { activity ->
            // Search query filter
            val matchesQuery = if (state.searchQuery.isBlank()) {
                true
            } else {
                StringUtils.fuzzyContains(activity.title, state.searchQuery) ||
                        StringUtils.fuzzyContains(activity.category, state.searchQuery) ||
                        StringUtils.fuzzyContains(activity.description, state.searchQuery)
            }

            // Category filter
            val matchesCategory = state.selectedCategoryFilter == null ||
                    activity.category.equals(state.selectedCategoryFilter, ignoreCase = true)

            // Type filter
            val matchesType = state.selectedTypeFilter == null ||
                    activity.type == state.selectedTypeFilter

            matchesQuery && matchesCategory && matchesType
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun setCategoryFilter(category: String?) {
        _uiState.value = _uiState.value.copy(selectedCategoryFilter = category)
    }

    fun setTypeFilter(type: ActivityType?) {
        _uiState.value = _uiState.value.copy(selectedTypeFilter = type)
    }

    fun openAddActivity() {
        _uiState.value = _uiState.value.copy(showAddSheet = true, editingActivity = null)
    }

    fun openEditActivity(activity: ActivityModel) {
        _uiState.value = _uiState.value.copy(showAddSheet = true, editingActivity = activity)
    }

    fun closeAddActivity() {
        _uiState.value = _uiState.value.copy(showAddSheet = false, editingActivity = null)
    }

    fun promptDeleteActivity(activity: ActivityModel) {
        _uiState.value = _uiState.value.copy(activityToDelete = activity)
    }

    fun dismissDeleteDialog() {
        _uiState.value = _uiState.value.copy(activityToDelete = null)
    }

    fun confirmDelete() {
        val toDelete = _uiState.value.activityToDelete ?: return
        viewModelScope.launch {
            activityRepository.deleteActivity(toDelete)
            _uiState.value = _uiState.value.copy(activityToDelete = null)
        }
    }

    fun duplicateActivity(activity: ActivityModel) {
        viewModelScope.launch {
            activityRepository.duplicateActivity(activity)
        }
    }

    fun saveActivity(activity: ActivityModel) {
        viewModelScope.launch {
            activityRepository.saveActivity(activity)
            closeAddActivity()
        }
    }

    fun saveActivityWithContext(activity: ActivityModel, contextModel: ActivityContextModel?) {
        viewModelScope.launch {
            val id = activityRepository.saveActivity(activity)
            if (contextModel != null) {
                val targetId = if (activity.id != 0L) activity.id else id
                contextRepository.saveContext(contextModel.copy(activityId = targetId))
            }
            closeAddActivity()
        }
    }

    companion object {
        fun provideFactory(
            repository: ActivityRepository,
            contextRepository: ContextRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ActivitiesViewModel(repository, contextRepository) as T
            }
        }
    }
}
