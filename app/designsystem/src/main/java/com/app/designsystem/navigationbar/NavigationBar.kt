package com.app.designsystem.navigationbar

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobTrackTopAppBar(title: String = "JobTrack Pro", onBack: (() -> Unit)? = null) {
    TopAppBar(title = { Text(title) }, navigationIcon = {
        onBack?.let { back -> TextButton(onClick = back) { Text("Voltar") } }
    })
}
