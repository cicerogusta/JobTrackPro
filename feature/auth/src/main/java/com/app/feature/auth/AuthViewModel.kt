package com.app.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.core.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AuthMode { LOGIN, REGISTER, RESET }
data class AuthUiState(val busy: Boolean = false, val failure: Failure? = null, val resetSent: Boolean = false)
@HiltViewModel
class AuthViewModel @Inject constructor(private val repository: AuthRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(AuthUiState())
    val state = mutableState.asStateFlow()
    fun submit(mode: AuthMode, email: String, password: String, name: String) {
        if (mutableState.value.busy) return
        mutableState.value = AuthUiState(busy = true)
        viewModelScope.launch {
            try {
                when (mode) {
                    AuthMode.LOGIN -> repository.signIn(email, password)
                    AuthMode.REGISTER -> repository.register(email, password, name)
                    AuthMode.RESET -> repository.resetPassword(email)
                }
                mutableState.value = AuthUiState(resetSent = mode == AuthMode.RESET)
            } catch (cancelled: CancellationException) { throw cancelled }
              catch (error: Exception) {
                mutableState.value = AuthUiState(failure = (error as? AppException)?.failure ?: Failure.UNKNOWN)
            }
        }
    }
}
