package com.app.feature.jobs

import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.text.HtmlCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.core.common.*
import com.app.designsystem.*

@Composable
fun JobsScreen(viewModel: JobsViewModel) {
    val sourceNote = stringResource(R.string.jobs_source_note)
    val unknownLocation = stringResource(R.string.jobs_location_unknown)
    val state = viewModel.state.collectAsStateWithLifecycle().value
    val save = viewModel.saveState.collectAsStateWithLifecycle().value
    val query = viewModel.query.collectAsStateWithLifecycle().value
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = Spacing.medium),
        contentPadding = PaddingValues(vertical = Spacing.medium), verticalArrangement = Arrangement.spacedBy(Spacing.medium)) {
        item {
            Text(stringResource(R.string.jobs_heading), style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(R.string.jobs_attribution))
            JobTrackTextField(query, viewModel::search, stringResource(R.string.jobs_search))
        }
        save.failure?.let { item { Text(stringResource(it.messageRes()), color = MaterialTheme.colorScheme.error) } }
        when (state) {
            UiState.Loading -> item { JobTrackLoading() }
            is UiState.Error -> item { JobTrackErrorState(stringResource(state.failure.messageRes()), viewModel::retry) }
            is UiState.Success -> {
                val filtered = state.data
                if (filtered.isEmpty()) item { JobTrackEmptyState(stringResource(R.string.jobs_empty), stringResource(R.string.jobs_empty_description)) }
                items(filtered, key = { it.id }) { job ->
                    JobTrackCard {
                        Text(job.title, style = MaterialTheme.typography.titleMedium)
                        Text(job.company)
                        Text(job.location.ifBlank { unknownLocation })
                        Text(if (job.remote) stringResource(R.string.jobs_remote) else stringResource(R.string.jobs_on_site))
                        val description = remember(job.description) {
                            HtmlCompat.fromHtml(job.description, HtmlCompat.FROM_HTML_MODE_COMPACT).toString()
                        }
                        Text(description.take(600))
                        JobTrackButton(if (job.id in save.saved) stringResource(R.string.jobs_saved) else stringResource(R.string.jobs_save),
                            { viewModel.save(job, sourceNote, unknownLocation) }, enabled = save.saving == null && job.id !in save.saved)
                    }
                }
            }
        }
    }
}
