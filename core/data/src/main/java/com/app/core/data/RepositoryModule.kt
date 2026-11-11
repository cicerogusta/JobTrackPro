package com.app.core.data

import com.app.core.model.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton abstract fun auth(repository: FirebaseAuthRepository): AuthRepository
    @Binds @Singleton abstract fun applications(repository: OfflineApplicationRepository): ApplicationRepository
    @Binds @Singleton abstract fun jobs(repository: RemoteJobsRepository): JobsRepository
    @Binds @Singleton abstract fun preferences(repository: DataStorePreferencesRepository): PreferencesRepository
    @Binds @Singleton abstract fun sync(repository: FirestoreSyncRepository): SyncRepository
}
