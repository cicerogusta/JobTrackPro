package com.app.jobtrackpro

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.app.core.data.BuildConfig
import com.app.core.data.FirebaseProvider
import org.junit.Assume.assumeFalse
import org.junit.Rule
import org.junit.Test

class UnconfiguredAppTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun jobTrackApp_firebaseNotConfigured_showsSetupInstructions() {
        assumeFalse(BuildConfig.FIREBASE_EMULATORS)
        assumeFalse(FirebaseProvider(compose.activity.applicationContext).configured)
        compose.onNodeWithText(testString(R.string.firebase_setup_title)).assertIsDisplayed()
    }
}
