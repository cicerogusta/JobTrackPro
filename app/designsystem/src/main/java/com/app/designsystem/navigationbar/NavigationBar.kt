package com.app.designsystem.navigationbar

import com.app.designsystem.R
import androidx.compose.ui.res.stringResource

import androidx.compose.material3.*
import androidx.compose.runtime.Composable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobTrackTopAppBar(title: String = stringResource(R.string.ds_app_name), onBack: (() -> Unit)? = null) {
    TopAppBar(title = { Text(title) }, navigationIcon = {
        onBack?.let { back -> TextButton(onClick = back) { Text(stringResource(R.string.ds_back)) } }
    })
}
