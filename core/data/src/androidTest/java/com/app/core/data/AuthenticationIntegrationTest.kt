package com.app.core.data

import androidx.test.platform.app.InstrumentationRegistry
import com.app.core.model.*
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

class AuthenticationIntegrationTest {
    @Test fun registrationLoginResetAndLogoutUseRealEmulator() = runBlocking {
        assumeTrue(BuildConfig.FIREBASE_EMULATORS)
        val auth = FirebaseAuthRepository(FirebaseProvider(InstrumentationRegistry.getInstrumentation().targetContext))
        val email = "auth-${UUID.randomUUID()}@example.com"
        auth.signOut()
        withTimeout(30_000) {
            auth.register(email, "Testing123!", "QA User")
            assertEquals("QA User", (auth.session.value as Session.Authenticated).user.displayName)
            val userId = auth.requireUserId()
            val restored = FirebaseAuthRepository(FirebaseProvider(InstrumentationRegistry.getInstrumentation().targetContext))
            restored.session.first { it is Session.Authenticated }
            assertEquals(userId, restored.requireUserId())
            auth.signOut()
            auth.session.first { it is Session.SignedOut }
            try { auth.signIn(email, "Incorrect123!"); fail("Expected auth failure") }
            catch (error: AppException) { assertEquals(Failure.AUTHENTICATION, error.failure) }
            auth.signIn(email, "Testing123!")
            auth.session.first { it is Session.Authenticated }
            assertEquals(userId, auth.requireUserId())
            auth.resetPassword(email)
            auth.signOut()
            auth.session.first { it is Session.SignedOut }
        }
        Unit
    }
}
