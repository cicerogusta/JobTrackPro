package com.app.feature.jobs

import androidx.lifecycle.SavedStateHandle
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SaveJobState(
    val saving: String? = null,
    val saved: Set<String> = emptySet(),
    val failure: Failure? = null,
)

data class JobsUiState(
    val query: String = "",
    val location: String = "",
    val results: UiState<List<JobOpening>> = UiState.Loading,
    val loadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val pageFailure: Failure? = null,
    val save: SaveJobState = SaveJobState(),
)

@HiltViewModel
class JobsViewModel
@Inject
constructor(
    private val jobs: JobsRepository,
    private val applications: ApplicationRepository,
    private val auth: AuthRepository,
    private val savedState: SavedStateHandle = SavedStateHandle(),
) : ViewModel() {
    private val mutableState =
        MutableStateFlow(
            JobsUiState(
                query = savedState["query"] ?: "",
                location = savedState["location"] ?: "",
            )
        )
    val state = mutableState.asStateFlow()
    private var activeSearch = JobSearch(state.value.query, state.value.location).normalized()
    private var currentPage = 0
    private var request: Job? = null

    init {
        reload(activeSearch)
    }

    fun updateQuery(value: String) {
        savedState["query"] = value
        mutableState.update { it.copy(query = value) }
    }

    fun updateLocation(value: String) {
        savedState["location"] = value
        mutableState.update { it.copy(location = value) }
    }

    fun search() = reload(JobSearch(state.value.query, state.value.location).normalized())

    fun retry() = reload(activeSearch)

    private fun reload(search: JobSearch) {
        request?.cancel()
        activeSearch = search
        currentPage = 0
        mutableState.update {
            it.copy(
                results = UiState.Loading,
                hasMore = false,
                loadingMore = false,
                pageFailure = null,
            )
        }
        request = viewModelScope.launch {
            try {
                val page = jobs.getJobs(search, page = 1)
                currentPage = page.page
                mutableState.update {
                    it.copy(results = UiState.Success(page.jobs), hasMore = page.hasMore)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                mutableState.update { it.copy(results = UiState.Error(error.failure())) }
            }
        }
    }

    fun loadMore() {
        val current = state.value
        val loaded = (current.results as? UiState.Success)?.data ?: return
        if (!current.hasMore || request?.isActive == true) return
        mutableState.update { it.copy(loadingMore = true, pageFailure = null) }
        request = viewModelScope.launch {
            try {
                val page = jobs.getJobs(activeSearch, currentPage + 1)
                currentPage = page.page
                mutableState.update {
                    it.copy(
                        results = UiState.Success((loaded + page.jobs).distinctBy(JobOpening::id)),
                        loadingMore = false,
                        hasMore = page.hasMore,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                mutableState.update { it.copy(loadingMore = false, pageFailure = error.failure()) }
            }
        }
    }

    fun save(
        job: JobOpening,
        sourceNote: String,
        fallbackLocation: String,
        workMode: WorkMode? = job.workMode,
    ) {
        if (state.value.save.saving != null) return
        mutableState.update { it.copy(save = it.save.copy(saving = job.id, failure = null)) }
        viewModelScope.launch {
            try {
                val selectedMode = workMode ?: throw AppException(Failure.VALIDATION)
                val userId = auth.requireUserId()
                val id =
                    UUID.nameUUIDFromBytes(
                            "job:$userId:${job.id}".toByteArray(StandardCharsets.UTF_8)
                        )
                        .toString()
                if (applications.find(id) == null) {
                    val now = System.currentTimeMillis()
                    applications.save(
                        JobApplication(
                            id,
                            userId,
                            job.company,
                            job.title,
                            job.location.ifBlank { fallbackLocation },
                            selectedMode,
                            LocalDate.now(),
                            ApplicationStatus.SAVED,
                            job.url,
                            sourceNote,
                            now,
                            now,
                        )
                    )
                }
                mutableState.update {
                    it.copy(save = it.save.copy(saving = null, saved = it.save.saved + job.id))
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                mutableState.update {
                    it.copy(save = it.save.copy(saving = null, failure = error.failure()))
                }
            }
        }
    }

    private fun Exception.failure(): Failure = (this as? AppException)?.failure ?: Failure.UNKNOWN
}
