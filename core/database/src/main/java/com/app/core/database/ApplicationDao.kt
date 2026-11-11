package com.app.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.app.core.model.AppException
import com.app.core.model.ConflictResolver
import com.app.core.model.Failure
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ApplicationDao {
    @Query("SELECT * FROM applications WHERE userId = :userId AND deleted = 0 ORDER BY updatedAt DESC")
    abstract fun observe(userId: String): Flow<List<ApplicationEntity>>
    @Query("SELECT * FROM applications WHERE userId = :userId AND id = :id")
    abstract suspend fun find(userId: String, id: String): ApplicationEntity?
    @Query("SELECT * FROM applications WHERE userId = :userId AND dirty = 1")
    abstract suspend fun pending(userId: String): List<ApplicationEntity>
    @Upsert
    abstract suspend fun upsert(entity: ApplicationEntity)

    @Transaction
    open suspend fun saveLocal(entity: ApplicationEntity, expectedRevision: String) {
        val existing = find(entity.userId, entity.id)
        if (existing != null && existing.revision != expectedRevision) {
            throw AppException(Failure.CONFLICT)
        }
        if (existing == null && expectedRevision.isNotEmpty()) throw AppException(Failure.CONFLICT)
        upsert(entity)
    }

    /** Merge and acknowledge in one SQLite transaction to preserve concurrent local edits. */
    @Transaction
    open suspend fun mergeRemote(remote: ApplicationEntity) {
        val local = find(remote.userId, remote.id)
        val winner = local?.let { ConflictResolver.winner(it.toDomain(), remote.toDomain()) }
        when {
            local == null || winner == remote.toDomain() -> upsert(remote.copy(dirty = false))
            local.revision == remote.revision -> upsert(local.copy(dirty = false))
        }
    }
}
