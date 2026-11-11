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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DetailAction(val busy: Boolean = false, val deleted: Boolean = false, val failure: Failure? = null)
@HiltViewModel
class ApplicationDetailViewModel @Inject constructor(
    private val repository: ApplicationRepository, savedState: SavedStateHandle,
) : ViewModel() {
    private val id = requireNotNull(savedState.get<String>("id"))
    private val mutableState = MutableStateFlow<UiState<JobApplication?>>(UiState.Loading)
    val state = mutableState.asStateFlow()
    private val mutableAction = MutableStateFlow(DetailAction())
    val action = mutableAction.asStateFlow()
    private var observation: Job? = null
    init { retry() }
    fun retry() {
        observation?.cancel()
        observation = viewModelScope.launch {
            try {
                repository.observeApplications().collect { rows -> mutableState.value = UiState.Success(rows.find { it.id == id }) }
            } catch (cancelled: CancellationException) { throw cancelled }
              catch (error: Exception) { mutableState.value = UiState.Error((error as? AppException)?.failure ?: Failure.UNKNOWN) }
        }
    }
    fun delete() = mutate(deleting = true) { repository.delete(id) }
    fun changeStatus(status: ApplicationStatus) {
        val application = (state.value as? UiState.Success)?.data ?: return
        mutate { repository.save(application.copy(status = status)) }
    }
    private fun mutate(deleting: Boolean = false, operation: suspend () -> Unit) {
        if (mutableAction.value.busy) return
        mutableAction.value = DetailAction(busy = true)
        viewModelScope.launch {
            try {
                operation()
                mutableAction.value = DetailAction(deleted = deleting)
            } catch (cancelled: CancellationException) { throw cancelled }
              catch (error: Exception) { mutableAction.value = DetailAction(failure = (error as? AppException)?.failure ?: Failure.UNKNOWN) }
        }
    }
}
