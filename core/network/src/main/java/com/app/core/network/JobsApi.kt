package com.app.core.network

import com.app.core.model.AppException
import com.app.core.model.ApplicationValidator
import com.app.core.model.Failure
import com.app.core.model.JobOpening
import com.app.core.model.WorkMode
import retrofit2.http.GET
import retrofit2.http.Query

data class JobsResponse(val data: List<JobDto>? = null, val pagination: JobsPagination? = null)

data class JobsPagination(
    val page: Int = 0,
    val totalPages: Int = 0,
)

data class JobDto(
    val slug: String? = null,
    val company: String? = null,
    val title: String? = null,
    val country: String? = null,
    val city: String? = null,
    val region: String? = null,
    val remote: String? = null,
    val url: String? = null,
    val description: String? = null,
)

fun JobDto.toDomain(): JobOpening {
    if (country != BRAZIL_COUNTRY) throw AppException(Failure.MALFORMED_DATA)
    val identifier = slug?.takeIf(String::isNotBlank) ?: throw AppException(Failure.MALFORMED_DATA)
    val employer = company?.takeIf(String::isNotBlank) ?: throw AppException(Failure.MALFORMED_DATA)
    val role = title?.takeIf(String::isNotBlank) ?: throw AppException(Failure.MALFORMED_DATA)
    val link =
        url?.takeIf(ApplicationValidator::isHttpsUrl) ?: throw AppException(Failure.MALFORMED_DATA)
    val mode =
        when (remote) {
            "full" -> WorkMode.REMOTE
            "hybrid" -> WorkMode.HYBRID
            "onsite" -> WorkMode.ON_SITE
            else -> null
        }
    val place =
        listOfNotNull(city, region)
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()
            .joinToString(", ")
    return JobOpening(
        "okemprego:$identifier",
        employer.trim(),
        role.trim(),
        place,
        mode,
        link,
        description.orEmpty(),
    )
}

const val BRAZIL_COUNTRY = "BR"
const val JOBS_PAGE_SIZE = 20
const val MAX_JOBS_PAGE = 200

interface JobsApi {
    @GET("api/v1/jobs")
    suspend fun getJobs(
        @Query("country") country: String = BRAZIL_COUNTRY,
        @Query("q") query: String? = null,
        @Query("where") location: String? = null,
        @Query("page") page: Int = 1,
        @Query("perPage") perPage: Int = JOBS_PAGE_SIZE,
        @Query("sort") sort: String = "newest",
    ): JobsResponse
}
