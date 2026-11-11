package com.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ApplicationEntity::class], version = 1, exportSchema = true)
abstract class JobTrackDatabase : RoomDatabase() {
    abstract fun applications(): ApplicationDao
}
