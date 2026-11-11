package com.app.jobtrackpro

import com.app.core.model.ApplicationRepository
import com.app.core.model.AuthRepository
import com.app.core.model.SyncRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Allows integration tooling to exercise the same repositories as the running app. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ApplicationDependencies {
    fun auth(): AuthRepository
    fun applications(): ApplicationRepository
    fun sync(): SyncRepository
}
