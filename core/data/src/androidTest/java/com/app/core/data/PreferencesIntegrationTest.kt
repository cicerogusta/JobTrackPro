package com.app.core.data

import androidx.test.platform.app.InstrumentationRegistry
import com.app.core.model.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class PreferencesIntegrationTest {
    @Test
    fun theme_repositoryRecreated_preservesSelectedMode() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = DataStorePreferencesRepository(context)
        val previous = repository.theme.first()
        try {
            repository.setTheme(ThemeMode.DARK)
            assertEquals(ThemeMode.DARK, DataStorePreferencesRepository(context).theme.first())
        } finally {
            repository.setTheme(previous)
        }
    }
}
