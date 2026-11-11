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
    private fun opening(id: String = "okemprego:android") =
        JobOpening(
            id,
            "Example",
            "Android",
            "",
            WorkMode.HYBRID,
            "https://www.okemprego.com/br/empregos/android",
            "",
        )

    @Test
    fun search_editedFilters_queriesProviderOnlyWhenSubmitted() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val jobs = mockk<JobsRepository>()
            coEvery { jobs.getJobs(any(), any()) } returns JobPage(listOf(opening()), 1, false)
            val viewModel = JobsViewModel(jobs, mockk(), mockk())
            advanceUntilIdle()
            viewModel.updateQuery(" Android ")
            viewModel.updateLocation(" São Paulo ")
            advanceUntilIdle()
            coVerify(exactly = 1) { jobs.getJobs(JobSearch(), 1) }
            coVerify(exactly = 0) { jobs.getJobs(JobSearch("Android", "São Paulo"), 1) }
            viewModel.search()
            advanceUntilIdle()
            coVerify(exactly = 1) { jobs.getJobs(JobSearch("Android", "São Paulo"), 1) }
            assertEquals(UiState.Success(listOf(opening())), viewModel.state.value.results)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun retry_networkFailureThenEmptyResponse_emitsErrorThenEmptySuccess() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val jobs = mockk<JobsRepository>()
            coEvery { jobs.getJobs(any(), any()) } throws
                AppException(Failure.NETWORK) andThen
                JobPage(emptyList(), 1, false)
            val viewModel = JobsViewModel(jobs, mockk(), mockk())
            advanceUntilIdle()
            assertEquals(UiState.Error(Failure.NETWORK), viewModel.state.value.results)
            viewModel.retry()
            advanceUntilIdle()
            assertEquals(UiState.Success(emptyList<JobOpening>()), viewModel.state.value.results)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun loadMore_overlappingPages_mergesUniqueRowsAndStopsAtLastPage() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val jobs = mockk<JobsRepository>()
            coEvery { jobs.getJobs(any(), 1) } returns JobPage(listOf(opening()), 1, true)
            coEvery { jobs.getJobs(any(), 2) } returns
                JobPage(listOf(opening(), opening("okemprego:second")), 2, false)
            val viewModel = JobsViewModel(jobs, mockk(), mockk())
            advanceUntilIdle()
            viewModel.loadMore()
            advanceUntilIdle()
            assertEquals(2, (viewModel.state.value.results as UiState.Success).data.size)
            assertFalse(viewModel.state.value.hasMore)
            viewModel.loadMore()
            advanceUntilIdle()
            coVerify(exactly = 1) { jobs.getJobs(any(), 2) }
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun loadMore_networkFailure_retainsRowsAndRetriesSamePage() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val jobs = mockk<JobsRepository>()
            coEvery { jobs.getJobs(any(), 1) } returns JobPage(listOf(opening()), 1, true)
            coEvery { jobs.getJobs(any(), 2) } throws
                AppException(Failure.NETWORK) andThen
                JobPage(listOf(opening("okemprego:second")), 2, false)
            val viewModel = JobsViewModel(jobs, mockk(), mockk())
            advanceUntilIdle()
            viewModel.loadMore()
            advanceUntilIdle()
            assertEquals(UiState.Success(listOf(opening())), viewModel.state.value.results)
            assertEquals(Failure.NETWORK, viewModel.state.value.pageFailure)
            viewModel.loadMore()
            advanceUntilIdle()
            assertEquals(2, (viewModel.state.value.results as UiState.Success).data.size)
            assertNull(viewModel.state.value.pageFailure)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun loadMore_unsubmittedDraft_keepsOriginalSearchFilters() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val jobs = mockk<JobsRepository>()
            coEvery { jobs.getJobs(any(), 1) } returns JobPage(listOf(opening()), 1, true)
            coEvery { jobs.getJobs(any(), 2) } returns JobPage(emptyList(), 2, false)
            val viewModel = JobsViewModel(jobs, mockk(), mockk())
            advanceUntilIdle()
            viewModel.updateQuery("Unsubmitted draft")
            viewModel.loadMore()
            advanceUntilIdle()
            coVerify { jobs.getJobs(JobSearch(), 2) }
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun search_filtersChangedAfterPaging_restartsAtFirstPage() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val jobs = mockk<JobsRepository>()
            coEvery { jobs.getJobs(any(), 1) } returns JobPage(listOf(opening()), 1, true)
            coEvery { jobs.getJobs(any(), 2) } returns
                JobPage(listOf(opening("okemprego:second")), 2, false)
            val viewModel = JobsViewModel(jobs, mockk(), mockk())
            advanceUntilIdle()
            viewModel.loadMore()
            advanceUntilIdle()
            viewModel.updateQuery("Kotlin")
            viewModel.search()
            advanceUntilIdle()
            assertEquals(1, (viewModel.state.value.results as UiState.Success).data.size)
            coVerify { jobs.getJobs(JobSearch("Kotlin"), 1) }
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun save_sameJobImportedTwice_createsOneApplicationWithFallbackMetadata() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val jobs = mockk<JobsRepository>()
            coEvery { jobs.getJobs(any(), any()) } returns JobPage(emptyList(), 1, false)
            val applications = mockk<ApplicationRepository>()
            val auth = mockk<AuthRepository>()
            every { auth.requireUserId() } returns "alice"
            val stored = mutableListOf<JobApplication>()
            coEvery { applications.find(any()) } answers
                {
                    stored.firstOrNull { it.id == firstArg<String>() }
                }
            coEvery { applications.save(capture(stored)) } just Runs
            val viewModel = JobsViewModel(jobs, applications, auth)
            viewModel.save(opening(), "Source", "Unknown location")
            advanceUntilIdle()
            viewModel.save(opening(), "Source", "Unknown location")
            advanceUntilIdle()
            assertEquals(1, stored.size)
            assertEquals(WorkMode.HYBRID, stored.single().workMode)
            assertEquals(ApplicationStatus.SAVED, stored.single().status)
            assertEquals("Unknown location", stored.single().location)
            assertEquals("Source", stored.single().notes)
            assertTrue(opening().id in viewModel.state.value.save.saved)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun save_unknownWorkMode_requiresExplicitSelection() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val jobs = mockk<JobsRepository>()
            coEvery { jobs.getJobs(any(), any()) } returns JobPage(emptyList(), 1, false)
            val applications = mockk<ApplicationRepository>()
            val auth = mockk<AuthRepository>()
            every { auth.requireUserId() } returns "alice"
            coEvery { applications.find(any()) } returns null
            coEvery { applications.save(any()) } just Runs
            val viewModel = JobsViewModel(jobs, applications, auth)
            viewModel.save(opening().copy(workMode = null), "Source", "Unknown")
            advanceUntilIdle()
            assertEquals(Failure.VALIDATION, viewModel.state.value.save.failure)
            coVerify(exactly = 0) { applications.save(any()) }
            viewModel.save(opening().copy(workMode = null), "Source", "Unknown", WorkMode.REMOTE)
            advanceUntilIdle()
            coVerify(exactly = 1) { applications.save(match { it.workMode == WorkMode.REMOTE }) }
        } finally {
            Dispatchers.resetMain()
        }
    }
}
