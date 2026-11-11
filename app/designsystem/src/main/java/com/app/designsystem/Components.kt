package com.app.designsystem

import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.app.core.model.*

fun ApplicationStatus.labelRes(): Int = when (this) {
    ApplicationStatus.SAVED -> R.string.status_saved
    ApplicationStatus.APPLIED -> R.string.status_applied
    ApplicationStatus.INTERVIEW -> R.string.status_interview
    ApplicationStatus.OFFER -> R.string.status_offer
    ApplicationStatus.REJECTED -> R.string.status_rejected
}
fun WorkMode.labelRes(): Int = when (this) {
    WorkMode.REMOTE -> R.string.work_mode_remote
    WorkMode.HYBRID -> R.string.work_mode_hybrid
    WorkMode.ON_SITE -> R.string.work_mode_on_site
}
fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.theme_system
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
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
        Text(stringResource(R.string.ds_loading), Modifier.padding(top = Spacing.small))
    }
}
@Composable
fun JobTrackEmptyState(title: String = stringResource(R.string.ds_empty_title), description: String = stringResource(R.string.ds_empty_description)) {
    JobTrackCard {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        Text(description)
    }
}
@Composable
fun JobTrackErrorState(message: String, retry: () -> Unit) {
    JobTrackCard {
        Text(message, color = MaterialTheme.colorScheme.error)
        JobTrackButton(stringResource(R.string.ds_retry), retry)
    }
}
@Composable
fun JobTrackStatusBadge(status: ApplicationStatus) { Text(stringResource(status.labelRes()), style = MaterialTheme.typography.labelLarge) }
@Composable
fun <T> JobTrackChoice(label: String, selected: T, options: List<T>, text: @Composable (T) -> String, onSelect: (T) -> Unit) {
    Column(Modifier.selectableGroup()) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        options.forEach { option ->
            Row(Modifier.fillMaxWidth().heightIn(min = Spacing.touch)
                .selectable(selected == option, role = Role.RadioButton, onClick = { onSelect(option) }),
                verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected == option, onClick = null)
                Text(text(option))
            }
        }
    }
}
