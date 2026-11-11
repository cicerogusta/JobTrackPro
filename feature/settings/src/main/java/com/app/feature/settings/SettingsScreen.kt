package com.app.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.core.common.userMessage
import com.app.core.model.*
import com.app.designsystem.*

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val session = viewModel.session.collectAsStateWithLifecycle().value
    val theme = viewModel.theme.collectAsStateWithLifecycle().value
    val sync = viewModel.syncState.collectAsStateWithLifecycle().value
    val failure = viewModel.failure.collectAsStateWithLifecycle().value
    var confirm by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium)) {
        Text("Configurações", style = MaterialTheme.typography.headlineMedium)
        (session as? Session.Authenticated)?.user?.let { user ->
            JobTrackCard { Text(user.displayName.ifBlank { "Meu perfil" }); Text(user.email) }
        }
        JobTrackChoice("Tema", theme, ThemeMode.entries, { it.label() }, viewModel::setTheme)
        failure?.let { Text(it.userMessage(), color = MaterialTheme.colorScheme.error) }
        Text(when (sync.phase) {
            SyncPhase.IDLE -> "Aguardando sincronização."
            SyncPhase.RUNNING -> "Sincronizando…"
            SyncPhase.SUCCESS -> "Última sincronização concluída."
            SyncPhase.ERROR -> sync.failure?.userMessage() ?: "Falha na sincronização."
            SyncPhase.UNAVAILABLE -> "Entre na sua conta para sincronizar."
        })
        JobTrackButton("Sincronizar agora", viewModel::synchronize, enabled = sync.phase != SyncPhase.RUNNING)
        JobTrackCard {
            Text("Sobre o JobTrack Pro", style = MaterialTheme.typography.titleMedium)
            Text("Versão 1.0 • Organize o próximo passo da sua carreira.")
            Text("Suas candidaturas ficam disponíveis localmente. Alterações pendentes serão sincronizadas ao retomar o aplicativo com conexão.")
        }
        JobTrackButton("Sair da conta", { confirm = true })
    }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text("Sair da conta?") },
        text = { Text("Alterações ainda não sincronizadas permanecem neste aparelho. Volte com a mesma conta para enviá-las.") },
        confirmButton = { TextButton(onClick = { confirm = false; viewModel.signOut() }) { Text("Sair") } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancelar") } })
}
