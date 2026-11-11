package com.app.feature.settings

import com.app.core.model.*
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @Test
    fun setTheme_persistenceFailsThenSucceeds_exposesAndClearsError() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val auth = mockk<AuthRepository>()
            every { auth.session } returns MutableStateFlow(Session.SignedOut)
            val preferences = mockk<PreferencesRepository>()
            every { preferences.theme } returns MutableStateFlow(ThemeMode.SYSTEM)
            coEvery { preferences.setTheme(ThemeMode.DARK) } throws
                AppException(Failure.STORAGE) andThen
                Unit
            val sync = mockk<SyncRepository>()
            every { sync.state } returns MutableStateFlow(SyncState())
            val viewModel = SettingsViewModel(auth, preferences, sync)
            viewModel.setTheme(ThemeMode.DARK)
            advanceUntilIdle()
            assertEquals(Failure.STORAGE, viewModel.failure.value)
            viewModel.setTheme(ThemeMode.DARK)
            advanceUntilIdle()
            assertNull(viewModel.failure.value)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
