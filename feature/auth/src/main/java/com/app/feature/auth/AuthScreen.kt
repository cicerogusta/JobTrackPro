package com.app.feature.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.core.common.userMessage
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
        Text("JobTrack Pro", style = MaterialTheme.typography.headlineLarge)
        Text(when (mode) { AuthMode.LOGIN -> "Entrar"; AuthMode.REGISTER -> "Criar conta"; AuthMode.RESET -> "Recuperar senha" },
            style = MaterialTheme.typography.titleLarge)
        if (mode == AuthMode.REGISTER) JobTrackTextField(name, { name = it }, "Nome")
        JobTrackTextField(email, { email = it }, "E-mail")
        if (mode != AuthMode.RESET) {
            OutlinedTextField(password, { password = it }, label = { Text("Senha") },
                singleLine = true, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation())
            if (mode == AuthMode.REGISTER) Text("Use uma senha com pelo menos 8 caracteres.")
        }
        state.failure?.let { Text(it.userMessage(), color = MaterialTheme.colorScheme.error) }
        if (state.resetSent) Text("Se houver uma conta para este e-mail, você receberá as instruções.")
        if (state.busy) JobTrackLoading()
        JobTrackButton(when (mode) { AuthMode.LOGIN -> "Entrar"; AuthMode.REGISTER -> "Cadastrar"; AuthMode.RESET -> "Enviar recuperação" },
            { viewModel.submit(mode, email, password, name) }, enabled = !state.busy)
        TextButton(enabled = !state.busy, onClick = { mode = if (mode == AuthMode.REGISTER) AuthMode.LOGIN else AuthMode.REGISTER; password = "" }) {
            Text(if (mode == AuthMode.REGISTER) "Já tenho uma conta" else "Criar uma conta")
        }
        TextButton(enabled = !state.busy, onClick = { mode = if (mode == AuthMode.RESET) AuthMode.LOGIN else AuthMode.RESET; password = "" }) {
            Text(if (mode == AuthMode.RESET) "Voltar ao login" else "Esqueci minha senha")
        }
    }
}
