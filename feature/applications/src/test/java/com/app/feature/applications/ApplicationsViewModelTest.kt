package com.app.feature.applications

import androidx.lifecycle.SavedStateHandle
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
class ApplicationsViewModelTest {
    @Test
    fun filter_queryAndStatusSelected_emitsMatchingApplicationsAndRetainsQuery() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = mockk<ApplicationRepository>()
            val row =
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
            every { repository.observeApplications() } returns
                flowOf(
                    listOf(
                        row,
                        row.copy(id = "2", role = "Designer", status = ApplicationStatus.SAVED),
                    )
                )
            val savedState = SavedStateHandle()
            val viewModel = ApplicationsViewModel(repository, savedState)
            advanceUntilIdle()
            viewModel.search("Android")
            viewModel.filter(ApplicationStatus.INTERVIEW)
            advanceUntilIdle()
            assertEquals(listOf(row), (viewModel.state.value as UiState.Success).data)
            assertEquals("Android", savedState.get<String>("query"))
            viewModel.filter(ApplicationStatus.OFFER)
            advanceUntilIdle()
            assertTrue((viewModel.state.value as UiState.Success).data.isEmpty())
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun save_invalidDraftThenRepeatedSubmission_validatesAndPreventsDuplicateWrites() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = mockk<ApplicationRepository>()
            val auth = mockk<AuthRepository>()
            every { auth.requireUserId() } returns "alice"
            coEvery { repository.save(any()) } just Runs
            val viewModel = ApplicationFormViewModel(repository, auth, SavedStateHandle())
            viewModel.save()
            assertTrue(viewModel.state.value.errors.isNotEmpty())
            coVerify(exactly = 0) { repository.save(any()) }
            viewModel.update(ApplicationDraft("Example", "Android", "Remote"))
            viewModel.save()
            viewModel.save()
            advanceUntilIdle()
            assertTrue(viewModel.state.value.saved)
            coVerify(exactly = 1) {
                repository.save(
                    match { it.userId == "alice" && it.status == ApplicationStatus.APPLIED }
                )
            }
        } finally {
            Dispatchers.resetMain()
        }
    }
}
