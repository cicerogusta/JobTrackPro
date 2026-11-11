package com.app.core.model

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class DomainTest {
    private fun application(status: ApplicationStatus = ApplicationStatus.APPLIED) =
        JobApplication(
            "id",
            "alice",
            "Example",
            "Android Developer",
            "São Paulo",
            WorkMode.HYBRID,
            LocalDate.of(2026, 1, 1),
            status,
            "https://example.com",
            "",
            100,
            100,
            revision = "a",
        )

    @Test
    fun validateDraft_requiredFieldsBlank_returnsInvalidFields() {
        val errors =
            ApplicationValidator.validate(ApplicationDraft(company = "", role = " ", location = ""))
        assertTrue(
            errors.containsAll(
                listOf(ApplicationField.COMPANY, ApplicationField.ROLE, ApplicationField.LOCATION)
            )
        )
    }

    @Test
    fun validateDraft_invalidOrFutureDate_returnsDateError() {
        val draft = ApplicationDraft("Example", "Android", "Remote")
        assertTrue(
            ApplicationField.DATE in
                ApplicationValidator.validate(draft.copy(appliedOn = "invalid"))
        )
        assertTrue(
            ApplicationField.DATE in
                ApplicationValidator.validate(
                    draft.copy(appliedOn = LocalDate.now().plusDays(1).toString())
                )
        )
        assertFalse(ApplicationField.DATE in ApplicationValidator.validate(draft))
    }

    @Test
    fun isHttpsUrl_mixedUrls_acceptsOnlyHttpsWithoutCredentials() {
        assertTrue(ApplicationValidator.isHttpsUrl("https://example.com/jobs/1"))
        listOf(
                "http://example.com",
                "javascript:alert(1)",
                "https://",
                "https://user:pass@example.com",
                "bad url",
            )
            .forEach { assertFalse(it, ApplicationValidator.isHttpsUrl(it)) }
    }

    @Test
    fun validateDraft_validFields_returnsNoErrors() {
        assertTrue(
            ApplicationValidator.validate(ApplicationDraft("Example", "Android", "Remote"))
                .isEmpty()
        )
    }

    @Test
    fun validateDraft_valuesExceedLimits_returnsFieldErrors() {
        val errors =
            ApplicationValidator.validate(
                ApplicationDraft("x".repeat(201), "Android", "Remote", notes = "x".repeat(10001))
            )
        assertTrue(ApplicationField.COMPANY in errors)
        assertTrue(ApplicationField.NOTES in errors)
    }

    @Test
    fun from_applicationsWithDifferentStatuses_countsTotalsByStatus() {
        val rows = ApplicationStatus.entries.map(::application)
        assertEquals(DashboardStats(5, 1, 1, 1), DashboardStats.from(rows))
        assertEquals(DashboardStats(0, 0, 0, 0), DashboardStats.from(emptyList()))
    }

    @Test
    fun matches_queryAndStatusProvided_requiresBothFiltersToMatch() {
        val row = application()
        assertTrue(ApplicationFilter(" android ", ApplicationStatus.APPLIED).matches(row))
        assertFalse(ApplicationFilter("android", ApplicationStatus.OFFER).matches(row))
        assertFalse(ApplicationFilter("unknown").matches(row))
        assertTrue(ApplicationFilter("SÃO PAULO").matches(row))
    }

    @Test
    fun winner_concurrentUpdates_resolvesDeterministicallyAndPreservesDeletion() {
        val local = application()
        val remote = local.copy(updatedAt = 101, deleted = true, revision = "b")
        assertEquals(remote, ConflictResolver.winner(local, remote))
        assertEquals(remote, ConflictResolver.winner(remote, local))
        val sameTime = local.copy(revision = "z")
        assertEquals(sameTime, ConflictResolver.winner(local, sameTime))
        assertEquals(sameTime, ConflictResolver.winner(sameTime, local))
    }

    @Test
    fun winner_differentOwners_rejectsMerge() {
        assertThrows(IllegalArgumentException::class.java) {
            ConflictResolver.winner(application(), application().copy(userId = "bob"))
        }
    }

    @Test
    fun validate_invalidAuthenticationInput_rejectsCredentials() {
        assertTrue(AuthValidator.validEmail("person@example.com"))
        listOf("", "a", "a@", "a b@example.com").forEach {
            assertFalse(AuthValidator.validEmail(it))
        }
        assertThrows(AppException::class.java) {
            AuthValidator.validate("bad", "password123", false)
        }
        assertThrows(AppException::class.java) {
            AuthValidator.validate("person@example.com", "123", true, "Person")
        }
        AuthValidator.validate("person@example.com", "password123", true, "Person")
    }
}
