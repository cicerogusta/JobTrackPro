package com.app.jobtrackpro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.core.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive

@HiltViewModel
class AppViewModel @Inject constructor(
    private val auth: AuthRepository, preferences: PreferencesRepository, private val sync: SyncRepository,
) : ViewModel() {
    val session = auth.session
    val theme = preferences.theme.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)
    /** Called only while the host lifecycle is STARTED. Durable pending rows survive cancellation. */
    suspend fun syncWhileStarted() {
        auth.session.collectLatest { session ->
            if (session is Session.Authenticated) {
                while (currentCoroutineContext().isActive) {
                    sync.synchronize()
                    delay(30_000)
                }
            }
        }
    }
}
