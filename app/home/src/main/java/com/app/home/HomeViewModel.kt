package com.app.home

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

data class HomeData(val stats: DashboardStats, val recent: List<JobApplication>)
@HiltViewModel
class HomeViewModel @Inject constructor(private val repository: ApplicationRepository) : ViewModel() {
    private val mutableState = MutableStateFlow<UiState<HomeData>>(UiState.Loading)
    val state = mutableState.asStateFlow()
    private var observation: Job? = null
    init { retry() }
    fun retry() {
        observation?.cancel()
        observation = viewModelScope.launch {
            mutableState.value = UiState.Loading
            try {
                repository.observeApplications().collect { applications ->
                    mutableState.value = UiState.Success(HomeData(DashboardStats.from(applications), applications.take(5)))
                }
            } catch (cancelled: CancellationException) { throw cancelled }
              catch (error: Exception) { mutableState.value = UiState.Error((error as? AppException)?.failure ?: Failure.UNKNOWN) }
        }
    }
}
