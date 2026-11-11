package com.app.feature.jobs

import com.app.core.common.UiState
import com.app.core.model.*
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class JobsViewModelTest {
    @Test fun searchIsCaseInsensitiveAndRetainsLoadedJobs() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val jobs = mockk<JobsRepository>()
            val opening = JobOpening("1", "Example", "Android", "Remote", true, "https://example.com", "")
            coEvery { jobs.getJobs() } returns listOf(opening, opening.copy(id = "2", title = "Designer"))
            val viewModel = JobsViewModel(jobs, mockk(), mockk())
            advanceUntilIdle()
            viewModel.search(" android ")
            advanceUntilIdle()
            assertEquals(UiState.Success(listOf(opening)), viewModel.state.value)
            viewModel.search("")
            advanceUntilIdle()
            assertEquals(2, (viewModel.state.value as UiState.Success).data.size)
        } finally { Dispatchers.resetMain() }
    }
    @Test fun networkFailureRetriesAndEmptyResponseIsSuccess() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val jobs = mockk<JobsRepository>()
            coEvery { jobs.getJobs() } throws AppException(Failure.NETWORK) andThen emptyList()
            val viewModel = JobsViewModel(jobs, mockk(), mockk())
            advanceUntilIdle()
            assertEquals(UiState.Error(Failure.NETWORK), viewModel.state.value)
            viewModel.retry()
            advanceUntilIdle()
            assertEquals(UiState.Success(emptyList<JobOpening>()), viewModel.state.value)
        } finally { Dispatchers.resetMain() }
    }
    @Test fun repeatedJobImportUsesSameStableIdentifier() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val jobs = mockk<JobsRepository>()
            coEvery { jobs.getJobs() } returns emptyList()
            val applications = mockk<ApplicationRepository>()
            val auth = mockk<AuthRepository>()
            every { auth.requireUserId() } returns "alice"
            val stored = mutableListOf<JobApplication>()
            coEvery { applications.find(any()) } answers { stored.firstOrNull { it.id == firstArg<String>() } }
            coEvery { applications.save(capture(stored)) } just Runs
            val viewModel = JobsViewModel(jobs, applications, auth)
            val opening = JobOpening("provider-id", "Example", "Android", "Remote", true, "https://example.com/job", "")
            viewModel.save(opening)
            advanceUntilIdle()
            viewModel.save(opening)
            advanceUntilIdle()
            assertEquals(1, stored.size)
            coVerify(exactly = 1) { applications.save(any()) }
            assertEquals(ApplicationStatus.SAVED, stored[0].status)
            assertTrue("provider-id" in viewModel.saveState.value.saved)
        } finally { Dispatchers.resetMain() }
    }
}
