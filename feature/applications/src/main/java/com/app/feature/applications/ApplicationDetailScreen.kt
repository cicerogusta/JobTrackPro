package com.app.feature.applications

import androidx.compose.ui.res.stringResource

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.core.net.toUri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.core.common.*
import com.app.core.model.*
import com.app.designsystem.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun ApplicationDetailScreen(viewModel: ApplicationDetailViewModel, onEdit: (String) -> Unit, onDeleted: () -> Unit) {
    val noNotes = stringResource(R.string.application_no_notes)
    val noUrl = stringResource(R.string.application_no_url)
    val state = viewModel.state.collectAsStateWithLifecycle().value
    val action = viewModel.action.collectAsStateWithLifecycle().value
    var confirm by rememberSaveable { mutableStateOf(false) }
    var urlError by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    LaunchedEffect(action.deleted) { if (action.deleted) onDeleted() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium)) {
        when (state) {
            UiState.Loading -> JobTrackLoading()
            is UiState.Error -> JobTrackErrorState(stringResource(state.failure.messageRes()), viewModel::retry)
            is UiState.Success -> {
                val application = state.data
                if (application == null) JobTrackEmptyState(stringResource(R.string.application_unavailable), stringResource(R.string.application_unavailable_description))
                else {
                    Text(application.role, style = MaterialTheme.typography.headlineMedium)
                    Text(application.company, style = MaterialTheme.typography.titleLarge)
                    Text(application.location)
                    Text(stringResource(application.workMode.labelRes()))
                    Text(stringResource(R.string.application_applied_on, application.appliedOn))
                    Text(stringResource(R.string.application_created_at, formatTimestamp(application.createdAt, locale)))
                    Text(stringResource(R.string.application_updated_at, formatTimestamp(application.updatedAt, locale)))
                    JobTrackChoice(stringResource(R.string.application_status), application.status, ApplicationStatus.entries, { stringResource(it.labelRes()) }) {
                        if (!action.busy) viewModel.changeStatus(it)
                    }
                    Text(stringResource(R.string.application_notes), style = MaterialTheme.typography.titleMedium)
                    Text(application.notes.ifBlank { noNotes })
                    Text(application.jobUrl.ifBlank { noUrl })
                    if (application.jobUrl.isNotBlank()) JobTrackButton(stringResource(R.string.application_open_url), {
                        try { context.startActivity(Intent(Intent.ACTION_VIEW, application.jobUrl.toUri())) }
                        catch (_: ActivityNotFoundException) { urlError = true }
                    })
                    if (urlError) Text(stringResource(R.string.application_no_browser))
                    JobTrackButton(stringResource(R.string.application_edit), { onEdit(application.id) }, enabled = !action.busy)
                    JobTrackButton(stringResource(R.string.application_delete), { confirm = true }, enabled = !action.busy)
                }
            }
        }
        action.failure?.let { Text(stringResource(it.messageRes()), color = MaterialTheme.colorScheme.error) }
        if (action.busy) JobTrackLoading()
    }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text(stringResource(R.string.application_delete_title)) },
        text = { Text(stringResource(R.string.application_delete_message)) },
        confirmButton = { TextButton(onClick = { confirm = false; viewModel.delete() }) { Text(stringResource(R.string.application_delete_confirm)) } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text(stringResource(R.string.application_cancel)) } })
}
private fun formatTimestamp(value: Long, locale: Locale): String =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(locale)
        .withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(value))
