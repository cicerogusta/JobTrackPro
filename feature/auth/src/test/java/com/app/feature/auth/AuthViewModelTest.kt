package com.app.feature.auth

import com.app.core.model.*
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {
    @Test fun loginFailureIsVisibleAndRetrySucceeds() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = mockk<AuthRepository>()
            coEvery { repository.signIn(any(), any()) } throws AppException(Failure.AUTHENTICATION) andThen Unit
            val viewModel = AuthViewModel(repository)
            viewModel.submit(AuthMode.LOGIN, "person@example.com", "password123", "")
            advanceUntilIdle()
            assertEquals(Failure.AUTHENTICATION, viewModel.state.value.failure)
            assertFalse(viewModel.state.value.busy)
            viewModel.submit(AuthMode.LOGIN, "person@example.com", "password123", "")
            advanceUntilIdle()
            assertNull(viewModel.state.value.failure)
        } finally { Dispatchers.resetMain() }
    }
    @Test fun resetHasExplicitSuccessFeedback() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = mockk<AuthRepository>()
            coEvery { repository.resetPassword(any()) } just Runs
            val viewModel = AuthViewModel(repository)
            viewModel.submit(AuthMode.RESET, "person@example.com", "", "")
            advanceUntilIdle()
            assertTrue(viewModel.state.value.resetSent)
            coVerify { repository.resetPassword("person@example.com") }
        } finally { Dispatchers.resetMain() }
    }
}
