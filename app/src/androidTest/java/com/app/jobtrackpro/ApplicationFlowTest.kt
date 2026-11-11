package com.app.jobtrackpro

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import com.app.core.model.*
import com.app.feature.applications.R
import dagger.hilt.android.EntryPointAccessors
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue

class ApplicationFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private lateinit var repositories: ApplicationDependencies
    @Before fun signIn() = runBlocking {
        assumeTrue(com.app.core.data.BuildConfig.FIREBASE_EMULATORS)
        repositories = EntryPointAccessors.fromApplication(compose.activity.application, ApplicationDependencies::class.java)
        repositories.auth().signOut()
        withTimeout(30_000) { repositories.auth().register("ui-${UUID.randomUUID()}@example.com", "Testing123!", "Portfolio QA") }
        compose.waitUntil(15_000) { compose.onAllNodesWithText(testString(com.app.home.R.string.home_add_application)).fetchSemanticsNodes().isNotEmpty() }
    }
    @Test fun composeCreateXmlSearchEditAndDelete() {
        compose.onNodeWithText(testString(com.app.home.R.string.home_add_application)).performClick()
        compose.onNodeWithText(testString(R.string.application_company)).performTextInput("Portfolio Company")
        compose.onNodeWithText(testString(R.string.application_role)).performTextInput("Android Developer")
        compose.onNodeWithText(testString(R.string.application_location)).performTextInput("São Paulo")
        compose.onNodeWithText(testString(R.string.application_save)).performScrollTo().performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithText(testString(com.app.home.R.string.home_total, 1)).fetchSemanticsNodes().isNotEmpty() }
        capture("jobtrack-dashboard")
        compose.onNodeWithText(testString(com.app.jobtrackpro.R.string.tab_applications)).performClick()
        compose.waitForIdle()
        onView(withId(R.id.search)).perform(typeText("Android"), closeSoftKeyboard())
        capture("jobtrack-applications-xml")
        onView(withText("Android Developer")).check(matches(isDisplayed())).perform(click())
        compose.onNodeWithText(testString(R.string.application_edit)).performScrollTo().performClick()
        compose.onNodeWithText(testString(R.string.application_role)).performTextReplacement("Senior Android Developer")
        compose.onNodeWithText(testString(R.string.application_save)).performScrollTo().performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithText("Senior Android Developer").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(testString(R.string.application_delete)).performScrollTo().performClick()
        compose.onNodeWithText(testString(R.string.application_delete_confirm), useUnmergedTree = true).performClick()
        compose.waitForIdle()
        onView(withId(R.id.message)).check(matches(withText(testString(R.string.empty_applications))))
        runBlocking { assertTrue(repositories.applications().observeApplications().first().isEmpty()) }
    }
    @Test fun settingsPersistsThemeAndLogoutProtectsRoutes() {
        compose.onNodeWithText(testString(com.app.jobtrackpro.R.string.tab_settings)).performClick()
        compose.onNodeWithText(testString(com.app.designsystem.R.string.theme_dark)).performScrollTo().performClick()
        compose.onNodeWithText(testString(com.app.feature.settings.R.string.settings_logout)).performScrollTo().performClick()
        compose.onNodeWithText(testString(com.app.feature.settings.R.string.settings_logout_confirm), useUnmergedTree = true).performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithText(testString(com.app.feature.auth.R.string.auth_forgot_password)).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(testString(com.app.home.R.string.home_add_application)).assertDoesNotExist()
    }
    @Test fun formRemainsUsableAfterRotation() {
        compose.onNodeWithText(testString(com.app.home.R.string.home_add_application)).performClick()
        compose.onNodeWithText(testString(R.string.application_company)).performTextInput("Rotation QA")
        compose.activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        compose.waitUntil(15_000) { compose.onAllNodesWithText("Rotation QA").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText(testString(R.string.application_save)).performScrollTo().assertIsDisplayed()
        capture("jobtrack-form-landscape")
        compose.activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }
    private fun capture(name: String) {
        compose.waitForIdle()
        val automation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().uiAutomation
        automation.waitForIdle(500, 5000)
        val descriptor = automation.executeShellCommand("screencap -p /sdcard/Download/$name.png")
        android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes() }
    }
}
