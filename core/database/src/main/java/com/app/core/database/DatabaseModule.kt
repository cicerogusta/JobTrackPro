package com.app.core.database

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun database(@ApplicationContext context: Context): JobTrackDatabase =
        Room.databaseBuilder(context, JobTrackDatabase::class.java, "jobtrack.db").build()
    @Provides fun applications(database: JobTrackDatabase) = database.applications()
}
