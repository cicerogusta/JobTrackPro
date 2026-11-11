package com.app.jobtrackpro

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.app.core.common.UiState
import com.app.core.model.*
import com.app.designsystem.JobTrackTheme
import com.app.designsystem.R as DesignResources
import com.app.designsystem.Spacing
import com.app.feature.jobs.JobsContent
import com.app.feature.jobs.JobsUiState
import com.app.feature.jobs.R as JobsResources
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class JobsContentTest {
    @get:Rule val compose = createComposeRule()

    private fun opening(mode: WorkMode? = WorkMode.HYBRID) =
        JobOpening(
            "okemprego:android-sp",
            "Example",
            "Android Developer",
            "São Paulo, SP",
            mode,
            "https://www.okemprego.com/br/empregos/android-sp",
            "Build Android apps.",
        )

    @Test
    fun search_brazilFiltersEntered_submitsKeywordAndCity() {
        var state by mutableStateOf(JobsUiState(results = UiState.Success(emptyList())))
        var submitted: JobSearch? = null
        compose.setContent {
            JobTrackTheme {
                JobsContent(
                    state,
                    { state = state.copy(query = it) },
                    { state = state.copy(location = it) },
                    { submitted = JobSearch(state.query, state.location) },
                    {},
                    {},
                    { _, _ -> },
                )
            }
        }
        val locationBounds =
            compose
                .onNodeWithText(testString(JobsResources.string.jobs_location_search))
                .getUnclippedBoundsInRoot()
        val searchBounds =
            compose
                .onNodeWithText(testString(JobsResources.string.jobs_search_action))
                .getUnclippedBoundsInRoot()
        assertTrue(searchBounds.top >= locationBounds.bottom + Spacing.medium)
        compose
            .onNodeWithText(testString(JobsResources.string.jobs_search))
            .performTextInput("Android")
        compose
            .onNodeWithText(testString(JobsResources.string.jobs_location_search))
            .performTextInput("São Paulo")
        compose
            .onNodeWithText(testString(JobsResources.string.jobs_search_action))
            .performScrollTo()
            .performClick()
        compose.runOnIdle { assertEquals(JobSearch("Android", "São Paulo"), submitted) }
    }

    @Test
    fun loadMore_nextPageAvailable_exposesPaginationAction() {
        var requested = false
        compose.setContent {
            JobTrackTheme {
                JobsContent(
                    JobsUiState(results = UiState.Success(listOf(opening())), hasMore = true),
                    {},
                    {},
                    {},
                    {},
                    { requested = true },
                    { _, _ -> },
                )
            }
        }
        compose.onNodeWithText(testString(JobsResources.string.jobs_heading)).assertIsDisplayed()
        compose
            .onNodeWithText(testString(JobsResources.string.jobs_load_more))
            .performScrollTo()
            .performClick()
        compose.runOnIdle { assertTrue(requested) }
    }

    @Test
    fun save_workModeMissing_requestsSelectionBeforeSaving() {
        var chosen: WorkMode? = null
        compose.setContent {
            JobTrackTheme {
                JobsContent(
                    JobsUiState(results = UiState.Success(listOf(opening(null)))),
                    {},
                    {},
                    {},
                    {},
                    {},
                    { _, mode -> chosen = mode },
                )
            }
        }
        compose
            .onNodeWithText(testString(JobsResources.string.jobs_save))
            .performScrollTo()
            .performClick()
        compose.onNodeWithText(testString(JobsResources.string.jobs_mode_title)).assertIsDisplayed()
        compose.runOnIdle { assertNull(chosen) }
        compose.onNodeWithText(testString(DesignResources.string.work_mode_hybrid)).performClick()
        compose.onNodeWithText(testString(JobsResources.string.jobs_save_confirm)).performClick()
        compose.runOnIdle { assertEquals(WorkMode.HYBRID, chosen) }
    }
}
