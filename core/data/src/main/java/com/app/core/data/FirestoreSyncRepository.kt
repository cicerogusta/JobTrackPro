package com.app.core.data

import com.app.core.database.LocalApplicationDataSource
import com.app.core.database.toDomain
import com.app.core.database.toEntity
import com.app.core.model.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class FirestoreSyncRepository @Inject constructor(
    private val auth: AuthRepository,
    private val local: LocalApplicationDataSource,
    private val remote: FirestoreApplicationDataSource,
) : SyncRepository {
    private val mutex = Mutex()
    private val mutableState = MutableStateFlow(SyncState())
    override val state = mutableState.asStateFlow()
    override suspend fun synchronize() = mutex.withLock {
        if (auth.session.value !is Session.Authenticated) {
            mutableState.value = SyncState(SyncPhase.UNAVAILABLE)
            return@withLock
        }
        val userId = auth.requireUserId()
        mutableState.value = SyncState(SyncPhase.RUNNING)
        try {
            local.pending(userId).forEach { pending ->
                ensureSession(userId)
                val winner = remote.merge(pending.toDomain())
                ensureSession(userId)
                local.mergeRemote(winner.toEntity(dirty = false))
            }
            remote.fetch(userId).forEach {
                ensureSession(userId)
                local.mergeRemote(it.toEntity(dirty = false))
            }
            mutableState.value = SyncState(SyncPhase.SUCCESS)
        } catch (cancelled: CancellationException) {
            mutableState.value = SyncState()
            throw cancelled
        } catch (error: Exception) {
            mutableState.value = SyncState(SyncPhase.ERROR, error.asAppException().failure)
        }
    }
    private fun ensureSession(userId: String) {
        if (auth.requireUserId() != userId) throw AppException(Failure.AUTHENTICATION)
    }
}
