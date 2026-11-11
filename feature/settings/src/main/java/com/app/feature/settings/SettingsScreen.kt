package com.app.feature.settings

import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.core.common.messageRes
import com.app.core.model.*
import com.app.designsystem.*

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val profileFallback = stringResource(R.string.settings_profile)
    val session = viewModel.session.collectAsStateWithLifecycle().value
    val theme = viewModel.theme.collectAsStateWithLifecycle().value
    val sync = viewModel.syncState.collectAsStateWithLifecycle().value
    val failure = viewModel.failure.collectAsStateWithLifecycle().value
    var confirm by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Spacing.medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium)) {
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineMedium)
        (session as? Session.Authenticated)?.user?.let { user ->
            JobTrackCard { Text(user.displayName.ifBlank { profileFallback }); Text(user.email) }
        }
        JobTrackChoice(stringResource(R.string.settings_theme), theme, ThemeMode.entries, { stringResource(it.labelRes()) }, viewModel::setTheme)
        failure?.let { Text(stringResource(it.messageRes()), color = MaterialTheme.colorScheme.error) }
        Text(when (sync.phase) {
            SyncPhase.IDLE -> stringResource(R.string.settings_sync_idle)
            SyncPhase.RUNNING -> stringResource(R.string.settings_sync_running)
            SyncPhase.SUCCESS -> stringResource(R.string.settings_sync_success)
            SyncPhase.ERROR -> stringResource(sync.failure?.messageRes() ?: R.string.settings_sync_error)
            SyncPhase.UNAVAILABLE -> stringResource(R.string.settings_sync_unavailable)
        })
        JobTrackButton(stringResource(R.string.settings_sync_now), viewModel::synchronize, enabled = sync.phase != SyncPhase.RUNNING)
        JobTrackCard {
            Text(stringResource(R.string.settings_about), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.settings_about_description))
            Text(stringResource(R.string.settings_offline_description))
        }
        JobTrackButton(stringResource(R.string.settings_logout), { confirm = true })
    }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text(stringResource(R.string.settings_logout_title)) },
        text = { Text(stringResource(R.string.settings_logout_message)) },
        confirmButton = { TextButton(onClick = { confirm = false; viewModel.signOut() }) { Text(stringResource(R.string.settings_logout_confirm)) } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text(stringResource(R.string.settings_cancel)) } })
}
