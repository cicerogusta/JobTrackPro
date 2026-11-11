package com.app.core.common

import com.app.core.model.Failure

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val failure: Failure) : UiState<Nothing>
}
fun Failure.messageRes(): Int = when (this) {
    Failure.NETWORK -> R.string.error_network
    Failure.TIMEOUT -> R.string.error_timeout
    Failure.AUTHENTICATION -> R.string.error_authentication
    Failure.VALIDATION -> R.string.error_validation
    Failure.STORAGE -> R.string.error_storage
    Failure.SERVER -> R.string.error_server
    Failure.MALFORMED_DATA -> R.string.error_malformed_data
    Failure.CONFIGURATION -> R.string.error_configuration
    Failure.CONFLICT -> R.string.error_conflict
    Failure.RATE_LIMITED -> R.string.error_rate_limited
    Failure.UNKNOWN -> R.string.error_unknown
}
