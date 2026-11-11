package com.app.core.data

import com.app.core.database.*
import com.app.core.model.*
import com.app.core.network.*
import io.mockk.*
import java.io.IOException
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class RepositoryTest {
    private fun row(user: String = "alice") =
        JobApplication(
            "1",
            user,
            "Example",
            "Android",
            "Remote",
            WorkMode.REMOTE,
            LocalDate.now(),
            ApplicationStatus.APPLIED,
            "",
            "",
            1,
            1,
        )

    @Test
    fun save_validApplication_normalizesTextAndMarksPendingRevision() = runTest {
        val local = mockk<LocalApplicationDataSource>()
        val auth = mockk<AuthRepository>()
        every { auth.requireUserId() } returns "alice"
        coEvery { local.find("alice", "1") } returns null
        coEvery { local.save(any(), any()) } just Runs
        val repository = OfflineApplicationRepository(local, auth)
        repository.save(row().copy(company = " Example "))
        coVerify {
            local.save(
                match {
                    it.dirty &&
                        it.company == "Example" &&
                        it.revision.isNotEmpty() &&
                        it.updatedAt >= it.createdAt
                },
                "",
            )
        }
    }

    @Test
    fun save_differentOwner_rejectsCrossAccountWrite() = runTest {
        val local = mockk<LocalApplicationDataSource>()
        val auth = mockk<AuthRepository>()
        every { auth.requireUserId() } returns "alice"
        val repository = OfflineApplicationRepository(local, auth)
        try {
            repository.save(row("bob"))
            fail("Expected authentication failure")
        } catch (error: AppException) {
            assertEquals(Failure.AUTHENTICATION, error.failure)
        }
        coVerify(exactly = 0) { local.save(any(), any()) }
    }

    @Test
    fun observeApplications_authenticatedUser_emitsOnlyOwnedRows() = runTest {
        val local = mockk<LocalApplicationDataSource>()
        val auth = mockk<AuthRepository>()
        every { auth.session } returns
            MutableStateFlow(Session.Authenticated(UserProfile("alice", "", "")))
        every { local.observe("alice") } returns MutableStateFlow(listOf(row().toEntity(false)))
        assertEquals(
            listOf(row()),
            OfflineApplicationRepository(local, auth).observeApplications().first(),
        )
        verify(exactly = 0) { local.observe("bob") }
    }

    @Test
    fun delete_existingApplication_marksTombstonePendingSync() = runTest {
        val local = mockk<LocalApplicationDataSource>()
        val auth = mockk<AuthRepository>()
        every { auth.requireUserId() } returns "alice"
        coEvery { local.find("alice", "1") } returns row().copy(revision = "old").toEntity(false)
        coEvery { local.save(any(), any()) } just Runs
        OfflineApplicationRepository(local, auth).delete("1")
        coVerify { local.save(match { it.deleted && it.dirty && it.revision != "old" }, "old") }
    }

    @Test
    fun getJobs_cacheHitOrNetworkFailure_reusesCacheAndReportsTypedErrors() = runTest {
        val api = mockk<JobsApi>()
        coEvery { api.getJobs() } returns
            JobsResponse(
                listOf(
                    JobDto("id", "Example", "Android", "Remote", true, "https://example.com", "")
                )
            )
        val repository = RemoteJobsRepository(api)
        assertEquals(repository.getJobs(), repository.getJobs())
        coVerify(exactly = 1) { api.getJobs() }
        val failing = mockk<JobsApi>()
        coEvery { failing.getJobs() } throws IOException()
        try {
            RemoteJobsRepository(failing).getJobs()
            fail("Expected network error")
        } catch (error: AppException) {
            assertEquals(Failure.NETWORK, error.failure)
        }
    }

    @Test
    fun synchronize_remoteFailure_preservesPendingRowsAndSupportsRetry() = runTest {
        val auth = mockk<AuthRepository>()
        every { auth.session } returns
            MutableStateFlow(Session.Authenticated(UserProfile("alice", "", "")))
        every { auth.requireUserId() } returns "alice"
        val local = mockk<LocalApplicationDataSource>()
        coEvery { local.pending("alice") } returns listOf(row().toEntity(true))
        coEvery { local.mergeRemote(any()) } just Runs
        val remote = mockk<FirestoreApplicationDataSource>()
        coEvery { remote.merge(any()) } throws IOException() andThen row()
        coEvery { remote.fetch("alice") } returns listOf(row())
        val sync = FirestoreSyncRepository(auth, local, remote)
        sync.synchronize()
        assertEquals(SyncPhase.ERROR, sync.state.value.phase)
        coVerify(exactly = 0) { local.mergeRemote(any()) }
        sync.synchronize()
        assertEquals(SyncPhase.SUCCESS, sync.state.value.phase)
        coVerify(atLeast = 1) { local.mergeRemote(match { !it.dirty }) }
    }
}
