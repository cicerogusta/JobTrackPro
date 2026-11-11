package com.app.core.data

import com.app.core.database.LocalApplicationDataSource
import com.app.core.database.toDomain
import com.app.core.database.toEntity
import com.app.core.model.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

class OfflineApplicationRepository @Inject constructor(
    private val local: LocalApplicationDataSource,
    private val auth: AuthRepository,
) : ApplicationRepository {
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeApplications() = auth.session.flatMapLatest { session ->
        if (session is Session.Authenticated) local.observe(session.user.id).map { rows -> rows.map { it.toDomain() } }
        else flow<List<JobApplication>> { throw AppException(Failure.AUTHENTICATION) }
    }.catch { throw it.asAppException() }

    override suspend fun find(id: String): JobApplication? = dataOperation {
        local.find(auth.requireUserId(), id)?.takeUnless { it.deleted }?.toDomain()
    }
    override suspend fun save(application: JobApplication) = dataOperation {
        val userId = auth.requireUserId()
        if (application.userId != userId || application.deleted) throw AppException(Failure.AUTHENTICATION)
        ApplicationValidator.validate(application)
        val existing = local.find(userId, application.id)
        val now = maxOf(System.currentTimeMillis(), (existing?.updatedAt ?: 0) + 1)
        val saved = application.copy(
            company = application.company.trim(), role = application.role.trim(), location = application.location.trim(),
            createdAt = existing?.createdAt ?: now, updatedAt = now, revision = UUID.randomUUID().toString(),
        )
        val expectedRevision = if (existing?.deleted == true && application.revision.isEmpty()) existing.revision else application.revision
        local.save(saved.toEntity(dirty = true), expectedRevision)
    }
    override suspend fun delete(id: String) = dataOperation {
        val existing = local.find(auth.requireUserId(), id) ?: throw AppException(Failure.VALIDATION)
        local.save(existing.copy(deleted = true, dirty = true,
            updatedAt = maxOf(System.currentTimeMillis(), existing.updatedAt + 1),
            revision = UUID.randomUUID().toString()), existing.revision)
    }
}
