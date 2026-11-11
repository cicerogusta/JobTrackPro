package com.app.feature.jobs

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
    val state = viewModel.state.collectAsStateWithLifecycle().value
    val save = viewModel.saveState.collectAsStateWithLifecycle().value
    val query = viewModel.query.collectAsStateWithLifecycle().value
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = Spacing.medium),
        contentPadding = PaddingValues(vertical = Spacing.medium), verticalArrangement = Arrangement.spacedBy(Spacing.medium)) {
        item {
            Text("Encontre oportunidades", style = MaterialTheme.typography.headlineMedium)
            Text("Fonte: Arbeitnow • vagas principalmente na Europa")
            JobTrackTextField(query, viewModel::search, "Pesquisar vagas carregadas")
        }
        save.failure?.let { item { Text(it.userMessage(), color = MaterialTheme.colorScheme.error) } }
        when (state) {
            UiState.Loading -> item { JobTrackLoading() }
            is UiState.Error -> item { JobTrackErrorState(state.failure.userMessage(), viewModel::retry) }
            is UiState.Success -> {
                val filtered = state.data
                if (filtered.isEmpty()) item { JobTrackEmptyState("Nenhuma vaga encontrada", "Tente outra pesquisa.") }
                items(filtered, key = { it.id }) { job ->
                    JobTrackCard {
                        Text(job.title, style = MaterialTheme.typography.titleMedium)
                        Text(job.company)
                        Text(job.location)
                        Text(if (job.remote) "Remoto" else "Presencial")
                        val description = remember(job.description) {
                            HtmlCompat.fromHtml(job.description, HtmlCompat.FROM_HTML_MODE_COMPACT).toString()
                        }
                        Text(description.take(600))
                        JobTrackButton(if (job.id in save.saved) "Vaga salva" else "Salvar vaga",
                            { viewModel.save(job) }, enabled = save.saving == null && job.id !in save.saved)
                    }
                }
            }
        }
    }
}
