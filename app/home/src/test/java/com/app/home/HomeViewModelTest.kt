package com.app.home

import com.app.core.common.UiState
import com.app.core.model.*
import io.mockk.*
import java.time.LocalDate
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @Test
    fun observeApplications_repositoryEmitsChanges_updatesDashboardStatistics() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = FakeApplicationRepository()
            val viewModel = HomeViewModel(repository)
            advanceUntilIdle()
            assertEquals(0, (viewModel.state.value as UiState.Success).data.stats.total)
            repository.save(
                JobApplication(
                    "1",
                    "alice",
                    "Example",
                    "Android",
                    "Remote",
                    WorkMode.REMOTE,
                    LocalDate.now(),
                    ApplicationStatus.INTERVIEW,
                    "",
                    "",
                    1,
                    1,
                )
            )
            advanceUntilIdle()
            assertEquals(1, (viewModel.state.value as UiState.Success).data.stats.interviews)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun retry_observationFails_recoversAfterRetry() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = mockk<ApplicationRepository>()
            every { repository.observeApplications() } returns
                flow { throw AppException(Failure.STORAGE) } andThen
                flowOf(emptyList())
            val viewModel = HomeViewModel(repository)
            advanceUntilIdle()
            assertEquals(UiState.Error(Failure.STORAGE), viewModel.state.value)
            viewModel.retry()
            advanceUntilIdle()
            assertTrue(viewModel.state.value is UiState.Success)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
