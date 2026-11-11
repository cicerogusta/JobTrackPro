package com.app.designsystem

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.app.core.model.*

fun ApplicationStatus.label(): String = when (this) {
    ApplicationStatus.SAVED -> "Salva"
    ApplicationStatus.APPLIED -> "Enviada"
    ApplicationStatus.INTERVIEW -> "Entrevista"
    ApplicationStatus.OFFER -> "Oferta"
    ApplicationStatus.REJECTED -> "Recusada"
}
fun WorkMode.label(): String = when (this) {
    WorkMode.REMOTE -> "Remoto"
    WorkMode.HYBRID -> "Híbrido"
    WorkMode.ON_SITE -> "Presencial"
}
fun ThemeMode.label(): String = when (this) {
    ThemeMode.SYSTEM -> "Sistema"
    ThemeMode.LIGHT -> "Claro"
    ThemeMode.DARK -> "Escuro"
}
@Composable
fun JobTrackButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(onClick = onClick, enabled = enabled, modifier = modifier.heightIn(min = Spacing.touch)) { Text(text) }
}
@Composable
fun JobTrackTextField(value: String, onValueChange: (String) -> Unit, label: String,
    modifier: Modifier = Modifier, error: String? = null, singleLine: Boolean = true) {
    OutlinedTextField(value = value, onValueChange = onValueChange, label = { Text(label) },
        modifier = modifier.fillMaxWidth(), singleLine = singleLine, isError = error != null,
        supportingText = error?.let { message -> { Text(message) } })
}
@Composable
fun JobTrackCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(modifier.fillMaxWidth()) { Column(Modifier.padding(Spacing.medium),
        verticalArrangement = Arrangement.spacedBy(Spacing.small), content = content) }
}
@Composable
fun JobTrackLoading() {
    Column(Modifier.fillMaxWidth().padding(Spacing.large)) {
        CircularProgressIndicator()
        Text("Carregando…", Modifier.padding(top = Spacing.small))
    }
}
@Composable
fun JobTrackEmptyState(title: String = "Nenhuma candidatura ainda", description: String = "Adicione uma candidatura para começar.") {
    JobTrackCard {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        Text(description)
    }
}
@Composable
fun JobTrackErrorState(message: String, retry: () -> Unit) {
    JobTrackCard {
        Text(message, color = MaterialTheme.colorScheme.error)
        JobTrackButton("Tentar novamente", retry)
    }
}
@Composable
fun JobTrackStatusBadge(status: ApplicationStatus) { Text(status.label(), style = MaterialTheme.typography.labelLarge) }
@Composable
fun <T> JobTrackChoice(label: String, selected: T, options: List<T>, text: (T) -> String, onSelect: (T) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge)
        options.forEach { option ->
            Row {
                RadioButton(selected == option, onClick = { onSelect(option) })
                TextButton(onClick = { onSelect(option) }, modifier = Modifier.weight(1f)) { Text(text(option)) }
            }
        }
    }
}
