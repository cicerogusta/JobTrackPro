package com.app.core.network

import com.google.gson.annotations.SerializedName
import com.app.core.model.AppException
import com.app.core.model.Failure
import com.app.core.model.JobOpening
import com.app.core.model.ApplicationValidator
import retrofit2.http.GET

data class JobsResponse(val data: List<JobDto>? = null)
data class JobDto(
    val slug: String? = null,
    @SerializedName("company_name") val company: String? = null,
    val title: String? = null,
    val location: String? = null,
    val remote: Boolean = false,
    val url: String? = null,
    val description: String? = null,
)
fun JobDto.toDomain(): JobOpening {
    val id = slug?.takeIf { it.isNotBlank() } ?: throw AppException(Failure.MALFORMED_DATA)
    val name = company?.takeIf { it.isNotBlank() } ?: throw AppException(Failure.MALFORMED_DATA)
    val role = title?.takeIf { it.isNotBlank() } ?: throw AppException(Failure.MALFORMED_DATA)
    val link = url?.takeIf(ApplicationValidator::isHttpsUrl) ?: throw AppException(Failure.MALFORMED_DATA)
    return JobOpening(id, name, role, location.orEmpty().ifBlank { "Não informada" }, remote, link, description.orEmpty())
}
interface JobsApi {
    @GET("api/job-board-api")
    suspend fun getJobs(): JobsResponse
}
