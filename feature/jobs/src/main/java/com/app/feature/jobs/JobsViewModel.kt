package com.app.feature.jobs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.core.common.UiState
import com.app.core.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SaveJobState(val saving: String? = null, val saved: Set<String> = emptySet(), val failure: Failure? = null)
@HiltViewModel
class JobsViewModel @Inject constructor(
    private val jobs: JobsRepository, private val applications: ApplicationRepository, private val auth: AuthRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow<UiState<List<JobOpening>>>(UiState.Loading)
    val state = mutableState.asStateFlow()
    private val mutableSaveState = MutableStateFlow(SaveJobState())
    val saveState = mutableSaveState.asStateFlow()
    private var request: Job? = null
    init { retry() }
    fun retry() {
        if (request?.isActive == true) return
        request = viewModelScope.launch {
            mutableState.value = UiState.Loading
            try { mutableState.value = UiState.Success(jobs.getJobs()) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { mutableState.value = UiState.Error((error as? AppException)?.failure ?: Failure.UNKNOWN) }
        }
    }
    fun save(job: JobOpening) {
        if (mutableSaveState.value.saving != null) return
        mutableSaveState.value = mutableSaveState.value.copy(saving = job.id, failure = null)
        viewModelScope.launch {
            try {
                val userId = auth.requireUserId()
                val id = UUID.nameUUIDFromBytes("arbeitnow:$userId:${job.id}".toByteArray(StandardCharsets.UTF_8)).toString()
                if (applications.find(id) == null) {
                    val now = System.currentTimeMillis()
                    applications.save(JobApplication(id, userId, job.company, job.title, job.location,
                        if (job.remote) WorkMode.REMOTE else WorkMode.ON_SITE, LocalDate.now(), ApplicationStatus.SAVED,
                        job.url, "Fonte: Arbeitnow", now, now))
                }
                mutableSaveState.value = mutableSaveState.value.copy(saving = null, saved = mutableSaveState.value.saved + job.id)
            } catch (cancelled: CancellationException) { throw cancelled }
              catch (error: Exception) { mutableSaveState.value = mutableSaveState.value.copy(saving = null,
                  failure = (error as? AppException)?.failure ?: Failure.UNKNOWN) }
        }
    }
}
