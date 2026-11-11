package com.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.core.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val auth: AuthRepository, private val preferences: PreferencesRepository, private val sync: SyncRepository,
) : ViewModel() {
    val session = auth.session
    val theme = preferences.theme.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)
    val syncState = sync.state
    private val mutableFailure = MutableStateFlow<Failure?>(null)
    val failure = mutableFailure.asStateFlow()
    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch {
            try { preferences.setTheme(mode); mutableFailure.value = null }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { mutableFailure.value = (error as? AppException)?.failure ?: Failure.UNKNOWN }
        }
    }
    fun synchronize() { viewModelScope.launch { sync.synchronize() } }
    fun signOut() = auth.signOut()
}
