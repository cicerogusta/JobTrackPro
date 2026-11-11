package com.app.feature.applications

import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.core.common.messageRes
import com.app.core.model.*
import com.app.designsystem.*

@Composable
fun ApplicationFormScreen(viewModel: ApplicationFormViewModel, onSaved: () -> Unit) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    LaunchedEffect(state.saved) { if (state.saved) onSaved() }
    ApplicationFormContent(state, viewModel::update, viewModel::save, viewModel::reloadLatest)
}
@Composable
fun ApplicationFormContent(state: FormState, update: (ApplicationDraft) -> Unit, save: () -> Unit, retry: () -> Unit) {
    val draft = state.draft
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(Spacing.medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium)) {
        Text(stringResource(R.string.application_form_title), style = MaterialTheme.typography.headlineMedium)
        if (state.loading) JobTrackLoading()
        else if (state.loadFailed) JobTrackErrorState(stringResource((state.failure ?: Failure.UNKNOWN).messageRes()), retry)
        else {
            JobTrackTextField(draft.company, { update(draft.copy(company = it)) }, stringResource(R.string.application_company), error = state.errors.message(ApplicationField.COMPANY))
            JobTrackTextField(draft.role, { update(draft.copy(role = it)) }, stringResource(R.string.application_role), error = state.errors.message(ApplicationField.ROLE))
            JobTrackTextField(draft.location, { update(draft.copy(location = it)) }, stringResource(R.string.application_location), error = state.errors.message(ApplicationField.LOCATION))
            JobTrackChoice(stringResource(R.string.application_work_mode), draft.workMode, WorkMode.entries, { stringResource(it.labelRes()) }) { update(draft.copy(workMode = it)) }
            JobTrackTextField(draft.appliedOn, { update(draft.copy(appliedOn = it)) }, stringResource(R.string.application_date_label), error = state.errors.message(ApplicationField.DATE))
            JobTrackChoice(stringResource(R.string.application_status), draft.status, ApplicationStatus.entries, { stringResource(it.labelRes()) }) { update(draft.copy(status = it)) }
            JobTrackTextField(draft.jobUrl, { update(draft.copy(jobUrl = it)) }, stringResource(R.string.application_job_url), error = state.errors.message(ApplicationField.URL))
            JobTrackTextField(draft.notes, { update(draft.copy(notes = it)) }, stringResource(R.string.application_notes), error = state.errors.message(ApplicationField.NOTES), singleLine = false)
            state.failure?.let { Text(stringResource(it.messageRes()), color = MaterialTheme.colorScheme.error) }
            if (state.failure == Failure.CONFLICT) JobTrackButton(stringResource(R.string.application_reload), retry)
            if (state.saving) JobTrackLoading()
            JobTrackButton(stringResource(R.string.application_save), save, enabled = !state.saving && !state.saved)
        }
    }
}

@Composable
private fun Set<ApplicationField>.message(field: ApplicationField): String? =
    if (field in this) stringResource(field.validationMessageRes()) else null
