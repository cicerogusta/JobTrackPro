package com.app.feature.jobs

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.core.text.HtmlCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.core.common.*
import com.app.core.model.*
import com.app.designsystem.*

@Composable
fun JobsScreen(viewModel: JobsViewModel) {
    val sourceNote = stringResource(R.string.jobs_source_note)
    val unknownLocation = stringResource(R.string.jobs_location_unknown)
    JobsContent(
        state = viewModel.state.collectAsStateWithLifecycle().value,
        onQueryChange = viewModel::updateQuery,
        onLocationChange = viewModel::updateLocation,
        onSearch = viewModel::search,
        onRetry = viewModel::retry,
        onLoadMore = viewModel::loadMore,
        onSave = { job, mode -> viewModel.save(job, sourceNote, unknownLocation, mode) },
    )
}

@Composable
fun JobsContent(
    state: JobsUiState,
    onQueryChange: (String) -> Unit,
    onLocationChange: (String) -> Unit,
    onSearch: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onSave: (JobOpening, WorkMode) -> Unit,
) {
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    var linkError by remember { mutableStateOf(false) }
    var pendingJob by remember { mutableStateOf<JobOpening?>(null) }
    var selectedMode by remember { mutableStateOf(WorkMode.REMOTE) }
    LazyColumn(
        modifier = Modifier.fillMaxSize().imePadding().padding(horizontal = Spacing.medium),
        contentPadding = PaddingValues(vertical = Spacing.medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.medium)) {
                Text(
                    stringResource(R.string.jobs_heading),
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(stringResource(R.string.jobs_attribution))
                JobTrackTextField(state.query, onQueryChange, stringResource(R.string.jobs_search))
                JobTrackTextField(
                    state.location,
                    onLocationChange,
                    stringResource(R.string.jobs_location_search),
                )
                JobTrackButton(
                    stringResource(R.string.jobs_search_action),
                    {
                        focus.clearFocus()
                        keyboard?.hide()
                        onSearch()
                    },
                )
            }
        }
        state.save.failure?.let {
            item { Text(stringResource(it.messageRes()), color = MaterialTheme.colorScheme.error) }
        }
        if (linkError)
            item {
                Text(
                    stringResource(R.string.jobs_no_browser),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        when (val result = state.results) {
            UiState.Loading -> item { JobTrackLoading() }
            is UiState.Error ->
                item { JobTrackErrorState(stringResource(result.failure.messageRes()), onRetry) }
            is UiState.Success -> {
                if (result.data.isEmpty())
                    item {
                        JobTrackEmptyState(
                            stringResource(R.string.jobs_empty),
                            stringResource(R.string.jobs_empty_description),
                        )
                    }
                else item { Text(stringResource(R.string.jobs_count, result.data.size)) }
                items(result.data, key = JobOpening::id) { job ->
                    JobOpeningCard(
                        job,
                        state.save,
                        onOpen = {
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, job.url.toUri()))
                            } catch (_: ActivityNotFoundException) {
                                linkError = true
                            }
                        },
                        onSave = {
                            val mode = job.workMode
                            if (mode == null) pendingJob = job else onSave(job, mode)
                        },
                    )
                }
                state.pageFailure?.let {
                    item {
                        Text(
                            stringResource(it.messageRes()),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                if (state.loadingMore) item { JobTrackLoading() }
                if (state.hasMore)
                    item {
                        JobTrackButton(
                            stringResource(R.string.jobs_load_more),
                            onLoadMore,
                            enabled = !state.loadingMore,
                        )
                    }
            }
        }
    }
    pendingJob?.let { job ->
        AlertDialog(
            onDismissRequest = { pendingJob = null },
            title = { Text(stringResource(R.string.jobs_mode_title)) },
            text = {
                JobTrackChoice(
                    stringResource(R.string.jobs_mode_description),
                    selectedMode,
                    WorkMode.entries,
                    { stringResource(it.labelRes()) },
                    { selectedMode = it },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingJob = null
                        onSave(job, selectedMode)
                    }
                ) {
                    Text(stringResource(R.string.jobs_save_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingJob = null }) {
                    Text(stringResource(R.string.jobs_cancel))
                }
            },
        )
    }
}

@Composable
private fun JobOpeningCard(
    job: JobOpening,
    save: SaveJobState,
    onOpen: () -> Unit,
    onSave: () -> Unit,
) {
    val unknownLocation = stringResource(R.string.jobs_location_unknown)
    JobTrackCard {
        Text(job.title, style = MaterialTheme.typography.titleMedium)
        Text(job.company)
        Text(job.location.ifBlank { unknownLocation })
        Text(stringResource(job.workMode?.labelRes() ?: R.string.jobs_mode_unknown))
        val description =
            remember(job.description) {
                HtmlCompat.fromHtml(job.description, HtmlCompat.FROM_HTML_MODE_COMPACT).toString()
            }
        Text(description.take(600))
        Column(
            modifier = Modifier.padding(top = Spacing.small),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        ) {
            TextButton(onClick = onOpen) { Text(stringResource(R.string.jobs_open_source)) }
            JobTrackButton(
                text =
                    stringResource(
                        if (job.id in save.saved) R.string.jobs_saved else R.string.jobs_save
                    ),
                onClick = onSave,
                enabled = save.saving == null && job.id !in save.saved,
            )
        }
    }
}
