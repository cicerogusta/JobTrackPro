package com.app.core.common

import com.app.core.model.Failure

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val failure: Failure) : UiState<Nothing>
}
fun Failure.userMessage(): String = when (this) {
    Failure.NETWORK -> "Sem conexão. Verifique sua internet e tente novamente."
    Failure.TIMEOUT -> "O serviço demorou para responder. Tente novamente."
    Failure.AUTHENTICATION -> "Não foi possível autenticar. Verifique sua conta e senha."
    Failure.VALIDATION -> "Confira os campos informados."
    Failure.STORAGE -> "Não foi possível acessar os dados locais."
    Failure.SERVER -> "O serviço está indisponível. Tente novamente mais tarde."
    Failure.MALFORMED_DATA -> "O serviço retornou dados inválidos."
    Failure.CONFIGURATION -> "O Firebase ainda não foi configurado neste aplicativo."
    Failure.CONFLICT -> "Os dados mudaram. Atualize e tente novamente."
    Failure.UNKNOWN -> "Não foi possível concluir a operação. Tente novamente."
}
