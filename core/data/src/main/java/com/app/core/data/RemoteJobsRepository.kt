package com.app.core.data

import com.app.core.model.*
import com.app.core.network.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class RemoteJobsRepository @Inject constructor(private val api: JobsApi) : JobsRepository {
    private data class CacheKey(val search: JobSearch, val page: Int)

    private data class CachedPage(val result: JobPage, val fetchedAt: Long)

    private val mutex = Mutex()
    private val cache = LinkedHashMap<CacheKey, CachedPage>()

    override suspend fun getJobs(search: JobSearch, page: Int): JobPage = mutex.withLock {
        val normalized = search.normalized()
        if (
            page !in 1..MAX_JOBS_PAGE ||
                normalized.query.length > MAX_FILTER_LENGTH ||
                normalized.location.length > MAX_FILTER_LENGTH
        )
            throw AppException(Failure.VALIDATION)
        val key = CacheKey(normalized, page)
        val now = System.nanoTime() / NANOS_PER_MILLI
        cache[key]
            ?.takeIf { now - it.fetchedAt < CACHE_MILLIS }
            ?.let {
                return@withLock it.result
            }
        dataOperation {
            val response =
                api.getJobs(
                    country = BRAZIL_COUNTRY,
                    query = normalized.query.ifBlank { null },
                    location = normalized.location.ifBlank { null },
                    page = page,
                )
            val rows = response.data ?: throw AppException(Failure.MALFORMED_DATA)
            val pagination = response.pagination ?: throw AppException(Failure.MALFORMED_DATA)
            if (
                pagination.page != page ||
                    pagination.totalPages < 0 ||
                    (rows.isNotEmpty() && pagination.totalPages == 0)
            )
                throw AppException(Failure.MALFORMED_DATA)
            val result =
                JobPage(
                    rows.map(JobDto::toDomain).distinctBy(JobOpening::id),
                    page,
                    page < minOf(pagination.totalPages, MAX_JOBS_PAGE),
                )
            cache.remove(key)
            cache[key] = CachedPage(result, System.nanoTime() / NANOS_PER_MILLI)
            while (cache.size > MAX_CACHED_PAGES) cache.remove(cache.keys.first())
            result
        }
    }

    private companion object {
        const val CACHE_MILLIS = 15 * 60 * 1000L
        const val MAX_CACHED_PAGES = 20
        const val MAX_FILTER_LENGTH = 200
        const val NANOS_PER_MILLI = 1_000_000L
    }
}
