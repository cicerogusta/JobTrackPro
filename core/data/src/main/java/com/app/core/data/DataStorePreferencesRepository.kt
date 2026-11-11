package com.app.core.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.app.core.model.PreferencesRepository
import com.app.core.model.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.settings by preferencesDataStore("settings")
@Singleton
class DataStorePreferencesRepository @Inject constructor(@ApplicationContext private val context: Context) : PreferencesRepository {
    private val themeKey = stringPreferencesKey("theme")
    override val theme = context.settings.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }.map { preferences -> ThemeMode.entries.firstOrNull { it.name == preferences[themeKey] } ?: ThemeMode.SYSTEM }
    override suspend fun setTheme(mode: ThemeMode) = dataOperation {
        context.settings.edit { it[themeKey] = mode.name }
        Unit
    }
}
