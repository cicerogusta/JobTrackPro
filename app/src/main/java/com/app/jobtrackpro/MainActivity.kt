package com.app.jobtrackpro

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.app.feature.applications.ApplicationNavigation
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), ApplicationNavigation {
    private val viewModel: AppViewModel by viewModels()
    var applicationNavigation: ((String) -> Unit)? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycleScope.launch { repeatOnLifecycle(Lifecycle.State.STARTED) { viewModel.syncWhileStarted() } }
        setContent { JobTrackApp(viewModel, this) }
    }
    override fun openApplication(id: String) { applicationNavigation?.invoke(id) }
}
