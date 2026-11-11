package com.app.core.data

import com.app.core.model.Failure
import kotlinx.coroutines.CancellationException
import java.io.IOException
import java.net.SocketTimeoutException
import org.junit.Assert.*
import org.junit.Test

class ErrorsTest {
    @Test fun cancellationIsNeverConvertedToUserFailure() {
        assertThrows(CancellationException::class.java) { CancellationException().asAppException() }
    }
    @Test fun errorsRetainTheirCategory() {
        assertEquals(Failure.NETWORK, IOException().asAppException().failure)
        assertEquals(Failure.TIMEOUT, SocketTimeoutException().asAppException().failure)
        assertEquals(Failure.UNKNOWN, IllegalStateException().asAppException().failure)
    }
}
