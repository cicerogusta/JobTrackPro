package com.app.core.model

import java.time.LocalDate

enum class ApplicationStatus { SAVED, APPLIED, INTERVIEW, OFFER, REJECTED }
enum class WorkMode { REMOTE, HYBRID, ON_SITE }
enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class JobApplication(
    val id: String,
    val userId: String,
    val company: String,
    val role: String,
    val location: String,
    val workMode: WorkMode,
    val appliedOn: LocalDate,
    val status: ApplicationStatus,
    val jobUrl: String,
    val notes: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deleted: Boolean = false,
    val revision: String = "",
)

data class UserProfile(val id: String, val email: String, val displayName: String)
sealed interface Session {
    data object Loading : Session
    data object SignedOut : Session
    data object Unconfigured : Session
    data class Authenticated(val user: UserProfile) : Session
}
data class JobOpening(
    val id: String,
    val company: String,
    val title: String,
    val location: String,
    val remote: Boolean,
    val url: String,
    val description: String,
)
data class DashboardStats(val total: Int, val applied: Int, val interviews: Int, val offers: Int) {
    companion object {
        fun from(applications: List<JobApplication>) = DashboardStats(
            applications.size,
            applications.count { it.status == ApplicationStatus.APPLIED },
            applications.count { it.status == ApplicationStatus.INTERVIEW },
            applications.count { it.status == ApplicationStatus.OFFER },
        )
    }
}
data class ApplicationFilter(val query: String = "", val status: ApplicationStatus? = null) {
    fun matches(application: JobApplication): Boolean =
        (status == null || application.status == status) &&
            listOf(application.company, application.role, application.location)
                .any { it.contains(query.trim(), ignoreCase = true) }
}

enum class Failure { NETWORK, TIMEOUT, AUTHENTICATION, VALIDATION, STORAGE, SERVER, MALFORMED_DATA, CONFIGURATION, CONFLICT, UNKNOWN }
class AppException(val failure: Failure, cause: Throwable? = null) : Exception(failure.name, cause)

enum class SyncPhase { IDLE, RUNNING, SUCCESS, ERROR, UNAVAILABLE }
data class SyncState(val phase: SyncPhase = SyncPhase.IDLE, val failure: Failure? = null)
