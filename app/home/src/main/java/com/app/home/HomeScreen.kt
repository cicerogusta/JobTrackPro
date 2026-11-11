package com.app.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.core.common.UiState
import com.app.core.common.userMessage
import com.app.core.model.JobApplication
import com.app.designsystem.*

@Composable
fun HomeScreen(viewModel: HomeViewModel, onAdd: () -> Unit, onOpen: (String) -> Unit) {
    HomeContent(viewModel.state.collectAsStateWithLifecycle().value, onAdd, onOpen, viewModel::retry)
}
@Composable
fun HomeContent(state: UiState<HomeData>, onAdd: () -> Unit, onOpen: (String) -> Unit, onRetry: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = Spacing.medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium),
        contentPadding = PaddingValues(vertical = Spacing.medium)) {
        item {
            Text("Seu próximo passo começa aqui", style = MaterialTheme.typography.headlineMedium)
            Text("Acompanhe suas candidaturas e oportunidades.")
            JobTrackButton("Adicionar candidatura", onAdd)
        }
        when (state) {
            UiState.Loading -> item { JobTrackLoading() }
            is UiState.Error -> item { JobTrackErrorState(state.failure.userMessage(), onRetry) }
            is UiState.Success -> {
                item { JobTrackCard {
                    Text("Visão geral", style = MaterialTheme.typography.titleLarge)
                    Text("Total: ${state.data.stats.total}")
                    Text("Enviadas: ${state.data.stats.applied}")
                    Text("Entrevistas: ${state.data.stats.interviews}")
                    Text("Ofertas: ${state.data.stats.offers}")
                } }
                if (state.data.recent.isEmpty()) item { JobTrackEmptyState() }
                else {
                    item { Text("Candidaturas recentes", style = MaterialTheme.typography.titleLarge) }
                    items(state.data.recent, key = JobApplication::id) { application ->
                        JobTrackCard {
                            Text(application.role, style = MaterialTheme.typography.titleMedium)
                            Text(application.company)
                            JobTrackStatusBadge(application.status)
                            TextButton(onClick = { onOpen(application.id) }) { Text("Ver detalhes de ${application.role}") }
                        }
                    }
                }
            }
        }
    }
}
