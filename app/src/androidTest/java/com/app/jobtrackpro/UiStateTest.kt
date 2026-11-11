package com.app.jobtrackpro

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.app.core.common.UiState
import com.app.core.model.*
import com.app.designsystem.JobTrackTheme
import com.app.feature.applications.ApplicationFormContent
import com.app.feature.applications.FormState
import com.app.home.HomeContent
import com.app.home.HomeData
import org.junit.Rule
import org.junit.Test

class UiStateTest {
    @get:Rule val compose = createComposeRule()
    @Test fun dashboardEmptyStateHasAddAction() {
        var added = false
        compose.setContent { JobTrackTheme { HomeContent(UiState.Success(HomeData(DashboardStats(0,0,0,0), emptyList())), { added = true }, {}, {}) } }
        compose.onNodeWithText("Nenhuma candidatura ainda").assertIsDisplayed()
        compose.onNodeWithText("Adicionar candidatura").performClick()
        compose.runOnIdle { check(added) }
    }
    @Test fun dashboardErrorCanRetry() {
        var retried = false
        compose.setContent { JobTrackTheme { HomeContent(UiState.Error(Failure.STORAGE), {}, {}, { retried = true }) } }
        compose.onNodeWithText("Não foi possível acessar os dados locais.").assertIsDisplayed()
        compose.onNodeWithText("Tentar novamente").performClick()
        compose.runOnIdle { check(retried) }
    }
    @Test fun formShowsFieldValidation() {
        val errors = ApplicationValidator.validate(ApplicationDraft())
        compose.setContent { JobTrackTheme { ApplicationFormContent(FormState(errors = errors), {}, {}, {}) } }
        compose.onNodeWithText(requireNotNull(errors[ApplicationField.COMPANY])).assertIsDisplayed()
        compose.onNodeWithText(requireNotNull(errors[ApplicationField.ROLE])).assertIsDisplayed()
    }
    @Test fun largeFontsKeepDashboardActionReachable() {
        compose.setContent {
            val density = androidx.compose.ui.platform.LocalDensity.current
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(density.density, 1.5f)
            ) { JobTrackTheme { HomeContent(UiState.Success(HomeData(DashboardStats(0,0,0,0), emptyList())), {}, {}, {}) } }
        }
        compose.onNodeWithText("Adicionar candidatura").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Nenhuma candidatura ainda").performScrollTo().assertIsDisplayed()
    }
}
