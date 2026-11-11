package com.app.core.model

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface ApplicationRepository {
    /** Emits active rows for the authenticated user only; fails when no user is signed in. */
    fun observeApplications(): Flow<List<JobApplication>>
    suspend fun find(id: String): JobApplication?
    /** Validates and commits locally before returning, including a durable sync marker. */
    suspend fun save(application: JobApplication)
    suspend fun delete(id: String)
}
interface AuthRepository {
    val session: StateFlow<Session>
    suspend fun signIn(email: String, password: String)
    suspend fun register(email: String, password: String, name: String)
    suspend fun resetPassword(email: String)
    fun signOut()
    fun requireUserId(): String
}
interface JobsRepository {
    suspend fun getJobs(): List<JobOpening>
}
interface PreferencesRepository {
    val theme: Flow<ThemeMode>
    suspend fun setTheme(mode: ThemeMode)
}
interface SyncRepository {
    val state: StateFlow<SyncState>
    suspend fun synchronize()
}
