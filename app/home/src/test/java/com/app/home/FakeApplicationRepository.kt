package com.app.home

import com.app.core.model.*
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** In-memory implementation of the domain contract for dashboard tests. */
class FakeApplicationRepository(private val userId: String = "alice") : ApplicationRepository {
    private val rows = MutableStateFlow<List<JobApplication>>(emptyList())
    override fun observeApplications() = rows.asStateFlow()
    override suspend fun find(id: String) = rows.value.find { it.id == id }
    override suspend fun save(application: JobApplication) {
        if (application.userId != userId) throw AppException(Failure.AUTHENTICATION)
        ApplicationValidator.validate(application)
        val current = find(application.id)
        if (current != null && current.revision != application.revision) throw AppException(Failure.CONFLICT)
        val now = maxOf(System.currentTimeMillis(), (current?.updatedAt ?: 0) + 1)
        val saved = application.copy(createdAt = current?.createdAt ?: now, updatedAt = now, revision = UUID.randomUUID().toString())
        rows.value = (rows.value.filterNot { it.id == saved.id } + saved).sortedByDescending { it.updatedAt }
    }
    override suspend fun delete(id: String) {
        if (find(id) == null) throw AppException(Failure.VALIDATION)
        rows.value = rows.value.filterNot { it.id == id }
    }
}
