package com.app.feature.applications

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.core.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

data class FormState(
    val draft: ApplicationDraft = ApplicationDraft(), val errors: Map<ApplicationField, String> = emptyMap(),
    val loading: Boolean = false, val saving: Boolean = false, val saved: Boolean = false,
    val failure: Failure? = null, val loadFailed: Boolean = false,
)
@HiltViewModel
class ApplicationFormViewModel @Inject constructor(
    private val repository: ApplicationRepository,
    private val auth: AuthRepository,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private val id: String = savedState.get<String>("id").orEmpty()
    private var original: JobApplication? = null
    private val mutableState = MutableStateFlow(FormState(loading = id.isNotBlank(),
        draft = savedState.get<ApplicationDraft>("draft") ?: ApplicationDraft()))
    val state = mutableState.asStateFlow()
    private val newId: String = savedState.get<String>("newId") ?: UUID.randomUUID().toString().also { savedState["newId"] = it }
    init { if (id.isNotBlank()) load() }
    fun update(draft: ApplicationDraft) {
        if (mutableState.value.saving || mutableState.value.loading) return
        savedState["draft"] = draft
        mutableState.value = mutableState.value.copy(draft = draft, errors = emptyMap(), failure = null)
    }
    fun load() {
        mutableState.value = mutableState.value.copy(loading = true, loadFailed = false, failure = null)
        viewModelScope.launch {
            try {
                val application = repository.find(id) ?: throw AppException(Failure.VALIDATION)
                val baseRevision = savedState.get<String>("baseRevision") ?: application.revision.also { savedState["baseRevision"] = it }
                original = application.copy(revision = baseRevision)
                val draft = savedState.get<ApplicationDraft>("draft") ?: ApplicationDraft(application.company,
                    application.role, application.location, application.workMode, application.appliedOn.toString(),
                    application.status, application.jobUrl, application.notes)
                mutableState.value = FormState(draft)
            } catch (cancelled: CancellationException) { throw cancelled }
              catch (error: Exception) { mutableState.value = mutableState.value.copy(loading = false, loadFailed = true,
                  failure = (error as? AppException)?.failure ?: Failure.UNKNOWN) }
        }
    }
    fun reloadLatest() {
        savedState.remove<ApplicationDraft>("draft")
        savedState.remove<String>("baseRevision")
        load()
    }
    fun save() {
        val current = mutableState.value
        if (current.saving || current.loading || current.saved || current.loadFailed) return
        val errors = ApplicationValidator.validate(current.draft)
        if (errors.isNotEmpty()) { mutableState.value = current.copy(errors = errors); return }
        mutableState.value = current.copy(saving = true, failure = null)
        viewModelScope.launch {
            try {
                val draft = current.draft
                val now = System.currentTimeMillis()
                repository.save(JobApplication(original?.id ?: newId, auth.requireUserId(), draft.company,
                    draft.role, draft.location, draft.workMode, LocalDate.parse(draft.appliedOn), draft.status,
                    draft.jobUrl, draft.notes, original?.createdAt ?: now, original?.updatedAt ?: now,
                    revision = original?.revision.orEmpty()))
                mutableState.value = mutableState.value.copy(saving = false, saved = true)
            } catch (cancelled: CancellationException) { throw cancelled }
              catch (error: Exception) { mutableState.value = mutableState.value.copy(saving = false,
                  failure = (error as? AppException)?.failure ?: Failure.UNKNOWN) }
        }
    }
}
