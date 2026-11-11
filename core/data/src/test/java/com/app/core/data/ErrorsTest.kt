package com.app.core.data

import com.app.core.model.Failure
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.CancellationException
import org.junit.Assert.*
import org.junit.Test

class ErrorsTest {
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
