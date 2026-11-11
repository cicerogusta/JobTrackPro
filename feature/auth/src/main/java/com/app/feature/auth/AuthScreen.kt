package com.app.feature.auth

import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.core.common.messageRes
import com.app.designsystem.*

@Composable
fun AuthScreen(viewModel: AuthViewModel) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    var mode by rememberSaveable { mutableStateOf(AuthMode.LOGIN) }
    var email by rememberSaveable { mutableStateOf("") }
    // Password stays in memory only; it is deliberately excluded from saved state.
    var password by remember { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(Spacing.large),
        verticalArrangement = Arrangement.spacedBy(Spacing.medium)) {
        Text(stringResource(R.string.auth_app_name), style = MaterialTheme.typography.headlineLarge)
        Text(when (mode) { AuthMode.LOGIN -> stringResource(R.string.auth_login); AuthMode.REGISTER -> stringResource(R.string.auth_register_title); AuthMode.RESET -> stringResource(R.string.auth_reset_title) },
            style = MaterialTheme.typography.titleLarge)
        if (mode == AuthMode.REGISTER) JobTrackTextField(name, { name = it }, stringResource(R.string.auth_name))
        JobTrackTextField(email, { email = it }, stringResource(R.string.auth_email))
        if (mode != AuthMode.RESET) {
            OutlinedTextField(password, { password = it }, label = { Text(stringResource(R.string.auth_password)) },
                singleLine = true, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation())
            if (mode == AuthMode.REGISTER) Text(stringResource(R.string.auth_password_hint))
        }
        state.failure?.let { Text(stringResource(it.messageRes()), color = MaterialTheme.colorScheme.error) }
        if (state.resetSent) Text(stringResource(R.string.auth_reset_sent))
        if (state.busy) JobTrackLoading()
        JobTrackButton(when (mode) { AuthMode.LOGIN -> stringResource(R.string.auth_login); AuthMode.REGISTER -> stringResource(R.string.auth_register); AuthMode.RESET -> stringResource(R.string.auth_reset_send) },
            { viewModel.submit(mode, email, password, name) }, enabled = !state.busy)
        TextButton(enabled = !state.busy, onClick = { mode = if (mode == AuthMode.REGISTER) AuthMode.LOGIN else AuthMode.REGISTER; password = "" }) {
            Text(if (mode == AuthMode.REGISTER) stringResource(R.string.auth_already_registered) else stringResource(R.string.auth_create_account))
        }
        TextButton(enabled = !state.busy, onClick = { mode = if (mode == AuthMode.RESET) AuthMode.LOGIN else AuthMode.RESET; password = "" }) {
            Text(if (mode == AuthMode.RESET) stringResource(R.string.auth_back_to_login) else stringResource(R.string.auth_forgot_password))
        }
    }
}
