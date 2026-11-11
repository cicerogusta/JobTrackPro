package com.app.feature.applications

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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.core.common.*
import com.app.core.model.*
import com.app.designsystem.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ApplicationDetailScreen(viewModel: ApplicationDetailViewModel, onEdit: (String) -> Unit, onDeleted: () -> Unit) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    val action = viewModel.action.collectAsStateWithLifecycle().value
    var confirm by rememberSaveable { mutableStateOf(false) }
    var urlError by remember { mutableStateOf(false) }
    val context = LocalContext.current
    LaunchedEffect(action.deleted) { if (action.deleted) onDeleted() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium)) {
        when (state) {
            UiState.Loading -> JobTrackLoading()
            is UiState.Error -> JobTrackErrorState(state.failure.userMessage(), viewModel::retry)
            is UiState.Success -> {
                val application = state.data
                if (application == null) JobTrackEmptyState("Candidatura indisponível", "Ela pode ter sido excluída em outro dispositivo.")
                else {
                    Text(application.role, style = MaterialTheme.typography.headlineMedium)
                    Text(application.company, style = MaterialTheme.typography.titleLarge)
                    Text(application.location)
                    Text(application.workMode.label())
                    Text("Data da candidatura: ${application.appliedOn}")
                    Text("Criada em: ${formatTimestamp(application.createdAt)}")
                    Text("Atualizada em: ${formatTimestamp(application.updatedAt)}")
                    JobTrackChoice("Status", application.status, ApplicationStatus.entries, { it.label() }) {
                        if (!action.busy) viewModel.changeStatus(it)
                    }
                    Text("Observações", style = MaterialTheme.typography.titleMedium)
                    Text(application.notes.ifBlank { "Nenhuma observação." })
                    Text(application.jobUrl.ifBlank { "URL não informada." })
                    if (application.jobUrl.isNotBlank()) JobTrackButton("Abrir vaga", {
                        try { context.startActivity(Intent(Intent.ACTION_VIEW, application.jobUrl.toUri())) }
                        catch (_: ActivityNotFoundException) { urlError = true }
                    })
                    if (urlError) Text("Nenhum navegador disponível para abrir a vaga.")
                    JobTrackButton("Editar candidatura", { onEdit(application.id) }, enabled = !action.busy)
                    JobTrackButton("Excluir candidatura", { confirm = true }, enabled = !action.busy)
                }
            }
        }
        action.failure?.let { Text(it.userMessage(), color = MaterialTheme.colorScheme.error) }
        if (action.busy) JobTrackLoading()
    }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text("Excluir candidatura?") },
        text = { Text("A exclusão será sincronizada com seus outros dispositivos.") },
        confirmButton = { TextButton(onClick = { confirm = false; viewModel.delete() }) { Text("Excluir") } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancelar") } })
}
private fun formatTimestamp(value: Long): String =
    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(value))
