package com.app.core.model

import java.net.URI
import java.time.LocalDate

data class ApplicationDraft(
    val company: String = "",
    val role: String = "",
    val location: String = "",
    val workMode: WorkMode = WorkMode.REMOTE,
    val appliedOn: String = LocalDate.now().toString(),
    val status: ApplicationStatus = ApplicationStatus.APPLIED,
    val jobUrl: String = "",
    val notes: String = "",
) : java.io.Serializable
enum class ApplicationField { COMPANY, ROLE, LOCATION, DATE, URL, NOTES }
object ApplicationValidator {
    fun validate(draft: ApplicationDraft): Map<ApplicationField, String> = buildMap {
        if (draft.company.isBlank() || draft.company.length > 200) put(ApplicationField.COMPANY, "Informe uma empresa com até 200 caracteres.")
        if (draft.role.isBlank() || draft.role.length > 200) put(ApplicationField.ROLE, "Informe um cargo com até 200 caracteres.")
        if (draft.location.isBlank() || draft.location.length > 200) put(ApplicationField.LOCATION, "Informe a localização com até 200 caracteres.")
        val date = try { LocalDate.parse(draft.appliedOn) } catch (_: java.time.format.DateTimeParseException) { null }
        if (date == null || date.isAfter(LocalDate.now())) put(ApplicationField.DATE, "Use uma data válida até hoje (AAAA-MM-DD).")
        if (draft.jobUrl.length > 2048 || (draft.jobUrl.isNotBlank() && !isHttpsUrl(draft.jobUrl))) put(ApplicationField.URL, "Informe uma URL HTTPS válida com até 2048 caracteres.")
        if (draft.notes.length > 10000) put(ApplicationField.NOTES, "As observações devem ter até 10000 caracteres.")
    }
    fun isHttpsUrl(value: String): Boolean = try {
        val uri = URI(value)
        uri.scheme == "https" && !uri.host.isNullOrBlank() && uri.userInfo == null
    } catch (_: java.net.URISyntaxException) { false }

    fun validate(application: JobApplication) {
        val draft = ApplicationDraft(application.company, application.role, application.location,
            application.workMode, application.appliedOn.toString(), application.status, application.jobUrl, application.notes)
        if (validate(draft).isNotEmpty()) throw AppException(Failure.VALIDATION)
    }
}
object AuthValidator {
    fun validEmail(email: String): Boolean =
        email.length <= 254 && Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(email.trim())
    fun validate(email: String, password: String, registering: Boolean, name: String = "") {
        if (!validEmail(email) || password.isBlank() || (registering && (password.length < 8 || name.isBlank() || name.length > 100))) {
            throw AppException(Failure.VALIDATION)
        }
    }
}
/** Deterministic last-write-wins ordering, including same-millisecond updates. */
object ConflictResolver {
    fun winner(local: JobApplication, remote: JobApplication): JobApplication {
        require(local.id == remote.id && local.userId == remote.userId)
        return if (compareValuesBy(local, remote, { it.updatedAt }, { it.revision }) >= 0) local else remote
    }
}
