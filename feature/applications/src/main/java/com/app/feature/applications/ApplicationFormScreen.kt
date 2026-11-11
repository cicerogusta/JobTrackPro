package com.app.feature.applications

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.core.common.userMessage
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
        Text("Candidatura", style = MaterialTheme.typography.headlineMedium)
        if (state.loading) JobTrackLoading()
        else if (state.loadFailed) JobTrackErrorState((state.failure ?: Failure.UNKNOWN).userMessage(), retry)
        else {
            JobTrackTextField(draft.company, { update(draft.copy(company = it)) }, "Empresa", error = state.errors[ApplicationField.COMPANY])
            JobTrackTextField(draft.role, { update(draft.copy(role = it)) }, "Cargo", error = state.errors[ApplicationField.ROLE])
            JobTrackTextField(draft.location, { update(draft.copy(location = it)) }, "Localização", error = state.errors[ApplicationField.LOCATION])
            JobTrackChoice("Modalidade", draft.workMode, WorkMode.entries, { it.label() }) { update(draft.copy(workMode = it)) }
            JobTrackTextField(draft.appliedOn, { update(draft.copy(appliedOn = it)) }, "Data (AAAA-MM-DD)", error = state.errors[ApplicationField.DATE])
            JobTrackChoice("Status", draft.status, ApplicationStatus.entries, { it.label() }) { update(draft.copy(status = it)) }
            JobTrackTextField(draft.jobUrl, { update(draft.copy(jobUrl = it)) }, "URL da vaga (HTTPS)", error = state.errors[ApplicationField.URL])
            JobTrackTextField(draft.notes, { update(draft.copy(notes = it)) }, "Observações", error = state.errors[ApplicationField.NOTES], singleLine = false)
            state.failure?.let { Text(it.userMessage(), color = MaterialTheme.colorScheme.error) }
            if (state.failure == Failure.CONFLICT) JobTrackButton("Recarregar dados atuais", retry)
            if (state.saving) JobTrackLoading()
            JobTrackButton("Salvar candidatura", save, enabled = !state.saving && !state.saved)
        }
    }
}
