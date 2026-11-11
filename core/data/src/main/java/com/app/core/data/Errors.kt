package com.app.core.data

import android.database.SQLException
import com.app.core.model.AppException
import com.app.core.model.Failure
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.gson.JsonParseException
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException

fun Throwable.asAppException(): AppException = when (this) {
    is CancellationException -> throw this
    is AppException -> this
    is SocketTimeoutException -> AppException(Failure.TIMEOUT, this)
    is FirebaseNetworkException, is IOException -> AppException(Failure.NETWORK, this)
    is FirebaseAuthException -> AppException(Failure.AUTHENTICATION, this)
    is FirebaseFirestoreException -> AppException(
        when (code) {
            FirebaseFirestoreException.Code.UNAVAILABLE -> Failure.NETWORK
            FirebaseFirestoreException.Code.UNAUTHENTICATED,
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> Failure.AUTHENTICATION
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> Failure.TIMEOUT
            else -> Failure.SERVER
        }, this)
    is HttpException -> AppException(if (code() == 401 || code() == 403) Failure.AUTHENTICATION else Failure.SERVER, this)
    is JsonParseException -> AppException(Failure.MALFORMED_DATA, this)
    is SQLException -> AppException(Failure.STORAGE, this)
    else -> AppException(Failure.UNKNOWN, this)
}
suspend fun <T> dataOperation(block: suspend () -> T): T = try { block() } catch (error: Exception) { throw error.asAppException() }
