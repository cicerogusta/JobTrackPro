package com.app.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.core.common.UiState
import com.app.core.common.messageRes
import com.app.core.model.JobApplication
import com.app.designsystem.*

@Composable
fun HomeScreen(viewModel: HomeViewModel, onAdd: () -> Unit, onOpen: (String) -> Unit) {
    HomeContent(
        viewModel.state.collectAsStateWithLifecycle().value,
        onAdd,
        onOpen,
        viewModel::retry,
    )
}

@Composable
fun HomeContent(
    state: UiState<HomeData>,
    onAdd: () -> Unit,
    onOpen: (String) -> Unit,
    onRetry: () -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = Spacing.medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        contentPadding = PaddingValues(vertical = Spacing.medium),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.medium)) {
                Text(
                    stringResource(R.string.home_heading),
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(stringResource(R.string.home_description))
                JobTrackButton(stringResource(R.string.home_add_application), onAdd)
            }
        }
        when (state) {
            UiState.Loading -> item { JobTrackLoading() }
            is UiState.Error ->
                item { JobTrackErrorState(stringResource(state.failure.messageRes()), onRetry) }
            is UiState.Success -> {
                item {
                    JobTrackCard {
                        Text(
                            stringResource(R.string.home_overview),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(stringResource(R.string.home_total, state.data.stats.total))
                        Text(stringResource(R.string.home_applied, state.data.stats.applied))
                        Text(stringResource(R.string.home_interviews, state.data.stats.interviews))
                        Text(stringResource(R.string.home_offers, state.data.stats.offers))
                    }
                }
                if (state.data.recent.isEmpty()) item { JobTrackEmptyState() }
                else {
                    item {
                        Text(
                            stringResource(R.string.home_recent),
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                    items(state.data.recent, key = JobApplication::id) { application ->
                        JobTrackCard {
                            Text(application.role, style = MaterialTheme.typography.titleMedium)
                            Text(application.company)
                            JobTrackStatusBadge(application.status)
                            TextButton(onClick = { onOpen(application.id) }) {
                                Text(stringResource(R.string.home_open_details, application.role))
                            }
                        }
                    }
                }
            }
        }
    }
}
