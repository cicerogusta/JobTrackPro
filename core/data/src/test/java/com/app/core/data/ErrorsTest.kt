package com.app.core.data

import com.app.core.model.Failure
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.CancellationException
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class ErrorsTest {
    @Test
    fun asAppException_providerRateLimit_reportsRetryableLimit() {
        val error = HttpException(Response.error<Unit>(429, "{}".toResponseBody()))
        assertEquals(Failure.RATE_LIMITED, error.asAppException().failure)
    }

    @Test
    fun asAppException_coroutineCancellation_rethrowsCancellation() {
        assertThrows(CancellationException::class.java) { CancellationException().asAppException() }
    }

    @Test
    fun asAppException_knownAndUnknownErrors_preservesFailureCategories() {
        assertEquals(Failure.NETWORK, IOException().asAppException().failure)
        assertEquals(Failure.TIMEOUT, SocketTimeoutException().asAppException().failure)
        assertEquals(Failure.UNKNOWN, IllegalStateException().asAppException().failure)
    }
}
