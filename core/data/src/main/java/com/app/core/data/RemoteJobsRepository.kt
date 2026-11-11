package com.app.core.data

import com.app.core.model.*
import com.app.core.network.JobsApi
import com.app.core.network.toDomain
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteJobsRepository @Inject constructor(private val api: JobsApi) : JobsRepository {
    private val mutex = Mutex()
    private var cached: List<JobOpening>? = null
    private var fetchedAt = 0L
    override suspend fun getJobs(): List<JobOpening> = mutex.withLock {
        val now = System.nanoTime() / 1_000_000
        cached?.takeIf { now - fetchedAt < CACHE_MILLIS }?.let { return@withLock it }
        dataOperation {
            val response = api.getJobs().data ?: throw AppException(Failure.MALFORMED_DATA)
            val jobs = response.map { it.toDomain() }.distinctBy { it.id }
            cached = jobs
            fetchedAt = now
            jobs
        }
    }
    private companion object { const val CACHE_MILLIS = 15 * 60 * 1000L }
}
