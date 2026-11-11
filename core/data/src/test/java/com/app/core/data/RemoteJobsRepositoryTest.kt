package com.app.core.data

import com.app.core.model.*
import com.app.core.network.*
import io.mockk.*
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class RemoteJobsRepositoryTest {
    private fun response(page: Int = 1, totalPages: Int = 2) =
        JobsResponse(
            listOf(
                JobDto(
                    slug = "android",
                    company = "Example",
                    title = "Android",
                    country = "BR",
                    remote = "full",
                    url = "https://www.okemprego.com/br/empregos/android",
                )
            ),
            JobsPagination(page, totalPages),
        )

    @Test
    fun getJobs_sameSearchAndPage_normalizesFiltersAndReusesCache() = runTest {
        val api = mockk<JobsApi>()
        coEvery { api.getJobs(any(), any(), any(), any(), any(), any()) } returns response()
        val repository = RemoteJobsRepository(api)
        val first = repository.getJobs(JobSearch(" Android ", " São Paulo "))
        val second = repository.getJobs(JobSearch("Android", "São Paulo"))
        assertEquals(first, second)
        coVerify(exactly = 1) { api.getJobs("BR", "Android", "São Paulo", 1, 20, "newest") }
    }

    @Test
    fun getJobs_differentFiltersOrPages_keepsCacheEntriesSeparate() = runTest {
        val api = mockk<JobsApi>()
        coEvery { api.getJobs(any(), any(), any(), any(), any(), any()) } answers
            {
                response(page = arg(3))
            }
        val repository = RemoteJobsRepository(api)
        repository.getJobs(JobSearch("Android"))
        repository.getJobs(JobSearch("Android"), 2)
        repository.getJobs(JobSearch("Kotlin"))
        coVerify(exactly = 3) { api.getJobs(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun getJobs_emptyFilters_requestsBrazilWithoutSearchTerms() = runTest {
        val api = mockk<JobsApi>()
        coEvery { api.getJobs(any(), any(), any(), any(), any(), any()) } returns response()
        RemoteJobsRepository(api).getJobs()
        coVerify { api.getJobs("BR", null, null, 1, 20, "newest") }
    }

    @Test
    fun getJobs_invalidPagination_rejectsMalformedResponse() = runTest {
        val api = mockk<JobsApi>()
        coEvery { api.getJobs(any(), any(), any(), any(), any(), any()) } returns response(page = 2)
        try {
            RemoteJobsRepository(api).getJobs()
            fail("Expected malformed data")
        } catch (error: AppException) {
            assertEquals(Failure.MALFORMED_DATA, error.failure)
        }
    }

    @Test
    fun getJobs_networkFailure_reportsTypedFailureAndAllowsRetry() = runTest {
        val api = mockk<JobsApi>()
        coEvery { api.getJobs(any(), any(), any(), any(), any(), any()) } throws
            IOException() andThen
            response()
        val repository = RemoteJobsRepository(api)
        try {
            repository.getJobs()
            fail("Expected connectivity error")
        } catch (error: AppException) {
            assertEquals(Failure.NETWORK, error.failure)
        }
        assertEquals(1, repository.getJobs().jobs.size)
        coVerify(exactly = 2) { api.getJobs(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun getJobs_providerPageLimit_stopsOfferingMorePages() = runTest {
        val api = mockk<JobsApi>()
        coEvery { api.getJobs(any(), any(), any(), any(), any(), any()) } returns
            response(page = 200, totalPages = 500)
        assertFalse(RemoteJobsRepository(api).getJobs(page = 200).hasMore)
    }

    @Test
    fun getJobs_invalidRequest_doesNotCallProvider() = runTest {
        val api = mockk<JobsApi>()
        val repository = RemoteJobsRepository(api)
        for ((search, page) in
            listOf(JobSearch() to 0, JobSearch() to 201, JobSearch("x".repeat(201)) to 1)) {
            try {
                repository.getJobs(search, page)
                fail("Expected validation error")
            } catch (error: AppException) {
                assertEquals(Failure.VALIDATION, error.failure)
            }
        }
        coVerify(exactly = 0) { api.getJobs(any(), any(), any(), any(), any(), any()) }
    }
}
