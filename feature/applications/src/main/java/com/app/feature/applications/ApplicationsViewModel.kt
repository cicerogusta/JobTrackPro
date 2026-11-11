package com.app.feature.applications

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.core.common.UiState
import com.app.core.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@HiltViewModel
class ApplicationsViewModel @Inject constructor(
    private val repository: ApplicationRepository, private val savedState: SavedStateHandle,
) : ViewModel() {
    private val filter = MutableStateFlow(ApplicationFilter(
        savedState["query"] ?: "", savedState.get<String>("status")?.let(ApplicationStatus::valueOf)))
    private val mutableState = MutableStateFlow<UiState<List<JobApplication>>>(UiState.Loading)
    val state = mutableState.asStateFlow()
    val currentFilter: ApplicationFilter get() = filter.value
    private var observation: Job? = null
    init { retry() }
    fun search(query: String) { savedState["query"] = query; filter.value = filter.value.copy(query = query) }
    fun filter(status: ApplicationStatus?) { savedState["status"] = status?.name; filter.value = filter.value.copy(status = status) }
    fun retry() {
        observation?.cancel()
        observation = viewModelScope.launch {
            mutableState.value = UiState.Loading
            try {
                combine(repository.observeApplications(), filter) { rows, selected -> rows.filter(selected::matches) }
                    .collect { mutableState.value = UiState.Success(it) }
            } catch (cancelled: CancellationException) { throw cancelled }
              catch (error: Exception) { mutableState.value = UiState.Error((error as? AppException)?.failure ?: Failure.UNKNOWN) }
        }
    }
}
